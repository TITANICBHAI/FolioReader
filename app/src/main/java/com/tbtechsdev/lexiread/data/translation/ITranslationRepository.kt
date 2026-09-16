package com.tbtechsdev.lexiread.data.translation

interface ITranslationRepository {
    /**
     * Checks if the ML Kit Hindi translation model is downloaded.
     */
    suspend fun isModelDownloaded(): Boolean

    /**
     * Downloads the Hindi translation model with progress updates from 0.0 to 1.0.
     */
    suspend fun downloadModel(onProgress: (Float) -> Unit)

    /**
     * Deletes the downloaded Hindi translation model from the device.
     */
    suspend fun deleteModel()

    /**
     * Translates English text to Hindi.
     * Returns the translated string, or null if the model is unavailable or on any error.
     */
    suspend fun translate(text: String): String?
}
