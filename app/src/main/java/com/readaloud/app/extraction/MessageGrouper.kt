package com.readaloud.app.extraction

class MessageGrouper {
    fun fromAccessibilityItems(items: List<ExtractedText>): List<String> {
        return items
            .sortedWith(compareBy<ExtractedText> { it.top }.thenBy { it.left }.thenBy { it.order })
            .map { clean(it.text) }
            .filter { isLikelyMessage(it) }
            .let { removeDuplicates(it) }
    }

    fun fromOcrText(rawText: String): List<String> {
        return rawText
            .lineSequence()
            .flatMap { splitVisualLine(it).asSequence() }
            .map { clean(it) }
            .filter { isLikelyMessage(it) }
            .let { removeDuplicates(it) }
    }

    private fun splitVisualLine(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return emptyList()
        return trimmed
            .split(Regex("\\s{3,}|(?<=\\?)\\s+(?=[A-Zઆ-હअ-ह])|(?<=।)\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun removeDuplicates(lines: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<String>()
        for (line in lines) {
            val key = line.lowercase()
                .replace(Regex("\\s+"), " ")
                .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            if (key.isBlank()) continue
            if (seen.add(key)) result += line
        }
        return result
    }

    private fun clean(text: String): String {
        return text
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isLikelyMessage(text: String): Boolean {
        if (text.length < 2 || text.length > 500) return false
        if (text.count { it.isLetter() } < 2) return false
        val lower = text.lowercase().trim()
        if (lower in exactNoise) return false
        if (noiseFragments.any { lower.contains(it) }) return false
        if (timestamp.matches(lower)) return false
        if (dateLine.matches(lower)) return false
        if (phoneStatus.matches(lower)) return false
        if (lower.startsWith("readaloud")) return false
        return true
    }

    private val exactNoise = setOf(
        "send", "back", "search", "settings", "camera", "gallery", "photo", "photos",
        "video", "call", "voice call", "missed voice call", "typing", "online", "today",
        "yesterday", "emoji", "sticker", "attach", "attachment", "more options", "new chat",
        "chats", "updates", "calls", "status", "archive", "mute", "delete", "copy",
        "forward", "reply", "edit", "done", "ok", "cancel", "type a message"
    )

    private val noiseFragments = listOf(
        "double tap to", "tap to", "selected", "unread messages", "message input",
        "record voice", "navigation bar", "system ui", "battery", "wifi", "bluetooth",
        "seen at", "last seen", "end-to-end encrypted", "messages and calls are encrypted",
        "write a message", "enter message", "compose message", "notification"
    )

    private val timestamp = Regex("^\\d{1,2}:\\d{2}(\\s?[ap]m)?$")
    private val dateLine = Regex("^(mon|tue|wed|thu|fri|sat|sun|today|yesterday|\\d{1,2}/\\d{1,2}/\\d{2,4}).*$")
    private val phoneStatus = Regex("^(\\d{1,3}%|\\d+ kb/s|lte|5g|4g|wifi)$")
}
