package com.sahedul.clipsave.core

/**
 * Reads a [LinkPreview] out of the embed pages Instagram and Facebook serve so other sites can show a post.
 * Unlike the post pages these need no sign-in, and the thumbnail comes in the first ~30 KB. It goes by the
 * markup, so if a site changes it the sheet just shows the spinner until yt-dlp is done, as before.
 */
object EmbedPage {
    /** Enough of an embed page to reach the caption; the rest is scripts. */
    const val READ_CHARS = 96_000

    /** `instagram.com/p/<code>/embed/captioned/`: the post image, the username and the caption's first line. */
    fun instagram(html: String): LinkPreview? {
        val image = Regex("""<img[^>]+class="EmbeddedMediaImage"[^>]*\ssrc="([^"]+)"""").find(html)?.groupValues?.get(1)
        val user = Regex("""class="UsernameText">([^<]+)<""").find(html)?.groupValues?.get(1)
        val caption = Regex("""<div class="Caption">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
            ?.replace(Regex("""<a class="CaptionUsername".*?</a>""", RegexOption.DOT_MATCHES_ALL), "")
            ?.replace(Regex("""<br\s*/?>"""), "\n")
            ?.replace(Regex("<[^>]+>"), "")
            ?.let(::unescape)
            ?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
            ?.take(150)
        return preview(caption, user, image)
    }

    /**
     * `facebook.com/plugins/video.php?href=…`: the first image is the video's frame and the owner's avatar is
     * labelled with their name. There is no title.
     */
    fun facebook(html: String): LinkPreview? {
        val image = Regex("""<img[^>]+\ssrc="([^"]+)"""").find(html)?.groupValues?.get(1)
        val owner = Regex("""<img[^>]+aria-label="([^"]+)"[^>]*\srole="img"""").find(html)?.groupValues?.get(1)
        return preview(null, owner, image)
    }

    private fun preview(title: String?, uploader: String?, thumbnail: String?): LinkPreview? =
        LinkPreview(
            title?.takeIf { it.isNotBlank() },
            uploader?.let(::unescape)?.takeIf { it.isNotBlank() },
            thumbnail?.let(::unescape)?.takeIf { it.startsWith("https://") },
        ).takeIf { it.title != null || it.thumbnail != null }

    private val entity = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|quot|apos|lt|gt);")
    private val named = mapOf("amp" to "&", "quot" to "\"", "apos" to "'", "lt" to "<", "gt" to ">")

    /** Signed CDN links break unless `&amp;` goes back to `&`; captions use numeric entities (`&#064;`). */
    fun unescape(s: String): String = entity.replace(s) { m ->
        val e = m.groupValues[1]
        val code = when {
            e.startsWith("#x") -> e.drop(2).toIntOrNull(16)
            e.startsWith("#") -> e.drop(1).toIntOrNull()
            else -> return@replace named.getValue(e)
        }
        code?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) } ?: m.value
    }
}
