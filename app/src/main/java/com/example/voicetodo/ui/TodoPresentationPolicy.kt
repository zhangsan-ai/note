package com.example.voicetodo.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatQuickOptionLabel(minutes: Long): String {
    return when {
        minutes <= 0L -> "无提醒"
        minutes == 2_880L -> "2天"
        minutes == 4_320L -> "3天"
        minutes % 60L == 0L -> "${minutes / 60L}小时"
        else -> "${minutes}分钟"
    }
}

fun prioritizeTodos(todos: List<TodoUiItem>): List<TodoUiItem> {
    return todos.sortedWith(
        compareBy<TodoUiItem> { it.status == "DONE" }
            .thenByDescending { it.createdAt }
    )
}

fun resolveAudioTodoTitle(manualInput: String, nowMs: Long): String {
    val trimmed = manualInput.trim()
    if (trimmed.isNotEmpty()) {
        return trimmed
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return "语音待办 ${formatter.format(Date(nowMs))}"
}
