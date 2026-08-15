package com.readaloud.app.language

import com.readaloud.app.model.AppSettings
import com.readaloud.app.model.MessageLanguage
import com.readaloud.app.model.ProcessedSpeech
import com.readaloud.app.model.SpeechPreference
import com.readaloud.app.translation.OfflineTranslationEngine

class SpeechTextProcessor(
    private val detector: LanguageDetector = LanguageDetector(),
    private val translationEngine: OfflineTranslationEngine = OfflineTranslationEngine()
) {
    fun process(text: String, settings: AppSettings): ProcessedSpeech {
        val detected = detector.detect(text)
        val targetLanguage = targetLanguageFor(detected, settings.speechPreference)
        val normalized = when {
            settings.translationEnabled && shouldTranslate(detected, settings.speechPreference) ->
                translationEngine.translate(text, targetLanguage)
            settings.romanConversionEnabled ->
                translationEngine.normalizeExistingIndic(text, detected)
            else -> text
        }
        val languageForVoice = when (targetLanguage) {
            MessageLanguage.HINGLISH -> MessageLanguage.HINDI
            MessageLanguage.ROMAN_GUJARATI -> MessageLanguage.GUJARATI
            MessageLanguage.MIXED_HINDI_ENGLISH -> MessageLanguage.HINDI
            MessageLanguage.MIXED_GUJARATI_ENGLISH -> MessageLanguage.GUJARATI
            MessageLanguage.UNKNOWN -> detected.takeIf { it != MessageLanguage.UNKNOWN } ?: MessageLanguage.ENGLISH
            else -> targetLanguage
        }
        return ProcessedSpeech(
            text = normalizePunctuation(normalized, languageForVoice),
            language = languageForVoice,
            locale = languageForVoice.locale
        )
    }

    private fun targetLanguageFor(detected: MessageLanguage, preference: SpeechPreference): MessageLanguage {
        return when (preference) {
            SpeechPreference.AUTOMATIC -> detected
            SpeechPreference.HINDI -> MessageLanguage.HINDI
            SpeechPreference.GUJARATI -> MessageLanguage.GUJARATI
            SpeechPreference.ENGLISH -> {
                if (detected == MessageLanguage.ENGLISH || detected == MessageLanguage.UNKNOWN) {
                    MessageLanguage.ENGLISH
                } else {
                    detected
                }
            }
        }
    }

    private fun shouldTranslate(detected: MessageLanguage, preference: SpeechPreference): Boolean {
        if (preference == SpeechPreference.AUTOMATIC) return false
        if (preference == SpeechPreference.HINDI && detected == MessageLanguage.ENGLISH) return true
        if (preference == SpeechPreference.GUJARATI && detected == MessageLanguage.ENGLISH) return true
        return false
    }

    private fun normalizePunctuation(text: String, language: MessageLanguage): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return trimmed
        val hasTerminal = trimmed.last() in listOf('.', '?', '!', '।')
        if (hasTerminal) return trimmed
        return when (language) {
            MessageLanguage.HINDI,
            MessageLanguage.GUJARATI -> "$trimmed।"
            else -> "$trimmed."
        }
    }
}
