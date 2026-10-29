package com.sahedul.clipsave.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

/** Copies a finished file into the public Movies/ or Music/ ClipSave folder (no storage permission needed on API 29+). */
object MediaStoreSaver {
    data class Saved(val uri: Uri, val mime: String, val displayName: String)

    private val audioExt = setOf("mp3", "m4a", "aac", "opus", "ogg", "wav", "flac", "weba")
    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif")

    fun mimeOf(file: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: when (file.extension.lowercase()) {
                "m4a" -> "audio/mp4"
                "opus" -> "audio/ogg"
                else -> "application/octet-stream"
            }

    /** Human-readable folder for the picker row, e.g. "Download/Clips"; the default when nothing is chosen. */
    fun folderLabel(tree: String?): String {
        if (tree == null) return DEFAULT_FOLDER
        val id = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(tree)) }.getOrNull() ?: return DEFAULT_FOLDER
        return id.substringAfter(':', "").ifEmpty { "Storage" }
    }

    /**
     * Saves into the user's chosen folder when there is one. If that folder is gone or its permission
     * was revoked the file still lands in the default folder rather than being lost.
     */
    fun save(context: Context, file: File, tree: String?, folder: String? = null): Saved {
        if (tree != null) runCatching { return saveToTree(context, file, Uri.parse(tree), folder) }
        return saveToMediaStore(context, file, folder)
    }

    private fun saveToTree(context: Context, file: File, tree: Uri, folder: String?): Saved {
        val resolver = context.contentResolver
        val mime = mimeOf(file)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val parent = folder?.let { childDir(context, tree, root, it) } ?: root
        val doc = DocumentsContract.createDocument(resolver, parent, mime, file.name) ?: error("Could not create file in the chosen folder")
        try {
            resolver.openOutputStream(doc)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
        } catch (e: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, doc) }
            throw e
        }
        return Saved(doc, mime, file.name)
    }

    /** Finds or creates the [name] directory in [parent]; createDocument alone would make "name (1)" when it exists. */
    private fun childDir(context: Context, tree: Uri, parent: Uri, name: String): Uri {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        resolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name && c.getString(2) == Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                }
            }
        }
        return DocumentsContract.createDocument(resolver, parent, Document.MIME_TYPE_DIR, name)
            ?: error("Could not create a folder in the chosen folder")
    }

    private fun saveToMediaStore(context: Context, file: File, folder: String?): Saved {
        val ext = file.extension.lowercase()
        val mime = mimeOf(file)
        val (collection, base) = when {
            ext in audioExt -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "${Environment.DIRECTORY_MUSIC}/ClipSave"
            ext in imageExt -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "${Environment.DIRECTORY_PICTURES}/ClipSave"
            else -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "${Environment.DIRECTORY_MOVIES}/ClipSave"
        }
        val relative = folder?.let { "$base/$it" } ?: base
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: error("Could not create file in shared storage")
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
        return Saved(uri, mime, file.name)
    }

    fun delete(context: Context, uri: String?) {
        if (uri == null) return
        val parsed = Uri.parse(uri)
        runCatching {
            if (DocumentsContract.isDocumentUri(context, parsed)) DocumentsContract.deleteDocument(context.contentResolver, parsed)
            else context.contentResolver.delete(parsed, null, null)
        }
    }

    const val DEFAULT_FOLDER = "Movies/ClipSave"
}
