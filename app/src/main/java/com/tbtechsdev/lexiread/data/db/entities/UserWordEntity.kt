package com.tbtechsdev.lexiread.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey val word: String,
    val status: String,           // store WordStatus enum name as String
    val lookupCount: Int = 0,
    val firstSeenMs: Long = System.currentTimeMillis(),
    val lastSeenMs: Long = System.currentTimeMillis(),
    val sourceBookPath: String? = null
)
