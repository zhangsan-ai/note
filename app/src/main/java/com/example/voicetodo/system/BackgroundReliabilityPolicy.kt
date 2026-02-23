package com.example.voicetodo.system

enum class ReliabilityItem {
    EXACT_ALARM,
    BATTERY_OPTIMIZATION,
}

fun missingReliabilityItems(
    sdkInt: Int,
    canScheduleExactAlarms: Boolean,
    ignoringBatteryOptimizations: Boolean,
): List<ReliabilityItem> {
    val missing = mutableListOf<ReliabilityItem>()
    if (sdkInt >= 31 && !canScheduleExactAlarms) {
        missing += ReliabilityItem.EXACT_ALARM
    }
    if (!ignoringBatteryOptimizations) {
        missing += ReliabilityItem.BATTERY_OPTIMIZATION
    }
    return missing
}
