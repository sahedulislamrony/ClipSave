package com.sahed.clipsave.core

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class ExtractFailure(message: String, val needsLogin: Boolean = false) : Exception(message)

/** Resolves a shared link into title/thumbnail/available qualities using yt-dlp. */
class Extractor(private val engine: Engine, private val cookies: CookieStore) {

    fun baseRequest(url: String, playlist: Boolean = false): YoutubeDLRequest = YoutubeDLRequest(url).apply {
        if (!playlist) addOption("--no-playlist")
        addOption("--no-warnings")
        addOption("--socket-timeout", "20")
        cookies.mergedFileOrNull()?.let { addOption("--cookies", it.absolutePath) }
    }

    suspend fun fetch(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val info = ytDlp { YoutubeDL.getInstance().getInfo(baseRequest(url)) }

        val formats = info.formats.orEmpty()
        val videoFormats = formats.filter { (it.vcodec ?: "none") != "none" && it.height > 0 }
        val audioFormats = formats.filter { (it.vcodec ?: "none") == "none" && (it.acodec ?: "none") != "none" }
        val bestAudioSize = audioFormats.maxOfOrNull { size(it.fileSize, it.fileSizeApproximate) ?: 0L }?.takeIf { it > 0 }

        val byHeight = videoFormats.groupBy { it.height }.toSortedMap(compareByDescending { it })
        val options = buildList {
            byHeight.forEach { (h, fs) ->
                // Muxed formats already contain audio; separate streams need the audio size added.
                val best = fs.maxByOrNull { size(it.fileSize, it.fileSizeApproximate) ?: 0L }
                val videoSize = size(best?.fileSize, best?.fileSizeApproximate)
                val hasAudio = (best?.acodec ?: "none") != "none"
                val total = videoSize?.let { it + if (hasAudio) 0 else (bestAudioSize ?: 0) }
                add(FormatOption.video(h, total))
            }
            if (isEmpty()) add(FormatOption.video(null, null))
            add(FormatOption.audio("mp3", bestAudioSize))
            add(FormatOption.audio("m4a", bestAudioSize))
        }

        MediaInfo(
            url = url,
            title = info.title?.takeIf { it.isNotBlank() } ?: hostOf(url) ?: "Video",
            uploader = info.uploader,
            thumbnail = info.thumbnail,
            durationSec = info.duration.toLong().takeIf { it > 0 },
            platform = Platform.detect(url),
            options = options,
        )
    }

    /** Lists a playlist's videos without resolving each one (`--flat-playlist -J`), so even long playlists open fast. */
    suspend fun fetchPlaylist(url: String): PlaylistInfo = withContext(Dispatchers.IO) {
        val request = baseRequest(url, playlist = true).apply {
            addOption("--flat-playlist")
            addOption("-J")
        }
        val out = ytDlp { YoutubeDL.getInstance().execute(request).out }
        val root = runCatching { Json.parseToJsonElement(out).jsonObject }.getOrNull()
            ?: throw ExtractFailure("Could not read this playlist")

        val entries = (root["entries"] as? JsonArray).orEmpty().mapNotNull { el ->
            val e = el as? JsonObject ?: return@mapNotNull null
            val title = e.str("title") ?: return@mapNotNull null
            // Private/deleted videos stay listed in a playlist but can't be downloaded.
            if (unavailable.matches(title)) return@mapNotNull null
            val link = e.str("url")?.takeIf { it.startsWith("http") } ?: e.str("id")?.let { "https://www.youtube.com/watch?v=$it" }
                ?: return@mapNotNull null
            PlaylistEntry(link, title, e.lastThumbnail())
        }
        if (entries.isEmpty()) throw ExtractFailure("This playlist has no videos you can download.")

        PlaylistInfo(
            url = url,
            title = root.str("title")?.takeIf { it.isNotBlank() } ?: "Playlist",
            uploader = root.str("uploader") ?: root.str("channel"),
            thumbnail = root.lastThumbnail() ?: entries.first().thumbnail,
            platform = Platform.detect(url),
            entries = entries,
            videoUrl = UrlParser.youtubeVideoInPlaylist(url),
        )
    }

    private suspend fun <T> ytDlp(block: () -> T): T {
        try {
            engine.awaitReady()
        } catch (e: Throwable) {
            throw ExtractFailure("Downloader engine failed to start: ${e.message}")
        }
        return try {
            block()
        } catch (e: YoutubeDLException) {
            throw friendly(e)
        } catch (e: Exception) {
            Log.w("Extractor", "extract failed", e)
            throw ExtractFailure(e.message ?: "Could not read this link")
        }
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /** yt-dlp lists thumbnails smallest first. */
    private fun JsonObject.lastThumbnail(): String? =
        (this["thumbnails"] as? JsonArray)?.lastOrNull()?.let { (it as? JsonObject)?.str("url") }

    private val unavailable = Regex("""\[(Private|Deleted|Unavailable) video]""", RegexOption.IGNORE_CASE)

    private fun size(exact: Long?, approx: Long?): Long? =
        (exact?.takeIf { it > 0 } ?: approx?.takeIf { it > 0 })

    private fun friendly(e: YoutubeDLException): ExtractFailure {
        val raw = e.message.orEmpty()
        Log.w("Extractor", "yt-dlp failed: $raw")
        val lower = raw.lowercase()
        return when {
            "login" in lower || "cookies" in lower || "rate-limit" in lower || "not available" in lower && "private" in lower ->
                ExtractFailure("This post needs you to be signed in. Open Settings → sign in to Instagram/Facebook, then try again.", needsLogin = true)
            "unsupported url" in lower ->
                ExtractFailure("This link isn't supported.")
            "http error 404" in lower || "http error 410" in lower ->
                ExtractFailure("This video was removed or the link is wrong.")
            "http error 403" in lower || "http error 401" in lower ->
                ExtractFailure("Access denied. The video may be private or region-locked.")
            "name or service not known" in lower || "network is unreachable" in lower || "timed out" in lower ||
                "temporary failure in name resolution" in lower || "connection refused" in lower ->
                ExtractFailure("No connection. Check your internet and try again.")
            else -> ExtractFailure(
                raw.lineSequence().lastOrNull { it.isNotBlank() }?.removePrefix("ERROR: ")?.take(200)
                    ?: "Could not read this link",
            )
        }
    }
}
