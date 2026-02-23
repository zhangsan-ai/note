package com.example.voicetodo.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetodo.VoiceTodoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VoiceTodoApp).container
    private var pendingRecordingPath: String? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val quickOptions = listOf(0L, 5L, 10L, 15L, 30L, 60L, 120L, 180L, 360L, 720L, 1440L)

    init {
        observeTodos()
        container.notifier.ensureChannel()
        viewModelScope.launch {
            refreshResidentNotification()
        }
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

            val reminderMessage = when {
                triggerAt == null -> "待办已创建"
                container.alarmScheduler.canUseExactAlarms() -> "待办已创建并设置提醒"
                else -> "待办已创建并设置提醒（系统未授予精确闹钟，提醒可能延迟）"
            }

            refreshResidentNotification()

            _uiState.update {
                it.copy(
                    manualInput = "",
                    message = reminderMessage,
                )
            }
        }
    }

    fun startVoiceRecording() {
        if (_uiState.value.isRecording) return

        runCatching {
            val file = container.voiceStorage.createAudioFile("m4a")
            container.voiceRecorder.start(file)
            pendingRecordingPath = file.absolutePath
            _uiState.update { it.copy(isRecording = true, message = "录音中，点击“结束录音并创建”保存") }
        }.onFailure {
            container.voiceRecorder.stopSafely()
            pendingRecordingPath = null
            setMessage("无法开始录音，请检查麦克风权限")
        }
    }

    fun stopVoiceRecordingAndCreateTodo() {
        if (!_uiState.value.isRecording) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val triggerAt = _uiState.value.selectedMinutes
                ?.takeIf { it > 0L }
                ?.let { now + it * 60_000L }
            val audioPath = container.voiceRecorder.stop() ?: pendingRecordingPath
            pendingRecordingPath = null

            if (audioPath.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        message = "录音保存失败，请重试",
                    )
                }
                return@launch
            }

            val title = resolveAudioTodoTitle(_uiState.value.manualInput, now)
            val result = container.repository.createTodo(
                content = title,
                triggerAtEpochMs = triggerAt,
                audioPath = audioPath,
                sttText = null,
                parseStatus = if (triggerAt != null) "AUDIO_ONLY_WITH_TIME" else "AUDIO_ONLY",
                now = now,
            )

            result.reminderId?.let { reminderId ->
                triggerAt?.let { container.alarmScheduler.schedule(reminderId, it) }
            }
            refreshResidentNotification()

            val reminderMessage = when {
                triggerAt == null -> "未设置提醒"
                container.alarmScheduler.canUseExactAlarms() -> "提醒已设置"
                else -> "提醒已设置（系统未授予精确闹钟，可能延迟）"
            }

            _uiState.update {
                it.copy(
                    isRecording = false,
                    manualInput = "",
                    lastAudioPath = audioPath,
                    message = "语音待办已创建，$reminderMessage",
                )
            }
        }
    }

    fun cancelVoiceRecording() {
        container.voiceRecorder.stopSafely()
        pendingRecordingPath = null
        _uiState.update { it.copy(isRecording = false, message = "已取消录音") }
    }

    fun playLastAudio() {
        val path = _uiState.value.lastAudioPath ?: run {
            setMessage("没有可播放的原音")
            return
        }
        playAudioPath(path)
    }

    fun playAudioPath(path: String) {
        if (path.isBlank()) {
            setMessage("原音路径无效")
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
            refreshResidentNotification()
        }
    }

    private suspend fun refreshResidentNotification() {
        val activeCount = container.repository.activeReminderCount()
        container.notifier.showResidentStatus(activeCount)
    }

    private fun observeTodos() {
        viewModelScope.launch {
            container.repository.observeTodoRows().collect { rows ->
                val mapped = rows.map { row ->
                    TodoUiItem(
                        todoId = row.todoId,
                        title = row.contentText,
                        status = row.status,
                        reminderId = row.reminderId,
                        triggerAtEpochMs = row.triggerAtEpochMs,
                        audioPath = row.audioPath,
                        createdAt = row.createdAt,
                    )
                }
                _uiState.update {
                    it.copy(
                        todos = prioritizeTodos(mapped)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        container.voiceRecorder.stopSafely()
        pendingRecordingPath = null
        super.onCleared()
    }
}
