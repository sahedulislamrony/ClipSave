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
import java.io.File

class ExtractFailure(message: String, val needsLogin: Boolean = false) : Exception(message)

/** Resolves a shared link into title/thumbnail/available qualities using yt-dlp. */
class Extractor(private val engine: Engine, private val cookies: CookieStore, private val cacheDir: File) {

    fun baseRequest(url: String, playlist: Boolean = false): YoutubeDLRequest = YoutubeDLRequest(url).apply {
        if (!playlist) addOption("--no-playlist")
        addOption("--no-warnings")
        addOption("--socket-timeout", "20")
        // youtubedl-android passes --no-cache-dir unless given one, so YouTube's player and signature
        // functions would be fetched and worked out again on every run.
        addOption("--cache-dir", cacheDir.absolutePath)
        cookies.mergedFileOrNull()?.let { addOption("--cookies", it.absolutePath) }
    }

    suspend fun fetch(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val info = if (Platform.detect(url) == Platform.YOUTUBE) {
            // Solving YouTube's player challenge in QuickJS is ~2/3 of a fetch (16 of 24 s on a phone), but
            // the picker only needs heights and sizes, which come without it. Downloads still solve it.
            // Fall back to the full run if skipping the player ever loses the video formats.
            val fast = try {
                ytDlp { YoutubeDL.getInstance().getInfo(baseRequest(url).addOption("--extractor-args", "youtube:player_skip=js")) }
            } catch (e: ExtractFailure) {
                if (e.needsLogin) throw e
                null
            }
            fast?.takeIf { f -> f.formats.orEmpty().any { (it.vcodec ?: "none") != "none" && it.height > 0 } }
                ?: ytDlp { YoutubeDL.getInstance().getInfo(baseRequest(url)) }
        } else {
            ytDlp { YoutubeDL.getInstance().getInfo(baseRequest(url)) }
        }
        Log.d("Extractor", "fetched in ${System.currentTimeMillis() - started} ms")

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

    /**
     * Lists one page of a playlist's videos, starting at the 1-based position [start], without resolving
     * each one (`--flat-playlist -J`). yt-dlp stops paging YouTube once it has the requested items, so a
     * page costs about the same whether the playlist has 100 videos or 3,000.
     */
    suspend fun fetchPlaylist(url: String, start: Int = 1): PlaylistInfo = withContext(Dispatchers.IO) {
        val end = start + PlaylistInfo.PAGE_SIZE - 1
        val request = baseRequest(url, playlist = true).apply {
            addOption("--flat-playlist")
            addOption("--playlist-items", "$start:$end")
            addOption("-J")
        }
        val out = ytDlp { YoutubeDL.getInstance().execute(request).out }
        val root = runCatching { Json.parseToJsonElement(out).jsonObject }.getOrNull()
            ?: throw ExtractFailure("Could not read this playlist")

        val raw = (root["entries"] as? JsonArray).orEmpty()
        val entries = raw.mapIndexedNotNull { i, el ->
            val e = el as? JsonObject ?: return@mapIndexedNotNull null
            val title = e.str("title") ?: return@mapIndexedNotNull null
            // Private/deleted videos stay listed in a playlist but can't be downloaded.
            if (unavailable.matches(title)) return@mapIndexedNotNull null
            val link = e.str("url")?.takeIf { it.startsWith("http") } ?: e.str("id")?.let { "https://www.youtube.com/watch?v=$it" }
                ?: return@mapIndexedNotNull null
            val duration = (e["duration"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 }
            PlaylistEntry(link, title, e.lastThumbnail(), index = start - 1 + i, durationSec = duration)
        }
        val total = (root["playlist_count"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()?.takeIf { it > 0 }
        // A short page is the last one; so is reaching the reported length.
        val nextStart = (end + 1).takeIf { raw.size >= PlaylistInfo.PAGE_SIZE && (total == null || it <= total) }
        if (entries.isEmpty() && nextStart == null && start == 1) throw ExtractFailure("This playlist has no videos you can download.")

        PlaylistInfo(
            url = url,
            title = root.str("title")?.takeIf { it.isNotBlank() } ?: "Playlist",
            uploader = root.str("uploader") ?: root.str("channel"),
            thumbnail = root.lastThumbnail() ?: entries.firstOrNull()?.thumbnail,
            platform = Platform.detect(url),
            entries = entries,
            videoUrl = UrlParser.youtubeVideoInPlaylist(url),
            total = total,
            nextStart = nextStart,
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
