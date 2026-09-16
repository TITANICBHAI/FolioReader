package com.tbtechsdev.lexiread.ui.reader

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

private const val TAG = "ReaderScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToClassroomRemote: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val highlightedWordsMap by viewModel.pageHighlights.collectAsStateWithLifecycle()
    val isVocabAssistanceEnabled by viewModel.isVocabAssistanceEnabled.collectAsStateWithLifecycle()
    val classroomState by viewModel.classroomState.collectAsStateWithLifecycle()
    val classroomCursorIndex by viewModel.classroomCursorIndex.collectAsStateWithLifecycle()

    var showClassroomSheet by remember { mutableStateOf(false) }
    var isHostingModeSelected by remember { mutableStateOf(false) }
    var sessionNameInput by remember { mutableStateOf(Build.MODEL ?: "Classroom") }
    var pendingHostStart by remember { mutableStateOf(false) }

    val nearbyPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }
    val nearbyPermissionState = rememberMultiplePermissionsState(permissions = nearbyPermissions)
    val pageDimensionsMap by viewModel.pageDimensions.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val selectedWordDefinition by viewModel.selectedWordDefinition.collectAsStateWithLifecycle()
    val selectedWordContextSentence by viewModel.selectedWordContextSentence.collectAsStateWithLifecycle()
    val isHindiModelDownloaded by viewModel.isHindiModelDownloaded.collectAsStateWithLifecycle()
    val sentenceTranslationState by viewModel.sentenceTranslationState.collectAsStateWithLifecycle()
    val showTranslationDownloadPrompt by viewModel.showTranslationDownloadPrompt.collectAsStateWithLifecycle()

    val isGeminiConfigured by viewModel.isGeminiConfigured.collectAsStateWithLifecycle()
    val aiWordExplanationText by viewModel.aiWordExplanationText.collectAsStateWithLifecycle()
    val isAiWordExplaining by viewModel.isAiWordExplaining.collectAsStateWithLifecycle()
    val activeTextSelection by viewModel.activeTextSelection.collectAsStateWithLifecycle()
    val showExplainSheet by viewModel.showExplainSheet.collectAsStateWithLifecycle()
    val explainSheetSelectedText by viewModel.explainSheetSelectedText.collectAsStateWithLifecycle()
    val explainSheetResponseText by viewModel.explainSheetResponseText.collectAsStateWithLifecycle()
    val isExplainingSheet by viewModel.isExplainingSheet.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.roundToPx() }
    }

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshGeminiStatus()
        viewModel.events.collect { event ->
            when (event) {
                is ReaderEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ReaderEvent.ScrollToPage -> {
                    lazyListState.animateScrollToItem(event.pageIndex)
                }
            }
        }
    }

    LaunchedEffect(nearbyPermissionState.allPermissionsGranted, pendingHostStart) {
        if (pendingHostStart) {
            if (nearbyPermissionState.allPermissionsGranted) {
                viewModel.startHosting(sessionNameInput.ifBlank { Build.MODEL ?: "Classroom" }, context)
                showClassroomSheet = false
                isHostingModeSelected = false
                pendingHostStart = false
            } else {
                pendingHostStart = false
                snackbarHostState.showSnackbar("Location and Bluetooth permissions are required for Classroom hosting")
            }
        }
    }

    // File picker launcher requesting persistable URI permissions
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(TAG, "takePersistableUriPermission failed: ${e.message}")
            }
            viewModel.openPdf(uri)
        }
    }

    // Scroll restoration when a PDF is opened/restored
    LaunchedEffect(uiState.isPdfLoaded, uiState.initialScrollPage) {
        if (uiState.isPdfLoaded && uiState.initialScrollPage in 0 until uiState.pageCount) {
            lazyListState.scrollToItem(uiState.initialScrollPage)
        }
    }

    // Observe visible page changes to prefetch visible + 1 ahead + 1 behind and update DataStore
    LaunchedEffect(lazyListState, screenWidthPx, uiState.isPdfLoaded) {
        if (uiState.isPdfLoaded) {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .collect { visibleIndex ->
                    viewModel.onVisiblePageChanged(visibleIndex, screenWidthPx)
                }
        }
    }

    // Handle back button when reading a document
    BackHandler(enabled = uiState.isPdfLoaded) {
        viewModel.closeCurrentDocument()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_screen")
    ) {
        if (!uiState.isPdfLoaded) {
            // Empty State / Welcome Screen
            ReaderEmptyState(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onOpenDocument = { openDocLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // LazyColumn rendering pages with 8dp spacing
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = if (uiState.areBarsVisible) 68.dp else 16.dp,
                    bottom = if (uiState.areBarsVisible) 84.dp else 16.dp,
                    start = 8.dp,
                    end = 8.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("pdf_lazy_column")
            ) {
                items(
                    count = uiState.pageCount,
                    key = { it }
                ) { pageIndex ->
                    val pageBitmap by viewModel.getPageBitmap(pageIndex).collectAsStateWithLifecycle()

                    LaunchedEffect(pageIndex, screenWidthPx) {
                        viewModel.requestPageRender(pageIndex, screenWidthPx)
                    }

                    PdfPageView(
                        pageIndex = pageIndex,
                        bitmap = pageBitmap,
                        pdfDimensions = pageDimensionsMap[pageIndex],
                        highlightedWords = highlightedWordsMap[pageIndex] ?: emptyList(),
                        allPageWords = viewModel.getWordsForPage(pageIndex),
                        isVocabAssistanceEnabled = isVocabAssistanceEnabled,
                        selectedWord = selectedWord,
                        classroomCursorIndex = if (pageIndex == uiState.currentPage - 1) classroomCursorIndex else null,
                        onTap = {
                            viewModel.clearTextSelection()
                            viewModel.toggleBars()
                        },
                        onWordSelected = { word ->
                            viewModel.clearTextSelection()
                            viewModel.selectWord(word)
                        },
                        onTextLongPress = { text, offset, pageIdx ->
                            viewModel.onTextSelectedForExplanation(text, offset, pageIdx)
                        },
                        selectedTextSelection = activeTextSelection,
                        onCopyClick = { text ->
                            viewModel.copyTextToClipboard(text)
                        },
                        onDefineClick = { text ->
                            viewModel.selectWordByText(text, pageIndex)
                        },
                        onExplainClick = { text ->
                            if (isGeminiConfigured) {
                                viewModel.triggerExplainSelectedText(text)
                            } else {
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Add your Gemini API key in Settings to use AI explanations",
                                        actionLabel = "Settings",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onNavigateToSettings()
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Top Bar & Persistent Classroom TopBanner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                // Top Bar: Back button + Title + Vocab Assistance Toggle + Classroom Mode + Open Another File
                AnimatedVisibility(
                    visible = uiState.areBarsVisible,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.closeCurrentDocument() },
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Library",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = uiState.pdfName.ifBlank { "PDF Document" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("pdf_title_text")
                            )

                            IconButton(
                                onClick = { viewModel.toggleVocabAssistance() },
                                modifier = Modifier.testTag("toggle_vocab_assistance_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = if (isVocabAssistanceEnabled) "Disable Vocabulary Assistance" else "Enable Vocabulary Assistance",
                                    tint = if (isVocabAssistanceEnabled) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // Classroom Mode 📺 Button
                            IconButton(
                                onClick = {
                                    if (!uiState.isPdfLoaded) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Please open a PDF first")
                                        }
                                    } else {
                                        showClassroomSheet = true
                                    }
                                },
                                modifier = Modifier.testTag("classroom_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Classroom Mode",
                                    tint = if (classroomState !is ReaderViewModel.ClassroomState.Inactive) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { openDocLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.testTag("open_another_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open Another PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Persistent TopBanner composable (slim bar below top app bar)
                if (classroomState !is ReaderViewModel.ClassroomState.Inactive) {
                    val hostingName = when (val s = classroomState) {
                        is ReaderViewModel.ClassroomState.Hosting -> s.sessionName
                        is ReaderViewModel.ClassroomState.Connected -> sessionNameInput.ifBlank { Build.MODEL ?: "Classroom" }
                        else -> "Classroom"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!uiState.areBarsVisible) Modifier.statusBarsPadding() else Modifier)
                            .testTag("classroom_hosting_banner"),
                        color = Color(0xFF1976D2),
                        contentColor = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📺  Hosting · $hostingName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            TextButton(
                                onClick = { viewModel.stopClassroom() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier.testTag("stop_classroom_button")
                            ) {
                                Text(
                                    text = "Stop",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar: Navigation controls + Page indicator ("X / Y")
            AnimatedVisibility(
                visible = uiState.areBarsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                    lazyListState.animateScrollToItem(target)
                                }
                            },
                            enabled = uiState.currentPage > 1,
                            modifier = Modifier.testTag("prev_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Page",
                                tint = if (uiState.currentPage > 1) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "${uiState.currentPage} / ${uiState.pageCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("page_indicator")
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = (lazyListState.firstVisibleItemIndex + 1)
                                        .coerceAtMost(uiState.pageCount - 1)
                                    lazyListState.animateScrollToItem(target)
                                }
                            },
                            enabled = uiState.currentPage < uiState.pageCount,
                            modifier = Modifier.testTag("next_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Page",
                                tint = if (uiState.currentPage < uiState.pageCount) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }

        // OCR Progress Indicator Pill
        AnimatedVisibility(
            visible = uiState.isOcrRunning,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (uiState.areBarsVisible) 76.dp else 24.dp)
                .statusBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("ocr_progress_indicator"),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Processing OCR…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Floating Action Button to open file picker
        AnimatedVisibility(
            visible = !uiState.isPdfLoaded || uiState.areBarsVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = if (uiState.isPdfLoaded) 72.dp else 24.dp
                )
        ) {
            FloatingActionButton(
                onClick = { openDocLauncher.launch(arrayOf("application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("open_pdf_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Open PDF"
                )
            }
        }

        // SnackbarHost for ML Kit model download notifications and errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (uiState.areBarsVisible) 80.dp else 16.dp)
                .navigationBarsPadding()
                .testTag("reader_snackbar_host")
        )

        // First-launch translation model prompt dialog
        if (showTranslationDownloadPrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissTranslationPrompt(markAsShown = true) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "Hindi Sentence Translation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Download a 30 MB model to enable Hindi sentence translation directly in your reading view.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissTranslationPrompt(markAsShown = true)
                            onNavigateToSettings()
                        },
                        modifier = Modifier.testTag("translation_prompt_settings_button")
                    ) {
                        Text("Go to Settings")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissTranslationPrompt(markAsShown = true) },
                        modifier = Modifier.testTag("translation_prompt_dismiss_button")
                    ) {
                        Text("Later")
                    }
                },
                modifier = Modifier.testTag("translation_first_launch_dialog")
            )
        }

        // Word Information Bottom Sheet
        selectedWord?.let { word ->
            WordPanelBottomSheet(
                word = word.text,
                partOfSpeech = selectedWordDefinition?.partOfSpeech,
                englishDefinition = selectedWordDefinition?.englishDefinition,
                hindiMeaning = selectedWordDefinition?.hindiMeaning,
                contextSentence = selectedWordContextSentence,
                isModelDownloaded = isHindiModelDownloaded,
                translationState = sentenceTranslationState,
                onTranslateSentence = { sentence -> viewModel.translateSentence(sentence) },
                onNavigateToSettings = onNavigateToSettings,
                isGeminiConfigured = isGeminiConfigured,
                aiExplanationText = aiWordExplanationText,
                isAiExplaining = isAiWordExplaining,
                onAskAi = { viewModel.requestAiExplanationForSelectedWord() },
                onSave = { viewModel.saveSelectedWord() },
                onKnowIt = { viewModel.markSelectedWordKnown() },
                onIgnore = { viewModel.markSelectedWordIgnored() },
                onDismissRequest = { viewModel.dismissWordPanel() },
                onPronounceWord = { w -> viewModel.pronounceWord(w) },
                onCopyWord = { w -> viewModel.copyTextToClipboard(w, "Word") }
            )
        }

        // Floating "Explain this ▸" action bar when text is selected on any page
        AnimatedVisibility(
            visible = activeTextSelection != null && !showExplainSheet,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (uiState.areBarsVisible) 80.dp else 24.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable {
                        activeTextSelection?.let { selection ->
                            if (isGeminiConfigured) {
                                viewModel.triggerExplainSelectedText(selection.text)
                            } else {
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Add your Gemini API key in Settings to use AI explanations",
                                        actionLabel = "Settings",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onNavigateToSettings()
                                    }
                                }
                            }
                        }
                    }
                    .testTag("explain_this_floating_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explain this ▸",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Explain Selection Modal Bottom Sheet for streaming Gemini AI response
        if (showExplainSheet && explainSheetSelectedText != null) {
            ExplainSelectionBottomSheet(
                selectedText = explainSheetSelectedText ?: "",
                explanationText = explainSheetResponseText,
                isExplaining = isExplainingSheet,
                onDismissRequest = { viewModel.dismissExplainSheet() }
            )
        }

        // Classroom Mode Modal Bottom Sheet
        if (showClassroomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showClassroomSheet = false
                    isHostingModeSelected = false
                },
                modifier = Modifier.testTag("classroom_modal_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    if (!isHostingModeSelected) {
                        Text(
                            text = "Classroom Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ListItem(
                            headlineContent = {
                                Text("📺  Host classroom session", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text("Display highlighted words on this screen for the class")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isHostingModeSelected = true
                                }
                                .testTag("option_host_classroom")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ListItem(
                            headlineContent = {
                                Text("🎤  Join as remote", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text("Control word highlighting from teacher's phone")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showClassroomSheet = false
                                    onNavigateToClassroomRemote()
                                }
                                .testTag("option_join_remote")
                        )
                    } else {
                        Text(
                            text = "Host Classroom Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = sessionNameInput,
                            onValueChange = { sessionNameInput = it },
                            label = { Text("Session Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("session_name_text_field")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (nearbyPermissionState.allPermissionsGranted) {
                                    viewModel.startHosting(
                                        sessionNameInput.ifBlank { Build.MODEL ?: "Classroom" },
                                        context
                                    )
                                    showClassroomSheet = false
                                    isHostingModeSelected = false
                                } else {
                                    pendingHostStart = true
                                    nearbyPermissionState.launchMultiplePermissionRequest()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_hosting_button")
                        ) {
                            Text("Start hosting")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { isHostingModeSelected = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReaderEmptyState(
    isLoading: Boolean,
    errorMessage: String?,
    onOpenDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = "Reader",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "LexiRead PDF Reader",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Open any PDF document to read with instant zoom, smooth page navigation, and automatic progress saving.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Opening PDF document...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Button(
                        onClick = onOpenDocument,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Open PDF Document")
                    }
                }
            }
        }
    }
}
