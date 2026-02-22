package com.example.voicetodo.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(file: File) {
        stopSafely()
        currentFile = file

        val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder = instance.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            currentFile?.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            recorder = null
            currentFile = null
        }
    }

    fun stopSafely() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
            currentFile = null
        }
    }
}
