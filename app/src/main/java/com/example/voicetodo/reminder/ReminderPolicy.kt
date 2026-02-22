package com.example.voicetodo.reminder

object ReminderPolicy {
    const val REPEAT_MINUTES: Long = 5
    private const val MINUTE_MS = 60_000L

    fun nextAtAfterNoAction(nowMs: Long): Long = nowMs + REPEAT_MINUTES * MINUTE_MS

    fun nextAtAfterSnooze(nowMs: Long): Long = nowMs + REPEAT_MINUTES * MINUTE_MS
}
