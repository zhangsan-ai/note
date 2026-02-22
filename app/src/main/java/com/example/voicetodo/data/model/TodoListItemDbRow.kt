package com.example.voicetodo.data.model

data class TodoListItemDbRow(
    val todoId: Long,
    val contentText: String,
    val status: String,
    val createdAt: Long,
    val reminderId: Long?,
    val triggerAtEpochMs: Long?,
    val reminderEnabled: Boolean?,
)
