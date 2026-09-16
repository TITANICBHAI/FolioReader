package com.tbtechsdev.lexiread.data.pdf

sealed class PdfType {
    data object Text : PdfType() {
        override fun toString(): String = "TEXT_PDF"
    }
    data object Scanned : PdfType() {
        override fun toString(): String = "SCANNED_PDF"
    }
}
