package com.tbtechs.folioreader.data.ai

import kotlinx.coroutines.flow.Flow

interface IGeminiRepository {
    fun isConfigured(): Boolean
    fun getStoredApiKey(): String?
    suspend fun saveAndTestKey(key: String): Result<Boolean>
    fun removeKey()
    fun explainWord(word: String, context: String): Flow<String>
    fun explainSentence(sentence: String): Flow<String>
    fun simplifyParagraph(text: String): Flow<String>
}
