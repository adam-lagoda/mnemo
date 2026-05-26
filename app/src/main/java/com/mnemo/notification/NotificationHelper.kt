package com.mnemo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "mnemo_digest"
    const val NOTIFICATION_ID = 1001

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
