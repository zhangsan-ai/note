package com.example.voicetodo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.VoiceTodoApp
import com.example.voicetodo.reminder.ReminderPolicy
import com.example.voicetodo.voice.VoicePlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as VoiceTodoApp
        val container = app.container

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DISMISS -> {
                        container.repository.disableReminder(reminderId)
                        container.alarmScheduler.cancel(reminderId)
                        container.notifier.cancel(reminderId)
                        container.notifier.showResidentStatus(container.repository.activeReminderCount())
                    }

                    ACTION_SNOOZE -> {
                        val nextAt = ReminderPolicy.nextAtAfterSnooze(System.currentTimeMillis())
                        container.repository.snoozeReminder(reminderId, nextAt)
                        container.alarmScheduler.schedule(reminderId, nextAt)
                        container.notifier.cancel(reminderId)
                        container.notifier.showResidentStatus(container.repository.activeReminderCount())
                    }

                    ACTION_PLAY_AUDIO -> {
                        val info = container.repository.reminderNotificationInfo(reminderId)
                        val audioPath = info?.audioPath
                        if (!audioPath.isNullOrBlank()) {
                            VoicePlaybackService.startPlayback(
                                context = context.applicationContext,
                                reminderId = reminderId,
                                contentText = info.contentText,
                                audioPath = audioPath,
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.voicetodo.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.voicetodo.ACTION_SNOOZE"
        const val ACTION_PLAY_AUDIO = "com.example.voicetodo.ACTION_PLAY_AUDIO"
        const val EXTRA_REMINDER_ID = "extra_action_reminder_id"
    }
}
