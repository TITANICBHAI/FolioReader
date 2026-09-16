package com.tbtechsdev.lexiread.data.classroom

import com.tbtechsdev.lexiread.domain.model.PdfWord

class WordMatchEngine {
    private var pdfWords: List<PdfWord> = emptyList()
    var cursorIndex: Int = 0
        private set
    private val windowSize = 40

    val words: List<PdfWord>
        get() = pdfWords

    fun getWord(index: Int): PdfWord? = pdfWords.getOrNull(index)

    fun loadWords(words: List<PdfWord>) {
        pdfWords = words
        cursorIndex = 0
    }

    // Returns new cursor index if a match is found, null otherwise.
    fun processSpokenText(spokenText: String): Int? {
        val tokens = spokenText.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val windowEnd = minOf(cursorIndex + windowSize, pdfWords.size)
        if (cursorIndex >= windowEnd) return null
        val window = pdfWords.subList(cursorIndex, windowEnd)

        // 1. Look for any token that matches forward in the window (idx > 0)
        // This ensures speech recognizer results containing previous words advance properly.
        for (token in tokens) {
            if (token.length < 2 && token != "a" && token != "i") continue
            for (i in 1 until window.size) {
                val norm = window[i].normalizedText
                val isMatch = norm == token ||
                    (token.length >= 3 && (norm.startsWith(token) || token.startsWith(norm)))
                if (isMatch) {
                    cursorIndex += i
                    return cursorIndex
                }
            }
        }

        // 2. If no forward match found, check if the current word matches any token
        for (token in tokens) {
            if (token.length < 2 && token != "a" && token != "i") continue
            val norm = window[0].normalizedText
            val isMatch = norm == token ||
                (token.length >= 3 && (norm.startsWith(token) || token.startsWith(norm)))
            if (isMatch) {
                return cursorIndex
            }
        }

        return null
    }

    fun setCursor(index: Int) {
        cursorIndex = index.coerceIn(0, maxOf(pdfWords.size - 1, 0))
    }

    fun advance(by: Int = 1) {
        cursorIndex = minOf(cursorIndex + by, maxOf(pdfWords.size - 1, 0))
    }

    fun back(by: Int = 1) {
        cursorIndex = maxOf(cursorIndex - by, 0)
    }
}
