package com.tbtechs.folioreader.data.translation

import com.google.mlkit.nl.translate.TranslateLanguage

interface ITranslationRepository {
    /**
     * Checks if the ML Kit translation model is downloaded for the default (Hindi) language.
     */
    suspend fun isModelDownloaded(): Boolean = isModelDownloaded(TranslateLanguage.HINDI)

    /**
     * Checks if the ML Kit translation model is downloaded for the given language code.
     */
    suspend fun isModelDownloaded(languageCode: String): Boolean

    /**
     * Downloads the translation model with progress updates from 0.0 to 1.0.
     */
    suspend fun downloadModel(onProgress: (Float) -> Unit): Unit =
        downloadModel(TranslateLanguage.HINDI, onProgress)

    /**
     * Downloads the translation model for the given language code.
     */
    suspend fun downloadModel(languageCode: String, onProgress: (Float) -> Unit)

    /**
     * Deletes the downloaded translation model from the device.
     */
    suspend fun deleteModel(): Unit =
        deleteModel(TranslateLanguage.HINDI)

    /**
     * Deletes the downloaded translation model for the given language code.
     */
    suspend fun deleteModel(languageCode: String)

    /**
     * Translates English text to the default (Hindi) language.
     */
    suspend fun translate(text: String): String? = translate(text, TranslateLanguage.HINDI)

    /**
     * Translates English text to the specified target language code.
     * Returns the translated string, or null if the model is unavailable or on any error.
     */
    suspend fun translate(text: String, targetLanguageCode: String): String?
}


