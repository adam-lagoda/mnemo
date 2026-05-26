package com.mnemo.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import java.util.UUID

object NotificationHelper {
    const val CHANNEL_ID         = "mnemo_digest"
    const val NOTIFICATION_ID    = 1001

    const val INDEXING_CHANNEL_ID              = "mnemo_indexing_v2"
    const val INDEXING_NOTIFICATION_ID         = 1002
    const val INDEXING_COMPLETE_NOTIFICATION_ID = 1003

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(com.mnemo.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(com.mnemo.R.string.notification_channel_desc)
            enableVibration(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun createIndexingChannel(context: Context) {
        val channel = NotificationChannel(
            INDEXING_CHANNEL_ID,
            "Indexing progress",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows progress while Mnemo indexes screenshots in the background"
            enableVibration(false)
            setShowBadge(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, Class.forName("com.mnemo.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildIndexingNotification(
        context: Context,
        done: Int,
        total: Int,
        workId: UUID,
        remainingSeconds: Int = -1,
    ): Notification {
        val progress = if (total > 0) (done * 100 / total) else 0
        val eta = if (remainingSeconds >= 0) " · ${formatEta(remainingSeconds)}" else ""
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(workId)
        return NotificationCompat.Builder(context, INDEXING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Mnemo · Indexing in progress")
            .setContentText("$done / $total complete$eta")
            .setProgress(100, progress, total == 0)
            .setContentIntent(mainActivityIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .build()
    }

    private fun formatEta(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s left" else "${s}s left"
    }

    fun sendIndexingCompleteNotification(context: Context, indexed: Int, failed: Int) {
        val text = if (failed == 0) "Indexed $indexed screenshots"
                   else "Indexed $indexed screenshots · $failed failed"
        // Use a separate ID (1003) so WorkManager's stopForeground cleanup of ID 1002
        // (the in-progress notification) doesn't wipe this one.
        val notification = NotificationCompat.Builder(context, INDEXING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Mnemo · Indexing complete")
            .setContentText(text)
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(INDEXING_COMPLETE_NOTIFICATION_ID, notification)
    }

    fun sendDigestNotification(
        context: Context,
        communitySummary: Map<String, Int> // communityLabel -> count
    ) {
        val total = communitySummary.values.sum()
        if (total == 0) return

        val bodyLines = communitySummary.entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString("\n") { (label, count) -> "• $count in '$label'" }

        val tapIntent = Intent(context, Class.forName("com.mnemo.MainActivity")).apply {
            action = "com.mnemo.ACTION_OPEN_GALLERY"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Mnemo: $total new items captured")
            .setContentText(communitySummary.entries.first().let { "${it.value} in '${it.key}'" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyLines))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
