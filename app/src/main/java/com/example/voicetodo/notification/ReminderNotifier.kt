package com.example.voicetodo.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.voicetodo.MainActivity
import com.example.voicetodo.alarm.ReminderActionReceiver

class ReminderNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var testRingtone: Ringtone? = null
    private var reminderRingtone: Ringtone? = null

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmUri = defaultAlarmUri()
        val alarmChannel = NotificationChannel(
            CHANNEL_ID,
            "待办提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "待办与重复提醒通知"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setSound(
                alarmUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(alarmChannel)

        val statusChannel = NotificationChannel(
            STATUS_CHANNEL_ID,
            "提醒服务状态",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "提醒服务常驻状态通知"
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setSound(null, null)
        }
        manager.createNotificationChannel(statusChannel)
    }

    fun showReminder(reminderId: Long, contentText: String, hasAudio: Boolean) {
        if (!canPostNotifications()) return
        ensureChannel()
        val detailText = normalizeNotificationText(contentText)
        val collapsedTitle = buildCollapsedTitle(detailText)
        val collapsedContent = buildCollapsedContent(detailText)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
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

        val playAudioPending = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_PLAY_AUDIO)
            .putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            .let { playIntent ->
                PendingIntent.getBroadcast(
                    context,
                    reminderId.toInt() + 30_000,
                    playIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(collapsedTitle)
            .setContentText(collapsedContent)
            .setSubText("待办提醒")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("待办详情")
                    .bigText(detailText)
                    .setSummaryText("待办提醒")
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setFullScreenIntent(openPending, true)
            .setContentIntent(openPending)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(collapsedTitle)
                    .setContentText(detailText)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPending)
            .addAction(android.R.drawable.ic_media_pause, "延后5分钟", snoozePending)
        if (hasAudio) {
            notificationBuilder.addAction(android.R.drawable.ic_media_play, "播放语音", playAudioPending)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder
                .setSound(defaultAlarmUri())
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        val notification = notificationBuilder.build()
        manager.cancel(reminderId.toInt())
        manager.notify(reminderId.toInt(), notification)
        playReminderTone()
    }

    fun showResidentStatus(activeCount: Int) {
        if (activeCount <= 0) {
            manager.cancel(RESIDENT_NOTIFICATION_ID)
            return
        }
        if (!canPostNotifications()) return

        ensureChannel()

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            RESIDENT_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("提醒服务运行中")
            .setContentText("未完成提醒 $activeCount 条，通知常驻直到全部完成")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .build()

        manager.notify(RESIDENT_NOTIFICATION_ID, notification)
    }

    fun cancel(reminderId: Long) {
        manager.cancel(reminderId.toInt())
        stopReminderTone()
    }

    fun playTestAlarmTone(): Boolean {
        val ringtone = runCatching {
            RingtoneManager.getRingtone(context, defaultAlarmUri())
        }.getOrNull() ?: return false
        stopTestAlarmTone()
        ringtone.play()
        testRingtone = ringtone
        return true
    }

    fun stopTestAlarmTone() {
        testRingtone?.stop()
        testRingtone = null
    }

    fun stopReminderTone() {
        reminderRingtone?.stop()
        reminderRingtone = null
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun defaultAlarmUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Uri.parse("content://settings/system/notification_sound")
    }

    private fun normalizeNotificationText(raw: String): String {
        val normalized = raw
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.ifBlank { "待办内容为空" }
    }

    private fun buildCollapsedTitle(detailText: String): String {
        val maxLen = 18
        return if (detailText.length <= maxLen) {
            detailText
        } else {
            "${detailText.take(maxLen)}…"
        }
    }

    private fun buildCollapsedContent(detailText: String): String {
        val maxLen = 28
        return if (detailText.length <= maxLen) {
            detailText
        } else {
            "${detailText.take(maxLen)}…"
        }
    }

    private fun playReminderTone() {
        val ringtone = runCatching {
            RingtoneManager.getRingtone(context, defaultAlarmUri())
        }.getOrNull() ?: return
        stopReminderTone()
        ringtone.play()
        reminderRingtone = ringtone
    }

    companion object {
        const val CHANNEL_ID = "todo_reminder_channel_v3"
        private const val STATUS_CHANNEL_ID = "todo_reminder_status_channel_v1"
        private const val RESIDENT_NOTIFICATION_ID = 990099
    }
}
