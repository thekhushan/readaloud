package com.readaloud.app.language

class HinglishProcessor {
    fun normalize(input: String): String {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return input
        val words = tokens.map { token ->
            if (!token.any { it.isLetter() }) token else hindiDictionary[token.lowercase()]
                ?: transliterateWord(token, Script.DEVANAGARI)
        }
        return words.joinToString("")
            .replace(" ।", "।")
            .replace(" ?", "?")
            .replace(" !", "!")
            .replace(" ,", ",")
            .trim()
    }

    private fun tokenize(text: String): List<String> {
        val parts = mutableListOf<String>()
        val builder = StringBuilder()
        var letterMode: Boolean? = null
        for (char in text) {
            val currentIsLetter = char.isLetterOrDigit()
            if (letterMode == null || currentIsLetter == letterMode) {
                builder.append(char)
            } else {
                parts += builder.toString()
                builder.clear()
                builder.append(char)
            }
            letterMode = currentIsLetter
        }
        if (builder.isNotEmpty()) parts += builder.toString()
        return parts
    }

    companion object {
        val hindiDictionary = mapOf(
            "aaj" to "आज", "aj" to "आज", "kal" to "कल", "parso" to "परसों",
            "aap" to "आप", "ap" to "आप", "tum" to "तुम", "tu" to "तू",
            "mai" to "मैं", "main" to "मैं", "mein" to "में", "me" to "में",
            "kaha" to "कहाँ", "kahan" to "कहाँ", "kidhar" to "किधर",
            "kyun" to "क्यों", "kyo" to "क्यों", "kya" to "क्या",
            "kaise" to "कैसे", "kaisa" to "कैसा", "kaisi" to "कैसी",
            "hai" to "है", "he" to "है", "hain" to "हैं", "hu" to "हूँ",
            "ho" to "हो", "tha" to "था", "thi" to "थी", "the" to "थे",
            "rahe" to "रहे", "raha" to "रहा", "rahi" to "रही", "jana" to "जाना",
            "jaana" to "जाना", "ja" to "जा", "jao" to "जाओ", "aa" to "आ",
            "aana" to "आना", "aao" to "आओ", "ghar" to "घर", "ghare" to "घर",
            "kar" to "कर", "karo" to "करो", "karna" to "करना", "karte" to "करते",
            "karoge" to "करोगे", "mat" to "मत", "nahi" to "नहीं", "nahin" to "नहीं",
            "haan" to "हाँ", "ha" to "हाँ", "bhai" to "भाई", "dost" to "दोस्त",
            "jaldi" to "जल्दी", "thik" to "ठीक", "theek" to "ठीक",
            "achha" to "अच्छा", "accha" to "अच्छा", "pata" to "पता",
            "school" to "स्कूल", "college" to "कॉलेज", "meeting" to "मीटिंग",
            "office" to "ऑफिस", "practice" to "प्रैक्टिस", "match" to "मैच",
            "cricket" to "क्रिकेट", "mobile" to "मोबाइल", "laptop" to "लैपटॉप",
            "internet" to "इंटरनेट", "virat" to "विराट", "ahmedabad" to "अहमदाबाद",
            "mumbai" to "मुंबई", "delhi" to "दिल्ली", "india" to "इंडिया"
        )
    }
}
