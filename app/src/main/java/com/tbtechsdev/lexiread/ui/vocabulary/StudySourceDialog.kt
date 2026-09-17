package com.tbtechsdev.lexiread.ui.vocabulary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbtechsdev.lexiread.domain.model.WordStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudySourceDialog(
    currentSource: StudySource,
    isRandom: Boolean,
    recentPdf: Pair<Uri, String>?,
    learningCount: Int,
    reviewingCount: Int,
    masteredCount: Int,
    allCount: Int,
    isGeminiConfigured: Boolean,
    onSaveGeminiKey: (String, (Boolean, String?) -> Unit) -> Unit,
    onStartFlashcards: (StudySource, Boolean) -> Unit,
    onStartQuiz: (StudySource, Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedSource by remember { mutableStateOf<StudySource>(currentSource) }
    var randomOption by remember { mutableStateOf(isRandom) }
    var customPdfUri by remember { mutableStateOf<Uri?>(null) }
    var customPdfName by remember { mutableStateOf<String?>(null) }
    var showGeminiKeyDialog by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected Document"
            customPdfUri = uri
            customPdfName = fileName
            selectedSource = StudySource.FromPdf(uri, fileName, isRecent = false)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("study_source_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice & Quiz Setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose words from your reading or documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Study Source Selection
            Text(
                text = "1. VOCABULARY SOURCE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Saved Words Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == null,
                    onClick = { selectedSource = StudySource.SavedWords(null) },
                    label = { Text("All Saved ($allCount)") },
                    leadingIcon = {
                        if (selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == null) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("source_all_saved")
                )

                FilterChip(
                    selected = selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.LEARNING,
                    onClick = { selectedSource = StudySource.SavedWords(WordStatus.LEARNING) },
                    label = { Text("Learning ($learningCount)") },
                    leadingIcon = {
                        if (selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.LEARNING) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("source_learning")
                )

                FilterChip(
                    selected = selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.REVIEWING,
                    onClick = { selectedSource = StudySource.SavedWords(WordStatus.REVIEWING) },
                    label = { Text("Reviewing ($reviewingCount)") },
                    leadingIcon = {
                        if (selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.REVIEWING) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("source_reviewing")
                )

                FilterChip(
                    selected = selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.MASTERED,
                    onClick = { selectedSource = StudySource.SavedWords(WordStatus.MASTERED) },
                    label = { Text("Mastered ($masteredCount)") },
                    leadingIcon = {
                        if (selectedSource is StudySource.SavedWords && (selectedSource as StudySource.SavedWords).statusFilter == WordStatus.MASTERED) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("source_mastered")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PDF Source Options
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Or Extract Words From PDF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (recentPdf != null) {
                        val isRecentSelected = selectedSource is StudySource.FromPdf && (selectedSource as StudySource.FromPdf).uri == recentPdf.first
                        Surface(
                            onClick = {
                                selectedSource = StudySource.FromPdf(recentPdf.first, recentPdf.second, isRecent = true)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRecentSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isRecentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("source_recent_pdf")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (isRecentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Recent: ${recentPdf.second}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Extract key vocabulary from your last read PDF",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isRecentSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Pick Custom PDF Button
                    val isCustomPdfSelected = selectedSource is StudySource.FromPdf && !(selectedSource as StudySource.FromPdf).isRecent
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customPdfName != null && isCustomPdfSelected) {
                                "PDF: $customPdfName"
                            } else {
                                "Choose Any PDF from Storage..."
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Options (Random & AI)
            Text(
                text = "2. PRACTICE OPTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Random Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Random Selection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Shuffle & pick a random sample of words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = randomOption,
                        onCheckedChange = { randomOption = it },
                        modifier = Modifier.testTag("random_option_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gemini BYOK Status / Config
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isGeminiConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini AI Tutor (BYOK)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isGeminiConfigured) {
                                "Configured & Active for rich explanations"
                            } else {
                                "Optional: Add your Gemini key for AI explanations"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showGeminiKeyDialog = true },
                        modifier = Modifier.testTag("configure_gemini_key_button")
                    ) {
                        Text(if (isGeminiConfigured) "Manage" else "Add Key")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onStartFlashcards(selectedSource, randomOption)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_flashcards_button")
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flashcards", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onStartQuiz(selectedSource, randomOption)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_quick_quiz_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Quiz", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showGeminiKeyDialog) {
        var inputKey by remember { mutableStateOf("") }
        var isTesting by remember { mutableStateOf(false) }
        var keyError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isTesting) showGeminiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini BYOK Setup")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API key to enable AI-powered word explanations, examples, and study breakdowns:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        isError = keyError != null,
                        supportingText = keyError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputKey.isBlank()) return@Button
                        isTesting = true
                        keyError = null
                        onSaveGeminiKey(inputKey.trim()) { success, err ->
                            isTesting = false
                            if (success) {
                                showGeminiKeyDialog = false
                            } else {
                                keyError = err ?: "Invalid API key"
                            }
                        }
                    },
                    enabled = !isTesting && inputKey.isNotBlank(),
                    modifier = Modifier.testTag("save_gemini_key_btn")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...")
                    } else {
                        Text("Save & Activate")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGeminiKeyDialog = false },
                    enabled = !isTesting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
