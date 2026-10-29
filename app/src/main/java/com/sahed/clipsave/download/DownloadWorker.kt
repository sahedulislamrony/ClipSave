package com.sahed.clipsave.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.sahed.clipsave.ClipSaveApp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs yt-dlp downloads as a foreground (data-sync) task, so they survive leaving the app —
 * the share sheet closes right after the user picks a quality and the transfer continues here.
 * One work request is either a single download ([KEY_ID]) or a whole playlist ([KEY_BATCH]),
 * whose videos download one after another.
 */
class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val container get() = (applicationContext as ClipSaveApp).container

    private sealed interface Outcome {
        class Saved(val uri: String, val mime: String) : Outcome
        class Failed(val reason: String?) : Outcome
        /** This one item was paused or canceled by the user while its process ran. */
        data object Stopped : Outcome
    }

    override suspend fun doWork(): Result {
        inputData.getString(KEY_BATCH)?.let { return runBatch(it) }
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val item = container.downloads.get(id) ?: return Result.failure()
        return when (val outcome = downloadOne(item, Notifications.idFor(id), item.title)) {
            is Outcome.Saved -> {
                Notifications.finished(applicationContext, id, item.title, outcome.uri, outcome.mime)
                Result.success()
            }
            is Outcome.Failed -> {
                Notifications.failed(applicationContext, id, item.title, outcome.reason)
                Result.failure()
            }
            Outcome.Stopped -> Result.success()
        }
    }

    private suspend fun runBatch(batchId: String): Result {
        val repo = container.downloads
        // After a process death the repository marks every waiting video "Interrupted"; WorkManager reran
        // this batch, so they are queued again (partial files are still in their work dirs).
        repo.updateWhere({ it.batchId == batchId && it.status == DownloadStatus.FAILED && it.error == DownloadRepository.INTERRUPTED }) {
            it.copy(status = DownloadStatus.QUEUED, error = null)
        }
        val ids = repo.items.value.filter { it.batchId == batchId }.sortedBy { it.createdAt }.map { it.id }
        var saved = 0
        var failed = 0
        var folder = ""
        var current: String? = null
        try {
            ids.forEachIndexed { i, id ->
                val item = repo.get(id)?.takeIf { it.status == DownloadStatus.QUEUED } ?: return@forEachIndexed
                current = id
                folder = item.folder.orEmpty()
                when (downloadOne(item, Notifications.idFor(batchId), "$folder · ${i + 1}/${ids.size}")) {
                    is Outcome.Saved -> saved++
                    is Outcome.Failed -> failed++
                    Outcome.Stopped -> Unit
                }
            }
        } catch (e: CancellationException) {
            // getStopReason() needs API 31. Earlier versions can't tell, so a stop counts as the user's Cancel.
            val cancelledByApp = if (Build.VERSION.SDK_INT >= 31) stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP else true
            if (cancelledByApp) {
                // The notification's Cancel stops the whole playlist. (A pause already marked items PAUSED.)
                ids.forEach { id -> repo.update(id) { if (it.status == DownloadStatus.QUEUED) it.copy(status = DownloadStatus.CANCELED) else it } }
            } else {
                // The system stopped us (constraints, quota); WorkManager reruns the batch, so requeue this video.
                current?.let { id -> repo.update(id) { if (it.status == DownloadStatus.CANCELED) it.copy(status = DownloadStatus.QUEUED) else it } }
            }
            throw e
        }
        if (!isStopped && saved + failed > 0) Notifications.batchFinished(applicationContext, batchId, folder, saved, failed)
        return Result.success()
    }

    private suspend fun downloadOne(item: DownloadItem, notificationId: Int, label: String): Outcome {
        val repo = container.downloads
        val id = item.id
        // Progress is kept so a resumed download doesn't jump back to 0 before yt-dlp reports.
        repo.update(id) { it.copy(status = DownloadStatus.RUNNING, error = null) }
        setForeground(foreground(notificationId, label, null, "Preparing…"))

        // Not wiped on start: after a pause yt-dlp continues its .part files from here.
        val workDir = workDir(applicationContext, id).apply { mkdirs() }
        var keepPartial = false

        try {
            container.engine.awaitReady()
            runYtDlp(item, workDir, notificationId, label)

            repo.update(id) { it.copy(status = DownloadStatus.PROCESSING, progress = 1f, etaSec = null) }
            val files = workDir.listFiles { f -> f.isFile && f.extension.lowercase() !in JUNK_EXT }.orEmpty()
            if (files.isEmpty()) throw IllegalStateException("yt-dlp finished but produced no file")

            // Carousels can yield several files; record the first, save all.
            val saved = files.sortedBy { it.name }.map { MediaStoreSaver.save(applicationContext, it, container.prefs.downloadTree, item.folder) }
            repo.update(id) {
                it.copy(status = DownloadStatus.DONE, progress = 1f, outputUri = saved.first().uri.toString(), mimeType = saved.first().mime)
            }
            return Outcome.Saved(saved.first().uri.toString(), saved.first().mime)
        } catch (e: CancellationException) {
            keepPartial = repo.get(id)?.status == DownloadStatus.PAUSED
            if (!keepPartial) {
                repo.update(id) { it.copy(status = DownloadStatus.CANCELED, etaSec = null) }
                Notifications.cancel(applicationContext, id)
            }
            throw e
        } catch (e: Throwable) {
            // Pausing/canceling one playlist video kills its yt-dlp process; that isn't a failure.
            val now = repo.get(id)?.status
            if (now == DownloadStatus.PAUSED || now == DownloadStatus.CANCELED) {
                keepPartial = now == DownloadStatus.PAUSED
                return Outcome.Stopped
            }
            val reason = (e as? YoutubeDLException)?.message?.lineSequence()?.lastOrNull { it.isNotBlank() }?.removePrefix("ERROR: ")
                ?: e.message
            repo.update(id) { it.copy(status = DownloadStatus.FAILED, error = reason?.take(300), etaSec = null) }
            return Outcome.Failed(reason)
        } finally {
            if (!keepPartial) workDir.deleteRecursively()
        }
    }

    private suspend fun runYtDlp(item: DownloadItem, workDir: File, notificationId: Int, label: String) = coroutineScope {
        val request = container.extractor.baseRequest(item.url).apply {
            addOption("-f", item.selector)
            addOption("-S", "res,vcodec:h264,acodec:m4a")
            addOption("--concurrent-fragments", "4")
            addOption("--no-mtime")
            addOption("--embed-metadata")
            addOption("--windows-filenames")
            // An edited name replaces the title; the playlist index keeps carousel items from overwriting each other.
            val template = item.customName?.let { it.replace("%", "%%") + "%(playlist_index& {}|)s.%(ext)s" }
                ?: "%(title).100B [%(id)s].%(ext)s"
            addOption("-o", "${workDir.absolutePath}/$template")
            if (item.audioFormat != null) {
                addOption("-x")
                addOption("--audio-format", item.audioFormat)
                addOption("--audio-quality", "0")
            } else {
                addOption("--merge-output-format", "mp4")
            }
        }

        val done = AtomicBoolean(false)
        // yt-dlp runs in a child process that a plain coroutine cancel would not stop.
        val killer = launch {
            try { awaitCancellation() } finally { if (!done.get()) YoutubeDL.getInstance().destroyProcessById(item.id) }
        }
        var lastUi = 0L
        try {
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().execute(request, item.id) { progress, etaSec, _ ->
                    val now = System.currentTimeMillis()
                    if (now - lastUi < 400) return@execute
                    lastUi = now
                    val fraction = (progress / 100f).takeIf { it in 0f..1f }
                    container.downloads.update(item.id) { it.copy(progress = fraction ?: it.progress, etaSec = etaSec.takeIf { e -> e > 0 }) }
                    Notifications.update(
                        applicationContext, notificationId,
                        Notifications.progress(
                            applicationContext, label,
                            fraction?.let { (it * 100).toInt() },
                            etaSec.takeIf { it > 0 }?.let { "${it}s left" },
                        ),
                    )
                }
            }
        } finally {
            done.set(true)
            killer.cancel()
        }
    }

    private fun foreground(notificationId: Int, title: String, percent: Int?, text: String?) = ForegroundInfo(
        notificationId,
        Notifications.progress(applicationContext, title, percent, text),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_BATCH = "batch_id"
        private val JUNK_EXT = setOf("part", "ytdl", "temp", "tmp", "json", "vtt", "srt")

        /** Scratch dir for one download; survives a pause so the partial file can be resumed. */
        fun workDir(context: Context, id: String) = File(context.getExternalFilesDir(null) ?: context.cacheDir, "tmp/$id")
    }
}
