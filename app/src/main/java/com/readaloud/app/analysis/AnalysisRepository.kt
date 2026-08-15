package com.readaloud.app.analysis

import android.accessibilityservice.AccessibilityService
import com.readaloud.app.extraction.AccessibilityTextExtractor
import com.readaloud.app.extraction.OcrTextExtractor
import com.readaloud.app.language.SpeechTextProcessor
import com.readaloud.app.model.ReadAloudMessage
import com.readaloud.app.model.ScreenAnalysis
import com.readaloud.app.model.TextSource
import com.readaloud.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AnalysisState(
    val isAnalyzing: Boolean = false,
    val screenAnalysis: ScreenAnalysis = ScreenAnalysis(emptyList(), source = TextSource.SAMPLE),
    val error: String? = null
)

class AnalysisRepository(
    private val settingsRepository: SettingsRepository,
    private val speechTextProcessor: SpeechTextProcessor,
    private val accessibilityTextExtractor: AccessibilityTextExtractor = AccessibilityTextExtractor(),
    private val ocrTextExtractor: OcrTextExtractor = OcrTextExtractor()
) {
    private val _state = MutableStateFlow(AnalysisState())
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    suspend fun analyzeFromAccessibilityService(service: AccessibilityService) {
        val settings = settingsRepository.read()
        _state.value = _state.value.copy(isAnalyzing = true, error = null)

        val root = service.rootInActiveWindow
        val foregroundPackage = root?.packageName?.toString()
        val accessibilityMessages = withContext(Dispatchers.Default) {
            accessibilityTextExtractor.extract(root)
        }

        val source: TextSource
        val extractedMessages: List<String>
        if (accessibilityMessages.isNotEmpty()) {
            source = TextSource.ACCESSIBILITY
            extractedMessages = accessibilityMessages
        } else if (settings.ocrFallbackEnabled) {
            source = TextSource.OCR
            extractedMessages = ocrTextExtractor.extractFromAccessibilityScreenshot(service)
        } else {
            source = TextSource.ACCESSIBILITY
            extractedMessages = emptyList()
        }

        val readAloudMessages = extractedMessages.map { text ->
            val processed = speechTextProcessor.process(text, settings)
            ReadAloudMessage(
                originalText = text,
                processedText = processed.text,
                language = processed.language,
                locale = processed.locale,
                source = source
            )
        }

        val note = when {
            readAloudMessages.isEmpty() && source == TextSource.OCR ->
                "No readable message text was found from accessibility or local OCR."
            readAloudMessages.isEmpty() ->
                "No readable message text was found. Some apps hide message content from accessibility."
            else -> null
        }

        _state.value = AnalysisState(
            isAnalyzing = false,
            screenAnalysis = ScreenAnalysis(
                messages = readAloudMessages,
                foregroundPackage = foregroundPackage,
                source = source,
                note = note
            ),
            error = note
        )
    }

    fun setManualMessages(messages: List<String>) {
        val settings = settingsRepository.read()
        _state.value = AnalysisState(
            screenAnalysis = ScreenAnalysis(
                messages = messages.map {
                    val processed = speechTextProcessor.process(it, settings)
                    ReadAloudMessage(
                        originalText = it,
                        processedText = processed.text,
                        language = processed.language,
                        locale = processed.locale,
                        source = TextSource.MANUAL
                    )
                },
                source = TextSource.MANUAL
            )
        )
    }
}
