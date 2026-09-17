package com.tbtechs.folioreader.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_definitions")
data class CachedDefinition(
    @PrimaryKey val word: String,
    val partOfSpeech: String,
    val englishDefinition: String,
    val hindiMeaning: String = "",   // populated later by ML Kit in Prompt 8
    val cachedAtMs: Long = System.currentTimeMillis(),
    val phonetic: String = "",
    val example: String = "",
    val synonyms: String = "",
    val antonyms: String = ""
)
