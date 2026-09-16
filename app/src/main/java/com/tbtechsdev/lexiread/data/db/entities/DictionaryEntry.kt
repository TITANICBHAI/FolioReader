package com.tbtechsdev.lexiread.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary")
data class DictionaryEntry(
    @PrimaryKey val word: String,
    val lemma: String,
    val partOfSpeech: String,
    val englishDefinition: String,
    val hindiMeaning: String,
    val frequency: Int,
    val cefrLevel: String?
)
