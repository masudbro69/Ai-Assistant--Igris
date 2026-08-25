package com.igris.assistant.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.igris.assistant.R

object NotificationHelper {
    const val CHANNEL_ACTIONS = "igris_actions"
    const val CHANNEL_BRIEFING = "igris_briefing"
    private var nextId = 1000

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val actions = NotificationChannel(CHANNEL_ACTIONS, context.getString(R.string.notif_channel_actions), NotificationManager.IMPORTANCE_HIGH)
        val briefing = NotificationChannel(CHANNEL_BRIEFING, context.getString(R.string.notif_channel_briefing), NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(actions)
        nm.createNotificationChannel(briefing)
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(context: Context, channel: String, title: String, text: String) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(nextId++, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }
}
