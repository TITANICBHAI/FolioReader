package com.tbtechs.folioreader.data.dictionary

import com.tbtechs.folioreader.data.db.entities.CachedDefinition

interface IDictionaryRepository {
    suspend fun getDefinition(word: String): CachedDefinition?
    fun isCommonWord(word: String): Boolean
    fun getFrequencyRank(word: String): Int
    fun getFrequency(word: String): Int = getFrequencyRank(word)
    suspend fun lookupWord(raw: String): CachedDefinition? = getDefinition(raw)
    suspend fun getAllCachedDefinitions(): List<CachedDefinition> = emptyList()
}
