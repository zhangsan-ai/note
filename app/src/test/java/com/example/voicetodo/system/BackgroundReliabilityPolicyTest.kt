package com.example.voicetodo.system

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundReliabilityPolicyTest {
    @Test
    fun missingItemsIncludeExactAlarmOnAndroidSAndAbove() {
        val items = missingReliabilityItems(
            sdkInt = 35,
            canScheduleExactAlarms = false,
            ignoringBatteryOptimizations = false,
        )

        assertEquals(
            listOf(ReliabilityItem.EXACT_ALARM, ReliabilityItem.BATTERY_OPTIMIZATION),
            items,
        )
    }

    @Test
    fun missingItemsSkipExactAlarmBelowAndroidS() {
        val items = missingReliabilityItems(
            sdkInt = 30,
            canScheduleExactAlarms = false,
            ignoringBatteryOptimizations = false,
        )

        assertEquals(listOf(ReliabilityItem.BATTERY_OPTIMIZATION), items)
    }
}
