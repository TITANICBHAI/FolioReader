package com.tbtechs.folioreader.ui.reader

sealed interface SentenceTranslationState {
    data object Idle : SentenceTranslationState
    data object Translating : SentenceTranslationState
    data class Success(val translation: String) : SentenceTranslationState
    data class Error(val message: String = "Translation unavailable") : SentenceTranslationState
}
