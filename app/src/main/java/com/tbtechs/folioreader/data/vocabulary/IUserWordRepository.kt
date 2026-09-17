package com.tbtechs.folioreader.data.vocabulary

import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import com.tbtechs.folioreader.domain.model.WordStatus
import kotlinx.coroutines.flow.Flow

interface IUserWordRepository {
    suspend fun getStatus(word: String): WordStatus
    suspend fun markAs(word: String, status: WordStatus, bookPath: String? = null)
    fun getAllByStatus(status: WordStatus): Flow<List<UserWordEntity>>
    fun getAllWords(): Flow<List<UserWordEntity>>
    suspend fun saveWord(word: String, bookPath: String? = null): UserWordEntity
    suspend fun getWord(word: String): UserWordEntity?
    suspend fun deleteWord(word: String)
    suspend fun deleteAll()
}
