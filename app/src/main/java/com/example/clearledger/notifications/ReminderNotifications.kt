package com.example.clearledger.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clearledger.R

object ReminderNotifications {
    const val CHANNEL_ID = "clearledger_reminders"
    const val WEEKLY_CHECK_IN_ID = 10_001

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "ClearLedger reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly check-in and freelance payment reminders"
            }
        )
    }

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
