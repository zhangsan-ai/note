package com.example.voicetodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    quickOptions: List<Long>,
    onInputChange: (String) -> Unit,
    onQuickSelected: (Long) -> Unit,
    onAddManual: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit,
    onVoiceCancel: () -> Unit,
    onPlayAudio: () -> Unit,
    onPlayItemAudio: (String) -> Unit,
    onMarkDone: (Long, Long?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("待办提醒") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ManualCreateCard(
                        state = state,
                        quickOptions = quickOptions,
                        onInputChange = onInputChange,
                        onQuickSelected = onQuickSelected,
                        onAddManual = onAddManual,
                    )
                }

                item {
                    VoiceCreateCard(
                        state = state,
                        onVoiceStart = onVoiceStart,
                        onVoiceStop = onVoiceStop,
                        onVoiceCancel = onVoiceCancel,
                        onPlayAudio = onPlayAudio,
                    )
                }

                state.message?.takeIf { it.isNotBlank() }?.let { message ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "待办列表（未完成优先）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.todos.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "还没有待办，先创建一条吧。",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    items(items = state.todos, key = { it.todoId }) { item ->
                        TodoCard(
                            item = item,
                            onPlayAudio = onPlayItemAudio,
                            onMarkDone = onMarkDone,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualCreateCard(
    state: MainUiState,
    quickOptions: List<Long>,
    onInputChange: (String) -> Unit,
    onQuickSelected: (Long) -> Unit,
    onAddManual: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "文本创建", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.manualInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("待办标题") },
                placeholder = { Text("例如：喝水、开会、复盘") },
                singleLine = true,
            )
            Text(text = "快捷提醒", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickOptions.forEach { minute ->
                    val label = when {
                        minute == 0L -> "无提醒"
                        minute < 60L -> "${minute}m"
                        else -> "${minute / 60}h"
                    }
                    FilterChip(
                        selected = state.selectedMinutes == minute,
                        onClick = { onQuickSelected(minute) },
                        label = { Text(text = label) },
                    )
                }
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddManual,
            ) {
                Text("添加文本待办")
            }
        }
    }
}

@Composable
private fun VoiceCreateCard(
    state: MainUiState,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit,
    onVoiceCancel: () -> Unit,
    onPlayAudio: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "语音创建（仅原音）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "已取消语音识别，录音会直接作为待办原音保存。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isRecording) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onVoiceStop) {
                        Text("结束录音并创建")
                    }
                    TextButton(onClick = onVoiceCancel) {
                        Text("取消录音")
                    }
                }
            } else {
                Button(onClick = onVoiceStart) {
                    Text("开始录音")
                }
            }

            if (!state.lastAudioPath.isNullOrBlank()) {
                TextButton(onClick = onPlayAudio) {
                    Text("播放最近原音")
                }
            }
        }
    }
}

@Composable
private fun TodoCard(
    item: TodoUiItem,
    onPlayAudio: (String) -> Unit,
    onMarkDone: (Long, Long?) -> Unit,
) {
    val isDone = item.status == "DONE"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                )
                Text(
                    text = if (isDone) "状态：已完成" else "状态：进行中",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                item.triggerAtEpochMs?.let {
                    Text(
                        text = "下次提醒：${formatTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item.audioPath?.takeIf { it.isNotBlank() }?.let { audioPath ->
                    TextButton(
                        modifier = Modifier.padding(start = 0.dp),
                        onClick = { onPlayAudio(audioPath) },
                    ) {
                        Text("播放原音")
                    }
                }
            }
            if (!isDone) {
                TextButton(onClick = { onMarkDone(item.todoId, item.reminderId) }) {
                    Text("完成")
                }
            }
        }
    }
}

private fun formatTime(epochMs: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
}
