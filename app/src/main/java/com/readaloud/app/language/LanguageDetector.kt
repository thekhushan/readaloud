package com.readaloud.app.language

import com.readaloud.app.model.MessageLanguage
import java.text.Normalizer
import kotlin.math.max

class LanguageDetector {
    fun detect(text: String): MessageLanguage {
        val clean = Normalizer.normalize(text, Normalizer.Form.NFC).trim()
        if (clean.isBlank()) return MessageLanguage.UNKNOWN

        val devanagariCount = clean.count { it.code in 0x0900..0x097F }
        val gujaratiCount = clean.count { it.code in 0x0A80..0x0AFF }
        val latinCount = clean.count { it.isLetter() && it.code <= 0x024F }

        if (gujaratiCount > 0 && latinCount > 0) return MessageLanguage.MIXED_GUJARATI_ENGLISH
        if (devanagariCount > 0 && latinCount > 0) return MessageLanguage.MIXED_HINDI_ENGLISH
        if (gujaratiCount > 0) return MessageLanguage.GUJARATI
        if (devanagariCount > 0) return MessageLanguage.HINDI

        val words = clean.lowercase()
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return MessageLanguage.UNKNOWN

        val hindiScore = words.sumOf { romanHindiSignals[it] ?: 0 }
        val gujaratiScore = words.sumOf { romanGujaratiSignals[it] ?: 0 }
        val englishScore = words.count { it in commonEnglishWords }
        val indianLoanScore = words.count { it in indianLoanWords }

        val maxIndic = max(hindiScore, gujaratiScore)
        if (gujaratiScore >= 4 && gujaratiScore > hindiScore) return MessageLanguage.ROMAN_GUJARATI
        if (hindiScore >= 4 && hindiScore > gujaratiScore) return MessageLanguage.HINGLISH
        if (gujaratiScore >= 3 && gujaratiScore == hindiScore && words.any { it in gujaratiStrongSignals }) {
            return MessageLanguage.ROMAN_GUJARATI
        }
        if (maxIndic >= 3 && indianLoanScore > 0) {
            return if (gujaratiScore > hindiScore) MessageLanguage.ROMAN_GUJARATI else MessageLanguage.HINGLISH
        }
        if (englishScore >= maxIndic) return MessageLanguage.ENGLISH
        return if (gujaratiScore > hindiScore) MessageLanguage.ROMAN_GUJARATI else MessageLanguage.HINGLISH
    }

    private val romanHindiSignals = mapOf(
        "aaj" to 2, "kal" to 2, "parso" to 2, "hai" to 2, "he" to 2, "hain" to 2,
        "ho" to 2, "hu" to 1, "mein" to 2, "mai" to 2, "main" to 2, "kaha" to 3,
        "kahan" to 3, "kidhar" to 2, "kyun" to 2, "kya" to 2, "kaise" to 3,
        "kaisa" to 3, "kaisi" to 3, "aap" to 3, "ap" to 3, "tum" to 2,
        "tu" to 1, "ja" to 2, "jana" to 2, "jaana" to 2, "rahe" to 3,
        "raha" to 3, "rahi" to 3, "ghar" to 2, "ghare" to 1, "school" to 1,
        "office" to 1, "meeting" to 1, "cricket" to 1, "match" to 1, "practice" to 1,
        "chalo" to 2, "mat" to 1, "nahi" to 2, "nahin" to 2, "ha" to 1,
        "haan" to 2, "bhai" to 2, "dost" to 2, "jaldi" to 2, "thik" to 2,
        "theek" to 2, "achha" to 2, "accha" to 2, "pata" to 2, "kar" to 1,
        "karo" to 2, "karna" to 2, "aa" to 1, "aana" to 2, "aaraha" to 2,
        "aa raha" to 2
    )

    private val romanGujaratiSignals = mapOf(
        "tame" to 4, "kem" to 4, "cho" to 4, "chho" to 4, "su" to 3,
        "shu" to 3, "karo" to 2, "karu" to 2, "chu" to 4, "chhu" to 4,
        "hu" to 3, "ghare" to 4, "ghar" to 1, "kya" to 2, "kyan" to 3,
        "majama" to 4, "maja" to 2, "aaje" to 3, "kaale" to 3, "kal" to 1,
        "aavso" to 3, "aavjo" to 3, "jao" to 2, "jau" to 2, "gayo" to 2,
        "gai" to 2, "raho" to 2, "rahi" to 1, "rahya" to 3, "office" to 1,
        "school" to 1, "meeting" to 1, "cricket" to 1, "match" to 1, "ma" to 2,
        "maa" to 2, "ne" to 1, "ane" to 2, "pan" to 2, "haji" to 2
    )

    private val gujaratiStrongSignals = setOf(
        "tame", "kem", "cho", "chho", "shu", "su", "chu", "chhu", "ghare", "majama"
    )

    private val commonEnglishWords = setOf(
        "the", "is", "are", "am", "was", "were", "where", "what", "when", "why",
        "how", "you", "your", "we", "they", "going", "come", "coming", "today",
        "tomorrow", "school", "office", "meeting", "practice", "match", "cricket"
    )

    private val indianLoanWords = setOf(
        "cricket", "school", "college", "meeting", "office", "practice", "match",
        "mobile", "laptop", "internet", "team", "bus", "train", "class"
    )
}
