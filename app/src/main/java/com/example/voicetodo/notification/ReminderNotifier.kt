package com.example.voicetodo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.voicetodo.MainActivity
import com.example.voicetodo.alarm.ReminderActionReceiver

class ReminderNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "待办提醒",
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminder(reminderId: Long, title: String) {
        ensureChannel()

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_DISMISS)
            .putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        val dismissPending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 10_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_SNOOZE)
            .putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        val snoozePending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 20_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("待办提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPending)
            .addAction(android.R.drawable.ic_media_pause, "延后5分钟", snoozePending)
            .build()

        manager.notify(reminderId.toInt(), notification)
    }

    fun cancel(reminderId: Long) {
        manager.cancel(reminderId.toInt())
    }

    companion object {
        const val CHANNEL_ID = "todo_reminder_channel"
    }
}
