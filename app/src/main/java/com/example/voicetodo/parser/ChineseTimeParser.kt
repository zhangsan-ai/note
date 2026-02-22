package com.example.voicetodo.parser

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import java.util.Locale
import kotlin.math.max

class ChineseTimeParser {
    private val relativeRegex = Regex("(\\d{1,3})\\s*(分钟|分|小时|个小时)\\s*后")

    private val absoluteRegex = Regex("(今天|今晚|明天|后天)?\\s*(早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})(?:点|:)(\\d{1,2})?")

    fun parse(rawText: String, nowMs: Long = System.currentTimeMillis()): ParseResult {
        val text = rawText.trim()
        if (text.isEmpty()) {
            return ParseResult(content = "", triggerAtEpochMs = null, matched = false)
        }

        val now = Instant.ofEpochMilli(nowMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        relativeRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toLong()
            val unit = match.groupValues[2]
            val deltaMinutes = when (unit) {
                "小时", "个小时" -> value * 60
                else -> value
            }
            val triggerAt = now.plusMinutes(max(1, deltaMinutes)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val content = cleanupContent(text.replace(match.value, ""))
            return ParseResult(content = if (content.isBlank()) "语音待办" else content, triggerAtEpochMs = triggerAt, matched = true)
        }

        absoluteRegex.find(text)?.let { match ->
            val dayWord = match.groupValues[1]
            val periodWord = match.groupValues[2]
            var hour = match.groupValues[3].toInt()
            val minute = match.groupValues[4].ifBlank { "0" }.toInt()

            if ((periodWord == "下午" || periodWord == "晚上") && hour in 1..11) {
                hour += 12
            }
            if (periodWord == "中午" && hour < 11) {
                hour += 12
            }
            if (periodWord == "今晚" && hour in 1..11) {
                hour += 12
            }
            if (dayWord == "今晚" && hour in 1..11) {
                hour += 12
            }

            val baseDate = when (dayWord) {
                "明天" -> now.toLocalDate().plusDays(1)
                "后天" -> now.toLocalDate().plusDays(2)
                else -> now.toLocalDate()
            }

            var candidate = LocalDateTime.of(baseDate, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))

            if ((dayWord.isBlank() || dayWord == "今天" || dayWord == "今晚") && candidate.isBefore(now)) {
                candidate = candidate.plusDays(1)
            }

            val triggerAt = candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val content = cleanupContent(text.replace(match.value, ""))
            return ParseResult(content = if (content.isBlank()) "语音待办" else content, triggerAtEpochMs = triggerAt, matched = true)
        }

        return ParseResult(content = cleanupContent(text).ifBlank { "语音待办" }, triggerAtEpochMs = null, matched = false)
    }

    private fun cleanupContent(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace("提醒我", "")
            .replace("提醒", "")
            .replace("记得", "")
            .replace("，", " ")
            .replace(",", " ")
            .trim()
    }
}
