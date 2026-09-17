package com.tbtechs.folioreader.data.vocabulary

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbtechs.folioreader.data.db.AppDatabase
import com.tbtechs.folioreader.domain.model.WordStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserWordRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: UserWordRepository
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserWordRepository(db.userWordDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getStatus_nonExistentWord_returnsUnknown() = runTest(testDispatcher) {
        val status = repository.getStatus("serendipity")
        assertEquals(WordStatus.UNKNOWN, status)
    }

    @Test
    fun saveWord_firstTime_createsLearningWithLookupCountOne() = runTest(testDispatcher) {
        val saved = repository.saveWord("ubiquitous", "sample.pdf")
        assertEquals("ubiquitous", saved.word)
        assertEquals(WordStatus.LEARNING.name, saved.status)
        assertEquals(1, saved.lookupCount)

        val retrieved = repository.getWord("ubiquitous")
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.lookupCount)
        assertEquals(WordStatus.LEARNING, repository.getStatus("ubiquitous"))
    }

    @Test
    fun saveWord_multipleTimes_incrementsLookupCount() = runTest(testDispatcher) {
        repository.saveWord("resilient", "sample.pdf")
        repository.saveWord("resilient", "sample.pdf")
        val third = repository.saveWord("resilient", "sample.pdf")

        assertEquals(3, third.lookupCount)
        val entity = repository.getWord("resilient")
        assertEquals(3, entity?.lookupCount)
    }

    @Test
    fun markAs_known_updatesStatusToKnown() = runTest(testDispatcher) {
        repository.markAs("ephemeral", WordStatus.KNOWN, "doc.pdf")
        assertEquals(WordStatus.KNOWN, repository.getStatus("ephemeral"))
    }

    @Test
    fun markAs_ignored_updatesStatusToIgnored() = runTest(testDispatcher) {
        repository.markAs("gregarious", WordStatus.IGNORED, null)
        assertEquals(WordStatus.IGNORED, repository.getStatus("gregarious"))
    }

    @Test
    fun getAllByStatus_filtersCorrectly() = runTest(testDispatcher) {
        repository.saveWord("word1") // LEARNING
        repository.saveWord("word2") // LEARNING
        repository.markAs("word3", WordStatus.KNOWN, null)
        repository.markAs("word4", WordStatus.IGNORED, null)

        val learningList = repository.getAllByStatus(WordStatus.LEARNING).first()
        val knownList = repository.getAllByStatus(WordStatus.KNOWN).first()
        val allList = repository.getAllWords().first()

        assertEquals(2, learningList.size)
        assertEquals(1, knownList.size)
        assertEquals(4, allList.size)
    }

    @Test
    fun deleteWord_removesEntity() = runTest(testDispatcher) {
        repository.saveWord("to_delete")
        assertNotNull(repository.getWord("to_delete"))

        repository.deleteWord("to_delete")
        assertNull(repository.getWord("to_delete"))
        assertEquals(WordStatus.UNKNOWN, repository.getStatus("to_delete"))
    }

    @Test
    fun deleteAll_clearsAllWords() = runTest(testDispatcher) {
        repository.saveWord("w1")
        repository.saveWord("w2")
        assertEquals(2, repository.getAllWords().first().size)

        repository.deleteAll()
        assertEquals(0, repository.getAllWords().first().size)
    }
}
