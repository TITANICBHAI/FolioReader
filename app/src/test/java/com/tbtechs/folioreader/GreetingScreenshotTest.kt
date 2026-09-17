package com.tbtechs.folioreader

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tbtechs.folioreader.data.db.AppDatabase
import com.tbtechs.folioreader.data.dictionary.DictionaryRepository
import com.tbtechs.folioreader.data.pdf.PdfOcrExtractor
import com.tbtechs.folioreader.data.pdf.PdfTextExtractor
import com.tbtechs.folioreader.data.pdf.PdfTypeDetector
import com.tbtechs.folioreader.data.preferences.ReaderPreferencesRepository
import com.tbtechs.folioreader.data.preferences.readerDataStore
import com.tbtechs.folioreader.data.vocabulary.UserWordRepositoryImpl
import com.tbtechs.folioreader.domain.usecase.DetectDifficultWordsUseCase
import androidx.room.Room
import com.tbtechs.folioreader.ui.FolioReaderMainScreen
import com.tbtechs.folioreader.ui.reader.ReaderViewModel
import com.tbtechs.folioreader.ui.theme.FolioReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context)
    val preferencesRepository = ReaderPreferencesRepository(context, context.readerDataStore)
    val typeDetector = PdfTypeDetector(context)
    val textExtractor = PdfTextExtractor(context)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val ocrExtractor = PdfOcrExtractor(recognizer)

    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val dictRepo = DictionaryRepository(db.cachedDefinitionDao())
    val userWordRepo = UserWordRepositoryImpl(db.userWordDao())
    val detectDifficultWordsUseCase = DetectDifficultWordsUseCase(dictRepo, userWordRepo)
    val fakeTranslationRepo = FakeTranslationRepository()
    val fakeGeminiRepo = FakeGeminiRepository()
    val ttsHelper = com.tbtechs.folioreader.util.TextToSpeechHelper(context)

    val viewModel = ReaderViewModel(
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

    composeTestRule.setContent {
      FolioReaderTheme {
        FolioReaderMainScreen(readerViewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

