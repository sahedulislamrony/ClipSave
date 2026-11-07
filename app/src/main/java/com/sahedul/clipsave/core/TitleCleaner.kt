package com.sahedul.clipsave.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns what a site reports as a video's title into a sensible file name. Facebook prefixes its titles with
 * counters ("1.2K views · 345 reactions | …"), and Instagram, TikTok and X use the whole caption, so those
 * are trimmed to their first few words; anything unusable gets a fallback: "Video by <uploader>", or the site's
 * name with the time when no uploader is known, so two such videos never share a name.
 */
object TitleCleaner {
    /** Roughly how much of a caption is kept, cut at a word boundary. */
    const val MAX_CHARS = 60

    /** These sites give a real title, so it is kept as it is. */
    private val realTitles = setOf(Platform.YOUTUBE, Platform.VIMEO, Platform.DAILYMOTION)

    private val counter = Regex(
        """^[\d.,]+\s?[KMBkmb]?\+?\s+(views?|reactions?|comments?|likes?|shares?|plays?|reposts?|retweets?|replies|followers?)$""",
        RegexOption.IGNORE_CASE,
    )
    private val instagramCaption = Regex("""^.+ on Instagram:\s*["“”]?(.*?)["“”]?$""", RegexOption.DOT_MATCHES_ALL)
    private val generic = Regex(
        """^(video|reel|photo|post|story|clip|watch|facebook|instagram|tiktok|twitter|x|(video|reel|photo|post|story|clip)s? by .*|[\d_\-\s]+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val url = Regex("""https?://\S+""")
    private val trailingTags = Regex("""(\s+[#@][\w.]+)+\s*$""")
    private val space = Regex("""\s+""")

    /** The file name to offer for [raw], never empty. */
    fun name(raw: String?, link: String, uploader: String? = null, nowMs: Long = System.currentTimeMillis()): String =
        clean(raw, Platform.detect(link)) ?: fallback(link, uploader, nowMs)

    /** [raw] without counters and cut to its first words, or null when nothing meaningful is left. */
    fun clean(raw: String?, platform: Platform): String? {
        var text = raw?.takeIf { it.isNotBlank() } ?: return null
        if (platform == Platform.INSTAGRAM) instagramCaption.matchEntire(text.trim())?.let { text = it.groupValues[1] }
        text = withoutCounters(text)
        text = url.replace(text, " ").replace(trailingTags, "").replace(space, " ").trim()
        if (text.isEmpty() || generic.matches(text)) return null
        return if (platform in realTitles) text else firstWords(text)
    }

    /** "Video by <uploader>" when the uploader is known, else e.g. "Instagram video 2026-11-07 19.52.03". */
    fun fallback(link: String, uploader: String? = null, nowMs: Long = System.currentTimeMillis()): String {
        uploader?.trim()?.takeIf { it.isNotEmpty() }?.let { return "Video by ${it.take(MAX_CHARS)}" }
        val platform = Platform.detect(link)
        val site = (if (platform == Platform.OTHER) hostOf(link) else platform.label)?.takeIf { it.isNotBlank() }
        val time = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US).format(Date(nowMs))
        return "${site ?: "Video"}${if (site != null) " video" else ""} $time"
    }

    /** Drops "12K views · 3 reactions" parts, whether they lead the title or sit in their own `|` section. */
    private fun withoutCounters(text: String): String {
        val sections = text.split('|').map { section ->
            section.split('·').map { it.trim() }.dropWhile { counter.matches(it) }.filterNot { counter.matches(it) }.joinToString(" · ")
        }.filter { it.isNotBlank() }
        // Facebook ends with the page's name; the first section is the caption.
        return sections.firstOrNull().orEmpty()
    }

    private fun firstWords(text: String): String {
        if (text.length <= MAX_CHARS) return text
        val cut = text.take(MAX_CHARS + 1)
        val atWord = cut.lastIndexOf(' ').takeIf { it >= MAX_CHARS / 2 } ?: MAX_CHARS
        return cut.take(atWord).trimEnd(' ', ',', '.', ';', ':', '-', '–', '—', '|', '·')
    }
}
