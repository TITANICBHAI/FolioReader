package com.tbtechs.folioreader.ui.reader

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.MlKitException
import com.tbtechs.folioreader.data.db.entities.CachedDefinition
import com.tbtechs.folioreader.data.dictionary.DictionaryRepository
import com.tbtechs.folioreader.data.pdf.PdfOcrExtractor
import com.tbtechs.folioreader.data.pdf.PdfTextExtractor
import com.tbtechs.folioreader.data.pdf.PdfType
import com.tbtechs.folioreader.data.pdf.PdfTypeDetector
import com.tbtechs.folioreader.data.preferences.ReaderPreferencesRepository
import com.tbtechs.folioreader.data.translation.ITranslationRepository
import com.tbtechs.folioreader.data.vocabulary.UserWordRepository
import com.tbtechs.folioreader.domain.model.PdfWord
import com.tbtechs.folioreader.domain.model.WordStatus
import com.tbtechs.folioreader.domain.usecase.DetectDifficultWordsUseCase
import com.tbtechs.folioreader.util.ScreenOrientationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

import com.tbtechs.folioreader.data.ai.IGeminiRepository
import com.tbtechs.folioreader.data.classroom.NearbyConnectionManager
import com.tbtechs.folioreader.data.classroom.WordMatchEngine

private const val TAG = "ReaderViewModel"
private const val MAX_CACHED_PAGES = 5

sealed interface ReaderEvent {
    data class ShowSnackbar(val message: String) : ReaderEvent
    data class ScrollToPage(val pageIndex: Int) : ReaderEvent
}

