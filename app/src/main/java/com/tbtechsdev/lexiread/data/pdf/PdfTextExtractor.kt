package com.tbtechsdev.lexiread.data.pdf

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tbtechsdev.lexiread.domain.model.PdfWord
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PdfTextExtractor"
private const val MIN_WORD_LENGTH = 3

@Singleton
class PdfTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Extracts words with bounding boxes from a given 0-based page index of the PDF.
     * Executes entirely on Dispatchers.IO.
     *
     * Skips words with length < 3, purely numeric strings, and empty text.
     * Returns an empty list if page is empty or unextractable without throwing exceptions.
     */
    suspend fun extractWords(uri: Uri, pageIndex: Int): List<PdfWord> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                return@withContext extractWordsFromStream(inputStream, pageIndex)
            }
            // Fallback to cached file if content stream cannot be reopened
            val cacheFile = File(context.cacheDir, "current_pdf_cache.pdf")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return@withContext extractWordsFromFile(cacheFile, pageIndex)
            }
            Log.w(TAG, "Cannot open stream or cache file for URI $uri at page $pageIndex")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting words from URI $uri on page $pageIndex: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun extractWords(file: File, pageIndex: Int): List<PdfWord> = withContext(Dispatchers.IO) {
        extractWordsFromFile(file, pageIndex)
    }

    suspend fun extractWords(inputStream: InputStream, pageIndex: Int): List<PdfWord> = withContext(Dispatchers.IO) {
        extractWordsFromStream(inputStream, pageIndex)
    }

    private fun extractWordsFromFile(file: File, pageIndex: Int): List<PdfWord> {
        return try {
            PDDocument.load(file).use { document ->
                extractWordsFromDocument(document, pageIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading document from file on page $pageIndex: ${e.message}", e)
            emptyList()
        }
    }

    private fun extractWordsFromStream(inputStream: InputStream, pageIndex: Int): List<PdfWord> {
        return try {
            PDDocument.load(inputStream).use { document ->
                extractWordsFromDocument(document, pageIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading document from stream on page $pageIndex: ${e.message}", e)
            emptyList()
        }
    }

    private fun extractWordsFromDocument(document: PDDocument, pageIndex: Int): List<PdfWord> {
        val totalPages = document.numberOfPages
        if (totalPages <= 0 || pageIndex < 0 || pageIndex >= totalPages) {
            Log.d(TAG, "Page $pageIndex out of range (total pages: $totalPages)")
            return emptyList()
        }

        val extractedWords = mutableListOf<PdfWord>()

        try {
            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                    var currentWordPositions = mutableListOf<TextPosition>()

                    fun flushCurrentWord() {
                        if (currentWordPositions.isNotEmpty()) {
                            val rawText = currentWordPositions.joinToString("") { it.unicode }
                            val normalized = normalizeWord(rawText)

                            // Filtering rules:
                            // 1. Skip words under 3 characters
                            // 2. Skip purely numeric strings ("2024", "123")
                            // 3. Skip if no letters remain
                            val isPurelyNumeric = normalized.all { it.isDigit() }
                            val hasLetters = normalized.any { it.isLetter() }

                            if (normalized.length >= MIN_WORD_LENGTH && !isPurelyNumeric && hasLetters) {
                                val minX = currentWordPositions.minOf { it.xDirAdj }
                                val maxX = currentWordPositions.maxOf { it.xDirAdj + it.widthDirAdj }
                                val minY = currentWordPositions.minOf { it.yDirAdj - it.heightDir }
                                val maxY = currentWordPositions.maxOf { it.yDirAdj }

                                val pdfWord = PdfWord(
                                    text = rawText,
                                    normalizedText = normalized,
                                    page = pageIndex,
                                    x = minX,
                                    y = minY,
                                    width = (maxX - minX).coerceAtLeast(0f),
                                    height = (maxY - minY).coerceAtLeast(0f)
                                )
                                extractedWords.add(pdfWord)
                            }
                            currentWordPositions = mutableListOf()
                        }
                    }

                    for (tp in textPositions) {
                        val unicode = tp.unicode
                        if (unicode.isBlank()) {
                            flushCurrentWord()
                        } else {
                            // Check for significant horizontal gap between glyphs in case spaces were omitted
                            if (currentWordPositions.isNotEmpty()) {
                                val prev = currentWordPositions.last()
                                val gap = tp.xDirAdj - (prev.xDirAdj + prev.widthDirAdj)
                                if (gap > prev.widthDirAdj * 0.9f || Math.abs(tp.yDirAdj - prev.yDirAdj) > prev.heightDir * 0.6f) {
                                    flushCurrentWord()
                                }
                            }
                            currentWordPositions.add(tp)
                        }
                    }
                    flushCurrentWord()
                    super.writeString(text, textPositions)
                }
            }

            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.sortByPosition = true
            stripper.getText(document)

            Log.d(TAG, "Page $pageIndex extracted ${extractedWords.size} words:")
            extractedWords.take(10).forEach { word ->
                Log.d(TAG, "  PdfWord(text='${word.text}', normalized='${word.normalizedText}', page=${word.page}, x=${word.x}, y=${word.y}, w=${word.width}, h=${word.height})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting words from page $pageIndex: ${e.message}", e)
        }

        return extractedWords
    }

    companion object {
        /**
         * Normalizes extracted word text:
         * - Lowercase
         * - All punctuation stripped
         * - Trimmed
         */
        fun normalizeWord(rawText: String): String {
            return rawText
                .lowercase()
                .replace("\\p{P}".toRegex(), "")
                .replace("[`'\"“”‘’«»„.,!?;:()\\[\\]{}<>@#$%^&*_+=~|/\\\\-]".toRegex(), "")
                .trim()
        }
    }
}
