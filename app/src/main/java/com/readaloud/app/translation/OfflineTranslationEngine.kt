package com.readaloud.app.translation

import com.readaloud.app.language.HinglishProcessor
import com.readaloud.app.language.RomanGujaratiProcessor
import com.readaloud.app.model.MessageLanguage

class OfflineTranslationEngine(
    private val hinglishProcessor: HinglishProcessor = HinglishProcessor(),
    private val romanGujaratiProcessor: RomanGujaratiProcessor = RomanGujaratiProcessor()
) {
    fun translate(text: String, target: MessageLanguage): String {
        val normalized = text.trim()
        if (normalized.isBlank()) return normalized
        return when (target) {
            MessageLanguage.HINDI,
            MessageLanguage.HINGLISH,
            MessageLanguage.MIXED_HINDI_ENGLISH -> translateEnglishToHindi(normalized)
            MessageLanguage.GUJARATI,
            MessageLanguage.ROMAN_GUJARATI,
            MessageLanguage.MIXED_GUJARATI_ENGLISH -> translateEnglishToGujarati(normalized)
            else -> normalized
        }
    }

    private fun translateEnglishToHindi(text: String): String {
        val lower = text.lowercase().trim().trimEnd('.', '?', '!')
        englishToHindiPhrase[lower]?.let { return punctuateLike(text, it) }
        val words = text.split(Regex("(\\s+)"))
        return words.joinToString(" ") { word ->
            val clean = word.lowercase().trim { !it.isLetterOrDigit() }
            englishToHindiWord[clean] ?: HinglishProcessor.hindiDictionary[clean] ?: word
        }
    }

    private fun translateEnglishToGujarati(text: String): String {
        val lower = text.lowercase().trim().trimEnd('.', '?', '!')
        englishToGujaratiPhrase[lower]?.let { return punctuateLike(text, it) }
        val words = text.split(Regex("(\\s+)"))
        return words.joinToString(" ") { word ->
            val clean = word.lowercase().trim { !it.isLetterOrDigit() }
            englishToGujaratiWord[clean] ?: RomanGujaratiProcessor.gujaratiDictionary[clean] ?: word
        }
    }

    fun normalizeExistingIndic(text: String, language: MessageLanguage): String {
        return when (language) {
            MessageLanguage.HINGLISH,
            MessageLanguage.MIXED_HINDI_ENGLISH -> hinglishProcessor.normalize(text)
            MessageLanguage.ROMAN_GUJARATI,
            MessageLanguage.MIXED_GUJARATI_ENGLISH -> romanGujaratiProcessor.normalize(text)
            else -> text
        }
    }

    private fun punctuateLike(source: String, translated: String): String {
        return when {
            source.trim().endsWith("?") && !translated.endsWith("?") -> "$translated?"
            source.trim().endsWith("!") && !translated.endsWith("!") -> "$translated!"
            source.trim().endsWith(".") && !translated.endsWith("।") -> "$translated।"
            else -> translated
        }
    }

    private val englishToHindiPhrase = mapOf(
        "where are you going" to "आप कहाँ जा रहे हो",
        "where are you" to "आप कहाँ हो",
        "what are you doing" to "आप क्या कर रहे हो",
        "are you coming" to "क्या आप आ रहे हो",
        "come home" to "घर आ जाओ",
        "come quickly" to "जल्दी आओ",
        "i am at home" to "मैं घर पर हूँ",
        "i am in office" to "मैं ऑफिस में हूँ",
        "school tomorrow" to "कल स्कूल है",
        "there is a cricket match today" to "आज क्रिकेट मैच है",
        "today is cricket match" to "आज क्रिकेट मैच है",
        "call me" to "मुझे कॉल करो",
        "message me" to "मुझे मैसेज करो"
    )

    private val englishToGujaratiPhrase = mapOf(
        "where are you going" to "તમે ક્યાં જઈ રહ્યા છો",
        "where are you" to "તમે ક્યાં છો",
        "what are you doing" to "તમે શું કરી રહ્યા છો",
        "are you coming" to "તમે આવી રહ્યા છો",
        "come home" to "ઘરે આવો",
        "come quickly" to "જલદી આવો",
        "i am at home" to "હું ઘરે છું",
        "i am in office" to "હું ઓફિસમાં છું",
        "school tomorrow" to "કાલે સ્કૂલ છે",
        "there is a cricket match today" to "આજે ક્રિકેટ મેચ છે",
        "today is cricket match" to "આજે ક્રિકેટ મેચ છે",
        "call me" to "મને કોલ કરો",
        "message me" to "મને મેસેજ કરો"
    )

    private val englishToHindiWord = mapOf(
        "where" to "कहाँ", "what" to "क्या", "when" to "कब", "why" to "क्यों",
        "how" to "कैसे", "you" to "आप", "your" to "आपका", "going" to "जा रहे",
        "coming" to "आ रहे", "home" to "घर", "today" to "आज", "tomorrow" to "कल",
        "call" to "कॉल", "message" to "मैसेज", "me" to "मुझे", "quickly" to "जल्दी",
        "school" to "स्कूल", "office" to "ऑफिस", "meeting" to "मीटिंग",
        "practice" to "प्रैक्टिस", "match" to "मैच", "cricket" to "क्रिकेट"
    )

    private val englishToGujaratiWord = mapOf(
        "where" to "ક્યાં", "what" to "શું", "when" to "ક્યારે", "why" to "કેમ",
        "how" to "કેવી રીતે", "you" to "તમે", "your" to "તમારું", "going" to "જઈ રહ્યા",
        "coming" to "આવી રહ્યા", "home" to "ઘર", "today" to "આજે", "tomorrow" to "કાલે",
        "call" to "કોલ", "message" to "મેસેજ", "me" to "મને", "quickly" to "જલદી",
        "school" to "સ્કૂલ", "office" to "ઓફિસ", "meeting" to "મીટિંગ",
        "practice" to "પ્રેક્ટિસ", "match" to "મેચ", "cricket" to "ક્રિકેટ"
    )
}
