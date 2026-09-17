package com.tbtechsdev.lexiread.data.translation

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor() : ITranslationRepository {

    companion object {
        private const val TAG = "TranslationRepository"
    }

    private val modelManager: RemoteModelManager by lazy {
        RemoteModelManager.getInstance()
    }

    private val translators = ConcurrentHashMap<String, Translator>()

    private fun getModel(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }

    private fun getOrCreateTranslator(languageCode: String): Translator {
        return translators.computeIfAbsent(languageCode) { code ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(code)
                .build()
            Translation.getClient(options)
        }
    }

    override suspend fun isModelDownloaded(): Boolean {
        return isModelDownloaded(TranslateLanguage.HINDI)
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = getModel(languageCode)
            val task = modelManager.isModelDownloaded(model)
            val isDownloaded = Tasks.await(task) == true
            isDownloaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check model download status for $languageCode", e)
            false
        }
    }

    override suspend fun downloadModel(onProgress: (Float) -> Unit) {
        downloadModel(TranslateLanguage.HINDI, onProgress)
    }

    override suspend fun downloadModel(languageCode: String, onProgress: (Float) -> Unit): Unit = withContext(Dispatchers.IO) {
        val conditions = DownloadConditions.Builder().build()
        onProgress(0.05f)

        var currentProgress = 0.10f
        val progressJob = launch {
            while (isActive && currentProgress < 0.90f) {
                delay(200)
                currentProgress += 0.05f
                onProgress(currentProgress.coerceAtMost(0.90f))
            }
        }

        try {
            val client = getOrCreateTranslator(languageCode)
            val downloadTask = client.downloadModelIfNeeded(conditions)
            Tasks.await(downloadTask)
            progressJob.cancel()
            onProgress(1.0f)
            Log.d(TAG, "Translation model for $languageCode downloaded successfully")
        } catch (e: Exception) {
            progressJob.cancel()
            Log.e(TAG, "Error downloading model for $languageCode", e)
            throw e
        }
    }

    override suspend fun deleteModel() {
        deleteModel(TranslateLanguage.HINDI)
    }

    override suspend fun deleteModel(languageCode: String): Unit = withContext(Dispatchers.IO) {
        try {
            val removed = translators.remove(languageCode)
            removed?.close()
            val model = getModel(languageCode)
            val deleteTask = modelManager.deleteDownloadedModel(model)
            Tasks.await(deleteTask)
            Log.d(TAG, "Translation model for $languageCode deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting translation model for $languageCode", e)
            throw e
        }
    }

    override suspend fun translate(text: String): String? {
        return translate(text, TranslateLanguage.HINDI)
    }

    override suspend fun translate(text: String, targetLanguageCode: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        try {
            val isDownloaded = isModelDownloaded(targetLanguageCode)
            if (!isDownloaded) {
                Log.w(TAG, "Cannot translate: model for $targetLanguageCode is not downloaded")
                return@withContext null
            }
            val client = getOrCreateTranslator(targetLanguageCode)
            val task = client.translate(text)
            val result = Tasks.await(task)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Translation error for text ($targetLanguageCode): $text", e)
            null
        }
    }
}
