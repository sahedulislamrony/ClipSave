package com.sahedul.clipsave.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sahedul.clipsave.R
import com.sahedul.clipsave.ui.MainActivity

object Notifications {
    /** Was "downloads" at IMPORTANCE_LOW. A channel's importance can't be raised after creation, so it has a new id. */
    const val CHANNEL_PROGRESS = "downloads_active"
    private const val OLD_CHANNEL_PROGRESS = "downloads"
    const val CHANNEL_DONE = "done"

    /** Brand iris: Android tints the white logo's backdrop with it where it draws one. */
    private const val BRAND = 0xFF5B3BF0.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL_PROGRESS)
        // DEFAULT, not LOW: Android files low-importance notifications under "Silent" and hides their
        // status-bar icon, so a running download looked like nothing was happening. Still no sound.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_downloads), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun idFor(downloadId: String) = downloadId.hashCode()

    /** Ongoing progress; tapping opens the Downloads tab, where pausing and canceling live. */
    fun progress(context: Context, title: String, percent: Int?, text: String?): Notification =
        builder(context, CHANNEL_PROGRESS)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(false)
            .setProgress(100, percent ?: 0, percent == null)
            .setContentIntent(openDownloads(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun finished(context: Context, downloadId: String, title: String, uri: String?, mime: String?) {
        val n = builder(context, CHANNEL_DONE)
            .setContentTitle(title)
            .setContentText("Download complete")
            .setAutoCancel(true)
            .setContentIntent(if (uri != null) openFile(context, uri, mime) else openDownloads(context))
            .build()
        notify(context, idFor(downloadId), n)
    }

    fun failed(context: Context, downloadId: String, title: String, reason: String?) {
        val n = builder(context, CHANNEL_DONE)
            .setContentTitle("Download failed")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n${reason.orEmpty()}"))
            .setAutoCancel(true)
            .setContentIntent(openDownloads(context))
            .build()
        notify(context, idFor(downloadId), n)
    }

    /** Every notification carries the white ClipSave mark on the brand colour. */
    private fun builder(context: Context, channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_clipsave)
        .setColor(BRAND)

    /** One summary per playlist instead of a notification per video. */
    fun batchFinished(context: Context, batchId: String, folder: String, saved: Int, failed: Int) {
        val text = buildString {
            append(if (saved == 1) "1 video saved" else "$saved videos saved")
            if (failed > 0) append(" · $failed failed")
        }
        val n = builder(context, CHANNEL_DONE)
            .setContentTitle(folder)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openDownloads(context))
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

    private fun openDownloads(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_DOWNLOADS)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
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
