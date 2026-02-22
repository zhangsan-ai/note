package com.example.voicetodo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechToTextClient(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun startOnce(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                onError("语音识别失败($error)")
                destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val text = texts.firstOrNull().orEmpty()
                if (text.isBlank()) {
                    onError("没有识别到语音")
                } else {
                    onResult(text)
                }
                destroy()
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.startListening(intent)
    }

    fun cancel() {
        recognizer?.cancel()
        destroy()
    }

    private fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
