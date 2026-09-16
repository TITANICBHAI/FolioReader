package com.tbtechsdev.lexiread.ui.reader

import android.net.Uri
import com.tbtechsdev.lexiread.data.pdf.PdfType
import com.tbtechsdev.lexiread.domain.model.PdfWord

data class ReaderUiState(
    val isPdfLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val pdfUri: Uri? = null,
    val pdfName: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 1,             // 1-based page number for UI ("X / Y")
    val initialScrollPage: Int = 0,       // 0-based page index to restore scroll
    val errorMessage: String? = null,
    val isReadingMode: Boolean = false,   // True when actively reading a document
    val areBarsVisible: Boolean = true,   // Toggled by tapping anywhere on reader
    val isOcrRunning: Boolean = false,    // True while ML Kit OCR is actively running
    val pdfType: PdfType? = null,
    val extractedWords: Map<Int, List<PdfWord>> = emptyMap()
)

