package com.example.voicetodo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_note",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todo_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["todo_id"])],
)
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "todo_id")
    val todoId: Long,
    @ColumnInfo(name = "audio_path")
    val audioPath: String,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "stt_text")
    val sttText: String?,
    @ColumnInfo(name = "parse_status")
    val parseStatus: String,
)
