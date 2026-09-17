package com.tbtechs.folioreader.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "common_words")
data class CommonWord(
    @PrimaryKey val word: String
)
