package com.example.voicetodo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.voicetodo.data.entity.VoiceNoteEntity

@Dao
interface VoiceNoteDao {
    @Insert
    suspend fun insert(note: VoiceNoteEntity): Long

    @Query(
        """
        SELECT audio_path
        FROM voice_note
        WHERE todo_id = :todoId
        ORDER BY id DESC
        LIMIT 1
        """
    )
    suspend fun latestAudioPathByTodoId(todoId: Long): String?
}