data class TextSelectionAction(
    val text: String,
    val offset: androidx.compose.ui.geometry.Offset,
    val pageIndex: Int
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val pdfTypeDetector: PdfTypeDetector,
    private val pdfTextExtractor: PdfTextExtractor,
    private val pdfOcrExtractor: PdfOcrExtractor,
    private val detectDifficultWordsUseCase: DetectDifficultWordsUseCase,
    private val dictionaryRepository: DictionaryRepository,
    private val userWordRepository: UserWordRepository,
    private val translationRepository: ITranslationRepository,
    private val geminiRepository: IGeminiRepository,
    private val ttsHelper: com.tbtechs.folioreader.util.TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReaderEvent>()
    val events: SharedFlow<ReaderEvent> = _events.asSharedFlow()

    // Expose current PDF name and page count as StateFlow
    val currentPdfName: StateFlow<String> = _uiState
        .map { it.pdfName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val pageCount: StateFlow<Int> = _uiState
        .map { it.pageCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Expose pdfType as StateFlow (needed later for OCR decision)
    val pdfType: StateFlow<PdfType?> = _uiState
        .map { it.pdfType }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Extracted words Map<Int, List<PdfWord>> (page -> words)
    private val _extractedWords = MutableStateFlow<Map<Int, List<PdfWord>>>(emptyMap())
    val extractedWords: StateFlow<Map<Int, List<PdfWord>>> = _extractedWords.asStateFlow()

    // Difficult words highlights Map<Int, List<PdfWord>> (page -> highlighted words)
    private val _pageHighlights = MutableStateFlow<Map<Int, List<PdfWord>>>(emptyMap())
    val pageHighlights: StateFlow<Map<Int, List<PdfWord>>> = _pageHighlights.asStateFlow()

    private val _highlightedWords = MutableStateFlow<List<PdfWord>>(emptyList())
    val highlightedWords: StateFlow<List<PdfWord>> = _highlightedWords.asStateFlow()

    private val _selectedWord = MutableStateFlow<PdfWord?>(null)
    val selectedWord: StateFlow<PdfWord?> = _selectedWord.asStateFlow()

    private val _selectedWordDefinition = MutableStateFlow<CachedDefinition?>(null)
    val selectedWordDefinition: StateFlow<CachedDefinition?> = _selectedWordDefinition.asStateFlow()

    private val _selectedWordContextSentence = MutableStateFlow<String?>(null)
    val selectedWordContextSentence: StateFlow<String?> = _selectedWordContextSentence.asStateFlow()

    val isVocabAssistanceEnabled: StateFlow<Boolean> = preferencesRepository.isVocabAssistanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isHindiModelDownloaded = MutableStateFlow<Boolean>(false)
    val isHindiModelDownloaded: StateFlow<Boolean> = _isHindiModelDownloaded.asStateFlow()

    val targetLanguage: StateFlow<String> = preferencesRepository.targetLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "hi")

    private var isTranslationPromptShownPref: Boolean = false

    private val _sentenceTranslationState = MutableStateFlow<SentenceTranslationState>(SentenceTranslationState.Idle)
    val sentenceTranslationState: StateFlow<SentenceTranslationState> = _sentenceTranslationState.asStateFlow()

    private val _showTranslationDownloadPrompt = MutableStateFlow<Boolean>(false)
    val showTranslationDownloadPrompt: StateFlow<Boolean> = _showTranslationDownloadPrompt.asStateFlow()

    private val _pageDimensions = MutableStateFlow<Map<Int, Pair<Int, Int>>>(emptyMap())
    val pageDimensions: StateFlow<Map<Int, Pair<Int, Int>>> = _pageDimensions.asStateFlow()

    // Gemini AI state
    private val _isGeminiConfigured = MutableStateFlow(geminiRepository.isConfigured())
    val isGeminiConfigured: StateFlow<Boolean> = _isGeminiConfigured.asStateFlow()

    private val _aiWordExplanationText = MutableStateFlow<String?>(null)
    val aiWordExplanationText: StateFlow<String?> = _aiWordExplanationText.asStateFlow()

    private val _isAiWordExplaining = MutableStateFlow(false)
    val isAiWordExplaining: StateFlow<Boolean> = _isAiWordExplaining.asStateFlow()

    private var activeWordAiJob: Job? = null

    // Long press text selection floating action & explain sheet
    private val _activeTextSelection = MutableStateFlow<TextSelectionAction?>(null)
    val activeTextSelection: StateFlow<TextSelectionAction?> = _activeTextSelection.asStateFlow()

    private val _showExplainSheet = MutableStateFlow(false)
    val showExplainSheet: StateFlow<Boolean> = _showExplainSheet.asStateFlow()

    private val _explainSheetSelectedText = MutableStateFlow<String?>(null)
    val explainSheetSelectedText: StateFlow<String?> = _explainSheetSelectedText.asStateFlow()

    private val _explainSheetResponseText = MutableStateFlow<String?>(null)
    val explainSheetResponseText: StateFlow<String?> = _explainSheetResponseText.asStateFlow()

    private val _isExplainingSheet = MutableStateFlow(false)
    val isExplainingSheet: StateFlow<Boolean> = _isExplainingSheet.asStateFlow()

    private var activeSheetAiJob: Job? = null

    // Screen Orientation & Rotation Control
    val screenOrientation: StateFlow<ScreenOrientationMode> = preferencesRepository.screenOrientationPreference
        .map { ScreenOrientationMode.fromKey(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenOrientationMode.SENSOR)

    fun setScreenOrientation(mode: ScreenOrientationMode) {
        viewModelScope.launch {
            preferencesRepository.setScreenOrientationPreference(mode.key)
        }
    }

    fun cycleScreenOrientation() {
        val current = screenOrientation.value
        val next = when (current) {
            ScreenOrientationMode.SENSOR -> ScreenOrientationMode.PORTRAIT
            ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.LANDSCAPE
            ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.SENSOR
        }
        setScreenOrientation(next)
    }

    fun toggleScreenOrientation() {
        val current = screenOrientation.value
        val next = when (current) {
            ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.LANDSCAPE
            ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
            ScreenOrientationMode.SENSOR -> ScreenOrientationMode.LANDSCAPE
        }
        setScreenOrientation(next)
    }

    fun quickRotate() = toggleScreenOrientation()

    // Active extraction background jobs per page (Text & OCR)
    private val activeExtractionJobs = ConcurrentHashMap<Int, Job>()
    private val activeOcrJobs = ConcurrentHashMap<Int, Job>()

    // Classroom Mode State
    sealed class ClassroomState {
        object Inactive : ClassroomState()
        data class Hosting(val sessionName: String) : ClassroomState()
        object Connected : ClassroomState()
    }

    private val nearbyManager = NearbyConnectionManager()
    private val wordMatchEngine = WordMatchEngine()

    private val _classroomState = MutableStateFlow<ClassroomState>(ClassroomState.Inactive)
    val classroomState: StateFlow<ClassroomState> = _classroomState.asStateFlow()

    private val _classroomCursorIndex = MutableStateFlow<Int?>(null)
    val classroomCursorIndex: StateFlow<Int?> = _classroomCursorIndex.asStateFlow()

    private var currentHostingSessionName: String? = null
    private var nearbyStateJob: Job? = null

    fun getCurrentPageWords(): List<PdfWord> {
        val pageIndex = maxOf(0, _uiState.value.currentPage - 1)
        return _extractedWords.value[pageIndex] ?: emptyList()
    }

    fun startHosting(sessionName: String, context: Context) {
        currentHostingSessionName = sessionName
        _classroomState.value = ClassroomState.Hosting(sessionName)

        val pageIndex = maxOf(0, _uiState.value.currentPage - 1)
        val currentWords = _extractedWords.value[pageIndex] ?: emptyList()
        wordMatchEngine.loadWords(currentWords)
        _classroomCursorIndex.value = if (currentWords.isNotEmpty()) 0 else null

        nearbyManager.onCommandReceived = { json ->
            try {
                var action: String? = null
                var setIndex: Int? = null

                val trimmed = json.trim()
                if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    action = obj.optString("action")
                    if (obj.has("index")) {
                        setIndex = obj.getInt("index")
                    }
                } else {
                    action = trimmed
                }

                when (action) {
                    "NEXT" -> {
                        val wordsCount = wordMatchEngine.words.size
                        if (wordsCount > 0 && wordMatchEngine.cursorIndex >= wordsCount - 1) {
                            val nextPageIndex = _uiState.value.currentPage // currentPage is 1-indexed
                            if (nextPageIndex < _uiState.value.pageCount) {
                                _uiState.update { it.copy(currentPage = nextPageIndex + 1) }
                                val nextWords = _extractedWords.value[nextPageIndex] ?: emptyList()
                                wordMatchEngine.loadWords(nextWords)
                                _classroomCursorIndex.value = if (nextWords.isNotEmpty()) 0 else null
                                sendWordChangedToRemote()
                                viewModelScope.launch {
                                    _events.emit(ReaderEvent.ScrollToPage(nextPageIndex))
                                }
                            } else {
                                wordMatchEngine.advance()
                                _classroomCursorIndex.value = wordMatchEngine.cursorIndex
                                sendWordChangedToRemote()
                            }
                        } else {
                            wordMatchEngine.advance()
                            _classroomCursorIndex.value = wordMatchEngine.cursorIndex
                            sendWordChangedToRemote()
                        }
                    }
                    "BACK" -> {
                        if (wordMatchEngine.cursorIndex <= 0) {
                            val prevPageIndex = _uiState.value.currentPage - 2
                            if (prevPageIndex >= 0) {
                                _uiState.update { it.copy(currentPage = prevPageIndex + 1) }
                                val prevWords = _extractedWords.value[prevPageIndex] ?: emptyList()
                                wordMatchEngine.loadWords(prevWords)
                                val lastIndex = maxOf(0, prevWords.size - 1)
                                wordMatchEngine.setCursor(lastIndex)
                                _classroomCursorIndex.value = if (prevWords.isNotEmpty()) lastIndex else null
                                sendWordChangedToRemote()
                                viewModelScope.launch {
                                    _events.emit(ReaderEvent.ScrollToPage(prevPageIndex))
                                }
                            } else {
                                wordMatchEngine.back()
                                _classroomCursorIndex.value = wordMatchEngine.cursorIndex
                                sendWordChangedToRemote()
                            }
                        } else {
                            wordMatchEngine.back()
                            _classroomCursorIndex.value = wordMatchEngine.cursorIndex
                            sendWordChangedToRemote()
                        }
                    }
                    "SET_WORD" -> {
                        if (setIndex != null) {
                            wordMatchEngine.setCursor(setIndex)
                            _classroomCursorIndex.value = wordMatchEngine.cursorIndex
                            sendWordChangedToRemote()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling nearby command: $json", e)
            }
        }

        nearbyStateJob?.cancel()
        nearbyStateJob = viewModelScope.launch {
            nearbyManager.connectionState.collect { state ->
                when (state) {
                    is NearbyConnectionManager.ConnectionState.Connected -> {
                        _classroomState.value = ClassroomState.Connected
                        sendWordChangedToRemote()
                    }
                    is NearbyConnectionManager.ConnectionState.Disconnected -> {
                        val current = _classroomState.value
                        if (current is ClassroomState.Connected) {
                            _classroomState.value = ClassroomState.Hosting(currentHostingSessionName ?: "")
                            _events.emit(ReaderEvent.ShowSnackbar("Remote disconnected — reconnect or tap Stop to end session"))
                        }
                    }
                    else -> {}
                }
            }
        }

        nearbyManager.startAdvertising(sessionName, context)
        sendWordChangedToRemote()
    }

    private fun sendWordChangedToRemote() {
        val idx = _classroomCursorIndex.value ?: return
        val word = wordMatchEngine.getWord(idx)?.text ?: ""
        val total = wordMatchEngine.words.size
        val json = JSONObject().apply {
            put("event", "WORD_CHANGED")
            put("word", word)
            put("index", idx)
            put("total", total)
        }.toString()
        nearbyManager.sendCommand(json)
    }

    fun stopClassroom() {
        nearbyStateJob?.cancel()
        nearbyStateJob = null
        nearbyManager.stop()
        _classroomCursorIndex.value = null
        _classroomState.value = ClassroomState.Inactive
    }

    // Renderer and file descriptors
    private var currentRenderer: PdfRenderer? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()

    // Bitmap reuse pool to avoid allocations when page dimensions match
    private val bitmapPool = Collections.synchronizedList(mutableListOf<Bitmap>())

    // LruCache holding at most 5 page bitmaps
    private val pageCache = object : LruCache<Int, Bitmap>(MAX_CACHED_PAGES) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted && key != null && oldValue != newValue) {
                // Remove reference from state flow when evicted so old memory can be GC'd
                _pageBitmaps[key]?.value = null
                if (oldValue != null && !oldValue.isRecycled && oldValue.isMutable) {
                    synchronized(bitmapPool) {
                        if (bitmapPool.size < 10) {
                            bitmapPool.add(oldValue)
                        }
                    }
                }
            }
        }
    }

    private fun obtainReusableBitmap(width: Int, height: Int): Bitmap {
        synchronized(bitmapPool) {
            val iterator = bitmapPool.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (!candidate.isRecycled && candidate.width == width && candidate.height == height && candidate.isMutable) {
                    iterator.remove()
                    return candidate
                } else if (candidate.isRecycled) {
                    iterator.remove()
                }
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    // Onboarding and post-onboarding prompt states
    val isOnboardingCompleted: StateFlow<Boolean> = preferencesRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _showPostOnboardingHindiPrompt = MutableStateFlow(false)
    val showPostOnboardingHindiPrompt: StateFlow<Boolean> = _showPostOnboardingHindiPrompt.asStateFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            val isDownloaded = translationRepository.isModelDownloaded()
            if (!isDownloaded) {
                _showPostOnboardingHindiPrompt.value = true
            }
        }
    }

    fun dismissPostOnboardingHindiPrompt() {
        _showPostOnboardingHindiPrompt.value = false
    }

    fun downloadHindiModelFromPrompt() {
        _showPostOnboardingHindiPrompt.value = false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lang = targetLanguage.value
                translationRepository.downloadModel(lang) { }
                _isHindiModelDownloaded.value = true
                if (lang == "hi") {
                    preferencesRepository.setHindiModelDownloaded(true)
                }
                _events.emit(ReaderEvent.ShowSnackbar("Translation model downloaded"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download translation model", e)
            }
        }
    }

    // OCR File Caching (MD5 of URI + Page Number)
    private fun getOcrCacheFile(uri: Uri?, pageIndex: Int): File {
        val dir = File(context.cacheDir, "ocr_cache").apply { if (!exists()) mkdirs() }
        val input = "${uri?.toString().orEmpty()}_page_$pageIndex"
        val md5Hash = MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$md5Hash.json")
    }

    private fun readCachedOcrWords(uri: Uri?, pageIndex: Int): List<PdfWord>? {
        return try {
            val file = getOcrCacheFile(uri, pageIndex)
            if (!file.exists()) return null
            val jsonStr = file.readText(Charsets.UTF_8)
            val array = JSONArray(jsonStr)
            val list = ArrayList<PdfWord>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PdfWord(
                        text = obj.getString("text"),
                        normalizedText = obj.getString("normalizedText"),
                        page = obj.getInt("page"),
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat(),
                        width = obj.getDouble("width").toFloat(),
                        height = obj.getDouble("height").toFloat()
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read OCR cache for page $pageIndex: ${e.message}")
            null
        }
    }

    private fun writeCachedOcrWords(uri: Uri?, pageIndex: Int, words: List<PdfWord>) {
        if (words.isEmpty()) return
        try {
            val file = getOcrCacheFile(uri, pageIndex)
            val array = JSONArray()
            for (w in words) {
                val obj = JSONObject().apply {
                    put("text", w.text)
                    put("normalizedText", w.normalizedText)
                    put("page", w.page)
                    put("x", w.x.toDouble())
                    put("y", w.y.toDouble())
                    put("width", w.width.toDouble())
                    put("height", w.height.toDouble())
                }
                array.put(obj)
            }
            file.writeText(array.toString(), Charsets.UTF_8)
            Log.d(TAG, "Saved OCR cache for page $pageIndex: ${file.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save OCR cache for page $pageIndex: ${e.message}")
        }
    }

    // State flows observed by individual page composables
    private val _pageBitmaps = ConcurrentHashMap<Int, MutableStateFlow<Bitmap?>>()
    private val activeRenderJobs = ConcurrentHashMap<Int, Job>()
    private var persistPageJob: Job? = null

    init {
        restoreLastSession()
        viewModelScope.launch {
            preferencesRepository.targetLanguage.collect { lang ->
                refreshTranslationModelStatus(lang)
            }
        }
        viewModelScope.launch {
            preferencesRepository.isHindiModelDownloaded.collect { downloaded ->
                if (targetLanguage.value == "hi") {
                    _isHindiModelDownloaded.value = downloaded
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isTranslationPromptShown.collect { shown ->
                isTranslationPromptShownPref = shown
            }
        }
    }

    fun refreshTranslationModelStatus(lang: String? = null) {
        viewModelScope.launch {
            val currentLang = lang ?: targetLanguage.value
            val downloaded = translationRepository.isModelDownloaded(currentLang)
            _isHindiModelDownloaded.value = downloaded
            if (currentLang == "hi") {
                preferencesRepository.setHindiModelDownloaded(downloaded)
            }
        }
    }

    private fun restoreLastSession() {
        viewModelScope.launch {
            try {
                val savedState = preferencesRepository.readerStateFlow.firstOrNull()
                if (savedState != null && !savedState.uriString.isNullOrBlank()) {
                    val uri = Uri.parse(savedState.uriString)
                    openPdf(
                        uri = uri,
                        initialPage = savedState.pageIndex,
                        isRestoring = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore previous reader session", e)
            }
        }
    }

    fun openPdf(uri: Uri, initialPage: Int = 0, isRestoring: Boolean = false) {
        // Take persistable permission if not already held
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.w(TAG, "Persistable URI permission not granted or already retained: ${e.message}")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            withContext(Dispatchers.IO) {
                renderMutex.withLock {
                    // Clean up any previously opened renderer
                    cleanupRendererInternal()

                    val pfd = openParcelFileDescriptor(uri)
                    if (pfd == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to open document. The file may no longer be available."
                            )
                        }
                        return@withLock
                    }

                    try {
                        val renderer = PdfRenderer(pfd)
                        val totalPages = renderer.pageCount
                        val fileName = queryFileName(uri)

                        if (totalPages <= 0) {
                            renderer.close()
                            pfd.close()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "This PDF contains no pages."
                                )
                            }
                            return@withLock
                        }

                        currentRenderer = renderer
                        currentPfd = pfd

                        val dims = mutableMapOf<Int, Pair<Int, Int>>()
                        for (i in 0 until totalPages) {
                            try {
                                val p = renderer.openPage(i)
                                dims[i] = Pair(p.width, p.height)
                                p.close()
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not get dimensions for page $i: ${e.message}")
                            }
                        }
                        _pageDimensions.value = dims

                        val safeInitialPage = initialPage.coerceIn(0, totalPages - 1)

                        // Detect PDF type (Text vs Scanned) on Dispatchers.IO
                        val detectedType = pdfTypeDetector.detectType(uri)
                        Log.i(TAG, "PDF Type detected: $detectedType for $fileName")

                        _uiState.update {
                            it.copy(
                                isPdfLoaded = true,
                                isLoading = false,
                                pdfUri = uri,
                                pdfName = fileName,
                                pageCount = totalPages,
                                currentPage = safeInitialPage + 1,
                                initialScrollPage = safeInitialPage,
                                isReadingMode = true,
                                areBarsVisible = true,
                                errorMessage = null,
                                pdfType = detectedType
                            )
                        }

                        if (totalPages >= 500) {
                            _events.emit(ReaderEvent.ShowSnackbar("Large document — OCR may be slow on some pages"))
                        }

                        // Persist to DataStore
                        preferencesRepository.saveLastPdf(
                            uriString = uri.toString(),
                            fileName = fileName,
                            pageIndex = safeInitialPage
                        )

                        // Extract words for the current page on open, prefetch next page (+1)
                        processPageWords(uri, safeInitialPage, detectedType)
                        if (safeInitialPage + 1 < totalPages) {
                            processPageWords(uri, safeInitialPage + 1, detectedType)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing PdfRenderer", e)
                        try {
                            pfd.close()
                        } catch (ignored: Exception) {}

                        val isPasswordProtected = e is SecurityException ||
                            e.javaClass.name.contains("InvalidPasswordException", ignoreCase = true) ||
                            e.message?.contains("password", ignoreCase = true) == true

                        val errorMsg = if (isPasswordProtected) {
                            "This PDF is password protected"
                        } else {
                            "Cannot open this file"
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMsg
                            )
                        }
                        _events.emit(ReaderEvent.ShowSnackbar(errorMsg))
                    }
                }
            }
        }
    }

    /**
     * Called when the visible page in LazyColumn changes.
     * Prefetches visible page + 1 page ahead + 1 page behind on Dispatchers.IO.
     * Persists the current page index to DataStore.
     */
    fun onVisiblePageChanged(pageIndex: Int, targetWidthPx: Int) {
        val totalPages = _uiState.value.pageCount
        if (totalPages <= 0 || pageIndex < 0 || pageIndex >= totalPages) return

        val displayPage = pageIndex + 1
        val pageChanged = _uiState.value.currentPage != displayPage
        if (pageChanged) {
            _uiState.update { it.copy(currentPage = displayPage) }
        }

        // Classroom mode: update word match engine if hosting and page changed
        if (pageChanged && (_classroomState.value is ClassroomState.Hosting || _classroomState.value is ClassroomState.Connected)) {
            val pageWords = _extractedWords.value[pageIndex] ?: emptyList()
            wordMatchEngine.loadWords(pageWords)
            if (pageWords.isNotEmpty()) {
                _classroomCursorIndex.value = 0
                sendWordChangedToRemote()
            } else {
                _classroomCursorIndex.value = null
            }
        }

        // Debounce saving to DataStore
        persistPageJob?.cancel()
        persistPageJob = viewModelScope.launch {
            delay(350)
            preferencesRepository.saveLastPage(pageIndex)
        }

        // Evict extracted words and highlights for pages more than 20 away to free memory
        if (_extractedWords.value.size > 25) {
            _extractedWords.update { map ->
                map.filterKeys { kotlin.math.abs(it - pageIndex) <= 20 }
            }
            _pageHighlights.update { map ->
                map.filterKeys { kotlin.math.abs(it - pageIndex) <= 20 }
            }
        }

        // Prefetch: visible page, 1 page ahead (+1), and 1 page behind (-1)
        prefetchPages(pageIndex, targetWidthPx)

        // Process words for visible page and prefetch 1 page ahead (+1)
        val currentType = _uiState.value.pdfType
        val currentUri = _uiState.value.pdfUri
        if (currentType != null) {
            processPageWords(currentUri, pageIndex, currentType)
            if (pageIndex + 1 < totalPages) {
                processPageWords(currentUri, pageIndex + 1, currentType)
            }
        }
    }

    /**
     * Dispatches word extraction to either PdfTextExtractor (for text PDFs) or
     * PdfOcrExtractor (for scanned PDFs). Caches results in _extractedWords so
     * already processed pages are never re-processed.
     */
    fun processPageWords(uri: Uri?, pageIndex: Int, type: PdfType? = _uiState.value.pdfType) {
        val total = _uiState.value.pageCount
        if (pageIndex < 0 || (total > 0 && pageIndex >= total)) return
        if (_extractedWords.value.containsKey(pageIndex)) return

        when (type) {
            is PdfType.Text -> {
                if (uri != null) {
                    extractWordsForPage(uri, pageIndex)
                }
            }
            is PdfType.Scanned -> {
                ocrWordsForPage(pageIndex)
            }
            null -> {}
        }
    }

    /**
     * Extracts words from a given page index for text-based PDFs and caches them in _extractedWords.
     */
    fun extractWordsForPage(uri: Uri, pageIndex: Int) {
        if (_extractedWords.value.containsKey(pageIndex)) return
        if (activeExtractionJobs[pageIndex]?.isActive == true) return

        activeExtractionJobs[pageIndex] = viewModelScope.launch(Dispatchers.IO) {
            try {
                val words = pdfTextExtractor.extractWords(uri, pageIndex)
                onWordsExtracted(pageIndex, words)
                Log.d(TAG, "Extracted ${words.size} words for page $pageIndex")
            } catch (e: Exception) {
                Log.e(TAG, "Failed extracting words for page $pageIndex: ${e.message}", e)
            } finally {
                activeExtractionJobs.remove(pageIndex)
            }
        }
    }

    /**
     * Performs ML Kit OCR on a scanned page using PdfOcrExtractor.
     * Renders at 2x resolution, scales coordinates to display coordinate space,
     * updates isOcrRunning state, and caches results in _extractedWords.
     */
    fun ocrWordsForPage(pageIndex: Int) {
        if (_extractedWords.value.containsKey(pageIndex)) return
        if (activeOcrJobs[pageIndex]?.isActive == true) return

        activeOcrJobs[pageIndex] = viewModelScope.launch(Dispatchers.IO) {
            val uri = _uiState.value.pdfUri

            // 1. Check persistent JSON file cache first
            val cachedWords = readCachedOcrWords(uri, pageIndex)
            if (cachedWords != null) {
                onWordsExtracted(pageIndex, cachedWords)
                Log.d(TAG, "OCR persistent cache hit for page $pageIndex: ${cachedWords.size} words")
                return@launch
            }

            _uiState.update { it.copy(isOcrRunning = true) }
            try {
                val words = renderMutex.withLock {
                    val renderer = currentRenderer ?: return@withLock emptyList()
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock emptyList()
                    val page = renderer.openPage(pageIndex)
                    try {
                        pdfOcrExtractor.extractWords(page, pageIndex)
                    } finally {
                        try {
                            page.close()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error closing page after OCR: ${e.message}")
                        }
                    }
                }

                if (words.isNotEmpty()) {
                    writeCachedOcrWords(uri, pageIndex, words)
                }

                onWordsExtracted(pageIndex, words)
                Log.d(TAG, "OCR succeeded for page $pageIndex with ${words.size} words")
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed for page $pageIndex: ${e.message}", e)
                val msg = e.message ?: ""
                val isModelDownloading = (e is MlKitException && (
                    e.errorCode == MlKitException.UNAVAILABLE ||
                    e.errorCode == 14 ||
                    msg.contains("download", ignoreCase = true) ||
                    msg.contains("waiting", ignoreCase = true)
                )) || msg.contains("download", ignoreCase = true) || msg.contains("waiting", ignoreCase = true)

                if (isModelDownloading) {
                    _events.emit(ReaderEvent.ShowSnackbar("Preparing OCR — this may take a moment"))
                } else {
                    _events.emit(ReaderEvent.ShowSnackbar("Could not read page ${pageIndex + 1}"))
                }

                // Cache empty list to avoid continuous retries while scrolling
                onWordsExtracted(pageIndex, emptyList())
            } finally {
                activeOcrJobs.remove(pageIndex)
                val stillRunning = activeOcrJobs.isNotEmpty()
                _uiState.update { it.copy(isOcrRunning = stillRunning) }
            }
        }
    }

    private fun onWordsExtracted(pageIndex: Int, words: List<PdfWord>) {
        _extractedWords.update { prevMap ->
            prevMap + (pageIndex to words)
        }
        _uiState.update { current ->
            current.copy(extractedWords = current.extractedWords + (pageIndex to words))
        }

        if ((_classroomState.value is ClassroomState.Hosting || _classroomState.value is ClassroomState.Connected) &&
            pageIndex == _uiState.value.currentPage - 1
        ) {
            wordMatchEngine.loadWords(words)
            if (words.isNotEmpty() && _classroomCursorIndex.value == null) {
                _classroomCursorIndex.value = 0
                sendWordChangedToRemote()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            val maxCount = preferencesRepository.maxHighlightsPerPage.first()
            val highlights = detectDifficultWordsUseCase(words, maxCount)
            _pageHighlights.update { it + (pageIndex to highlights) }
            _highlightedWords.value = _pageHighlights.value.values.flatten()
            Log.d(TAG, "Page $pageIndex detected ${highlights.size} difficult words")
        }
    }

    /**
     * Sets the selected word, looks up its dictionary definition, and extracts the context sentence.
     */
    fun selectWord(word: PdfWord?): Job? {
        _selectedWord.value = word
        _sentenceTranslationState.value = SentenceTranslationState.Idle
        activeWordAiJob?.cancel()
        activeWordAiJob = null
        _aiWordExplanationText.value = null
        _isAiWordExplaining.value = false
        _isGeminiConfigured.value = geminiRepository.isConfigured()
        if (word != null) {
            Log.d(TAG, "selectedWord: ${word.text} (page ${word.page})")
            return viewModelScope.launch(Dispatchers.IO) {
                // Check model status & first-launch prompt
                val isDownloaded = translationRepository.isModelDownloaded()
                _isHindiModelDownloaded.value = isDownloaded

                if (!isDownloaded && !isTranslationPromptShownPref) {
                    _showTranslationDownloadPrompt.value = true
                }

                // Dictionary lookup (Room cache -> API -> Room cache, permanent & non-blocking)
                val entry = dictionaryRepository.getDefinition(word.normalizedText)
                    ?: dictionaryRepository.getDefinition(word.text)
                _selectedWordDefinition.value = entry

                // Context sentence: extract the 10 PdfWords surrounding selectedWord on the same page
                val pageWords = _extractedWords.value[word.page] ?: emptyList()
                val idx = pageWords.indexOfFirst {
                    it == word || (it.normalizedText == word.normalizedText && it.page == word.page && it.x == word.x && it.y == word.y)
                }

                val sentence = if (idx != -1 && pageWords.isNotEmpty()) {
                    val start = (idx - 5).coerceAtLeast(0)
                    val end = (idx + 5).coerceAtMost(pageWords.size - 1)
                    pageWords.subList(start, end + 1).joinToString(" ") { it.text }
                } else {
                    word.text
                }
                _selectedWordContextSentence.value = sentence
            }
        } else {
            _selectedWordDefinition.value = null
            _selectedWordContextSentence.value = null
            _showTranslationDownloadPrompt.value = false
            return null
        }
    }

    /**
     * Pronounce a word aloud using Android Text-to-Speech.
     */
    fun pronounceWord(word: String) {
        ttsHelper.speak(word)
    }

    /**
     * Copy arbitrary text to Android clipboard and notify the user via Snackbar.
     */
    fun copyTextToClipboard(text: String, label: String = "Folio Reader") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            viewModelScope.launch {
                _events.emit(ReaderEvent.ShowSnackbar("Copied to clipboard"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy text to clipboard: ${e.message}")
        }
    }

    /**
     * Looks up a word from raw text (e.g. from floating selection bar).
     */
    fun selectWordByText(text: String, pageIndex: Int = maxOf(0, _uiState.value.currentPage - 1)) {
        val clean = text.trim().trim('"', '\'', '.', ',', '!', '?', ';', ':', '(', ')')
        val firstWord = clean.split("\\s+".toRegex()).firstOrNull()?.trim() ?: clean
        if (firstWord.isNotBlank()) {
            val pdfWord = PdfWord(
                text = firstWord,
                page = pageIndex,
                x = 0f,
                y = 0f,
                width = 0f,
                height = 0f
            )
            clearTextSelection()
            selectWord(pdfWord)
        }
    }

    /**
     * Request Gemini AI explanation for current selected word in context.
     */
    fun requestAiExplanationForSelectedWord(): Job? {
        val word = _selectedWord.value?.text ?: return null
        val context = _selectedWordContextSentence.value.orEmpty()
        activeWordAiJob?.cancel()
        _isAiWordExplaining.value = true
        _aiWordExplanationText.value = ""

        activeWordAiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                geminiRepository.explainWord(word, context).collect { chunk ->
                    val current = _aiWordExplanationText.value.orEmpty()
                    _aiWordExplanationText.value = current + chunk
                }
            } catch (e: Exception) {
                _aiWordExplanationText.value = "Error: ${e.localizedMessage ?: "Failed to generate explanation"}"
            } finally {
                _isAiWordExplaining.value = false
            }
        }
        return activeWordAiJob
    }

    fun refreshGeminiStatus() {
        _isGeminiConfigured.value = geminiRepository.isConfigured()
    }

    fun onTextSelectedForExplanation(text: String, offset: androidx.compose.ui.geometry.Offset, pageIndex: Int) {
        if (text.isNotBlank()) {
            _activeTextSelection.value = TextSelectionAction(text.trim(), offset, pageIndex)
        }
    }

    fun clearTextSelection() {
        _activeTextSelection.value = null
    }

    fun triggerExplainSelectedText(text: String): Job? {
        _activeTextSelection.value = null
        _showExplainSheet.value = true
        _explainSheetSelectedText.value = text
        _explainSheetResponseText.value = ""
        _isExplainingSheet.value = true

        activeSheetAiJob?.cancel()
        activeSheetAiJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val wordCount = text.split("\\s+".toRegex()).size
                val flow = if (wordCount > 20) {
                    geminiRepository.simplifyParagraph(text)
                } else {
                    geminiRepository.explainSentence(text)
                }
                flow.collect { chunk ->
                    val current = _explainSheetResponseText.value.orEmpty()
                    _explainSheetResponseText.value = current + chunk
                }
            } catch (e: Exception) {
                _explainSheetResponseText.value = "Error: ${e.localizedMessage ?: "Failed to generate explanation"}"
            } finally {
                _isExplainingSheet.value = false
            }
        }
        return activeSheetAiJob
    }

    fun dismissExplainSheet() {
        activeSheetAiJob?.cancel()
        activeSheetAiJob = null
        _showExplainSheet.value = false
        _explainSheetSelectedText.value = null
        _explainSheetResponseText.value = null
        _isExplainingSheet.value = false
    }

    /**
     * Save button action: status = LEARNING, increment lookupCount, dismiss sheet.
     */
    fun saveSelectedWord(): kotlinx.coroutines.Job? {
        val word = _selectedWord.value ?: return null
        _selectedWord.value = null
        _selectedWordDefinition.value = null
        _selectedWordContextSentence.value = null
        _sentenceTranslationState.value = SentenceTranslationState.Idle
        _showTranslationDownloadPrompt.value = false
        activeWordAiJob?.cancel()
        activeWordAiJob = null
        _aiWordExplanationText.value = null
        _isAiWordExplaining.value = false
        return viewModelScope.launch {
            val bookPath = _uiState.value.pdfUri?.toString()
            userWordRepository.saveWord(word.normalizedText, bookPath)
            refreshHighlightsForPage(word.page)
        }
    }

    /**
     * Know it button action: status = KNOWN, dismiss sheet (word disappears from highlights).
     */
    fun markSelectedWordKnown(): kotlinx.coroutines.Job? {
        val word = _selectedWord.value ?: return null
        _selectedWord.value = null
        _selectedWordDefinition.value = null
        _selectedWordContextSentence.value = null
        _sentenceTranslationState.value = SentenceTranslationState.Idle
        _showTranslationDownloadPrompt.value = false
        activeWordAiJob?.cancel()
        activeWordAiJob = null
        _aiWordExplanationText.value = null
        _isAiWordExplaining.value = false
        return viewModelScope.launch {
            val bookPath = _uiState.value.pdfUri?.toString()
            userWordRepository.markAs(word.normalizedText, WordStatus.KNOWN, bookPath)
            refreshHighlightsForPage(word.page)
        }
    }

    /**
     * Ignore button action: status = IGNORED, dismiss sheet.
     */
    fun markSelectedWordIgnored(): kotlinx.coroutines.Job? {
        val word = _selectedWord.value ?: return null
        _selectedWord.value = null
        _selectedWordDefinition.value = null
        _selectedWordContextSentence.value = null
        _sentenceTranslationState.value = SentenceTranslationState.Idle
        _showTranslationDownloadPrompt.value = false
        activeWordAiJob?.cancel()
        activeWordAiJob = null
        _aiWordExplanationText.value = null
        _isAiWordExplaining.value = false
        return viewModelScope.launch {
            val bookPath = _uiState.value.pdfUri?.toString()
            userWordRepository.markAs(word.normalizedText, WordStatus.IGNORED, bookPath)
            refreshHighlightsForPage(word.page)
        }
    }

    fun dismissWordPanel() {
        _selectedWord.value = null
        _selectedWordDefinition.value = null
        _selectedWordContextSentence.value = null
        _sentenceTranslationState.value = SentenceTranslationState.Idle
        _showTranslationDownloadPrompt.value = false
        activeWordAiJob?.cancel()
        activeWordAiJob = null
        _aiWordExplanationText.value = null
        _isAiWordExplaining.value = false
    }

    fun dismissTranslationPrompt(markAsShown: Boolean = true): Job? {
        _showTranslationDownloadPrompt.value = false
        if (markAsShown) {
            isTranslationPromptShownPref = true
            return viewModelScope.launch {
                preferencesRepository.setTranslationPromptShown(true)
            }
        }
        return null
    }

    fun translateSentence(sentence: String): Job {
        _sentenceTranslationState.value = SentenceTranslationState.Translating
        return viewModelScope.launch(Dispatchers.IO) {
            val lang = targetLanguage.value
            val result = translationRepository.translate(sentence, lang)
            if (result != null) {
                _sentenceTranslationState.value = SentenceTranslationState.Success(result)
            } else {
                _sentenceTranslationState.value = SentenceTranslationState.Error("Translation unavailable")
            }
        }
    }

    /**
     * Re-runs difficult word detection on the given page to immediately reflect status changes.
     */
    fun refreshHighlightsForPage(pageIndex: Int) {
        val words = _extractedWords.value[pageIndex] ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val maxCount = preferencesRepository.maxHighlightsPerPage.first()
            val highlights = detectDifficultWordsUseCase(words, maxCount)
            _pageHighlights.update { it + (pageIndex to highlights) }
            _highlightedWords.value = _pageHighlights.value.values.flatten()
            Log.d(TAG, "Refreshed page $pageIndex highlights: ${highlights.size} words")
        }
    }

    /**
     * Toggles Vocabulary Assistance ON/OFF in DataStore.
     */
    fun toggleVocabAssistance(enabled: Boolean? = null) {
        viewModelScope.launch {
            val target = enabled ?: !isVocabAssistanceEnabled.value
            preferencesRepository.setVocabAssistanceEnabled(target)
        }
    }

    /**
     * Returns the detected difficult words for a specific page.
     */
    fun getHighlightsForPage(pageIndex: Int): List<PdfWord> {
        return _pageHighlights.value[pageIndex] ?: emptyList()
    }

    /**
     * Returns original PDF page dimensions (width, height) in PDF points.
     */
    fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? {
        return _pageDimensions.value[pageIndex]
    }

    /**
     * Returns extracted words for a page if available.
     */
    fun getWordsForPage(pageIndex: Int): List<PdfWord> {
        return _extractedWords.value[pageIndex] ?: emptyList()
    }

    fun requestPageRender(pageIndex: Int, targetWidthPx: Int) {
        val total = _uiState.value.pageCount
        if (pageIndex in 0 until total) {
            renderPageAsync(pageIndex, targetWidthPx)
        }
    }

    private fun prefetchPages(centerIndex: Int, targetWidthPx: Int) {
        val total = _uiState.value.pageCount
        if (total <= 0) return

        // Priority order: current visible page, next page, previous page
        val pages = listOf(centerIndex, centerIndex + 1, centerIndex - 1)
            .filter { it in 0 until total }

        for (p in pages) {
            renderPageAsync(p, targetWidthPx)
        }
    }

    private fun renderPageAsync(pageIndex: Int, targetWidthPx: Int) {
        // Check LruCache first
        val cachedBitmap = synchronized(pageCache) { pageCache.get(pageIndex) }
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            _pageBitmaps.getOrPut(pageIndex) { MutableStateFlow(cachedBitmap) }.value = cachedBitmap
            return
        }

        // If a render job for this page is already running, skip starting another
        if (activeRenderJobs[pageIndex]?.isActive == true) {
            return
        }

        activeRenderJobs[pageIndex] = viewModelScope.launch(Dispatchers.IO) {
            try {
                renderMutex.withLock {
                    // Double-check cache inside lock
                    val cachedInside = synchronized(pageCache) { pageCache.get(pageIndex) }
                    if (cachedInside != null && !cachedInside.isRecycled) {
                        _pageBitmaps.getOrPut(pageIndex) { MutableStateFlow(cachedInside) }.value = cachedInside
                        return@withLock
                    }

                    val renderer = currentRenderer ?: return@withLock
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock

                    val page = renderer.openPage(pageIndex)
                    try {
                        val pageWidth = page.width
                        val pageHeight = page.height
                        val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()

                        val safeWidth = targetWidthPx.coerceIn(360, 2400)
                        val calculatedHeight = (safeWidth * aspectRatio).toInt().coerceAtLeast(1)

                        val bitmap = obtainReusableBitmap(safeWidth, calculatedHeight)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        synchronized(pageCache) {
                            pageCache.put(pageIndex, bitmap)
                        }

                        _pageBitmaps.getOrPut(pageIndex) { MutableStateFlow(bitmap) }.value = bitmap
                    } finally {
                        page.close()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering page $pageIndex", e)
            } finally {
                activeRenderJobs.remove(pageIndex)
            }
        }
    }

    fun getPageBitmap(pageIndex: Int): StateFlow<Bitmap?> {
        return _pageBitmaps.getOrPut(pageIndex) {
            val cached = synchronized(pageCache) { pageCache.get(pageIndex) }
            MutableStateFlow(cached)
        }.asStateFlow()
    }

    fun toggleBars() {
        _uiState.update { it.copy(areBarsVisible = !it.areBarsVisible) }
    }

    fun setBarsVisible(visible: Boolean) {
        _uiState.update { it.copy(areBarsVisible = visible) }
    }

    fun closeCurrentDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock {
                cleanupRendererInternal()
                _uiState.update {
                    it.copy(
                        isPdfLoaded = false,
                        isReadingMode = false,
                        pdfUri = null,
                        pdfName = "",
                        pageCount = 0,
                        currentPage = 1,
                        initialScrollPage = 0,
                        pdfType = null,
                        extractedWords = emptyMap()
                    )
                }
                preferencesRepository.clearLastPdf()
            }
        }
    }

    private fun cleanupRendererInternal() {
        activeRenderJobs.values.forEach { it.cancel() }
        activeRenderJobs.clear()

        activeExtractionJobs.values.forEach { it.cancel() }
        activeExtractionJobs.clear()

        activeOcrJobs.values.forEach { it.cancel() }
        activeOcrJobs.clear()
        _extractedWords.value = emptyMap()
        _pageHighlights.value = emptyMap()
        _highlightedWords.value = emptyList()
        _selectedWord.value = null
        _selectedWordDefinition.value = null
        _selectedWordContextSentence.value = null
        _pageDimensions.value = emptyMap()
        _uiState.update { it.copy(isOcrRunning = false) }

        try {
            currentRenderer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PdfRenderer", e)
        } finally {
            currentRenderer = null
        }

        try {
            currentPfd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ParcelFileDescriptor", e)
        } finally {
            currentPfd = null
        }

        synchronized(pageCache) {
            pageCache.evictAll()
        }
        _pageBitmaps.clear()
    }

    private fun openParcelFileDescriptor(uri: Uri): ParcelFileDescriptor? {
        // Direct open
        try {
            val directPfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (directPfd != null) {
                return directPfd
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct openFileDescriptor failed, attempting fallback copy: ${e.message}")
        }

        // Fallback: Copy to app's cache directory
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val cacheFile = File(context.cacheDir, "current_pdf_cache.pdf")
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fallback copy for URI $uri", e)
            null
        }
    }

    private fun queryFileName(uri: Uri): String {
        var name = "Document.pdf"
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val displayName = cursor.getString(nameIndex)
                        if (!displayName.isNullOrBlank()) {
                            name = displayName
                        }
                    }
                }
            } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
                name = uri.lastPathSegment ?: "Document.pdf"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve file name for URI $uri", e)
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        stopClassroom()
        ttsHelper.shutdown()
        // Close PdfRenderer and all file descriptors on ViewModel cleared
        try {
            currentRenderer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing renderer in onCleared", e)
        } finally {
            currentRenderer = null
        }

        try {
            currentPfd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PFD in onCleared", e)
        } finally {
            currentPfd = null
        }

        synchronized(pageCache) {
            pageCache.evictAll()
        }
        _pageBitmaps.clear()
    }
}
