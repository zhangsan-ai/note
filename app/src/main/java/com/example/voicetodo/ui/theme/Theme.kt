package com.example.voicetodo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = Accent,
    secondary = Ink,
    background = Surface,
)

private val DarkScheme = darkColorScheme(
    primary = Accent,
)

@Composable
fun VoiceTodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = Typography,
        content = content,
    )
}
