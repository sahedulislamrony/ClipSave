package com.sahedul.clipsave.download

import kotlinx.serialization.Serializable

/** [PAUSED] keeps yt-dlp's partial files so a resume continues where it stopped. */
enum class DownloadStatus { QUEUED, RUNNING, PROCESSING, PAUSED, DONE, FAILED, CANCELED }

@Serializable
data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val platform: String,
    val optionKey: String,
    val optionLabel: String,
    val selector: String,
    val audioFormat: String?,
    val createdAt: Long,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val etaSec: Long? = null,
    val outputUri: String? = null,
    val mimeType: String? = null,
    val error: String? = null,
    /** User-edited file name (no extension). Null keeps yt-dlp's `title [id]` naming. */
    val customName: String? = null,
    /** Sub-folder inside the download folder; a playlist's items all share its name. */
    val folder: String? = null,
    /** Playlist items share one id and download one after another in a single worker. */
    val batchId: String? = null,
) {
    val isActive get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING || status == DownloadStatus.PROCESSING

    /** Queued or running: what "Pause all" stops. Saving (PROCESSING) is left to finish. */
    val isPausable get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING
}
