package com.example.voicetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetodo.ui.MainScreen
import com.example.voicetodo.ui.MainViewModel
import com.example.voicetodo.ui.theme.VoiceTodoTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true
        val notifyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        if (audioGranted && notifyGranted) {
            viewModel.startVoiceCaptureAndRecognize()
        } else {
            viewModel.setMessage("请授予录音和通知权限")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value

            VoiceTodoTheme {
                MainScreen(
                    state = state,
                    quickOptions = viewModel.quickOptions(),
                    onInputChange = viewModel::onManualInputChange,
                    onQuickSelected = viewModel::onQuickOptionSelected,
                    onAddManual = viewModel::addManualTodo,
                    onVoiceStart = ::requestPermissionsAndStartVoice,
                    onVoiceCancel = viewModel::cancelVoiceListening,
                    onPlayAudio = viewModel::playLastAudio,
                    onMarkDone = viewModel::markTodoDone,
                )
            }
        }
    }

    private fun requestPermissionsAndStartVoice() {
        val required = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val missing = required.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            viewModel.startVoiceCaptureAndRecognize()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
