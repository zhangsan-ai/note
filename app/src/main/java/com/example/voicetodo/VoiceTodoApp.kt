package com.example.voicetodo

import android.app.Application
import com.example.voicetodo.di.AppContainer

class VoiceTodoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
