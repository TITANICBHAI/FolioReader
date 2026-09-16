package com.tbtechsdev.lexiread

import com.tbtechsdev.lexiread.data.ai.IGeminiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeGeminiRepository(
    var configured: Boolean = false,
    var apiKey: String? = null,
    var validateResult: Boolean = true,
    var shouldError: Boolean = false
) : IGeminiRepository {

    override fun isConfigured(): Boolean = configured

    override fun getStoredApiKey(): String? = apiKey

    override suspend fun saveAndTestKey(key: String): Result<Boolean> {
        return if (validateResult) {
            apiKey = key
            configured = true
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid key"))
        }
    }

    override fun removeKey() {
        apiKey = null
        configured = false
    }

    override fun explainWord(word: String, context: String): Flow<String> = flow {
        if (shouldError) {
            emit("Error: Mock network failure")
        } else {
            emit("The word ")
            emit("\"$word\" means ")
            emit("something significant in this context.")
        }
    }

    override fun explainSentence(sentence: String): Flow<String> = flow {
        if (shouldError) {
            emit("Error: Mock network failure")
        } else {
            emit("This sentence explains: ")
            emit(sentence.take(30))
            emit("...")
        }
    }

    override fun simplifyParagraph(text: String): Flow<String> = flow {
        if (shouldError) {
            emit("Error: Mock network failure")
        } else {
            emit("Simplified version: ")
            emit(text.take(40))
            emit(" in simpler terms.")
        }
    }
}
