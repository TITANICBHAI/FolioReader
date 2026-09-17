package com.tbtechs.folioreader.ui.vocabulary

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbtechs.folioreader.data.ai.IGeminiRepository
import com.tbtechs.folioreader.data.db.entities.CachedDefinition
import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import com.tbtechs.folioreader.data.dictionary.DictionaryRepository
import com.tbtechs.folioreader.data.pdf.PdfTextExtractor
import com.tbtechs.folioreader.data.preferences.ReaderPreferencesRepository
import com.tbtechs.folioreader.data.translation.ITranslationRepository
import com.tbtechs.folioreader.data.vocabulary.UserWordRepository
import com.tbtechs.folioreader.data.vocabulary.VocabularyExportFormat
import com.tbtechs.folioreader.data.vocabulary.VocabularyExporter
import com.tbtechs.folioreader.domain.model.WordStatus
import com.tbtechs.folioreader.domain.usecase.DetectDifficultWordsUseCase
import com.tbtechs.folioreader.util.TextToSpeechHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "VocabularyViewModel"

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val pdfTextExtractor: PdfTextExtractor,
    private val detectDifficultWordsUseCase: DetectDifficultWordsUseCase,
    private val translationRepository: ITranslationRepository,
    private val geminiRepository: IGeminiRepository,
    private val ttsHelper: TextToSpeechHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 0 = Learning, 1 = Reviewing, 2 = Mastered, 3 = All
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val learningWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllByStatus(WordStatus.LEARNING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviewingWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllByStatus(WordStatus.REVIEWING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val masteredWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllByStatus(WordStatus.MASTERED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backwards compatibility for any tests or components expecting knownWords
    val knownWords: StateFlow<List<UserWordEntity>> = masteredWords

    val allWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWord = MutableStateFlow<UserWordEntity?>(null)
    val selectedWord: StateFlow<UserWordEntity?> = _selectedWord.asStateFlow()

    private val _selectedWordEntry = MutableStateFlow<CachedDefinition?>(null)
    val selectedWordEntry: StateFlow<CachedDefinition?> = _selectedWordEntry.asStateFlow()

    private val _wordToEdit = MutableStateFlow<UserWordEntity?>(null)
    val wordToEdit: StateFlow<UserWordEntity?> = _wordToEdit.asStateFlow()

    // --- Practice State (Flashcards & Quick Quiz) ---
    private val _practiceMode = MutableStateFlow(PracticeMode.NONE)
    val practiceMode: StateFlow<PracticeMode> = _practiceMode.asStateFlow()

    private val _practiceWords = MutableStateFlow<List<PracticeWordItem>>(emptyList())
    val practiceWords: StateFlow<List<PracticeWordItem>> = _practiceWords.asStateFlow()

    private val _currentFlashcardIndex = MutableStateFlow(0)
    val currentFlashcardIndex: StateFlow<Int> = _currentFlashcardIndex.asStateFlow()

    private val _isFlashcardFlipped = MutableStateFlow(false)
    val isFlashcardFlipped: StateFlow<Boolean> = _isFlashcardFlipped.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _selectedQuizOptionIndex = MutableStateFlow<Int?>(null)
    val selectedQuizOptionIndex: StateFlow<Int?> = _selectedQuizOptionIndex.asStateFlow()

    private val _isQuizOptionSubmitted = MutableStateFlow(false)
    val isQuizOptionSubmitted: StateFlow<Boolean> = _isQuizOptionSubmitted.asStateFlow()

    private val _quizAnswers = MutableStateFlow<List<QuizAnswer>>(emptyList())
    val quizAnswers: StateFlow<List<QuizAnswer>> = _quizAnswers.asStateFlow()

    private val _studySource = MutableStateFlow<StudySource>(StudySource.SavedWords())
    val studySource: StateFlow<StudySource> = _studySource.asStateFlow()

    private val _isRandomSelection = MutableStateFlow(false)
    val isRandomSelection: StateFlow<Boolean> = _isRandomSelection.asStateFlow()

    private val _isPracticeLoading = MutableStateFlow(false)
    val isPracticeLoading: StateFlow<Boolean> = _isPracticeLoading.asStateFlow()

    private val _practiceErrorMessage = MutableStateFlow<String?>(null)
    val practiceErrorMessage: StateFlow<String?> = _practiceErrorMessage.asStateFlow()

    private val _recentPdfState = MutableStateFlow<Pair<Uri, String>?>(null)
    val recentPdfState: StateFlow<Pair<Uri, String>?> = _recentPdfState.asStateFlow()

    private val _isGeminiConfigured = MutableStateFlow(geminiRepository.isConfigured())
    val isGeminiConfigured: StateFlow<Boolean> = _isGeminiConfigured.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    private val _isAiExplaining = MutableStateFlow(false)
    val isAiExplaining: StateFlow<Boolean> = _isAiExplaining.asStateFlow()

    init {
        // Observe last read PDF for quick "Make test using recent PDF"
        viewModelScope.launch {
            preferencesRepository.readerStateFlow.collect { state ->
                val uriStr = state.uriString
                val name = state.fileName
                if (!uriStr.isNullOrBlank()) {
                    try {
                        val uri = Uri.parse(uriStr)
                        _recentPdfState.value = Pair(uri, name ?: "Recent Document")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing saved PDF URI: ${e.message}")
                    }
                }
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectWord(entity: UserWordEntity?) {
        _selectedWord.value = entity
        if (entity != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val entry = dictionaryRepository.getDefinition(entity.word)
                _selectedWordEntry.value = entry
            }
        } else {
            _selectedWordEntry.value = null
        }
    }

    fun saveWord(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.saveWord(entity.word, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun markKnown(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.markAs(entity.word, WordStatus.MASTERED, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun markIgnored(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.markAs(entity.word, WordStatus.IGNORED, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun dismissWordPanel() {
        _selectedWord.value = null
        _selectedWordEntry.value = null
    }

    fun showEditDialog(entity: UserWordEntity) {
        _wordToEdit.value = entity
    }

    fun dismissEditDialog() {
        _wordToEdit.value = null
    }

    fun updateWordStatus(word: String, newStatus: WordStatus) {
        viewModelScope.launch {
            userWordRepository.markAs(word, newStatus, null)
            _wordToEdit.value = null
        }
    }

    fun deleteWord(word: String) {
        viewModelScope.launch {
            userWordRepository.deleteWord(word)
            _wordToEdit.value = null
        }
    }

    // --- Study Source and Settings ---

    fun setStudySource(source: StudySource) {
        _studySource.value = source
    }

    fun setRandomSelection(enabled: Boolean) {
        _isRandomSelection.value = enabled
    }

    fun clearPracticeError() {
        _practiceErrorMessage.value = null
    }

    fun pronounceWord(word: String) {
        ttsHelper.speak(word)
    }

    // --- Flashcards Mode ---

    fun startFlashcards(source: StudySource? = null, random: Boolean? = null) {
        val targetSource = source ?: _studySource.value
        val useRandom = random ?: _isRandomSelection.value
        _studySource.value = targetSource
        _isRandomSelection.value = useRandom

        viewModelScope.launch {
            _isPracticeLoading.value = true
            _practiceErrorMessage.value = null
            try {
                val words = resolvePracticeWords(targetSource, useRandom)
                if (words.isEmpty()) {
                    _practiceErrorMessage.value = "No words available for the selected study source. Try adding words while reading or select a PDF."
                    _isPracticeLoading.value = false
                    return@launch
                }
                _practiceWords.value = words
                _currentFlashcardIndex.value = 0
                _isFlashcardFlipped.value = false
                _aiExplanation.value = null
                _practiceMode.value = PracticeMode.FLASHCARDS
            } catch (e: Exception) {
                Log.e(TAG, "Error starting flashcards: ${e.message}", e)
                _practiceErrorMessage.value = "Could not prepare flashcards: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isPracticeLoading.value = false
            }
        }
    }

    fun flipFlashcard() {
        _isFlashcardFlipped.value = !_isFlashcardFlipped.value
    }

    fun nextFlashcard() {
        if (_currentFlashcardIndex.value < _practiceWords.value.size - 1) {
            _currentFlashcardIndex.value++
            _isFlashcardFlipped.value = false
            _aiExplanation.value = null
        }
    }

    fun previousFlashcard() {
        if (_currentFlashcardIndex.value > 0) {
            _currentFlashcardIndex.value--
            _isFlashcardFlipped.value = false
            _aiExplanation.value = null
        }
    }

    fun shuffleFlashcards() {
        val currentList = _practiceWords.value.shuffled()
        _practiceWords.value = currentList
        _currentFlashcardIndex.value = 0
        _isFlashcardFlipped.value = false
        _aiExplanation.value = null
    }

    fun rateFlashcardWord(newStatus: WordStatus) {
        val list = _practiceWords.value
        val index = _currentFlashcardIndex.value
        if (index in list.indices) {
            val item = list[index]
            viewModelScope.launch {
                userWordRepository.markAs(item.word, newStatus, item.sourceBook)
                // Update in-memory item status
                val updatedList = list.toMutableList().apply {
                    this[index] = item.copy(status = newStatus)
                }
                _practiceWords.value = updatedList
            }
            if (index < list.size - 1) {
                nextFlashcard()
            }
        }
    }

    // --- Quick Quiz Mode ---

    fun startQuiz(source: StudySource? = null, random: Boolean? = null) {
        val targetSource = source ?: _studySource.value
        val useRandom = random ?: _isRandomSelection.value
        _studySource.value = targetSource
        _isRandomSelection.value = useRandom

        viewModelScope.launch {
            _isPracticeLoading.value = true
            _practiceErrorMessage.value = null
            try {
                val words = resolvePracticeWords(targetSource, useRandom = true)
                if (words.isEmpty()) {
                    _practiceErrorMessage.value = "No words available for the quiz. Save words from reading or select a PDF to generate a quiz."
                    _isPracticeLoading.value = false
                    return@launch
                }
                val questions = generateQuizQuestions(words, questionCount = 5)
                if (questions.isEmpty()) {
                    _practiceErrorMessage.value = "Not enough unique vocabulary words to build multiple choice questions. Please add more words."
                    _isPracticeLoading.value = false
                    return@launch
                }
                _quizQuestions.value = questions
                _currentQuizIndex.value = 0
                _selectedQuizOptionIndex.value = null
                _isQuizOptionSubmitted.value = false
                _quizAnswers.value = emptyList()
                _practiceMode.value = PracticeMode.QUIZ
            } catch (e: Exception) {
                Log.e(TAG, "Error starting quiz: ${e.message}", e)
                _practiceErrorMessage.value = "Could not build quiz: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isPracticeLoading.value = false
            }
        }
    }

    fun submitQuizAnswer(optionIndex: Int) {
        if (_isQuizOptionSubmitted.value) return
        val currentIdx = _currentQuizIndex.value
        val questions = _quizQuestions.value
        if (currentIdx !in questions.indices) return

        val question = questions[currentIdx]
        val isCorrect = optionIndex == question.correctIndex
        _selectedQuizOptionIndex.value = optionIndex
        _isQuizOptionSubmitted.value = true

        // Spaced Repetition Mastery Progression:
        // Correct: Learning -> Reviewing, Reviewing -> Mastered, Mastered -> Mastered
        // Incorrect: Reverts to Learning
        val currentStatus = question.targetWord.status
        val newStatus = if (isCorrect) {
            when (currentStatus) {
                WordStatus.LEARNING -> WordStatus.REVIEWING
                WordStatus.REVIEWING -> WordStatus.MASTERED
                WordStatus.MASTERED, WordStatus.KNOWN -> WordStatus.MASTERED
                else -> WordStatus.REVIEWING
            }
        } else {
            WordStatus.LEARNING
        }

        viewModelScope.launch {
            userWordRepository.markAs(question.targetWord.word, newStatus, question.targetWord.sourceBook)
        }

        val answer = QuizAnswer(
            question = question,
            selectedIndex = optionIndex,
            isCorrect = isCorrect,
            updatedStatus = newStatus
        )
        _quizAnswers.value = _quizAnswers.value + answer
    }

    fun nextQuizQuestion() {
        val nextIdx = _currentQuizIndex.value + 1
        if (nextIdx < _quizQuestions.value.size) {
            _currentQuizIndex.value = nextIdx
            _selectedQuizOptionIndex.value = null
            _isQuizOptionSubmitted.value = false
        } else {
            // Quiz finished -> Show Results
            _practiceMode.value = PracticeMode.QUIZ_RESULTS
        }
    }

    fun retakeQuiz() {
        startQuiz()
    }

    fun exitPractice() {
        _practiceMode.value = PracticeMode.NONE
        _aiExplanation.value = null
    }

    // --- AI BYOK Explanation Support ---

    fun requestAiExplanation(word: String, contextSentence: String = "") {
        if (!geminiRepository.isConfigured()) {
            _aiExplanation.value = "Gemini API key is not configured. You can configure your key in Settings."
            return
        }
        viewModelScope.launch {
            _isAiExplaining.value = true
            _aiExplanation.value = ""
            try {
                val flow = geminiRepository.explainWord(word, contextSentence)
                flow.collect { chunk ->
                    _aiExplanation.value = (_aiExplanation.value ?: "") + chunk
                }
            } catch (e: Exception) {
                _aiExplanation.value = "Unable to get AI explanation: ${e.message}"
            } finally {
                _isAiExplaining.value = false
            }
        }
    }

    fun saveGeminiApiKey(key: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = geminiRepository.saveAndTestKey(key)
            result.onSuccess {
                _isGeminiConfigured.value = true
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Invalid key")
            }
        }
    }

    // --- Helper to resolve practice words from source ---

    private suspend fun resolvePracticeWords(
        source: StudySource,
        useRandom: Boolean
    ): List<PracticeWordItem> = withContext(Dispatchers.IO) {
        when (source) {
            is StudySource.SavedWords -> {
                val filter = source.statusFilter
                val entities = when (filter) {
                    null -> userWordRepository.getAllWordsList()
                    WordStatus.LEARNING -> userWordRepository.getWordsListByStatuses(listOf(WordStatus.LEARNING))
                    WordStatus.REVIEWING -> userWordRepository.getWordsListByStatuses(listOf(WordStatus.REVIEWING))
                    WordStatus.MASTERED, WordStatus.KNOWN -> userWordRepository.getWordsListByStatuses(listOf(WordStatus.MASTERED, WordStatus.KNOWN))
                    else -> userWordRepository.getAllWordsList()
                }

                val selectedEntities = if (useRandom) {
                    entities.shuffled().take(15.coerceAtMost(entities.size))
                } else {
                    entities
                }

                selectedEntities.map { entity ->
                    resolveWordDetails(entity.word, parseStatus(entity.status), entity.sourceBookPath)
                }
            }

            is StudySource.FromPdf -> {
                val words = extractWordsFromPdf(source.uri, source.title)
                val selectedWords = if (useRandom) {
                    words.shuffled().take(15.coerceAtMost(words.size))
                } else {
                    words
                }
                selectedWords.map { word ->
                    val status = userWordRepository.getStatus(word)
                    resolveWordDetails(word, status, source.title)
                }
            }
        }
    }

    private suspend fun extractWordsFromPdf(uri: Uri, title: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // Extract from initial 10 pages
            val extractedWords = mutableListOf<com.tbtechs.folioreader.domain.model.PdfWord>()
            for (page in 0 until 10) {
                val pageWords = pdfTextExtractor.extractWords(uri, page)
                if (pageWords.isNotEmpty()) {
                    extractedWords.addAll(pageWords)
                }
            }

            if (extractedWords.isNotEmpty()) {
                val difficult = detectDifficultWordsUseCase(extractedWords, maxCount = 20)
                if (difficult.isNotEmpty()) {
                    return@withContext difficult.map { it.normalizedText }.distinct()
                }
                return@withContext extractedWords
                    .map { it.normalizedText }
                    .filter { it.length >= 4 }
                    .distinct()
                    .take(20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting words from PDF $uri: ${e.message}", e)
        }
        emptyList()
    }

    private suspend fun resolveWordDetails(
        word: String,
        status: WordStatus,
        sourceBook: String?
    ): PracticeWordItem = withContext(Dispatchers.IO) {
        val def = dictionaryRepository.getDefinition(word)
        val definition = def?.englishDefinition?.takeIf { it.isNotBlank() }
            ?: "Saved vocabulary term from reading session"
        val hindi = def?.hindiMeaning?.takeIf { it.isNotBlank() } ?: run {
            try {
                translationRepository.translate(word, "hi") ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        val phonetic = def?.phonetic.orEmpty()
        val pos = def?.partOfSpeech.orEmpty()
        val example = def?.example.orEmpty()
        val synonyms = def?.synonyms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val antonyms = def?.antonyms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        PracticeWordItem(
            word = word,
            definition = definition,
            hindiMeaning = hindi,
            phonetic = phonetic,
            partOfSpeech = pos,
            example = example,
            synonyms = synonyms,
            antonyms = antonyms,
            status = status,
            sourceBook = sourceBook
        )
    }

    private suspend fun generateQuizQuestions(
        words: List<PracticeWordItem>,
        questionCount: Int = 5
    ): List<QuizQuestion> = withContext(Dispatchers.Default) {
        val pool = words.filter { it.definition.isNotBlank() && it.word.isNotBlank() }
        if (pool.isEmpty()) return@withContext emptyList()

        val selected = pool.shuffled().take(questionCount)
        val allDefinitions = pool.map { it.definition }.distinct()

        val fallbackDistractors = listOf(
            "Lasting for a very short time; fleeting or momentary",
            "Present, appearing, or found everywhere simultaneously",
            "Difficult to understand; obscure or known by only a few",
            "Extremely rigorous, severe, or cruel in nature",
            "Displaying fond or excessively enthusiastic approval",
            "Capable of making mistakes or being erroneous",
            "Tending to talk a great deal; extremely talkative"
        )

        selected.mapIndexed { index, target ->
            val correctAnswer = target.definition
            val otherDefs = (allDefinitions - correctAnswer).shuffled()
            val distractors = if (otherDefs.size >= 3) {
                otherDefs.take(3)
            } else {
                (otherDefs + (fallbackDistractors - correctAnswer)).distinct().take(3)
            }

            val options = (distractors + correctAnswer).shuffled()
            val correctIdx = options.indexOf(correctAnswer)

            val explanation = buildString {
                append("\"${target.word}\"")
                if (target.partOfSpeech.isNotBlank()) append(" (${target.partOfSpeech})")
                if (target.phonetic.isNotBlank()) append(" [${target.phonetic}]")
                append(": ${target.definition}")
                if (target.hindiMeaning.isNotBlank()) {
                    append("\nMeaning: ${target.hindiMeaning}")
                }
                if (target.example.isNotBlank()) {
                    append("\nExample: \"${target.example}\"")
                }
            }

            QuizQuestion(
                id = index + 1,
                targetWord = target,
                questionPrompt = "What is the meaning of \"${target.word}\"?",
                options = options,
                correctIndex = correctIdx,
                explanation = explanation
            )
        }
    }

    private fun parseStatus(statusStr: String): WordStatus {
        return try {
            WordStatus.valueOf(statusStr)
        } catch (e: Exception) {
            WordStatus.LEARNING
        }
    }

    suspend fun getExportContent(
        format: VocabularyExportFormat,
        scope: ExportScope
    ): Pair<String, String> {
        val wordsToExport = when (scope) {
            ExportScope.ALL -> allWords.value
            ExportScope.CURRENT_TAB -> when (selectedTab.value) {
                0 -> learningWords.value
                1 -> reviewingWords.value
                2 -> masteredWords.value
                else -> allWords.value
            }
        }
        val defsList = dictionaryRepository.getAllCachedDefinitions()
        val defsMap = defsList.associateBy { it.word.lowercase() }

        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
        return when (format) {
            VocabularyExportFormat.ANKI_CSV -> {
                val filename = "lexiread_anki_vocab_$timestamp.csv"
                val content = VocabularyExporter.toAnkiCsv(wordsToExport, defsMap)
                Pair(filename, content)
            }
            VocabularyExportFormat.PLAIN_TEXT -> {
                val filename = "lexiread_vocabulary_$timestamp.txt"
                val content = VocabularyExporter.toPlainText(wordsToExport, defsMap)
                Pair(filename, content)
            }
        }
    }
}

enum class ExportScope {
    ALL,
    CURRENT_TAB
}


