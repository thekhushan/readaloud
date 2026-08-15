package com.readaloud.app.extraction

data class ExtractedText(
    val text: String,
    val top: Int,
    val left: Int,
    val order: Int,
    val className: String? = null,
    val viewId: String? = null
)
