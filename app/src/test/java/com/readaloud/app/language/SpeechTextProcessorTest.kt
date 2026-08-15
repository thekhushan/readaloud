package com.readaloud.app.language

import com.readaloud.app.model.AppSettings
import com.readaloud.app.model.MessageLanguage
import com.readaloud.app.model.SpeechPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextProcessorTest {
    private val processor = SpeechTextProcessor()

    @Test
    fun hinglishKeepsIndianEnglishLoanWordsNatural() {
        val result = processor.process("Aaj cricket match hai.", AppSettings())

        assertEquals(MessageLanguage.HINDI, result.language)
        assertTrue(result.text.contains("आज"))
        assertTrue(result.text.contains("क्रिकेट"))
        assertTrue(result.text.contains("मैच"))
    }

    @Test
    fun romanGujaratiSpeaksWithGujaratiVoice() {
        val result = processor.process("Hu office ma chu.", AppSettings())

        assertEquals(MessageLanguage.GUJARATI, result.language)
        assertTrue(result.text.contains("હું"))
        assertTrue(result.text.contains("ઓફિસ"))
    }

    @Test
    fun englishCanBeTranslatedToHindiWhenRequested() {
        val result = processor.process(
            "Where are you going?",
            AppSettings(speechPreference = SpeechPreference.HINDI)
        )

        assertEquals(MessageLanguage.HINDI, result.language)
        assertTrue(result.text.contains("आप कहाँ जा रहे हो"))
    }

    @Test
    fun englishCanBeTranslatedToGujaratiWhenRequested() {
        val result = processor.process(
            "Where are you going?",
            AppSettings(speechPreference = SpeechPreference.GUJARATI)
        )

        assertEquals(MessageLanguage.GUJARATI, result.language)
        assertTrue(result.text.contains("તમે ક્યાં જઈ રહ્યા છો"))
    }
}
