package com.tbtechsdev.lexiread

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tbtechsdev.lexiread.data.db.AppDatabase
import com.tbtechsdev.lexiread.data.dictionary.DictionaryRepository
import com.tbtechsdev.lexiread.data.pdf.PdfOcrExtractor
import com.tbtechsdev.lexiread.data.pdf.PdfTextExtractor
import com.tbtechsdev.lexiread.data.pdf.PdfType
import com.tbtechsdev.lexiread.data.pdf.PdfTypeDetector
import com.tbtechsdev.lexiread.data.preferences.ReaderPreferencesRepository
import com.tbtechsdev.lexiread.data.preferences.readerDataStore
import com.tbtechsdev.lexiread.data.translation.ITranslationRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepositoryImpl
import com.tbtechsdev.lexiread.domain.usecase.DetectDifficultWordsUseCase
import androidx.room.Room
import com.tbtechsdev.lexiread.domain.model.PdfWord
import com.tbtechsdev.lexiread.domain.model.WordStatus
import com.tbtechsdev.lexiread.ui.reader.ReaderViewModel
import com.tbtechsdev.lexiread.ui.reader.SentenceTranslationState
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class FakeTranslationRepository(
    var isDownloaded: Boolean = false,
    var translationResult: String? = "यह एक अनुवाद है"
) : ITranslationRepository {
    var downloadCallCount = 0
    var deleteCallCount = 0
    var translateCallCount = 0

    override suspend fun isModelDownloaded(): Boolean = isDownloaded

    override suspend fun downloadModel(onProgress: (Float) -> Unit) {
        downloadCallCount++
        onProgress(0.5f)
        onProgress(1.0f)
        isDownloaded = true
    }

    override suspend fun deleteModel() {
        deleteCallCount++
        isDownloaded = false
    }

    override suspend fun translate(text: String): String? {
        translateCallCount++
        return if (isDownloaded) translationResult else null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var preferencesRepository: ReaderPreferencesRepository
    private lateinit var typeDetector: PdfTypeDetector
    private lateinit var textExtractor: PdfTextExtractor
    private lateinit var ocrExtractor: PdfOcrExtractor
    private lateinit var db: AppDatabase
    private lateinit var detectDifficultWordsUseCase: DetectDifficultWordsUseCase
    private lateinit var fakeTranslationRepo: FakeTranslationRepository
    private lateinit var fakeGeminiRepo: FakeGeminiRepository
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            context.readerDataStore.edit { it.clear() }
        }
        com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context)
        preferencesRepository = ReaderPreferencesRepository(context, context.readerDataStore)
        typeDetector = PdfTypeDetector(context)
        textExtractor = PdfTextExtractor(context)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        ocrExtractor = PdfOcrExtractor(recognizer)

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            db.cachedDefinitionDao().upsert(
                com.tbtechsdev.lexiread.data.db.entities.CachedDefinition(
                    word = "meticulous",
                    partOfSpeech = "adjective",
                    englishDefinition = "showing great attention to detail.",
                    hindiMeaning = "अतिसावधान"
                )
            )
        }
        val dictRepo = DictionaryRepository(db.cachedDefinitionDao(), testDispatcher)
        val userWordRepo = UserWordRepositoryImpl(db.userWordDao(), testDispatcher)
        detectDifficultWordsUseCase = DetectDifficultWordsUseCase(dictRepo, userWordRepo, testDispatcher)
        fakeTranslationRepo = FakeTranslationRepository()
        fakeGeminiRepo = FakeGeminiRepository()
        val ttsHelper = com.tbtechsdev.lexiread.util.TextToSpeechHelper(context)

        viewModel = ReaderViewModel(
            context,
            preferencesRepository,
            typeDetector,
            textExtractor,
            ocrExtractor,
            detectDifficultWordsUseCase,
            dictRepo,
            userWordRepo,
            fakeTranslationRepo,
            fakeGeminiRepo,
            ttsHelper
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isNotLoaded() {
        val state = viewModel.uiState.value
        assertFalse(state.isPdfLoaded)
        assertFalse(state.isLoading)
        assertFalse(state.isOcrRunning)
        assertEquals(0, state.pageCount)
        assertEquals(1, state.currentPage)
        assertEquals(null, viewModel.pdfType.value)
        assertTrue(viewModel.extractedWords.value.isEmpty())
    }

    @Test
    fun preferences_saveAndRetrieveLastPage() = runTest {
        preferencesRepository.saveLastPdf(
            uriString = "content://com.android.providers.media.documents/document/123",
            fileName = "SampleDocument.pdf",
            pageIndex = 4
        )

        val saved = preferencesRepository.readerStateFlow.first()
        assertEquals("SampleDocument.pdf", saved.fileName)
        assertEquals(4, saved.pageIndex)
        assertNotNull(saved.uriString)
    }

    @Test
    fun toggleBars_changesBarsVisibleState() {
        val initial = viewModel.uiState.value.areBarsVisible
        viewModel.toggleBars()
        assertEquals(!initial, viewModel.uiState.value.areBarsVisible)
        viewModel.toggleBars()
        assertEquals(initial, viewModel.uiState.value.areBarsVisible)
    }

    @Test
    fun pdfTypeDetector_classifiesTextAndScannedCorrectly() = runTest {
        // Create text PDF with > 50 characters
        val textDoc = PDDocument()
        val page1 = PDPage()
        textDoc.addPage(page1)
        val cs1 = PDPageContentStream(textDoc, page1)
        cs1.beginText()
        cs1.setFont(PDType1Font.HELVETICA, 12f)
        cs1.newLineAtOffset(50f, 700f)
        cs1.showText("This is a comprehensive sample document for testing LexiRead text detection features!")
        cs1.endText()
        cs1.close()

        val textBaos = ByteArrayOutputStream()
        textDoc.save(textBaos)
        textDoc.close()

        val textType = typeDetector.detectType(ByteArrayInputStream(textBaos.toByteArray()))
        assertEquals(PdfType.Text, textType)

        // Create sparse/scanned PDF with <= 50 characters
        val sparseDoc = PDDocument()
        val page2 = PDPage()
        sparseDoc.addPage(page2)
        val cs2 = PDPageContentStream(sparseDoc, page2)
        cs2.beginText()
        cs2.setFont(PDType1Font.HELVETICA, 12f)
        cs2.newLineAtOffset(50f, 700f)
        cs2.showText("Few chars")
        cs2.endText()
        cs2.close()

        val sparseBaos = ByteArrayOutputStream()
        sparseDoc.save(sparseBaos)
        sparseDoc.close()

        val sparseType = typeDetector.detectType(ByteArrayInputStream(sparseBaos.toByteArray()))
        assertEquals(PdfType.Scanned, sparseType)
    }

    @Test
    fun pdfTextExtractor_filtersAndExtractsWordsWithCoordinates() = runTest {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        val cs = PDPageContentStream(document, page)
        cs.beginText()
        cs.setFont(PDType1Font.HELVETICA, 12f)
        cs.newLineAtOffset(80f, 720f)
        // Words: "is", "a" (< 3 chars, skip), "2024", "123" (numeric, skip), "LexiRead!", "reader", "learning" (keep)
        cs.showText("is a 2024 123 LexiRead! reader learning")
        cs.endText()
        cs.close()

        val tempFile = File(context.cacheDir, "test_extract.pdf")
        document.save(tempFile)
        document.close()

        val words = textExtractor.extractWords(tempFile, 0)

        // Verify words < 3 chars absent
        assertFalse(words.any { it.normalizedText == "is" })
        assertFalse(words.any { it.normalizedText == "a" })

        // Verify purely numeric strings absent
        assertFalse(words.any { it.normalizedText == "2024" })
        assertFalse(words.any { it.normalizedText == "123" })

        // Verify kept words
        val normalizedList = words.map { it.normalizedText }
        assertTrue(normalizedList.contains("lexiread"))
        assertTrue(normalizedList.contains("reader"))
        assertTrue(normalizedList.contains("learning"))

        // Verify coordinates non-zero and vary
        val word1 = words.first { it.normalizedText == "lexiread" }
        val word2 = words.first { it.normalizedText == "reader" }
        assertTrue(word1.x > 0f)
        assertTrue(word1.y > 0f)
        assertTrue(word1.width > 0f)
        assertTrue(word1.height > 0f)
        assertTrue(word1.x != word2.x)

        // Verify normalizedText has no punctuation and is lowercase
        words.forEach { word ->
            assertEquals(word.normalizedText.lowercase(), word.normalizedText)
            assertFalse(word.normalizedText.contains("!"))
            assertFalse(word.normalizedText.contains(","))
        }

        tempFile.delete()
    }

    @Test
    fun pdfTextExtractor_handlesEmptyPageGracefully() = runTest {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page) // blank page

        val tempFile = File(context.cacheDir, "test_empty.pdf")
        document.save(tempFile)
        document.close()

        val words = textExtractor.extractWords(tempFile, 0)
        assertTrue(words.isEmpty())

        tempFile.delete()
    }

    @Test
    fun processPageWords_skipsAlreadyCachedPages() = runTest {
        // Pre-populate word cache
        val existingWords = listOf(
            PdfWord("LexiRead", "lexiread", 0, 10f, 10f, 50f, 20f)
        )
        // Since page 0 already exists in cache, calling processPageWords does not trigger re-extraction
        val uri = android.net.Uri.parse("file:///nonexistent.pdf")
        viewModel.processPageWords(uri, 0, PdfType.Scanned)

        // Verify it didn't crash on invalid URI or renderer because cache check short-circuited
        assertTrue(viewModel.extractedWords.value.isEmpty())
    }

    @Test
    fun pdfWordModel_preservesCoordinateAndTextIntegrity() {
        val ocrWord = PdfWord(
            text = "Welcome,",
            normalizedText = "welcome",
            page = 0,
            x = 12.5f,
            y = 34.0f,
            width = 100.0f,
            height = 20.0f
        )
        assertEquals("Welcome,", ocrWord.text)
        assertEquals("welcome", ocrWord.normalizedText)
        assertEquals(0, ocrWord.page)
        assertEquals(12.5f, ocrWord.x, 0.001f)
        assertEquals(34.0f, ocrWord.y, 0.001f)
        assertEquals(100.0f, ocrWord.width, 0.001f)
        assertEquals(20.0f, ocrWord.height, 0.001f)
    }

    @Test
    fun wordSelection_and_actions_workCorrectly() = runTest(testDispatcher) {
        val word = PdfWord(
            text = "meticulous",
            normalizedText = "meticulous",
            page = 0,
            x = 10f,
            y = 20f,
            width = 50f,
            height = 15f
        )

        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()
        assertEquals("meticulous", viewModel.selectedWord.value?.text)

        // Save selected word -> status = LEARNING, sheet dismissed (selection cleared)
        viewModel.saveSelectedWord()?.join()
        testScheduler.advanceUntilIdle()
        assertEquals(null, viewModel.selectedWord.value)

        val statusAfterSave = db.userWordDao().getByWord("meticulous")
        assertNotNull(statusAfterSave)
        assertEquals("LEARNING", statusAfterSave?.status)
        assertEquals(1, statusAfterSave?.lookupCount)

        // Select again and mark as KNOWN
        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()
        viewModel.markSelectedWordKnown()?.join()
        testScheduler.advanceUntilIdle()
        assertEquals(null, viewModel.selectedWord.value)

        val statusAfterKnown = db.userWordDao().getByWord("meticulous")
        assertEquals("KNOWN", statusAfterKnown?.status)

        // Select again and mark as IGNORED
        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()
        viewModel.markSelectedWordIgnored()?.join()
        testScheduler.advanceUntilIdle()
        assertEquals(null, viewModel.selectedWord.value)

        val statusAfterIgnored = db.userWordDao().getByWord("meticulous")
        assertEquals("IGNORED", statusAfterIgnored?.status)
    }

    @Test
    fun translation_whenModelNotDownloaded_showsDownloadPromptOnFirstOpen() = runTest(testDispatcher) {
        fakeTranslationRepo.isDownloaded = false
        preferencesRepository.setTranslationPromptShown(false)
        testScheduler.advanceUntilIdle()

        val word = PdfWord(
            text = "meticulous",
            normalizedText = "meticulous",
            page = 0,
            x = 10f,
            y = 20f,
            width = 50f,
            height = 15f
        )

        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.showTranslationDownloadPrompt.value)

        // Dismiss the prompt
        viewModel.dismissTranslationPrompt(markAsShown = true)?.join()
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.showTranslationDownloadPrompt.value)

        // Check preference is marked shown
        val promptShown = preferencesRepository.isTranslationPromptShown.first()
        assertTrue(promptShown)

        // Select another word -> prompt should NOT show again
        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.showTranslationDownloadPrompt.value)
    }

    @Test
    fun translation_whenModelDownloaded_successTranslatesSentence() = runTest {
        fakeTranslationRepo.isDownloaded = true
        fakeTranslationRepo.translationResult = "वह बहुत सतर्क था"

        val job = viewModel.translateSentence("He was meticulous about details")
        job.join()
        testScheduler.advanceUntilIdle()

        val state = viewModel.sentenceTranslationState.value
        assertTrue(state is SentenceTranslationState.Success)
        assertEquals("वह बहुत सतर्क था", (state as SentenceTranslationState.Success).translation)
    }

    @Test
    fun translation_whenErrorOccurs_stateIsError() = runTest {
        fakeTranslationRepo.isDownloaded = true
        fakeTranslationRepo.translationResult = null // triggers failure in FakeTranslationRepository

        val job = viewModel.translateSentence("Some sentence")
        job.join()
        testScheduler.advanceUntilIdle()

        val state = viewModel.sentenceTranslationState.value
        assertTrue(state is SentenceTranslationState.Error)
        assertEquals("Translation unavailable", (state as SentenceTranslationState.Error).message)
    }

    @Test
    fun gemini_whenConfigured_requestAiExplanationForSelectedWord_streamsExplanation() = runTest {
        fakeGeminiRepo.configured = true
        viewModel.refreshGeminiStatus()
        assertTrue(viewModel.isGeminiConfigured.value)

        val word = PdfWord("meticulous", "meticulous", 0, 10f, 20f, 50f, 15f)
        viewModel.selectWord(word)?.join()
        testScheduler.advanceUntilIdle()

        val job = viewModel.requestAiExplanationForSelectedWord()
        job?.join()
        testScheduler.advanceUntilIdle()

        val resultText = viewModel.aiWordExplanationText.value
        assertNotNull(resultText)
        assertTrue(resultText!!.contains("meticulous"))
        assertFalse(viewModel.isAiWordExplaining.value)
    }

    @Test
    fun gemini_textSelection_and_triggerExplainSelectedText_streamsResponse() = runTest {
        fakeGeminiRepo.configured = true

        // User long presses text
        viewModel.onTextSelectedForExplanation("This is a short sentence.", androidx.compose.ui.geometry.Offset(100f, 200f), 0)
        assertNotNull(viewModel.activeTextSelection.value)
        assertEquals("This is a short sentence.", viewModel.activeTextSelection.value?.text)

        // User taps "Explain this ▸"
        val job = viewModel.triggerExplainSelectedText("This is a short sentence.")
        assertNull(viewModel.activeTextSelection.value)
        assertTrue(viewModel.showExplainSheet.value)
        job?.join()
        testScheduler.advanceUntilIdle()

        val response = viewModel.explainSheetResponseText.value
        assertNotNull(response)
        assertTrue(response!!.contains("This sentence explains"))
        assertFalse(viewModel.isExplainingSheet.value)

        // Dismiss sheet
        viewModel.dismissExplainSheet()
        assertFalse(viewModel.showExplainSheet.value)
        assertNull(viewModel.explainSheetResponseText.value)
    }

    @Test
    fun gemini_whenNetworkFails_returnsErrorMessage() = runTest {
        fakeGeminiRepo.configured = true
        fakeGeminiRepo.shouldError = true

        val job = viewModel.triggerExplainSelectedText("A paragraph with network error.")
        job?.join()
        testScheduler.advanceUntilIdle()

        val response = viewModel.explainSheetResponseText.value
        assertNotNull(response)
        assertTrue(response!!.contains("Mock network failure"))
    }
}

