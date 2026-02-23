package com.example.voicetodo.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.VoiceTodoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as VoiceTodoApp
        val container = app.container

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                container.repository.enabledReminders().forEach { reminder ->
                    val triggerAt = if (reminder.triggerAtEpochMs < now) now + 10_000L else reminder.triggerAtEpochMs
                    container.alarmScheduler.schedule(reminder.id, triggerAt)
                }
                container.notifier.showResidentStatus(container.repository.activeReminderCount())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
