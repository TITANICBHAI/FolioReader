package com.tbtechsdev.lexiread.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbtechsdev.lexiread.data.db.entities.CommonWord
import com.tbtechsdev.lexiread.data.db.entities.DictionaryEntry

@Dao
interface DictionaryDao {

    @Query("SELECT * FROM dictionary WHERE word = :word LIMIT 1")
    suspend fun lookupWord(word: String): DictionaryEntry?

    @Query("SELECT EXISTS(SELECT 1 FROM common_words WHERE word = :word LIMIT 1)")
    suspend fun isCommonWord(word: String): Boolean

    @Query("SELECT COALESCE((SELECT frequency FROM dictionary WHERE word = :word LIMIT 1), 99999)")
    suspend fun getFrequency(word: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryEntry(entry: DictionaryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryEntries(entries: List<DictionaryEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommonWord(commonWord: CommonWord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommonWords(commonWords: List<CommonWord>)
}
