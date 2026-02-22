package com.example.voicetodo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder",
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
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "todo_id")
    val todoId: Long,
    @ColumnInfo(name = "trigger_at_epoch_ms")
    val triggerAtEpochMs: Long,
    @ColumnInfo(name = "repeat_every_minutes")
    val repeatEveryMinutes: Int = 5,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "last_fired_at")
    val lastFiredAt: Long? = null,
)
