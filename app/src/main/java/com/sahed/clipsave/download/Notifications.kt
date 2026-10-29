package com.sahed.clipsave.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.sahed.clipsave.R
import com.sahed.clipsave.ui.MainActivity
import java.util.UUID

object Notifications {
    const val CHANNEL_PROGRESS = "downloads"
    const val CHANNEL_DONE = "done"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_downloads), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun idFor(downloadId: String) = downloadId.hashCode()

    fun progress(context: Context, workId: UUID, title: String, percent: Int?, text: String?): Notification {
        val cancel = WorkManager.getInstance(context).createCancelPendingIntent(workId)
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent ?: 0, percent == null)
            .setContentIntent(openApp(context))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel)
            .build()
    }

    fun finished(context: Context, downloadId: String, title: String, uri: String?, mime: String?) {
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("Download complete")
            .setAutoCancel(true)
            .setContentIntent(if (uri != null) openFile(context, uri, mime) else openApp(context))
            .build()
        notify(context, idFor(downloadId), n)
    }

    fun failed(context: Context, downloadId: String, title: String, reason: String?) {
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n${reason.orEmpty()}"))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        notify(context, idFor(downloadId), n)
    }

    /** One summary per playlist instead of a notification per video. */
    fun batchFinished(context: Context, batchId: String, folder: String, saved: Int, failed: Int) {
        val text = buildString {
            append(if (saved == 1) "1 video saved" else "$saved videos saved")
            if (failed > 0) append(" · $failed failed")
        }
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(if (saved == 0) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download_done)
            .setContentTitle(folder)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        // Its own id: the playlist's progress id belongs to the foreground worker, which clears it on exit.
        notify(context, idFor("$batchId-done"), n)
    }

    fun update(context: Context, id: Int, n: Notification) = notify(context, id, n)

    fun cancel(context: Context, downloadId: String) =
        NotificationManagerCompat.from(context).cancel(idFor(downloadId))

    @Suppress("MissingPermission")
    private fun notify(context: Context, id: Int, n: Notification) {
        val mgr = NotificationManagerCompat.from(context)
        if (mgr.areNotificationsEnabled()) runCatching { mgr.notify(id, n) }
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openFile(context: Context, uri: String, mime: String?): PendingIntent = PendingIntent.getActivity(
        context, uri.hashCode(),
        viewIntent(uri, mime),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun viewIntent(uri: String, mime: String?): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), mime ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}
