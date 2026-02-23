package com.example.voicetodo.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
