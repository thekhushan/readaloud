package com.readaloud.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.readaloud.app.ReadAloudApplication
import com.readaloud.app.accessibility.ReadAloudAccessibilityService
import com.readaloud.app.activation.AccessibilityActivation
import com.readaloud.app.model.AppSettings
import com.readaloud.app.model.MessageLanguage
import com.readaloud.app.model.ReadAloudMessage
import com.readaloud.app.model.SpeechPreference
import com.readaloud.app.settings.SettingsRepository
import com.readaloud.app.speech.SpeechState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReadAloudUiState(
    val settings: AppSettings = AppSettings(),
    val analysis: com.readaloud.app.analysis.AnalysisState = com.readaloud.app.analysis.AnalysisState(),
    val speech: SpeechState = SpeechState(),
    val accessibilityEnabled: Boolean = false
)

class ReadAloudViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ReadAloudApplication
    private val settingsRepository: SettingsRepository = app.settingsRepository
    private val runtimeRefresh = MutableStateFlow(0)

    val uiState: StateFlow<ReadAloudUiState> = combine(
        settingsRepository.settings,
        app.analysisRepository.state,
        app.ttsManager.state,
        runtimeRefresh
    ) { settings, analysis, speech, _ ->
        ReadAloudUiState(
            settings = settings,
            analysis = analysis,
            speech = speech,
            accessibilityEnabled = AccessibilityActivation.isServiceEnabled(getApplication())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadAloudUiState()
    )

    fun refreshVoices() {
        app.ttsManager.refreshVoices()
    }

    fun refreshRuntimeChecks() {
        runtimeRefresh.value += 1
        app.ttsManager.refreshVoices()
    }

    fun requestScreenAnalysis() {
        ReadAloudAccessibilityService.instance?.analyzeCurrentScreenAndOpen()
    }

    fun openAccessibilitySettings(): Intent = AccessibilityActivation.accessibilitySettingsIntent()

    fun openVoiceInstall(): Intent = app.ttsManager.installVoiceDataIntent()

    fun speak(message: ReadAloudMessage) {
        app.ttsManager.speak(message, settingsRepository.read())
    }

    fun playAll(messages: List<ReadAloudMessage>) {
        app.ttsManager.playAll(messages, settingsRepository.read())
    }

    fun pause() = app.ttsManager.pause()

    fun resume() = app.ttsManager.resume()

    fun stop() = app.ttsManager.stop()

    fun testVoice(language: MessageLanguage) {
        app.ttsManager.testVoice(language, settingsRepository.read())
    }

    fun setDoubleVolumeEnabled(enabled: Boolean) {
        settingsRepository.update { it.copy(doubleVolumeEnabled = enabled) }
    }

    fun setDoublePressInterval(value: Long) {
        settingsRepository.update { it.copy(doublePressIntervalMs = value.coerceIn(300L, 1200L)) }
    }

    fun setSpeechPreference(preference: SpeechPreference) {
        settingsRepository.update { it.copy(speechPreference = preference) }
    }

    fun setSpeechSpeed(value: Float) {
        settingsRepository.update { it.copy(speechSpeed = value.coerceIn(0.5f, 1.8f)) }
    }

    fun setPitch(value: Float) {
        settingsRepository.update { it.copy(pitch = value.coerceIn(0.5f, 1.8f)) }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        settingsRepository.update { it.copy(translationEnabled = enabled) }
    }

    fun setRomanConversionEnabled(enabled: Boolean) {
        settingsRepository.update { it.copy(romanConversionEnabled = enabled) }
    }

    fun setOcrFallbackEnabled(enabled: Boolean) {
        settingsRepository.update { it.copy(ocrFallbackEnabled = enabled) }
    }

    fun setShowProcessedText(enabled: Boolean) {
        settingsRepository.update { it.copy(showProcessedText = enabled) }
    }

    fun setHighlightSpeaking(enabled: Boolean) {
        settingsRepository.update { it.copy(highlightSpeaking = enabled) }
    }

    fun completeOnboarding() {
        settingsRepository.update { it.copy(onboardingComplete = true) }
    }

    fun loadSampleMessages() {
        viewModelScope.launch {
            app.analysisRepository.setManualMessages(
                listOf(
                    "Aaj cricket match hai?",
                    "Kal school jaana hai.",
                    "Tame kem cho?",
                    "Where are you going?"
                )
            )
        }
    }
}
