package com.tbtechs.folioreader.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWordDao {

    @Upsert
    suspend fun upsert(userWord: UserWordEntity)

    @Query("SELECT * FROM user_words WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): UserWordEntity?

    @Query("SELECT * FROM user_words WHERE status = :status ORDER BY lastSeenMs DESC")
    fun getAllByStatus(status: String): Flow<List<UserWordEntity>>

    @Query("SELECT * FROM user_words WHERE status IN (:statuses) ORDER BY lastSeenMs DESC")
    fun getAllByStatuses(statuses: List<String>): Flow<List<UserWordEntity>>

    @Query("SELECT * FROM user_words WHERE status IN (:statuses) ORDER BY lastSeenMs DESC")
    suspend fun getListByStatuses(statuses: List<String>): List<UserWordEntity>

    @Query("SELECT * FROM user_words ORDER BY lastSeenMs DESC")
    fun getAll(): Flow<List<UserWordEntity>>

    @Query("SELECT * FROM user_words ORDER BY lastSeenMs DESC")
    suspend fun getAllList(): List<UserWordEntity>

    @Query("DELETE FROM user_words WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("DELETE FROM user_words")
    suspend fun deleteAll()
}
