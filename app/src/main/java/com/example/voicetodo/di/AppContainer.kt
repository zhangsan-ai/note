package com.example.voicetodo.di

import android.content.Context
import androidx.room.Room
import com.example.voicetodo.alarm.AlarmScheduler
import com.example.voicetodo.data.AppDatabase
import com.example.voicetodo.data.AppRepository
import com.example.voicetodo.notification.ReminderNotifier
import com.example.voicetodo.voice.VoiceRecorder
import com.example.voicetodo.voice.VoiceStorage

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "voice_todo.db",
    ).fallbackToDestructiveMigration().build()

    val repository = AppRepository(
        todoDao = database.todoDao(),
        reminderDao = database.reminderDao(),
        voiceNoteDao = database.voiceNoteDao(),
    )

    val alarmScheduler = AlarmScheduler(context)
    val notifier = ReminderNotifier(context)
    val voiceStorage = VoiceStorage(context)
    val voiceRecorder = VoiceRecorder(context)
}
