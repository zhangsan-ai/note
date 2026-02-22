package com.example.voicetodo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    state: MainUiState,
    quickOptions: List<Long>,
    onInputChange: (String) -> Unit,
    onQuickSelected: (Long) -> Unit,
    onAddManual: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceCancel: () -> Unit,
    onPlayAudio: () -> Unit,
    onMarkDone: (Long, Long?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VoiceTodo") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.manualInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("手动输入待办") },
                placeholder = { Text("例如：喝水、开会、复盘") },
                singleLine = true,
            )

            Text(text = "快捷提醒", style = MaterialTheme.typography.titleSmall)

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
                    AssistChip(
                        onClick = { onQuickSelected(minute) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (state.selectedMinutes == minute) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddManual) {
                    Text("添加待办")
                }
                if (state.isListening) {
                    Button(onClick = onVoiceCancel) {
                        Text("取消语音")
                    }
                } else {
                    Button(onClick = onVoiceStart) {
                        Text("语音识别并创建")
                    }
                }
            }

            if (state.recognizedText.isNotBlank()) {
                Text(text = "识别文本：${state.recognizedText}")
            }

            if (!state.lastAudioPath.isNullOrBlank()) {
                TextButton(onClick = onPlayAudio) {
                    Text("播放原音")
                }
            }

            state.message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }

            Text(text = "待办列表", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.todos, key = { it.todoId }) { item ->
                    TodoCard(item = item, onMarkDone = onMarkDone)
                }
            }
        }
    }
}

@Composable
private fun TodoCard(item: TodoUiItem, onMarkDone: (Long, Long?) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = "状态：${item.status}")
                item.triggerAtEpochMs?.let {
                    Text(text = "下次提醒：${formatTime(it)}")
                }
            }
            TextButton(onClick = { onMarkDone(item.todoId, item.reminderId) }) {
                Text("完成")
            }
        }
    }
}

private fun formatTime(epochMs: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
}
