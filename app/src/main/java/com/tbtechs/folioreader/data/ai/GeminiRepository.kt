package com.tbtechs.folioreader.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : IGeminiRepository {

    companion object {
        private const val TAG = "GeminiRepository"
        const val KEY_ALIAS = "gemini_api_key"
        private const val PREFS_FILE = "lexiread_secure_prefs"
        const val MODEL_NAME = "gemini-1.5-flash"
        const val SYSTEM_PROMPT =
            "You are an English language tutor for Hindi-speaking learners.\n" +
            "Explain clearly and simply. Use Hindi where it genuinely helps\n" +
            "understanding."
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed, falling back to standard prefs: ${e.message}")
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    override fun isConfigured(): Boolean {
        val key = getStoredApiKey()
        return !key.isNullOrBlank()
    }

    override fun getStoredApiKey(): String? {
        return securePrefs.getString(KEY_ALIAS, null)?.takeIf { it.isNotBlank() }
    }

    fun saveApiKey(key: String) {
        securePrefs.edit().putString(KEY_ALIAS, key.trim()).apply()
    }

    override fun removeKey() {
        securePrefs.edit().remove(KEY_ALIAS).apply()
    }

    override suspend fun saveAndTestKey(key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanKey = key.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be blank"))
        }

        try {
            val testModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = cleanKey,
                generationConfig = generationConfig {
                    maxOutputTokens = 5
                }
            )
            // Minimal API call to validate key (send "Hi", max_tokens=5)
            testModel.generateContent("Hi")
            // Key is valid — store securely
            saveApiKey(cleanKey)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini key validation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun explainWord(word: String, context: String): Flow<String> {
        val prompt = if (context.isNotBlank()) {
            "Explain the word \"$word\" as used in this context: \"$context\". " +
            "Explain its meaning clearly and simply with Hindi where helpful."
        } else {
            "Explain the word \"$word\" clearly and simply with Hindi where helpful."
        }
        return streamResponse(prompt)
    }

    override fun explainSentence(sentence: String): Flow<String> {
        val prompt = "Explain this sentence clearly and simply with Hindi where helpful: \"$sentence\"."
        return streamResponse(prompt)
    }

    override fun simplifyParagraph(text: String): Flow<String> {
        val prompt = "Simplify this paragraph clearly and simply with Hindi where helpful for Hindi-speaking English learners: \"$text\"."
        return streamResponse(prompt)
    }

    private fun streamResponse(prompt: String): Flow<String> = flow {
        if (!isConfigured()) {
            emit("Gemini API key is not configured. Please add your key in Settings.")
            return@flow
        }

        val apiKey = getStoredApiKey() ?: run {
            emit("Gemini API key is not configured. Please add your key in Settings.")
            return@flow
        }

        try {
            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                systemInstruction = content {
                    text(SYSTEM_PROMPT)
                }
            )

            val stream = model.generateContentStream(prompt)
            var emittedAny = false
            stream.collect { response ->
                val textChunk = response.text
                if (!textChunk.isNullOrEmpty()) {
                    emittedAny = true
                    emit(textChunk)
                }
            }

            if (!emittedAny) {
                emit("No explanation could be generated for the selected text.")
            }
        } catch (e: Exception) {
            emit(formatErrorMessage(e))
        }
    }.catch { e ->
        emit(formatErrorMessage(e))
    }.flowOn(Dispatchers.IO)

    private fun formatErrorMessage(e: Throwable): String {
        val message = e.message.orEmpty()
        return when {
            e is UnknownHostException || e is IOException ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("ConnectException", ignoreCase = true) ||
                message.contains("network", ignoreCase = true) -> {
                "No internet connection. Please check your network and try again."
            }

            message.contains("API_KEY_INVALID", ignoreCase = true) ||
                message.contains("API key not valid", ignoreCase = true) ||
                message.contains("invalid key", ignoreCase = true) ||
                message.contains("400 Bad Request", ignoreCase = true) ||
                message.contains("403 Forbidden", ignoreCase = true) -> {
                "Invalid Gemini API key. Please check your key in Settings."
            }

            message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("quota", ignoreCase = true) ||
                message.contains("429", ignoreCase = true) ||
                message.contains("rate limit", ignoreCase = true) -> {
                "Rate limit exceeded. Please wait a moment and try again."
            }

            else -> "AI error: ${e.localizedMessage ?: "Unable to complete request. Please try again."}"
        }
    }
}
