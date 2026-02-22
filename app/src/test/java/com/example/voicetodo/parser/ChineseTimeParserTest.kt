package com.example.voicetodo.parser

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChineseTimeParserTest {
    private val parser = ChineseTimeParser()

    @Test
    fun parseRelativeMinutes() {
        val now = 1_700_000_000_000L
        val result = parser.parse("10分钟后提醒喝水", now)

        assertTrue(result.matched)
        assertTrue(result.content.contains("喝水"))
        assertNotNull(result.triggerAtEpochMs)
        assertTrue(result.triggerAtEpochMs!! > now)
    }

    @Test
    fun parseAbsoluteTonight() {
        val now = 1_700_000_000_000L
        val result = parser.parse("今晚8点提醒开会", now)

        assertTrue(result.matched)
        assertTrue(result.content.contains("开会"))
        assertNotNull(result.triggerAtEpochMs)
    }
}
