package com.example.voicetodo

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetodo.system.ReliabilityItem
import com.example.voicetodo.system.missingReliabilityItems
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

    private val reliabilitySettingsLauncher = registerForActivityResult(StartActivityForResult()) {
        val missing = currentMissingReliabilityItems()
        if (missing.isEmpty()) {
            viewModel.setMessage("后台保活关键权限已完成")
        } else {
            val tips = missing.joinToString("、") { item ->
                when (item) {
                    ReliabilityItem.EXACT_ALARM -> "精确闹钟"
                    ReliabilityItem.BATTERY_OPTIMIZATION -> "电池优化豁免"
                }
            }
            viewModel.setMessage("仍缺少：$tips")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        notifyReliabilityStatus()

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
                    onToggleTestAlarmTone = viewModel::toggleTestAlarmTone,
                    onRequestExactAlarmAccess = ::requestExactAlarmAccess,
                    onRequestBatteryOptimization = ::requestBatteryOptimizationExemption,
                    onOpenBackgroundSettings = ::openBackgroundSettings,
                    onMarkDone = viewModel::markTodoDone,
                    onRequestClearCompleted = viewModel::requestClearCompleted,
                    onConfirmClearCompleted = viewModel::clearCompletedTodos,
                    onDismissClearCompleted = viewModel::dismissClearCompletedConfirm,
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

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            viewModel.setMessage("当前系统无需单独申请精确闹钟")
            return
        }
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) {
            viewModel.setMessage("精确闹钟权限已开启")
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        reliabilitySettingsLauncher.launch(intent)
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            viewModel.setMessage("当前系统无需电池优化豁免")
            return
        }
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            viewModel.setMessage("已忽略电池优化")
            return
        }
        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val targetIntent = when {
            requestIntent.resolveActivity(packageManager) != null -> requestIntent
            fallbackIntent.resolveActivity(packageManager) != null -> fallbackIntent
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        reliabilitySettingsLauncher.launch(targetIntent)
    }

    private fun openBackgroundSettings() {
        val candidates = listOf(
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
        )
        val target = candidates.firstOrNull { it.resolveActivity(packageManager) != null }
        if (target != null) {
            reliabilitySettingsLauncher.launch(target)
        } else {
            viewModel.setMessage("未找到系统后台设置入口")
        }
    }

    private fun notifyReliabilityStatus() {
        val missing = currentMissingReliabilityItems()
        if (missing.isEmpty()) return
        val tips = missing.joinToString("、") { item ->
            when (item) {
                ReliabilityItem.EXACT_ALARM -> "精确闹钟"
                ReliabilityItem.BATTERY_OPTIMIZATION -> "电池优化豁免"
            }
        }
        viewModel.setMessage("建议开启：$tips")
    }

    private fun currentMissingReliabilityItems(): List<ReliabilityItem> {
        val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
        val ignoringBatteryOptimization = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        return missingReliabilityItems(
            sdkInt = Build.VERSION.SDK_INT,
            canScheduleExactAlarms = canExactAlarm,
            ignoringBatteryOptimizations = ignoringBatteryOptimization,
        )
    }
}
