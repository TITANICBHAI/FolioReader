package com.tbtechs.folioreader.data.pdf

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PdfTypeDetector"
private const val TEXT_CHAR_THRESHOLD = 50

@Singleton
class PdfTypeDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Detects whether a PDF is text-based (TEXT_PDF) or scanned/image-based (SCANNED_PDF).
     * Extracts text from page 1 using PdfBox.
     * If more than 50 non-whitespace characters are found -> Text, otherwise -> Scanned.
     */
    suspend fun detectType(uri: Uri): PdfType = withContext(Dispatchers.IO) {
        try {
            // Try loading from input stream
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                return@withContext detectFromStream(inputStream)
            }
            // Fallback to cache file if stream is not directly openable
            val cacheFile = File(context.cacheDir, "current_pdf_cache.pdf")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return@withContext detectFromFile(cacheFile)
            }
            Log.w(TAG, "Unable to open input stream or cached file for URI $uri. Defaulting to SCANNED_PDF.")
            PdfType.Scanned
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting PDF type for URI $uri: ${e.message}", e)
            PdfType.Scanned
        }
    }

    suspend fun detectType(file: File): PdfType = withContext(Dispatchers.IO) {
        detectFromFile(file)
    }

    suspend fun detectType(inputStream: InputStream): PdfType = withContext(Dispatchers.IO) {
        detectFromStream(inputStream)
    }

    private fun detectFromFile(file: File): PdfType {
        return try {
            PDDocument.load(file).use { document ->
                evaluateDocument(document)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PDDocument from file: ${e.message}", e)
            PdfType.Scanned
        }
    }

    private fun detectFromStream(inputStream: InputStream): PdfType {
        return try {
            PDDocument.load(inputStream).use { document ->
                evaluateDocument(document)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PDDocument from stream: ${e.message}", e)
            PdfType.Scanned
        }
    }

    private fun evaluateDocument(document: PDDocument): PdfType {
        if (document.numberOfPages <= 0) {
            Log.i(TAG, "PDF has 0 pages -> SCANNED_PDF")
            return PdfType.Scanned
        }

        return try {
            val stripper = PDFTextStripper().apply {
                startPage = 1
                endPage = 1
                sortByPosition = true
            }

            val page1Text = stripper.getText(document) ?: ""
            val nonWhitespaceCount = page1Text.count { !it.isWhitespace() }

            Log.d(TAG, "Page 1 non-whitespace character count: $nonWhitespaceCount")

            if (nonWhitespaceCount > TEXT_CHAR_THRESHOLD) {
                Log.i(TAG, "Classified as TEXT_PDF (characters: $nonWhitespaceCount > $TEXT_CHAR_THRESHOLD)")
                PdfType.Text
            } else {
                Log.i(TAG, "Classified as SCANNED_PDF (characters: $nonWhitespaceCount <= $TEXT_CHAR_THRESHOLD)")
                PdfType.Scanned
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stripping text from page 1: ${e.message}", e)
            PdfType.Scanned
        }
    }
}
