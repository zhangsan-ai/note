package com.example.voicetodo.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPolicyTest {
    @Test
    fun snoozeAlwaysAddsFiveMinutes() {
        val now = 1_000L
        assertEquals(now + 5 * 60_000L, ReminderPolicy.nextAtAfterSnooze(now))
    }

    @Test
    fun noActionSchedulesFiveMinutesLater() {
        val now = 2_000L
        assertEquals(now + 5 * 60_000L, ReminderPolicy.nextAtAfterNoAction(now))
    }
}
