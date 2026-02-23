package com.example.voicetodo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.voicetodo.data.entity.ReminderEntity

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminder WHERE id = :reminderId LIMIT 1")
    suspend fun findById(reminderId: Long): ReminderEntity?

    @Query("SELECT * FROM reminder WHERE is_enabled = 1")
    suspend fun findEnabled(): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminder WHERE is_enabled = 1")
    suspend fun countEnabled(): Int

    @Query("UPDATE reminder SET is_enabled = 0 WHERE id = :reminderId")
    suspend fun disable(reminderId: Long)

    @Query(
        """
        UPDATE reminder
        SET trigger_at_epoch_ms = :nextAt,
            last_fired_at = :firedAt,
            is_enabled = 1
        WHERE id = :reminderId
        """
    )
    suspend fun markFiredAndReschedule(reminderId: Long, firedAt: Long, nextAt: Long)

    @Query("UPDATE reminder SET trigger_at_epoch_ms = :nextAt, is_enabled = 1 WHERE id = :reminderId")
    suspend fun snooze(reminderId: Long, nextAt: Long)

    @Query("SELECT todo_id FROM reminder WHERE id = :reminderId LIMIT 1")
    suspend fun getTodoId(reminderId: Long): Long?

    @Query("SELECT trigger_at_epoch_ms FROM reminder WHERE id = :reminderId LIMIT 1")
    suspend fun getTriggerAt(reminderId: Long): Long?
}
