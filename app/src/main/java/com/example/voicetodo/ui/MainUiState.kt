package com.example.voicetodo.ui

data class TodoUiItem(
    val todoId: Long,
    val title: String,
    val status: String,
    val reminderId: Long?,
    val triggerAtEpochMs: Long?,
)

data class MainUiState(
    val manualInput: String = "",
    val selectedMinutes: Long? = 10,
    val recognizedText: String = "",
    val lastAudioPath: String? = null,
    val isListening: Boolean = false,
    val message: String? = null,
    val todos: List<TodoUiItem> = emptyList(),
)
