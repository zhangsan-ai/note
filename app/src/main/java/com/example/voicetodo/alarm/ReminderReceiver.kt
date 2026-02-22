package com.example.voicetodo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.VoiceTodoApp
import com.example.voicetodo.reminder.ReminderPolicy
import kotlinx.coroutines.runBlocking

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val app = context.applicationContext as VoiceTodoApp
        val container = app.container

        runBlocking {
            val reminder = container.repository.getReminder(reminderId) ?: return@runBlocking
            if (!reminder.isEnabled) return@runBlocking

            val todoId = container.repository.reminderTodoId(reminderId) ?: return@runBlocking
            val title = container.repository.todoTitle(todoId)
            container.notifier.showReminder(reminderId, title)

            // Default behavior: if user does not close it, keep reminding every 5 minutes.
            val now = System.currentTimeMillis()
            val nextAt = ReminderPolicy.nextAtAfterNoAction(now)
            container.repository.markFiredAndReschedule(reminderId, now, nextAt)
            container.alarmScheduler.schedule(reminderId, nextAt)
        }
    }
}
