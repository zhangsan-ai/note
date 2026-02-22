package com.example.voicetodo.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetodo.VoiceTodoApp
import com.example.voicetodo.reminder.ReminderPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VoiceTodoApp).container

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val quickOptions = listOf(0L, 5L, 10L, 15L, 30L, 60L, 120L, 180L, 360L, 720L, 1440L)

    init {
        observeTodos()
        container.notifier.ensureChannel()
    }

    fun quickOptions(): List<Long> = quickOptions

    fun onManualInputChange(value: String) {
        _uiState.update { it.copy(manualInput = value) }
    }

    fun onQuickOptionSelected(minutes: Long) {
        _uiState.update { it.copy(selectedMinutes = minutes) }
    }

    fun setMessage(message: String?) {
        _uiState.update { it.copy(message = message) }
    }

    fun addManualTodo() {
        val input = _uiState.value.manualInput.trim()
        if (input.isBlank()) {
            setMessage("请输入待办内容")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val triggerAt = _uiState.value.selectedMinutes
                ?.takeIf { it > 0L }
                ?.let { now + it * 60_000L }
            val result = container.repository.createTodo(
                content = input,
                triggerAtEpochMs = triggerAt,
                audioPath = null,
                sttText = null,
                parseStatus = if (triggerAt != null) "MANUAL_WITH_TIME" else "MANUAL",
                now = now,
            )

            result.reminderId?.let { reminderId ->
                triggerAt?.let { container.alarmScheduler.schedule(reminderId, it) }
            }

            _uiState.update {
                it.copy(
                    manualInput = "",
                    message = if (triggerAt != null) "待办已创建并设置提醒" else "待办已创建",
                )
            }
        }
    }

    fun startVoiceCaptureAndRecognize() {
        if (_uiState.value.isListening) return

        _uiState.update { it.copy(isListening = true, message = "开始说话，结束后自动创建") }

        val file = container.voiceStorage.createAudioFile()
        var recordingStarted = false
        try {
            container.voiceRecorder.start(file)
            recordingStarted = true
        } catch (_: Exception) {
            _uiState.update { it.copy(message = "录音保存失败，将仅做语音识别") }
        }

        container.speechClient.startOnce(
            onResult = { recognized ->
                viewModelScope.launch {
                    val path = if (recordingStarted) container.voiceRecorder.stop() else null
                    _uiState.update {
                        it.copy(
                            recognizedText = recognized,
                            lastAudioPath = path,
                            isListening = false,
                        )
                    }
                    createFromVoice(recognized, path)
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    val path = if (recordingStarted) container.voiceRecorder.stop() else null
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            lastAudioPath = path,
                            message = error,
                        )
                    }
                }
            },
        )
    }

    fun cancelVoiceListening() {
        container.speechClient.cancel()
        container.voiceRecorder.stopSafely()
        _uiState.update { it.copy(isListening = false, message = "已取消语音输入") }
    }

    fun playLastAudio() {
        val path = _uiState.value.lastAudioPath ?: run {
            setMessage("没有可播放的原音")
            return
        }

        try {
            val player = MediaPlayer()
            player.setDataSource(path)
            player.prepare()
            player.setOnCompletionListener { mp ->
                mp.release()
            }
            player.start()
            setMessage("正在播放原音")
        } catch (_: Exception) {
            setMessage("原音播放失败")
        }
    }

    fun markTodoDone(todoId: Long, reminderId: Long?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            container.repository.markDone(todoId, now)
            if (reminderId != null) {
                container.repository.disableReminder(reminderId)
                container.alarmScheduler.cancel(reminderId)
                container.notifier.cancel(reminderId)
            }
        }
    }

    private suspend fun createFromVoice(recognized: String, audioPath: String?) {
        val now = System.currentTimeMillis()
        val parse = container.parser.parse(recognized, now)

        val result = container.repository.createTodo(
            content = parse.content,
            triggerAtEpochMs = parse.triggerAtEpochMs,
            audioPath = audioPath,
            sttText = recognized,
            parseStatus = if (parse.matched) "PARSED" else "UNPARSED",
            now = now,
        )

        if (parse.triggerAtEpochMs != null && result.reminderId != null) {
            container.alarmScheduler.schedule(result.reminderId, parse.triggerAtEpochMs)
        }

        _uiState.update {
            it.copy(
                message = if (parse.matched) {
                    "语音待办已创建，提醒已设置；未关闭会每${ReminderPolicy.REPEAT_MINUTES}分钟继续提醒"
                } else {
                    "语音待办已创建，时间未识别，请手动选快捷时间"
                }
            )
        }
    }

    private fun observeTodos() {
        viewModelScope.launch {
            container.repository.observeTodoRows().collect { rows ->
                _uiState.update {
                    it.copy(
                        todos = rows.map { row ->
                            TodoUiItem(
                                todoId = row.todoId,
                                title = row.contentText,
                                status = row.status,
                                reminderId = row.reminderId,
                                triggerAtEpochMs = row.triggerAtEpochMs,
                            )
                        }
                    )
                }
            }
        }
    }
}
