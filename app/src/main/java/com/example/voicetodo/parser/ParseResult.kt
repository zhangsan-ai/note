package com.example.voicetodo.parser

data class ParseResult(
    val content: String,
    val triggerAtEpochMs: Long?,
    val matched: Boolean,
)
