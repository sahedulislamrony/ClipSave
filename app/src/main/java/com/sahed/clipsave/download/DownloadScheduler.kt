package com.sahed.clipsave.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sahed.clipsave.core.FormatOption
import com.sahed.clipsave.core.MediaInfo
import com.sahed.clipsave.core.Numbering
import com.sahed.clipsave.core.PlaylistInfo
import com.sahed.clipsave.core.Prefs
import com.yausername.youtubedl_android.YoutubeDL
import java.util.UUID

class DownloadScheduler(
    private val context: Context,
    private val repo: DownloadRepository,
    private val prefs: Prefs,
) {
    fun enqueue(info: MediaInfo, option: FormatOption, fileName: String? = null): String {
        val id = UUID.randomUUID().toString()
        val name = fileName?.let(::sanitizeFileName)?.takeIf { it.isNotEmpty() && it != sanitizeFileName(info.title) }
        repo.add(
            DownloadItem(
                id = id,
                url = info.url,
                title = name ?: info.title,
                thumbnail = info.thumbnail,
                platform = info.platform.label,
                optionKey = option.key,
                optionLabel = option.label,
                selector = option.selector,
                audioFormat = option.audioFormat,
                createdAt = System.currentTimeMillis(),
                customName = name,
            ),
        )
        start(id)
        return id
    }

    /**
     * Queues every video of [playlist] into `<download folder>/<folder>/`, numbered per [numbering].
     * They download one at a time in a single worker; returns how many were queued.
     */
    fun enqueuePlaylist(playlist: PlaylistInfo, option: FormatOption, folder: String, numbering: Numbering): Int {
        val batchId = UUID.randomUUID().toString()
        val dir = sanitizeFileName(folder).ifEmpty { sanitizeFileName(playlist.title) }.ifEmpty { "Playlist" }
        val total = playlist.entries.size
        val now = System.currentTimeMillis()
        val items = playlist.entries.mapIndexed { i, entry ->
            val name = sanitizeFileName(numbering.fileName(entry.title, i, total))
            DownloadItem(
                id = UUID.randomUUID().toString(),
                url = entry.url,
                title = name.ifEmpty { entry.title },
                thumbnail = entry.thumbnail ?: playlist.thumbnail,
                platform = playlist.platform.label,
                optionKey = option.key,
                optionLabel = option.label,
                selector = option.selector,
                audioFormat = option.audioFormat,
                // Distinct timestamps keep playlist order in the queue, which sorts by creation time.
                createdAt = now + i,
                customName = name.ifEmpty { null },
                folder = dir,
                batchId = batchId,
            )
        }
        repo.addAll(items)
        startBatch(batchId)
        return items.size
    }

    fun retry(id: String) {
        repo.update(id) { it.copy(status = DownloadStatus.QUEUED, progress = 0f, error = null) }
        startOne(id)
    }

    /** Pauses one queued or running download, keeping its partial file. Saving can't be paused. */
    fun pause(id: String) {
        val item = repo.get(id)?.takeIf { it.isPausable } ?: return
        // Mark first: the worker reads the status when stopped to tell a pause from a failure.
        repo.update(id) { if (it.isPausable) it.copy(status = DownloadStatus.PAUSED, etaSec = null) else it }
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        // A playlist shares one worker: stop only this video's process; the worker moves on to the next.
        if (item.batchId != null && item.status == DownloadStatus.RUNNING) YoutubeDL.getInstance().destroyProcessById(id)
    }

    /** Continues a paused download from its partial file. */
    fun resume(id: String) {
        repo.update(id) { it.copy(status = DownloadStatus.QUEUED, error = null) }
        startOne(id)
    }

    /** Playlist videos go back through their playlist's worker so they still download one at a time. */
    private fun startOne(id: String) {
        val batchId = repo.get(id)?.batchId
        if (batchId != null) startBatch(batchId) else start(id)
    }

    /**
     * Stops queued or running downloads (all, or those matching [which]), keeping partial files;
     * ones already saving are left to finish.
     */
    fun pauseAll(which: (DownloadItem) -> Boolean = { true }) {
        val targets = repo.items.value.filter { it.isPausable && which(it) }
        // Mark first: the worker reads the status when it is stopped to tell a pause from a cancel.
        val ids = targets.map { it.id }.toSet()
        repo.updateWhere({ it.id in ids && it.isPausable }) { it.copy(status = DownloadStatus.PAUSED, etaSec = null) }
        val wm = WorkManager.getInstance(context)
        targets.forEach { item ->
            wm.cancelUniqueWork(workName(item.id))
            item.batchId?.let { wm.cancelUniqueWork(workName(it)) }
        }
    }

    fun resumeAll(which: (DownloadItem) -> Boolean = { true }) {
        val paused = repo.items.value.filter { it.status == DownloadStatus.PAUSED && which(it) }
        val ids = paused.map { it.id }.toSet()
        repo.updateWhere({ it.id in ids }) { it.copy(status = DownloadStatus.QUEUED, error = null) }
        paused.filter { it.batchId == null }.forEach { start(it.id) }
        paused.mapNotNull { it.batchId }.distinct().forEach(::startBatch)
    }

    fun cancel(id: String) {
        val item = repo.get(id) ?: return
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        when {
            // A queued or paused job never reaches the worker's catch block, so mark it here too.
            item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.PAUSED -> {
                repo.update(id) { it.copy(status = DownloadStatus.CANCELED, etaSec = null) }
                DownloadWorker.workDir(context, id).deleteRecursively()
            }
            // A playlist shares one worker: stop only this video's process and let the rest carry on.
            item.batchId != null && item.status == DownloadStatus.RUNNING -> {
                repo.update(id) { it.copy(status = DownloadStatus.CANCELED, etaSec = null) }
                YoutubeDL.getInstance().destroyProcessById(id)
            }
        }
        Notifications.cancel(context, id)
    }

    private fun start(id: String) = enqueueWork(workName(id), workDataOf(DownloadWorker.KEY_ID to id))

    /**
     * If the playlist's worker is still running, a new pass is appended behind it (it picks up whatever is
     * queued by then) rather than replacing it, which would cancel the video in progress.
     */
    private fun startBatch(batchId: String) =
        enqueueWork(workName(batchId), workDataOf(DownloadWorker.KEY_BATCH to batchId), ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueueWork(name: String, input: Data, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build(),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, policy, request)
    }

    private fun workName(id: String) = "download-$id"
}

/** Strips characters Android/FAT filesystems reject and caps the length. */
internal fun sanitizeFileName(raw: String): String =
    raw.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "").trim().trim('.').take(100).trim()
