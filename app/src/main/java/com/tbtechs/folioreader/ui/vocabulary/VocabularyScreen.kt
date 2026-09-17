package com.tbtechs.folioreader.ui.vocabulary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import com.tbtechs.folioreader.data.vocabulary.VocabularyExportFormat
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import com.tbtechs.folioreader.domain.model.WordStatus
import com.tbtechs.folioreader.ui.reader.WordPanelBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    modifier: Modifier = Modifier,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val learningWords by viewModel.learningWords.collectAsStateWithLifecycle()
    val reviewingWords by viewModel.reviewingWords.collectAsStateWithLifecycle()
    val masteredWords by viewModel.masteredWords.collectAsStateWithLifecycle()
    val allWords by viewModel.allWords.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val selectedWordEntry by viewModel.selectedWordEntry.collectAsStateWithLifecycle()
    val wordToEdit by viewModel.wordToEdit.collectAsStateWithLifecycle()

    // Practice state
    val practiceMode by viewModel.practiceMode.collectAsStateWithLifecycle()
    val practiceWords by viewModel.practiceWords.collectAsStateWithLifecycle()
    val currentFlashcardIndex by viewModel.currentFlashcardIndex.collectAsStateWithLifecycle()
    val isFlashcardFlipped by viewModel.isFlashcardFlipped.collectAsStateWithLifecycle()
    val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val currentQuizIndex by viewModel.currentQuizIndex.collectAsStateWithLifecycle()
    val selectedQuizOptionIndex by viewModel.selectedQuizOptionIndex.collectAsStateWithLifecycle()
    val isQuizOptionSubmitted by viewModel.isQuizOptionSubmitted.collectAsStateWithLifecycle()
    val quizAnswers by viewModel.quizAnswers.collectAsStateWithLifecycle()
    val studySource by viewModel.studySource.collectAsStateWithLifecycle()
    val isRandomSelection by viewModel.isRandomSelection.collectAsStateWithLifecycle()
    val isPracticeLoading by viewModel.isPracticeLoading.collectAsStateWithLifecycle()
    val practiceErrorMessage by viewModel.practiceErrorMessage.collectAsStateWithLifecycle()
    val recentPdfState by viewModel.recentPdfState.collectAsStateWithLifecycle()
    val isGeminiConfigured by viewModel.isGeminiConfigured.collectAsStateWithLifecycle()
    val aiExplanation by viewModel.aiExplanation.collectAsStateWithLifecycle()
    val isAiExplaining by viewModel.isAiExplaining.collectAsStateWithLifecycle()

    var showSourceDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf("Learning", "Reviewing", "Mastered", "All")
    val tabCounts = listOf(learningWords.size, reviewingWords.size, masteredWords.size, allWords.size)

    val currentList = when (selectedTab) {
        0 -> learningWords
        1 -> reviewingWords
        2 -> masteredWords
        else -> allWords
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(VocabularyExportFormat.ANKI_CSV) }
    var exportScope by remember { mutableStateOf(com.tbtechs.folioreader.ui.vocabulary.ExportScope.ALL) }
    var pendingExportPayload by remember { mutableStateOf<Pair<String, String>?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (exportFormat == VocabularyExportFormat.ANKI_CSV) "text/csv" else "text/plain"
        )
    ) { uri ->
        if (uri != null && pendingExportPayload != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(pendingExportPayload!!.second.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Vocabulary saved successfully!", Toast.LENGTH_SHORT).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("vocabulary_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vocabulary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${allWords.size} words saved • Active Learning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("vocabulary_export_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Export Vocabulary",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Spaced Repetition Practice Hero Card
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .testTag("practice_hero_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Spaced Repetition",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Source selector button / chip
                        val sourceText = when (val s = studySource) {
                            is StudySource.SavedWords -> if (isRandomSelection) "Random Saved" else s.label
                            is StudySource.FromPdf -> if (isRandomSelection) "Random PDF" else "PDF: ${s.title}"
                        }
                        Surface(
                            onClick = { showSourceDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("change_study_source_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Change Source",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sourceText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Two Primary Practice Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Flashcards Action
                        FilledTonalButton(
                            onClick = { viewModel.startFlashcards() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("open_flashcards_button")
                        ) {
                            Icon(
                                Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Flashcards",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Flip & Memorize",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Daily Quick Quiz Action
                        Button(
                            onClick = { viewModel.startQuiz() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("open_daily_quiz_button")
                        ) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Daily Quiz",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "5-Question Test",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // TabRow with four tabs: Learning | Reviewing | Mastered | All
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vocabulary_tab_row")
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTab == index) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = "${tabCounts[index]}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedTab == index) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("vocabulary_tab_$index")
                    )
                }
            }

            // Words List or Empty State
            if (currentList.isEmpty()) {
                VocabularyEmptyState(tabName = tabTitles[selectedTab])
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vocabulary_lazy_column")
                ) {
                    items(
                        items = currentList,
                        key = { it.word }
                    ) { wordEntity ->
                        VocabularyWordItem(
                            entity = wordEntity,
                            onClick = { viewModel.selectWord(wordEntity) },
                            onLongClick = { viewModel.showEditDialog(wordEntity) }
                        )
                    }
                }
            }
        }

        // Flashcards Overlay Mode
        if (practiceMode == PracticeMode.FLASHCARDS) {
            FlashcardsOverlay(
                words = practiceWords,
                currentIndex = currentFlashcardIndex,
                isFlipped = isFlashcardFlipped,
                aiExplanation = aiExplanation,
                isAiExplaining = isAiExplaining,
                isGeminiConfigured = isGeminiConfigured,
                onFlip = { viewModel.flipFlashcard() },
                onNext = { viewModel.nextFlashcard() },
                onPrevious = { viewModel.previousFlashcard() },
                onShuffle = { viewModel.shuffleFlashcards() },
                onRateWord = { newStatus -> viewModel.rateFlashcardWord(newStatus) },
                onPronounce = { word -> viewModel.pronounceWord(word) },
                onRequestAiExplanation = { word, context -> viewModel.requestAiExplanation(word, context) },
                onClose = { viewModel.exitPractice() }
            )
        }

        // Quiz Overlay Mode
        if (practiceMode == PracticeMode.QUIZ || practiceMode == PracticeMode.QUIZ_RESULTS) {
            QuizOverlay(
                mode = practiceMode,
                questions = quizQuestions,
                currentIndex = currentQuizIndex,
                selectedIndex = selectedQuizOptionIndex,
                isSubmitted = isQuizOptionSubmitted,
                answers = quizAnswers,
                aiExplanation = aiExplanation,
                isAiExplaining = isAiExplaining,
                isGeminiConfigured = isGeminiConfigured,
                onSubmitOption = { optIndex -> viewModel.submitQuizAnswer(optIndex) },
                onNextQuestion = { viewModel.nextQuizQuestion() },
                onRetakeQuiz = { viewModel.retakeQuiz() },
                onStartFlashcards = { viewModel.startFlashcards() },
                onPronounce = { word -> viewModel.pronounceWord(word) },
                onRequestAiExplanation = { word, context -> viewModel.requestAiExplanation(word, context) },
                onClose = { viewModel.exitPractice() }
            )
        }

        // Study Source Setup Dialog / Sheet
        if (showSourceDialog) {
            StudySourceDialog(
                currentSource = studySource,
                isRandom = isRandomSelection,
                recentPdf = recentPdfState,
                learningCount = learningWords.size,
                reviewingCount = reviewingWords.size,
                masteredCount = masteredWords.size,
                allCount = allWords.size,
                isGeminiConfigured = isGeminiConfigured,
                onSaveGeminiKey = { key, callback -> viewModel.saveGeminiApiKey(key, callback) },
                onStartFlashcards = { source, random ->
                    viewModel.startFlashcards(source, random)
                    showSourceDialog = false
                },
                onStartQuiz = { source, random ->
                    viewModel.startQuiz(source, random)
                    showSourceDialog = false
                },
                onDismiss = { showSourceDialog = false }
            )
        }

        // Practice Loading Indicator Overlay
        if (isPracticeLoading) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preparing vocabulary & definitions...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Practice Error Dialog
        practiceErrorMessage?.let { errMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.clearPracticeError() },
                title = { Text("Practice") },
                text = { Text(errMsg) },
                confirmButton = {
                    Button(onClick = { viewModel.clearPracticeError() }) {
                        Text("OK")
                    }
                }
            )
        }

        // Long-Press Edit/Delete Dialog
        wordToEdit?.let { entity ->
            WordEditDialog(
                entity = entity,
                onDismiss = { viewModel.dismissEditDialog() },
                onStatusChange = { newStatus -> viewModel.updateWordStatus(entity.word, newStatus) },
                onDelete = { viewModel.deleteWord(entity.word) }
            )
        }

        // WordPanelBottomSheet when a word item is tapped
        selectedWord?.let { wordEntity ->
            WordPanelBottomSheet(
                word = wordEntity.word,
                partOfSpeech = selectedWordEntry?.partOfSpeech,
                englishDefinition = selectedWordEntry?.englishDefinition,
                hindiMeaning = selectedWordEntry?.hindiMeaning,
                contextSentence = null,
                onSave = { viewModel.saveWord(wordEntity) },
                onKnowIt = { viewModel.markKnown(wordEntity) },
                onIgnore = { viewModel.markIgnored(wordEntity) },
                onDismissRequest = { viewModel.dismissWordPanel() },
                phonetic = selectedWordEntry?.phonetic,
                example = selectedWordEntry?.example,
                synonyms = selectedWordEntry?.synonyms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                antonyms = selectedWordEntry?.antonyms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            )
        }

        // Export Vocabulary Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                modifier = Modifier.testTag("vocabulary_export_dialog"),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Export Vocabulary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Export Format",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Format Option 1: Anki CSV
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (exportFormat == VocabularyExportFormat.ANKI_CSV) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = if (exportFormat == VocabularyExportFormat.ANKI_CSV) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportFormat = VocabularyExportFormat.ANKI_CSV }
                                .testTag("export_format_anki_csv")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportFormat == VocabularyExportFormat.ANKI_CSV,
                                    onClick = { exportFormat = VocabularyExportFormat.ANKI_CSV }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Anki Flashcards (.csv)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Import directly into Anki or AnkiDroid (Word, IPA Phonetic, Meaning, Example).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Format Option 2: Plain Text
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (exportFormat == VocabularyExportFormat.PLAIN_TEXT) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = if (exportFormat == VocabularyExportFormat.PLAIN_TEXT) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportFormat = VocabularyExportFormat.PLAIN_TEXT }
                                .testTag("export_format_plain_text")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportFormat == VocabularyExportFormat.PLAIN_TEXT,
                                    onClick = { exportFormat = VocabularyExportFormat.PLAIN_TEXT }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Study Guide (.txt)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Clean human-readable list with definitions, IPA pronunciation, examples, and synonyms.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Words to Export",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (exportScope == com.tbtechs.folioreader.ui.vocabulary.ExportScope.ALL) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { exportScope = com.tbtechs.folioreader.ui.vocabulary.ExportScope.ALL }
                                    .testTag("export_scope_all")
                            ) {
                                Text(
                                    text = "All Words (${allWords.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (exportScope == com.tbtechs.folioreader.ui.vocabulary.ExportScope.ALL) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (exportScope == com.tbtechs.folioreader.ui.vocabulary.ExportScope.CURRENT_TAB) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { exportScope = com.tbtechs.folioreader.ui.vocabulary.ExportScope.CURRENT_TAB }
                                    .testTag("export_scope_tab")
                            ) {
                                Text(
                                    text = "${tabTitles[selectedTab]} (${tabCounts[selectedTab]})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (exportScope == com.tbtechs.folioreader.ui.vocabulary.ExportScope.CURRENT_TAB) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons: Share, Save to file, Copy
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val payload = viewModel.getExportContent(exportFormat, exportScope)
                                        val mimeType = if (exportFormat == VocabularyExportFormat.ANKI_CSV) "text/csv" else "text/plain"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = mimeType
                                            putExtra(Intent.EXTRA_TEXT, payload.second)
                                            putExtra(Intent.EXTRA_SUBJECT, "Folio Reader Vocabulary Export")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Vocabulary"))
                                        showExportDialog = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_share_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share / Send to Apps", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val payload = viewModel.getExportContent(exportFormat, exportScope)
                                        pendingExportPayload = payload
                                        createDocumentLauncher.launch(payload.first)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_save_file_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save File to Device", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val payload = viewModel.getExportContent(exportFormat, exportScope)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Folio Reader Vocabulary", payload.second))
                                        Toast.makeText(context, "Copied vocabulary to clipboard", Toast.LENGTH_SHORT).show()
                                        showExportDialog = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_copy_clipboard_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy to Clipboard", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showExportDialog = false },
                        modifier = Modifier.testTag("export_dismiss_button")
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VocabularyWordItem(
    entity: UserWordEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("vocabulary_item_${entity.word}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Word text and optional path/meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("vocab_word_text_${entity.word}")
                )
                if (!entity.sourceBookPath.isNullOrBlank()) {
                    Text(
                        text = entity.sourceBookPath.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Lookup count badge & Status chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Lookup count badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("lookup_count_${entity.word}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${entity.lookupCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status chip
                StatusChip(status = entity.status)
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (status.uppercase()) {
        WordStatus.LEARNING.name -> Triple(
            Color(0xFFFFF8E1),
            Color(0xFFE65100),
            "Learning"
        )
        WordStatus.REVIEWING.name -> Triple(
            Color(0xFFE1F5FE),
            Color(0xFF0277BD),
            "Reviewing"
        )
        WordStatus.MASTERED.name, WordStatus.KNOWN.name -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Mastered"
        )
        WordStatus.IGNORED.name -> Triple(
            Color(0xFFECEFF1),
            Color(0xFF546E7A),
            "Ignored"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Unknown"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier.testTag("status_chip_$status")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WordEditDialog(
    entity: UserWordEntity,
    onDismiss: () -> Unit,
    onStatusChange: (WordStatus) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage \"${entity.word}\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current status: ${entity.status} (Lookups: ${entity.lookupCount})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                DialogActionRow(
                    icon = Icons.Default.Star,
                    text = "Move to Learning",
                    tint = Color(0xFFF57C00),
                    onClick = { onStatusChange(WordStatus.LEARNING) }
                )
                DialogActionRow(
                    icon = Icons.Default.Replay,
                    text = "Move to Reviewing",
                    tint = Color(0xFF0288D1),
                    onClick = { onStatusChange(WordStatus.REVIEWING) }
                )
                DialogActionRow(
                    icon = Icons.Default.Check,
                    text = "Move to Mastered",
                    tint = Color(0xFF388E3C),
                    onClick = { onStatusChange(WordStatus.MASTERED) }
                )
                DialogActionRow(
                    icon = Icons.Outlined.Block,
                    text = "Mark as Ignored",
                    tint = Color(0xFF757575),
                    onClick = { onStatusChange(WordStatus.IGNORED) }
                )
                DialogActionRow(
                    icon = Icons.Default.Delete,
                    text = "Delete Word",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("word_edit_dialog")
    )
}

@Composable
private fun DialogActionRow(
    icon: ImageVector,
    text: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VocabularyEmptyState(
    tabName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No $tabName Words Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap highlighted words while reading PDFs to view definitions and save them to your vocabulary.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
