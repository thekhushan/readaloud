package com.readaloud.app.language

class RomanGujaratiProcessor {
    fun normalize(input: String): String {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return input
        return tokens.joinToString("") { token ->
            if (!token.any { it.isLetter() }) token else gujaratiDictionary[token.lowercase()]
                ?: transliterateWord(token, Script.GUJARATI)
        }
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
        val gujaratiDictionary = mapOf(
            "tame" to "તમે", "tu" to "તું", "hu" to "હું", "kem" to "કેમ",
            "cho" to "છો", "chho" to "છો", "chu" to "છું", "chhu" to "છું",
            "su" to "શું", "shu" to "શું", "kya" to "ક્યાં", "kyan" to "ક્યાં",
            "ghare" to "ઘરે", "ghar" to "ઘર", "ma" to "માં", "maa" to "માં",
            "karo" to "કરો", "karu" to "કરું", "karvu" to "કરવું",
            "aaje" to "આજે", "kaale" to "કાલે", "kal" to "કાલે",
            "majama" to "મજામાં", "maja" to "મજા", "aavjo" to "આવજો",
            "aavso" to "આવશો", "jao" to "જાઓ", "jau" to "જાઉં",
            "gayo" to "ગયો", "gai" to "ગઈ", "rahya" to "રહ્યા",
            "raho" to "રહો", "rahi" to "રહી", "ane" to "અને", "ne" to "ને",
            "pan" to "પણ", "haji" to "હજુ", "office" to "ઓફિસ",
            "school" to "સ્કૂલ", "college" to "કોલેજ", "meeting" to "મીટિંગ",
            "practice" to "પ્રેક્ટિસ", "match" to "મેચ", "cricket" to "ક્રિકેટ",
            "mobile" to "મોબાઇલ", "laptop" to "લેપટોપ", "internet" to "ઇન્ટરનેટ",
            "ahmedabad" to "અમદાવાદ", "surat" to "સુરત", "baroda" to "વડોદરા",
            "vadodara" to "વડોદરા"
        )
    }
}
