package com.tbtechs.folioreader.data.vocabulary

import android.util.Log
import com.tbtechs.folioreader.data.db.dao.UserWordDao
import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import com.tbtechs.folioreader.domain.model.WordStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserWordRepository"

@Singleton
open class UserWordRepository @Inject constructor(
    private val userWordDao: UserWordDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IUserWordRepository {

    override suspend fun getStatus(word: String): WordStatus = withContext(ioDispatcher) {
        val key = normalizeWord(word)
        if (key.isEmpty()) return@withContext WordStatus.UNKNOWN
        val entity = userWordDao.getByWord(key) ?: return@withContext WordStatus.UNKNOWN
        try {
            WordStatus.valueOf(entity.status)
        } catch (e: Exception) {
            WordStatus.UNKNOWN
        }
    }

    override suspend fun markAs(word: String, status: WordStatus, bookPath: String?) = withContext(ioDispatcher) {
        val key = normalizeWord(word)
        if (key.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val existing = userWordDao.getByWord(key)
        val entity = if (existing != null) {
            existing.copy(
                status = status.name,
                lastSeenMs = now,
                sourceBookPath = bookPath ?: existing.sourceBookPath
            )
        } else {
            UserWordEntity(
                word = key,
                status = status.name,
                lookupCount = 0,
                firstSeenMs = now,
                lastSeenMs = now,
                sourceBookPath = bookPath
            )
        }
        userWordDao.upsert(entity)
        Log.d(TAG, "Marked '$key' as $status (lookupCount: ${entity.lookupCount})")
    }

    override fun getAllByStatus(status: WordStatus): Flow<List<UserWordEntity>> {
        return if (status == WordStatus.MASTERED || status == WordStatus.KNOWN) {
            userWordDao.getAllByStatuses(listOf(WordStatus.MASTERED.name, WordStatus.KNOWN.name))
        } else {
            userWordDao.getAllByStatus(status.name)
        }
    }

    suspend fun getWordsListByStatuses(statuses: List<WordStatus>): List<UserWordEntity> = withContext(ioDispatcher) {
        val names = statuses.flatMap {
            if (it == WordStatus.MASTERED || it == WordStatus.KNOWN) {
                listOf(WordStatus.MASTERED.name, WordStatus.KNOWN.name)
            } else {
                listOf(it.name)
            }
        }.distinct()
        if (names.isEmpty()) userWordDao.getAllList() else userWordDao.getListByStatuses(names)
    }

    suspend fun getAllWordsList(): List<UserWordEntity> = withContext(ioDispatcher) {
        userWordDao.getAllList()
    }

    override fun getAllWords(): Flow<List<UserWordEntity>> {
        return userWordDao.getAll()
    }

    override suspend fun saveWord(word: String, bookPath: String?): UserWordEntity = withContext(ioDispatcher) {
        val key = normalizeWord(word)
        val now = System.currentTimeMillis()
        val existing = userWordDao.getByWord(key)
        val entity = if (existing != null) {
            existing.copy(
                status = WordStatus.LEARNING.name,
                lookupCount = existing.lookupCount + 1,
                lastSeenMs = now,
                sourceBookPath = bookPath ?: existing.sourceBookPath
            )
        } else {
            UserWordEntity(
                word = key,
                status = WordStatus.LEARNING.name,
                lookupCount = 1,
                firstSeenMs = now,
                lastSeenMs = now,
                sourceBookPath = bookPath
            )
        }
        userWordDao.upsert(entity)
        Log.d(TAG, "Saved word '$key' -> LEARNING, lookupCount: ${entity.lookupCount}")
        entity
    }

    override suspend fun getWord(word: String): UserWordEntity? = withContext(ioDispatcher) {
        val key = normalizeWord(word)
        if (key.isEmpty()) null else userWordDao.getByWord(key)
    }

    override suspend fun deleteWord(word: String) = withContext(ioDispatcher) {
        val key = normalizeWord(word)
        if (key.isNotEmpty()) {
            userWordDao.deleteByWord(key)
            Log.d(TAG, "Deleted word '$key'")
        }
    }

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            userWordDao.deleteAll()
            Log.d(TAG, "Deleted all user words")
        }
    }

    // Backwards compatibility helper
    suspend fun setStatus(word: String, status: WordStatus) {
        markAs(word, status, null)
    }

    private fun normalizeWord(raw: String): String {
        return raw.lowercase().trim().trim { !it.isLetterOrDigit() }
    }
}
