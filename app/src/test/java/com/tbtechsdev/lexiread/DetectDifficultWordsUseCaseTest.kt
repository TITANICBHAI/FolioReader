package com.tbtechsdev.lexiread

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tbtechsdev.lexiread.data.db.AppDatabase
import com.tbtechsdev.lexiread.data.db.entities.CommonWord
import com.tbtechsdev.lexiread.data.db.entities.DictionaryEntry
import com.tbtechsdev.lexiread.data.dictionary.DictionaryRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepositoryImpl
import com.tbtechsdev.lexiread.data.vocabulary.UserWordStatus
import com.tbtechsdev.lexiread.domain.model.PdfWord
import com.tbtechsdev.lexiread.domain.usecase.DetectDifficultWordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DetectDifficultWordsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dictRepo: DictionaryRepository
    private lateinit var userWordRepo: UserWordRepositoryImpl
    private lateinit var useCase: DetectDifficultWordsUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dictRepo = DictionaryRepository(db.cachedDefinitionDao(), testDispatcher)
        userWordRepo = UserWordRepositoryImpl(db.userWordDao(), testDispatcher)
        useCase = DetectDifficultWordsUseCase(dictRepo, userWordRepo, testDispatcher)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun createWord(text: String, x: Float = 0f, y: Float = 0f): PdfWord {
        return PdfWord(text = text, x = x, y = y, width = 40f, height = 15f, page = 0)
    }

    @Test
    fun filter1_commonWords_areExcluded() = runTest(testDispatcher) {
        db.dictionaryDao().insertCommonWords(listOf(CommonWord("common"), CommonWord("always")))

        val words = listOf(
            createWord("common"),
            createWord("uncommon"),
            createWord("always")
        )

        val result = useCase(words)
        val texts = result.map { it.text.lowercase() }
        assertFalse(texts.contains("common"))
        assertFalse(texts.contains("always"))
        assertTrue(texts.contains("uncommon"))
    }

    @Test
    fun filter2_frequencyRankBelow3000_areExcluded() = runTest(testDispatcher) {
        db.dictionaryDao().insertDictionaryEntries(
            listOf(
                DictionaryEntry(
                    word = "simple",
                    lemma = "simple",
                    partOfSpeech = "adj",
                    englishDefinition = "easy",
                    hindiMeaning = "सरल",
                    frequency = 1200,
                    cefrLevel = "A1"
                ),
                DictionaryEntry(
                    word = "esoteric",
                    lemma = "esoteric",
                    partOfSpeech = "adj",
                    englishDefinition = "obscure",
                    hindiMeaning = "गूढ़",
                    frequency = 25000,
                    cefrLevel = "C2"
                )
            )
        )

        val words = listOf(
            createWord("simple"),
            createWord("esoteric")
        )

        val result = useCase(words)
        val texts = result.map { it.text.lowercase() }
        assertFalse(texts.contains("simple"))
        assertTrue(texts.contains("esoteric"))
    }

    @Test
    fun filter3_userStatusKnownOrIgnored_areExcluded() = runTest(testDispatcher) {
        userWordRepo.setStatus("esoteric", UserWordStatus.KNOWN)
        userWordRepo.setStatus("draconian", UserWordStatus.IGNORED)
        userWordRepo.setStatus("ephemeral", UserWordStatus.LEARNING)

        val words = listOf(
            createWord("esoteric"),
            createWord("draconian"),
            createWord("ephemeral"),
            createWord("ubiquitous")
        )

        val result = useCase(words)
        val texts = result.map { it.text.lowercase() }
        assertFalse(texts.contains("esoteric"))
        assertFalse(texts.contains("draconian"))
        assertTrue(texts.contains("ephemeral"))
        assertTrue(texts.contains("ubiquitous"))
    }

    @Test
    fun filter4_wordLengthLessThan4_areExcluded() = runTest(testDispatcher) {
        val words = listOf(
            createWord("the"),
            createWord("and"),
            createWord("is"),
            createWord("he"),
            createWord("elephant")
        )

        val result = useCase(words)
        assertEquals(1, result.size)
        assertEquals("elephant", result[0].text)
    }

    @Test
    fun filter5_properNounHeuristic_uppercaseNotFirstWord_areExcluded() = runTest(testDispatcher) {
        // Sentence: "We visited London yesterday."
        val words = listOf(
            createWord("We"),
            createWord("visited"),
            createWord("London"),
            createWord("yesterday.")
        )

        val result = useCase(words)
        val texts = result.map { it.text }
        // "London" is capitalized and preceded by "visited" (which doesn't end in sentence terminator)
        assertFalse(texts.contains("London"))
    }

    @Test
    fun sorting_highestFrequencyNumberFirst_rarestFirst() = runTest(testDispatcher) {
        val words = listOf(
            createWord("agricultural"),
            createWord("ubiquitous"),
            createWord("lounge")
        )

        val result = useCase(words)
        assertEquals(3, result.size)
        // Highest frequency rank number first (rarest: ubiquitous=99999, lounge=4201, agricultural=3101)
        assertEquals("ubiquitous", result[0].text)
        assertEquals("lounge", result[1].text)
        assertEquals("agricultural", result[2].text)
    }

    @Test
    fun limit_topN_maxCountRespected() = runTest(testDispatcher) {
        val words = (1..15).map { i ->
            createWord("difficultword$i")
        }

        val result = useCase(words, maxCount = 6)
        assertEquals(6, result.size)
    }
}
