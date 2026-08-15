package com.readaloud.app.language

enum class Script {
    DEVANAGARI,
    GUJARATI
}

fun transliterateWord(raw: String, script: Script): String {
    val lower = raw.lowercase()
    if (lower.any { !it.isLetter() }) return raw
    if (lower.length <= 1) return raw

    val vowels = when (script) {
        Script.DEVANAGARI -> Vowels("अ", "आ", "इ", "ई", "उ", "ऊ", "ए", "ऐ", "ओ", "औ", "अं")
        Script.GUJARATI -> Vowels("અ", "આ", "ઇ", "ઈ", "ઉ", "ઊ", "એ", "ઐ", "ઓ", "ઔ", "અં")
    }
    val marks = when (script) {
        Script.DEVANAGARI -> Marks("", "ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं", "्")
        Script.GUJARATI -> Marks("", "ા", "િ", "ી", "ુ", "ૂ", "ે", "ૈ", "ો", "ૌ", "ં", "્")
    }
    val consonants = when (script) {
        Script.DEVANAGARI -> devanagariConsonants
        Script.GUJARATI -> gujaratiConsonants
    }

    val out = StringBuilder()
    var i = 0
    var pendingConsonant = false

    fun consumeVowel(): Pair<String, Int>? {
        val candidates = listOf("aa", "ai", "au", "ee", "ii", "oo", "ou", "a", "i", "u", "e", "o")
        for (candidate in candidates) {
            if (lower.startsWith(candidate, i)) {
                val independent = when (candidate) {
                    "aa" -> vowels.aa
                    "ai" -> vowels.ai
                    "au", "ou" -> vowels.au
                    "ee", "ii" -> vowels.ii
                    "oo" -> vowels.uu
                    "a" -> vowels.a
                    "i" -> vowels.i
                    "u" -> vowels.u
                    "e" -> vowels.e
                    "o" -> vowels.o
                    else -> vowels.a
                }
                val mark = when (candidate) {
                    "aa" -> marks.aa
                    "ai" -> marks.ai
                    "au", "ou" -> marks.au
                    "ee", "ii" -> marks.ii
                    "oo" -> marks.uu
                    "a" -> marks.a
                    "i" -> marks.i
                    "u" -> marks.u
                    "e" -> marks.e
                    "o" -> marks.o
                    else -> marks.a
                }
                val text = if (pendingConsonant) mark else independent
                return text to candidate.length
            }
        }
        return null
    }

    while (i < lower.length) {
        val vowel = consumeVowel()
        if (vowel != null) {
            out.append(vowel.first)
            pendingConsonant = false
            i += vowel.second
            continue
        }

        val cluster = consonants.keys.firstOrNull { lower.startsWith(it, i) }
        if (cluster != null) {
            if (pendingConsonant) out.append(marks.virama)
            out.append(consonants.getValue(cluster))
            pendingConsonant = true
            i += cluster.length
            continue
        }

        if (pendingConsonant) {
            pendingConsonant = false
        }
        out.append(raw[i])
        i++
    }
    return out.toString()
}

private data class Vowels(
    val a: String,
    val aa: String,
    val i: String,
    val ii: String,
    val u: String,
    val uu: String,
    val e: String,
    val ai: String,
    val o: String,
    val au: String,
    val anusvara: String
)

private data class Marks(
    val a: String,
    val aa: String,
    val i: String,
    val ii: String,
    val u: String,
    val uu: String,
    val e: String,
    val ai: String,
    val o: String,
    val au: String,
    val anusvara: String,
    val virama: String
)

private val devanagariConsonants = linkedMapOf(
    "ksh" to "क्ष", "gy" to "ज्ञ", "jny" to "ज्ञ", "chh" to "छ", "kh" to "ख",
    "gh" to "घ", "ch" to "च", "jh" to "झ", "th" to "थ", "dh" to "ध",
    "ph" to "फ", "bh" to "भ", "sh" to "श", "gn" to "ज्ञ", "tr" to "त्र",
    "k" to "क", "g" to "ग", "c" to "क", "j" to "ज", "t" to "त",
    "d" to "द", "n" to "न", "p" to "प", "b" to "ब", "m" to "म",
    "y" to "य", "r" to "र", "l" to "ल", "v" to "व", "w" to "व",
    "s" to "स", "h" to "ह", "f" to "फ", "z" to "ज"
)

private val gujaratiConsonants = linkedMapOf(
    "ksh" to "ક્ષ", "gy" to "જ્ઞ", "jny" to "જ્ઞ", "chh" to "છ", "kh" to "ખ",
    "gh" to "ઘ", "ch" to "ચ", "jh" to "ઝ", "th" to "થ", "dh" to "ધ",
    "ph" to "ફ", "bh" to "ભ", "sh" to "શ", "gn" to "જ્ઞ", "tr" to "ત્ર",
    "k" to "ક", "g" to "ગ", "c" to "ક", "j" to "જ", "t" to "ત",
    "d" to "દ", "n" to "ન", "p" to "પ", "b" to "બ", "m" to "મ",
    "y" to "ય", "r" to "ર", "l" to "લ", "v" to "વ", "w" to "વ",
    "s" to "સ", "h" to "હ", "f" to "ફ", "z" to "ઝ"
)
