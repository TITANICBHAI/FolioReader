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

    private val hindiModel: TranslateRemoteModel by lazy {
        TranslateRemoteModel.Builder(TranslateLanguage.HINDI).build()
    }

    private val translatorOptions by lazy {
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HINDI)
            .build()
    }

    @Volatile
    private var translator: Translator? = null

    private fun getOrCreateTranslator(): Translator {
        val current = translator
        if (current != null) return current
        return synchronized(this) {
            translator ?: Translation.getClient(translatorOptions).also { translator = it }
        }
    }

    override suspend fun isModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val task = modelManager.isModelDownloaded(hindiModel)
            val isDownloaded = Tasks.await(task) == true
            isDownloaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check model download status", e)
            false
        }
    }

    override suspend fun downloadModel(onProgress: (Float) -> Unit): Unit = withContext(Dispatchers.IO) {
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
            val client = getOrCreateTranslator()
            val downloadTask = client.downloadModelIfNeeded(conditions)
            Tasks.await(downloadTask)
            progressJob.cancel()
            onProgress(1.0f)
            Log.d(TAG, "Hindi model downloaded successfully")
        } catch (e: Exception) {
            progressJob.cancel()
            Log.e(TAG, "Error downloading Hindi model", e)
            throw e
        }
    }

    override suspend fun deleteModel(): Unit = withContext(Dispatchers.IO) {
        try {
            synchronized(this@TranslationRepository) {
                translator?.close()
                translator = null
            }
            val deleteTask = modelManager.deleteDownloadedModel(hindiModel)
            Tasks.await(deleteTask)
            Log.d(TAG, "Hindi model deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting Hindi model", e)
            throw e
        }
    }

    override suspend fun translate(text: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        try {
            val isDownloaded = isModelDownloaded()
            if (!isDownloaded) {
                Log.w(TAG, "Cannot translate: model is not downloaded")
                return@withContext null
            }
            val client = getOrCreateTranslator()
            val task = client.translate(text)
            val result = Tasks.await(task)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Translation error for text: $text", e)
            null
        }
    }
}
