package com.tbtechsdev.lexiread.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.tbtechsdev.lexiread.domain.model.PdfWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "PdfOcrExtractor"
private const val MIN_WORD_LENGTH = 3

@Singleton
class PdfOcrExtractor @Inject constructor(
    private val textRecognizer: TextRecognizer
) {

    /**
     * Extracts words from a scanned/image PDF page using ML Kit Text Recognition.
     * Renders the page to a Bitmap at 2× display resolution for enhanced OCR accuracy,
     * processes the image through the TextRecognizer (Latin script),
     * converts each Element into a PdfWord scaled to the page's display coordinate space,
     * and recycles the high-resolution Bitmap immediately after completion.
     */
    suspend fun extractWords(page: PdfRenderer.Page, pageIndex: Int = page.index): List<PdfWord> = withContext(Dispatchers.IO) {
        val renderWidth = (page.width * 2).coerceAtLeast(1)
        val renderHeight = (page.height * 2).coerceAtLeast(1)

        val bitmap: Bitmap = try {
            Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        } catch (oom: OutOfMemoryError) {
            Log.w(TAG, "OOM rendering 2x bitmap for page $pageIndex, falling back to 1x: ${oom.message}")
            System.gc()
            val fallbackWidth = page.width.coerceAtLeast(1)
            val fallbackHeight = page.height.coerceAtLeast(1)
            Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }

        extractWordsFromBitmap(
            bitmap = bitmap,
            pageIndex = pageIndex,
            pageWidth = page.width,
            pageHeight = page.height
        )
    }

    /**
     * Internal extraction method given a Bitmap, scaling coordinates to (pageWidth x pageHeight).
     * Guarantees Bitmap is recycled in finally.
     */
    suspend fun extractWordsFromBitmap(
        bitmap: Bitmap,
        pageIndex: Int,
        pageWidth: Int,
        pageHeight: Int
    ): List<PdfWord> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = suspendCancellableCoroutine { continuation ->
                textRecognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                    .addOnCanceledListener {
                        continuation.cancel()
                    }
            }

            val scaleX = if (bitmap.width > 0) pageWidth.toFloat() / bitmap.width.toFloat() else 1f
            val scaleY = if (bitmap.height > 0) pageHeight.toFloat() / bitmap.height.toFloat() else 1f

            val words = mutableListOf<PdfWord>()

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val rawText = element.text
                        val normalized = PdfTextExtractor.normalizeWord(rawText)

                        val isPurelyNumeric = normalized.all { it.isDigit() }
                        val hasLetters = normalized.any { it.isLetter() }

                        if (normalized.length >= MIN_WORD_LENGTH && !isPurelyNumeric && hasLetters) {
                            val box = element.boundingBox
                            val x = (box?.left?.toFloat() ?: 0f) * scaleX
                            val y = (box?.top?.toFloat() ?: 0f) * scaleY
                            val width = ((box?.width()?.toFloat() ?: 0f) * scaleX).coerceAtLeast(0f)
                            val height = ((box?.height()?.toFloat() ?: 0f) * scaleY).coerceAtLeast(0f)

                            val pdfWord = PdfWord(
                                text = rawText,
                                normalizedText = normalized,
                                page = pageIndex,
                                x = x,
                                y = y,
                                width = width,
                                height = height
                            )
                            words.add(pdfWord)
                        }
                    }
                }
            }

            Log.d(TAG, "Page $pageIndex OCR extracted ${words.size} words:")
            words.take(10).forEach { word ->
                Log.d(TAG, "  PdfWord(text='${word.text}', normalized='${word.normalizedText}', page=${word.page}, x=${word.x}, y=${word.y}, w=${word.width}, h=${word.height})")
            }

            words
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
