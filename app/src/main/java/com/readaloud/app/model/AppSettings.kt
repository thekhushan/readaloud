package com.readaloud.app.model

enum class SpeechPreference {
    AUTOMATIC,
    HINDI,
    GUJARATI,
    ENGLISH
}

data class AppSettings(
    val doubleVolumeEnabled: Boolean = true,
    val doublePressIntervalMs: Long = 650L,
    val speechPreference: SpeechPreference = SpeechPreference.AUTOMATIC,
    val speechSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val translationEnabled: Boolean = true,
    val romanConversionEnabled: Boolean = true,
    val ocrFallbackEnabled: Boolean = true,
    val showProcessedText: Boolean = false,
    val highlightSpeaking: Boolean = true,
    val onboardingComplete: Boolean = false
)
