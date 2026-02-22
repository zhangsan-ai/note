package com.example.voicetodo.voice

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceStorage(private val context: Context) {
    fun createAudioFile(): File {
        val dir = File(context.filesDir, "voice")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val name = "voice_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".m4a"
        return File(dir, name)
    }
}
