package com.sahed.clipsave.core

/** What the format picker shows. Built from yt-dlp's `--dump-json` output. */
data class MediaInfo(
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val durationSec: Long?,
    val platform: Platform,
    val options: List<FormatOption>,
)

/** A YouTube playlist, read with `--flat-playlist` (titles and links only, no per-video formats). */
data class PlaylistInfo(
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val platform: Platform,
    val entries: List<PlaylistEntry>,
    /** Set when the link was a video opened inside the playlist, so the sheet can offer just that video. */
    val videoUrl: String?,
)

data class PlaylistEntry(val url: String, val title: String, val thumbnail: String?)

/** How playlist files are numbered at the front of their names. */
enum class Numbering(val key: String) {
    ASCENDING("asc"), DESCENDING("desc"), NONE("none");

    /**
     * "07 - Title". [index] is the 0-based playlist position; the number is zero-padded to the
     * width of [total] so file managers that sort by name keep playlist order.
     */
    fun fileName(title: String, index: Int, total: Int): String {
        val n = when (this) {
            ASCENDING -> index + 1
            DESCENDING -> total - index
            NONE -> return title
        }
        return "${n.toString().padStart(total.toString().length, '0')} - $title"
    }

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: ASCENDING
    }
}

/**
 * One row in the format picker.
 *
 * [selector] is a yt-dlp `-f` expression; [audioFormat] is non-null for audio-only rows
 * (extracted with ffmpeg to mp3/m4a).
 */
data class FormatOption(
    val key: String,
    val label: String,
    val sizeBytes: Long?,
    val selector: String,
    val audioFormat: String? = null,
    val height: Int? = null,
) {
    val isAudio get() = audioFormat != null

    companion object {
        const val KEY_BEST = "best"
        const val KEY_MP3 = "mp3"
        const val KEY_M4A = "m4a"

        fun video(height: Int?, sizeBytes: Long?): FormatOption =
            if (height == null) {
                FormatOption(KEY_BEST, "Best quality", sizeBytes, "bv*+ba/b")
            } else {
                FormatOption(
                    key = height.toString(),
                    label = "${height}p",
                    sizeBytes = sizeBytes,
                    selector = "bv*[height<=$height]+ba/b[height<=$height]/b",
                    height = height,
                )
            }

        fun audio(codec: String, sizeBytes: Long?): FormatOption = FormatOption(
            key = codec,
            label = if (codec == "mp3") "MP3 audio" else "M4A audio",
            sizeBytes = sizeBytes,
            selector = "ba/b",
            audioFormat = codec,
        )

        /** A playlist has no per-video formats up front, so it offers fixed caps; each video gets the closest it has. */
        fun playlist(): List<FormatOption> =
            listOf(video(null, null), video(1080, null), video(720, null), video(480, null), audio("mp3", null), audio("m4a", null))

        /** Choose the row that matches a saved default ("1080", "mp3", "best"), else the closest lower quality. */
        fun pick(options: List<FormatOption>, preferred: String): FormatOption? {
            options.firstOrNull { it.key == preferred }?.let { return it }
            val want = preferred.toIntOrNull()
            val videos = options.filter { !it.isAudio }
            if (want != null) {
                videos.filter { (it.height ?: 0) <= want }.maxByOrNull { it.height ?: 0 }?.let { return it }
            }
            return videos.firstOrNull()
        }
    }
}

fun formatSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val mb = bytes / 1_048_576.0
    return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
}

fun formatDuration(sec: Long?): String? {
    if (sec == null || sec <= 0) return null
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
