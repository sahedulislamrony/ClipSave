package com.sahedul.clipsave.core

import java.net.URI

enum class Platform(val label: String, private val hosts: List<String>) {
    YOUTUBE("YouTube", listOf("youtube.com", "youtu.be", "youtube-nocookie.com")),
    INSTAGRAM("Instagram", listOf("instagram.com", "instagr.am")),
    FACEBOOK("Facebook", listOf("facebook.com", "fb.watch", "fb.com", "fb.me")),
    TIKTOK("TikTok", listOf("tiktok.com")),
    TWITTER("X / Twitter", listOf("twitter.com", "x.com", "t.co")),
    VIMEO("Vimeo", listOf("vimeo.com")),
    REDDIT("Reddit", listOf("reddit.com", "redd.it")),
    PINTEREST("Pinterest", listOf("pinterest.com", "pin.it")),
    DAILYMOTION("Dailymotion", listOf("dailymotion.com", "dai.ly")),
    OTHER("Web", emptyList());

    fun matches(host: String): Boolean = hosts.any { host == it || host.endsWith(".$it") }

    companion object {
        fun detect(url: String): Platform {
            val host = hostOf(url) ?: return OTHER
            return entries.firstOrNull { it != OTHER && it.matches(host) } ?: OTHER
        }
    }
}

object UrlParser {
    private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    // Characters that commonly trail a link when it is pasted inside a sentence.
    private const val TRAILING = ".,;:!?)]}”’"

    /** Share text is usually "Look at this https://… via @someone"; pull out the first link. */
    fun firstUrl(text: CharSequence?): String? {
        if (text.isNullOrBlank()) return null
        val match = urlRegex.find(text) ?: return null
        return match.value.trimEnd { it in TRAILING }.takeIf { it.length > 10 }
    }

    /** A YouTube link that names a playlist (`list=`): the playlist page, or a video opened inside one. */
    fun isYoutubePlaylist(url: String): Boolean =
        Platform.detect(url) == Platform.YOUTUBE && queryParam(url, "list") != null

    /** For `watch?v=…&list=…` (or `youtu.be/ID?list=…`), the video on its own; null for a bare playlist link. */
    fun youtubeVideoInPlaylist(url: String): String? {
        val id = queryParam(url, "v")
            ?: runCatching { URI(url).path?.trim('/') }.getOrNull()?.takeIf { hostOf(url) == "youtu.be" && it.isNotEmpty() }
        return id?.let { "https://www.youtube.com/watch?v=$it" }
    }

    /** The 11-character id in `watch?v=…`, `youtu.be/…`, `/shorts/…`, `/live/…` or `/embed/…` links; null otherwise. */
    fun youtubeVideoId(url: String): String? {
        if (Platform.detect(url) != Platform.YOUTUBE) return null
        val path = runCatching { URI(url).path }.getOrNull().orEmpty().trim('/').split('/')
        val id = queryParam(url, "v") ?: when {
            hostOf(url) == "youtu.be" -> path.firstOrNull()
            path.size >= 2 && path[0] in setOf("shorts", "live", "embed", "v") -> path[1]
            else -> null
        }
        return id?.takeIf { videoId.matches(it) }
    }

    private val videoId = Regex("[A-Za-z0-9_-]{11}")

    /**
     * The post code in `instagram.com/p/…`, `/reel/…`, `/reels/…` or `/tv/…` (also under `/<user>/`). Null for
     * stories, profiles and `/share/…` links, whose code isn't the post's.
     */
    fun instagramShortcode(url: String): String? {
        if (Platform.detect(url) != Platform.INSTAGRAM) return null
        val path = runCatching { URI(url).path }.getOrNull().orEmpty().trim('/').split('/')
        if (path.firstOrNull() == "share") return null
        val i = path.indexOfFirst { it in setOf("p", "reel", "reels", "tv") }
        return path.getOrNull(i + 1)?.takeIf { i >= 0 && shortcode.matches(it) }
    }

    private val shortcode = Regex("[A-Za-z0-9_-]{5,}")

    private fun queryParam(url: String, name: String): String? =
        runCatching { URI(url).rawQuery }.getOrNull()?.split('&')?.firstNotNullOfOrNull { pair ->
            pair.split('=', limit = 2).takeIf { it.size == 2 && it[0] == name && it[1].isNotBlank() }?.get(1)
        }
}

fun hostOf(url: String): String? = runCatching {
    URI(url).host?.lowercase()?.removePrefix("www.")?.removePrefix("m.")
}.getOrNull()
