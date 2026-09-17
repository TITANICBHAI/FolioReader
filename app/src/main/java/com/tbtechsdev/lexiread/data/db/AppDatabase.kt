package com.tbtechsdev.lexiread.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tbtechsdev.lexiread.data.db.dao.CachedDefinitionDao
import com.tbtechsdev.lexiread.data.db.dao.DictionaryDao
import com.tbtechsdev.lexiread.data.db.dao.UserWordDao
import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition
import com.tbtechsdev.lexiread.data.db.entities.CommonWord
import com.tbtechsdev.lexiread.data.db.entities.DictionaryEntry
import com.tbtechsdev.lexiread.data.db.entities.UserWordEntity

private const val TAG = "AppDatabase"
const val DICTIONARY_DB_NAME = "dictionary.db"

@Database(
    entities = [CachedDefinition::class, DictionaryEntry::class, CommonWord::class, UserWordEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedDefinitionDao(): CachedDefinitionDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun userWordDao(): UserWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DICTIONARY_DB_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true)

            if (hasDictionaryAsset(context)) {
                builder.createFromAsset(DICTIONARY_DB_NAME)
                Log.i(TAG, "Using pre-populated asset $DICTIONARY_DB_NAME for Room database")
            }

            return builder.build()
        }

        fun hasDictionaryAsset(context: Context): Boolean {
            return try {
                context.assets.open(DICTIONARY_DB_NAME).use { true }
            } catch (e: Exception) {
                false
            }
        }

        @Deprecated("Use hasDictionaryAsset instead to avoid throwing on missing asset", ReplaceWith("hasDictionaryAsset(context)"))
        fun validateDictionaryAsset(context: Context): Boolean {
            if (!hasDictionaryAsset(context)) {
                throw IllegalStateException("Asset missing: $DICTIONARY_DB_NAME")
            }
            return true
        }

        private fun seedDefaultCommonWords(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            val starterWords = listOf(
                "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
                "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
                "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
                "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
                "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
                "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
                "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
                "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
                "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
                "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
                "is", "am", "are", "was", "were", "been", "being", "had", "has", "did",
                "does", "doing", "done", "said", "saying", "went", "gone", "going", "goes"
            )
            try {
                db.beginTransaction()
                for (word in starterWords) {
                    db.execSQL("INSERT OR IGNORE INTO common_words (word) VALUES (?)", arrayOf(word))
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.w(TAG, "Failed seeding starter common words: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }
    }
}
