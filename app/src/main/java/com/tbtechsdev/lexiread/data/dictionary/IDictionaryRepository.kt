package com.tbtechsdev.lexiread.data.dictionary

import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition

interface IDictionaryRepository {
    suspend fun getDefinition(word: String): CachedDefinition?
    fun isCommonWord(word: String): Boolean
    fun getFrequencyRank(word: String): Int
    fun getFrequency(word: String): Int = getFrequencyRank(word)
    suspend fun lookupWord(raw: String): CachedDefinition? = getDefinition(raw)
}
