package com.example.voicetodo.ui

data class TodoUiItem(
    val todoId: Long,
    val title: String,
    val status: String,
    val reminderId: Long?,
    val triggerAtEpochMs: Long?,
    val audioPath: String?,
    val createdAt: Long,
)

data class MainUiState(
    val manualInput: String = "",
    val selectedMinutes: Long? = 10,
    val lastAudioPath: String? = null,
    val isRecording: Boolean = false,
    val isTestingAlarmTone: Boolean = false,
    val completedCount: Int = 0,
    val showClearCompletedConfirm: Boolean = false,
    val message: String? = null,
    val todos: List<TodoUiItem> = emptyList(),
)
