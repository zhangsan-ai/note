package com.example.voicetodo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.voicetodo.data.entity.TodoEntity
import com.example.voicetodo.data.model.TodoListItemDbRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Query(
        """
        SELECT 
            t.id AS todoId,
            t.content_text AS contentText,
            t.status AS status,
            t.created_at AS createdAt,
            r.id AS reminderId,
            r.trigger_at_epoch_ms AS triggerAtEpochMs,
            v.audio_path AS audioPath,
            r.is_enabled AS reminderEnabled
        FROM todo_item t
        LEFT JOIN reminder r ON r.todo_id = t.id AND r.is_enabled = 1
        LEFT JOIN voice_note v ON v.id = (
            SELECT vn.id
            FROM voice_note vn
            WHERE vn.todo_id = t.id
            ORDER BY vn.id DESC
            LIMIT 1
        )
        ORDER BY
            CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END ASC,
            t.created_at DESC
        """
    )
    fun observeTodoRows(): Flow<List<TodoListItemDbRow>>

    @Query("UPDATE todo_item SET status = :status, updated_at = :updatedAt WHERE id = :todoId")
    suspend fun updateStatus(todoId: Long, status: String, updatedAt: Long)

    @Query("SELECT content_text FROM todo_item WHERE id = :todoId LIMIT 1")
    suspend fun getContent(todoId: Long): String?
}
