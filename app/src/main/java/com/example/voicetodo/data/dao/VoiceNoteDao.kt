package com.example.voicetodo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.voicetodo.data.entity.VoiceNoteEntity

@Dao
interface VoiceNoteDao {
    @Insert
    suspend fun insert(note: VoiceNoteEntity): Long
}
