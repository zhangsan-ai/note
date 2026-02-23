package com.example.voicetodo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.VoiceTodoApp
import com.example.voicetodo.reminder.ReminderPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as VoiceTodoApp
        val container = app.container

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = container.repository.getReminder(reminderId) ?: return@launch
                if (!reminder.isEnabled) return@launch

                val info = container.repository.reminderNotificationInfo(reminderId) ?: return@launch
                container.notifier.showReminder(
                    reminderId = reminderId,
                    contentText = info.contentText,
                    hasAudio = !info.audioPath.isNullOrBlank(),
                )

                // Default behavior: if user does not close it, keep reminding every 5 minutes.
                val now = System.currentTimeMillis()
                val nextAt = ReminderPolicy.nextAtAfterNoAction(now)
                container.repository.markFiredAndReschedule(reminderId, now, nextAt)
                container.alarmScheduler.schedule(reminderId, nextAt)
                container.notifier.showResidentStatus(container.repository.activeReminderCount())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
