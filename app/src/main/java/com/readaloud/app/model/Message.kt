package com.readaloud.app.model

import java.util.Locale
import java.util.UUID

enum class MessageLanguage(val displayName: String, val locale: Locale) {
    ENGLISH("English", Locale.ENGLISH),
    HINDI("Hindi", Locale("hi", "IN")),
    HINGLISH("Hinglish", Locale("hi", "IN")),
    GUJARATI("Gujarati", Locale("gu", "IN")),
    ROMAN_GUJARATI("Roman Gujarati", Locale("gu", "IN")),
    MIXED_HINDI_ENGLISH("Hindi + English", Locale("hi", "IN")),
    MIXED_GUJARATI_ENGLISH("Gujarati + English", Locale("gu", "IN")),
    UNKNOWN("Automatic", Locale.ENGLISH)
}

enum class TextSource {
    ACCESSIBILITY,
    OCR,
    SAMPLE,
    MANUAL
}

data class ReadAloudMessage(
    val id: String = UUID.randomUUID().toString(),
    val originalText: String,
    val processedText: String = originalText,
    val language: MessageLanguage = MessageLanguage.UNKNOWN,
    val locale: Locale = Locale.ENGLISH,
    val source: TextSource = TextSource.ACCESSIBILITY
)

data class ScreenAnalysis(
    val messages: List<ReadAloudMessage>,
    val foregroundPackage: String? = null,
    val source: TextSource = TextSource.ACCESSIBILITY,
    val note: String? = null
)

data class ProcessedSpeech(
    val text: String,
    val language: MessageLanguage,
    val locale: Locale
)
