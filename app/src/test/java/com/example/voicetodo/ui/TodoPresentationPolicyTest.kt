package com.example.voicetodo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoPresentationPolicyTest {
    @Test
    fun prioritizeActiveTodosBeforeDone() {
        val todos = listOf(
            TodoUiItem(
                todoId = 1,
                title = "已完成任务",
                status = "DONE",
                reminderId = null,
                triggerAtEpochMs = null,
                audioPath = null,
                createdAt = 300L,
            ),
            TodoUiItem(
                todoId = 2,
                title = "未完成较新",
                status = "ACTIVE",
                reminderId = null,
                triggerAtEpochMs = null,
                audioPath = null,
                createdAt = 200L,
            ),
            TodoUiItem(
                todoId = 3,
                title = "未完成较早",
                status = "ACTIVE",
                reminderId = null,
                triggerAtEpochMs = null,
                audioPath = null,
                createdAt = 100L,
            ),
        )

        val sorted = prioritizeTodos(todos)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.todoId })
    }

    @Test
    fun resolveAudioTodoTitleUsesManualInputFirst() {
        val now = 1_700_000_000_000L

        assertEquals("喝水", resolveAudioTodoTitle("  喝水  ", now))
        assertTrue(resolveAudioTodoTitle("   ", now).startsWith("语音待办 "))
    }
}
