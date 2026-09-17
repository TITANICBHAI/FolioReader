package com.tbtechs.folioreader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPanelBottomSheet(
    word: String,
    partOfSpeech: String?,
    englishDefinition: String?,
    hindiMeaning: String?,
    contextSentence: String?,
    onSave: () -> Unit,
    onKnowIt: () -> Unit,
    onIgnore: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    isModelDownloaded: Boolean = false,
    translationState: SentenceTranslationState = SentenceTranslationState.Idle,
    onTranslateSentence: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    isGeminiConfigured: Boolean = false,
    aiExplanationText: String? = null,
    isAiExplaining: Boolean = false,
    onAskAi: () -> Unit = {},
    onPronounceWord: (String) -> Unit = {},
    onCopyWord: (String) -> Unit = {},
    phonetic: String? = null,
    example: String? = null,
    synonyms: List<String> = emptyList(),
    antonyms: List<String> = emptyList(),
    onSynonymClick: (String) -> Unit = {},
    targetLanguageCode: String = "hi"
) {
    var isAiExpanded by rememberSaveable { mutableStateOf(false) }
    val targetLang = remember(targetLanguageCode) {
        com.tbtechs.folioreader.data.translation.SupportedLanguages.getByCode(targetLanguageCode)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        },
        modifier = modifier.testTag("word_panel_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // 1. Word header with Part of Speech, Phonetic IPA, Pronunciation TTS, and Copy buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("word_panel_word_text")
                        )

                        if (!partOfSpeech.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = partOfSpeech.lowercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("word_panel_part_of_speech")
                                )
                            }
                        }
                    }

                    if (!phonetic.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.titleSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("word_panel_phonetic")
                        )
                    }
                }

                // Audio Pronunciation button
                IconButton(
                    onClick = { onPronounceWord(word) },
                    modifier = Modifier.testTag("word_panel_pronounce_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Pronounce word",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Copy Word button
                IconButton(
                    onClick = { onCopyWord(word) },
                    modifier = Modifier.testTag("word_panel_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy word",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // 2. English definition — from Free Dictionary API
            Text(
                text = "English Definition",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (!englishDefinition.isNullOrBlank()) {
                Text(
                    text = englishDefinition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("word_panel_english_definition")
                )
            } else {
                Text(
                    text = "Definition not found in offline dictionary. Connect to internet for online lookup, or tap 'Explain with AI' below.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("word_panel_english_definition")
                )
            }

            // Example Sentence (from Free Dictionary API)
            if (!example.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_panel_example_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 1.dp)
                        )
                        Column {
                            Text(
                                text = "Example",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "“$example”",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("word_panel_example_text")
                            )
                        }
                    }
                }
            }

            // Synonyms & Antonyms Chips
            val synList = remember(synonyms) {
                synonyms.map { it.trim() }.filter { it.isNotBlank() }
            }
            val antList = remember(antonyms) {
                antonyms.map { it.trim() }.filter { it.isNotBlank() }
            }

            if (synList.isNotEmpty() || antList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                if (synList.isNotEmpty()) {
                    Text(
                        text = "Synonyms",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("word_panel_synonyms_container")
                    ) {
                        synList.forEach { syn ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clickable { onSynonymClick(syn) }
                                    .testTag("synonym_chip_$syn")
                            ) {
                                Text(
                                    text = syn,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (antList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Antonyms",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("word_panel_antonyms_container")
                    ) {
                        antList.forEach { ant ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onSynonymClick(ant) }
                                    .testTag("antonym_chip_$ant")
                            ) {
                                Text(
                                    text = ant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Hindi meaning — from DictionaryEntry.hindiMeaning (show "—" if empty string)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Hindi Meaning",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (!hindiMeaning.isNullOrBlank()) hindiMeaning else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("word_panel_hindi_meaning")
            )

            // 5. Context sentence & Translation — below the Hindi dictionary meaning
            // Section label: "Translate this sentence"
            // Show the context sentence (already extracted in Prompt 7)
            // Button: "Translate →"
            //   If model not downloaded → label becomes "Download model first", clicking this navigates to Settings
            //   If downloaded → on tap, call onTranslateSentence(sentence)
            //   Show CircularProgressIndicator while in progress
            //   Show translated Hindi text on success
            //   Show "Translation unavailable" on any error — never crash
            if (!contextSentence.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_panel_context_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Translate this sentence",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("word_panel_translate_section_label")
                            )
                        }

                        Text(
                            text = "“… $contextSentence …”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("word_panel_context_sentence")
                        )

                        // Translate button or Download model first button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isModelDownloaded) {
                                OutlinedButton(
                                    onClick = onNavigateToSettings,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("word_panel_translate_button")
                                ) {
                                    Text(
                                        text = "Download model first",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onTranslateSentence(contextSentence) },
                                    enabled = translationState !is SentenceTranslationState.Translating,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("word_panel_translate_button")
                                ) {
                                    Text(
                                        text = "Translate →",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (translationState is SentenceTranslationState.Translating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("word_panel_translate_progress"),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }

                        // Translation result display
                        when (translationState) {
                            is SentenceTranslationState.Success -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${targetLang.displayName} Translation",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = translationState.translation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("word_panel_translated_text")
                                        )
                                    }
                                }
                            }
                            is SentenceTranslationState.Error -> {
                                Text(
                                    text = translationState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("word_panel_translate_error")
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // 5b. Gemini AI Explanation Section
            AnimatedVisibility(visible = isAiExpanded || !aiExplanationText.isNullOrEmpty() || isAiExplaining) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("word_panel_ai_section")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "AI Explanation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isAiExplaining) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .testTag("word_panel_ai_progress"),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Text(
                                text = aiExplanationText ?: if (isAiExplaining) "Thinking..." else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("word_panel_ai_response_text")
                            )

                            Text(
                                text = "Using your Gemini API key — standard charges may apply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.testTag("word_panel_ai_disclaimer")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Action buttons in a row:
            // [⭐ Save]   [✓ Know it]   [Ignore]   [Ask AI ▸]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save button
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("word_panel_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save", fontWeight = FontWeight.SemiBold)
                }

                // Know it button
                FilledTonalButton(
                    onClick = onKnowIt,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("word_panel_know_it_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Know it", fontWeight = FontWeight.Medium)
                }

                // Ignore button
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("word_panel_ignore_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ignore", fontWeight = FontWeight.Medium)
                }

                // Ask AI button: enabled only when GeminiRepository.isConfigured() == true
                OutlinedButton(
                    onClick = {
                        isAiExpanded = true
                        onAskAi()
                    },
                    enabled = isGeminiConfigured,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isGeminiConfigured) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .semantics {
                            if (!isGeminiConfigured) {
                                contentDescription = "Add Gemini key in Settings"
                            }
                        }
                        .testTag("word_panel_ask_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ask AI ▸")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
