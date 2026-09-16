package com.tbtechsdev.lexiread.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition

@Dao
interface CachedDefinitionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CachedDefinition)

    @Query("SELECT * FROM cached_definitions WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): CachedDefinition?
}
