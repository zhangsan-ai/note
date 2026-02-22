package com.example.voicetodo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.VoiceTodoApp
import com.example.voicetodo.reminder.ReminderPolicy
import kotlinx.coroutines.runBlocking

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val app = context.applicationContext as VoiceTodoApp
        val container = app.container

        runBlocking {
            when (intent.action) {
                ACTION_DISMISS -> {
                    container.repository.disableReminder(reminderId)
                    container.alarmScheduler.cancel(reminderId)
                    container.notifier.cancel(reminderId)
                }

                ACTION_SNOOZE -> {
                    val nextAt = ReminderPolicy.nextAtAfterSnooze(System.currentTimeMillis())
                    container.repository.snoozeReminder(reminderId, nextAt)
                    container.alarmScheduler.schedule(reminderId, nextAt)
                    container.notifier.cancel(reminderId)
                }
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.voicetodo.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.voicetodo.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "extra_action_reminder_id"
    }
}
