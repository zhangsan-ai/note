package com.example.voicetodo.data

import com.example.voicetodo.data.dao.ReminderDao
import com.example.voicetodo.data.dao.TodoDao
import com.example.voicetodo.data.dao.VoiceNoteDao
import com.example.voicetodo.data.entity.ReminderEntity
import com.example.voicetodo.data.entity.TodoEntity
import com.example.voicetodo.data.entity.VoiceNoteEntity
import com.example.voicetodo.data.model.TodoListItemDbRow
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val todoDao: TodoDao,
    private val reminderDao: ReminderDao,
    private val voiceNoteDao: VoiceNoteDao,
) {
    data class CreateTodoResult(val todoId: Long, val reminderId: Long?)

    fun observeTodoRows(): Flow<List<TodoListItemDbRow>> = todoDao.observeTodoRows()

    suspend fun createTodo(
        content: String,
        triggerAtEpochMs: Long?,
        audioPath: String?,
        sttText: String?,
        parseStatus: String,
        now: Long,
    ): CreateTodoResult {
        val todoId = todoDao.insert(
            TodoEntity(
                contentText = content,
                createdAt = now,
                updatedAt = now,
            )
        )

        val reminderId = if (triggerAtEpochMs != null) {
            reminderDao.insert(
                ReminderEntity(
                    todoId = todoId,
                    triggerAtEpochMs = triggerAtEpochMs,
                )
            )
        } else {
            null
        }

        if (!audioPath.isNullOrBlank()) {
            voiceNoteDao.insert(
                VoiceNoteEntity(
                    todoId = todoId,
                    audioPath = audioPath,
                    durationMs = 0,
                    sttText = sttText,
                    parseStatus = parseStatus,
                )
            )
        }

        return CreateTodoResult(todoId = todoId, reminderId = reminderId)
    }

    suspend fun markDone(todoId: Long, now: Long) {
        todoDao.updateStatus(todoId, "DONE", now)
    }

    suspend fun getReminder(reminderId: Long): ReminderEntity? = reminderDao.findById(reminderId)

    suspend fun disableReminder(reminderId: Long) = reminderDao.disable(reminderId)

    suspend fun snoozeReminder(reminderId: Long, nextAt: Long) = reminderDao.snooze(reminderId, nextAt)

    suspend fun markFiredAndReschedule(reminderId: Long, firedAt: Long, nextAt: Long) {
        reminderDao.markFiredAndReschedule(reminderId, firedAt, nextAt)
    }

    suspend fun enabledReminders(): List<ReminderEntity> = reminderDao.findEnabled()

    suspend fun activeReminderCount(): Int = reminderDao.countEnabled()

    suspend fun reminderTodoId(reminderId: Long): Long? = reminderDao.getTodoId(reminderId)

    suspend fun todoTitle(todoId: Long): String = todoDao.getContent(todoId) ?: "待办"
}
