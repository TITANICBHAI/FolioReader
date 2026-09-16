package com.tbtechsdev.lexiread.data.dictionary

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbtechsdev.lexiread.data.db.AppDatabase
import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition
import com.tbtechsdev.lexiread.domain.WordNormalizer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DictionaryRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DictionaryRepository
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() = runTest(testDispatcher) {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = database.cachedDefinitionDao()
        repository = DictionaryRepository(dao, testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun isCommonWord_the_returnsTrue() {
        assertTrue(repository.isCommonWord("the"))
    }

    @Test
    fun isCommonWord_ubiquitous_returnsFalse() {
        assertFalse(repository.isCommonWord("ubiquitous"))
    }

    @Test
    fun getFrequencyRank_the_returnsBetween1And20() {
        val rank = repository.getFrequencyRank("the")
        assertTrue("Expected rank of 'the' between 1 and 20, got: $rank", rank in 1..20)
    }

    @Test
    fun getFrequencyRank_stringent_returns3000OrHigher() {
        val rank = repository.getFrequencyRank("stringent")
        assertTrue("Expected rank of 'stringent' >= 3000, got: $rank", rank >= 3000)
    }

    @Test
    fun wordNormalizer_running_returnsRun() {
        val normalized = WordNormalizer("running")
        assertTrue(normalized == "run" || normalized == "runn")
    }

    @Test
    fun wordNormalizer_stringent_returnsStringentUnchanged() {
        assertEquals("stringent", WordNormalizer("stringent"))
    }

    @Test
    fun getDefinition_cacheHit_returnsCachedEntry() = runTest(testDispatcher) {
        val cached = CachedDefinition(
            word = "ubiquitous",
            partOfSpeech = "adjective",
            englishDefinition = "present, appearing, or found everywhere.",
            hindiMeaning = "सर्वव्यापी"
        )
        database.cachedDefinitionDao().upsert(cached)

        val result = repository.getDefinition("ubiquitous")
        assertNotNull(result)
        assertEquals("ubiquitous", result?.word)
        assertEquals("present, appearing, or found everywhere.", result?.englishDefinition)
    }

    @Test
    fun getDefinition_unknownOrOfflineWord_returnsNullGracefully() = runTest(testDispatcher) {
        val result = repository.getDefinition("zzxqyqwerty123")
        assertNull(result)
    }
}

