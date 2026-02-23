package com.example.voicetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetodo.ui.MainScreen
import com.example.voicetodo.ui.MainViewModel
import com.example.voicetodo.ui.theme.VoiceTodoTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private val audioPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            viewModel.startVoiceRecording()
        } else {
            viewModel.setMessage("请授予录音权限")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        if (!granted) {
            viewModel.setMessage("通知权限未开启，提醒可能不可见")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value

            VoiceTodoTheme {
                MainScreen(
                    state = state,
                    quickOptions = viewModel.quickOptions(),
                    onInputChange = viewModel::onManualInputChange,
                    onQuickSelected = viewModel::onQuickOptionSelected,
                    onAddManual = viewModel::addManualTodo,
                    onVoiceStart = ::requestAudioPermissionAndStartRecording,
                    onVoiceStop = viewModel::stopVoiceRecordingAndCreateTodo,
                    onVoiceCancel = viewModel::cancelVoiceRecording,
                    onPlayAudio = viewModel::playLastAudio,
                    onPlayItemAudio = viewModel::playAudioPath,
                    onMarkDone = viewModel::markTodoDone,
                )
            }
        }
    }

    private fun requestAudioPermissionAndStartRecording() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startVoiceRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
