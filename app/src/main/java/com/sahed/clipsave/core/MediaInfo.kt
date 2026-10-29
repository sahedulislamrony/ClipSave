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

/**
 * A YouTube playlist, read with `--flat-playlist` (titles and links only, no per-video formats).
 * Read [PAGE_SIZE] videos at a time: long playlists (and endless Mixes) would otherwise take minutes.
 */
data class PlaylistInfo(
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val platform: Platform,
    /** The videos read so far, in playlist order. */
    val entries: List<PlaylistEntry>,
    /** Set when the link was a video opened inside the playlist, so the sheet can offer just that video. */
    val videoUrl: String?,
    /** The playlist's full length when YouTube reports it. */
    val total: Int? = null,
    /** 1-based position of the next unread page, or null when everything has been read. */
    val nextStart: Int? = null,
) {
    /** Size of the next page: a full one, or whatever is left. Null when unknown or nothing is left. */
    val nextPageSize: Int?
        get() = nextStart?.let { start -> total?.let { (it - start + 1).coerceIn(0, PAGE_SIZE) } ?: PAGE_SIZE }?.takeIf { it > 0 }

    /** What file numbers are padded against: the whole playlist, so numbers match across batches. */
    val numberingTotal: Int get() = maxOf(total ?: 0, (entries.maxOfOrNull { it.index } ?: -1) + 1)

    /** Adds the next page's videos. */
    fun plus(page: PlaylistInfo) = copy(
        entries = entries + page.entries.filter { e -> entries.none { it.index == e.index } },
        total = page.total ?: total,
        nextStart = page.nextStart,
    )

    companion object {
        const val PAGE_SIZE = 100
        /** Most videos one playlist download can queue. */
        const val MAX_SELECTED = 100
    }
}

/** [index] is the 0-based position in the playlist; it numbers the file. */
data class PlaylistEntry(val url: String, val title: String, val thumbnail: String?, val index: Int, val durationSec: Long? = null)

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

        /**
         * Typical bits per second (video + audio) for a row, for estimating playlist sizes before any
         * video is resolved. Based on YouTube's H.264 + AAC streams, which the downloader prefers.
         */
        fun bitrate(option: FormatOption): Long = when {
            option.audioFormat == KEY_MP3 -> 245_000 // --audio-quality 0 ≈ LAME V0
            option.isAudio -> 130_000
            else -> when (option.height ?: 0) {
                0 -> 4_000_000 // "Best": usually 1080p or more
                in 1081..Int.MAX_VALUE -> 6_000_000
                in 721..1080 -> 3_000_000
                in 481..720 -> 1_700_000
                in 361..480 -> 900_000
                else -> 600_000
            }
        }

        /** Approximate bytes for [entries] at [option]; videos with no known duration count as zero. */
        fun estimateBytes(entries: Collection<PlaylistEntry>, option: FormatOption): Long =
            entries.sumOf { (it.durationSec ?: 0L) * bitrate(option) / 8 }

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
