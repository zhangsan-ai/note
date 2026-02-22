package com.example.voicetodo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voicetodo.data.dao.ReminderDao
import com.example.voicetodo.data.dao.TodoDao
import com.example.voicetodo.data.dao.VoiceNoteDao
import com.example.voicetodo.data.entity.ReminderEntity
import com.example.voicetodo.data.entity.TodoEntity
import com.example.voicetodo.data.entity.VoiceNoteEntity

@Database(
    entities = [TodoEntity::class, ReminderEntity::class, VoiceNoteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun reminderDao(): ReminderDao
    abstract fun voiceNoteDao(): VoiceNoteDao
}
