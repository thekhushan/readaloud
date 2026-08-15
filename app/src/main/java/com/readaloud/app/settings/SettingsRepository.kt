package com.readaloud.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.readaloud.app.model.AppSettings
import com.readaloud.app.model.SpeechPreference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("readaloud_settings", Context.MODE_PRIVATE)

    val settings: Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(read())
        }
        trySend(read())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun read(): AppSettings = AppSettings(
        doubleVolumeEnabled = prefs.getBoolean(KEY_DOUBLE_VOLUME, true),
        doublePressIntervalMs = prefs.getLong(KEY_DOUBLE_INTERVAL, 650L),
        speechPreference = prefs.getString(KEY_SPEECH_PREF, SpeechPreference.AUTOMATIC.name)
            ?.let { runCatching { SpeechPreference.valueOf(it) }.getOrNull() }
            ?: SpeechPreference.AUTOMATIC,
        speechSpeed = prefs.getFloat(KEY_SPEED, 1.0f),
        pitch = prefs.getFloat(KEY_PITCH, 1.0f),
        translationEnabled = prefs.getBoolean(KEY_TRANSLATION, true),
        romanConversionEnabled = prefs.getBoolean(KEY_ROMAN, true),
        ocrFallbackEnabled = prefs.getBoolean(KEY_OCR, true),
        showProcessedText = prefs.getBoolean(KEY_SHOW_PROCESSED, false),
        highlightSpeaking = prefs.getBoolean(KEY_HIGHLIGHT, true),
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING, false)
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(read())
        prefs.edit()
            .putBoolean(KEY_DOUBLE_VOLUME, next.doubleVolumeEnabled)
            .putLong(KEY_DOUBLE_INTERVAL, next.doublePressIntervalMs)
            .putString(KEY_SPEECH_PREF, next.speechPreference.name)
            .putFloat(KEY_SPEED, next.speechSpeed)
            .putFloat(KEY_PITCH, next.pitch)
            .putBoolean(KEY_TRANSLATION, next.translationEnabled)
            .putBoolean(KEY_ROMAN, next.romanConversionEnabled)
            .putBoolean(KEY_OCR, next.ocrFallbackEnabled)
            .putBoolean(KEY_SHOW_PROCESSED, next.showProcessedText)
            .putBoolean(KEY_HIGHLIGHT, next.highlightSpeaking)
            .putBoolean(KEY_ONBOARDING, next.onboardingComplete)
            .apply()
    }

    companion object {
        private const val KEY_DOUBLE_VOLUME = "double_volume"
        private const val KEY_DOUBLE_INTERVAL = "double_interval"
        private const val KEY_SPEECH_PREF = "speech_pref"
        private const val KEY_SPEED = "speed"
        private const val KEY_PITCH = "pitch"
        private const val KEY_TRANSLATION = "translation"
        private const val KEY_ROMAN = "roman"
        private const val KEY_OCR = "ocr"
        private const val KEY_SHOW_PROCESSED = "show_processed"
        private const val KEY_HIGHLIGHT = "highlight"
        private const val KEY_ONBOARDING = "onboarding"
    }
}
