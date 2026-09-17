package com.tbtechs.folioreader.domain.model

/**
 * Unified word representation — both Text PDF and Scanned OCR PDF produce this model.
 */
data class PdfWord(
    val text: String,
    val normalizedText: String = text.lowercase().trim().trim { !it.isLetterOrDigit() },
    val page: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)
