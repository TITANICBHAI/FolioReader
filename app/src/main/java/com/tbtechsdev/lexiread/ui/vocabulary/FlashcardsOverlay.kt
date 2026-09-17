package com.tbtechsdev.lexiread.ui.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtechsdev.lexiread.domain.model.WordStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashcardsOverlay(
    words: List<PracticeWordItem>,
    currentIndex: Int,
    isFlipped: Boolean,
    aiExplanation: String?,
    isAiExplaining: Boolean,
    isGeminiConfigured: Boolean,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRateWord: (WordStatus) -> Unit,
    onPronounce: (String) -> Unit,
    onRequestAiExplanation: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No words available for flashcards",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val currentWord = words[currentIndex.coerceIn(words.indices)]
    val progress = (currentIndex + 1).toFloat() / words.size.toFloat()

    // 3D Flip animation
    val animatedRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flashcard_rotation"
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("flashcards_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar: Exit button, Title, Card count, Shuffle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("exit_flashcards_button")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Exit Flashcards"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interactive Flashcards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Card ${currentIndex + 1} of ${words.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.testTag("shuffle_flashcards_button")
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle Cards",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("flashcards_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // The Flip Card Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = animatedRotation
                        cameraDistance = 12f * density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onFlip()
                    }
                    .testTag("interactive_flashcard")
            ) {
                if (animatedRotation <= 90f) {
                    // FRONT SIDE
                    FlashcardFront(
                        wordItem = currentWord,
                        onPronounce = { onPronounce(currentWord.word) },
                        onFlip = onFlip
                    )
                } else {
                    // BACK SIDE (Mirrored horizontally so text is right-side up)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        FlashcardBack(
                            wordItem = currentWord,
                            aiExplanation = aiExplanation,
                            isAiExplaining = isAiExplaining,
                            isGeminiConfigured = isGeminiConfigured,
                            onPronounce = { onPronounce(currentWord.word) },
                            onRequestAi = { onRequestAiExplanation(currentWord.word, currentWord.example) },
                            onRate = onRateWord,
                            onFlip = onFlip
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_flashcard_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Card"
                    )
                }

                FilledTonalButton(
                    onClick = onFlip,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("flip_flashcard_action_button")
                ) {
                    Icon(
                        Icons.Default.Flip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFlipped) "Show Word" else "Flip for Meaning",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = currentIndex < words.size - 1,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_flashcard_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Card"
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardFront(
    wordItem: PracticeWordItem,
    onPronounce: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top badges (Status & Part of Speech)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = wordItem.status)

                if (wordItem.partOfSpeech.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = wordItem.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Center: Big Word & Phonetic & Pronounce Button
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = wordItem.word,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (wordItem.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "[${wordItem.phonetic}]",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                IconButton(
                    onClick = onPronounce,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("flashcard_front_pronounce_btn")
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Pronounce Word",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Bottom Tap to Flip Cue
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Flip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap card to reveal definition & translation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardBack(
    wordItem: PracticeWordItem,
    aiExplanation: String?,
    isAiExplaining: Boolean,
    isGeminiConfigured: Boolean,
    onPronounce: () -> Unit,
    onRequestAi: () -> Unit,
    onRate: (WordStatus) -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Word + Audio + Flip Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wordItem.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onPronounce,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                StatusBadge(status = wordItem.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // English Definition
            Text(
                text = "DEFINITION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wordItem.definition,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Language Meaning / Translation
            if (wordItem.hindiMeaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "TRANSLATION / MEANING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wordItem.hindiMeaning,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Example Sentence
            if (wordItem.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "EXAMPLE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${wordItem.example}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Synonyms
            if (wordItem.synonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SYNONYMS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wordItem.synonyms.take(4).forEach { syn ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = syn,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // AI Tutor Breakdown (BYOK)
            if (isGeminiConfigured) {
                Spacer(modifier = Modifier.height(14.dp))
                if (aiExplanation == null && !isAiExplaining) {
                    OutlinedButton(
                        onClick = onRequestAi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcard_ai_explain_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explain with Gemini AI", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (isAiExplaining) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Generating AI explanation...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (aiExplanation != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Gemini AI Tutor",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiExplanation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Spaced Repetition Rating Buttons
            Text(
                text = "UPDATE MASTERY STATUS:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Still Learning
                Button(
                    onClick = { onRate(WordStatus.LEARNING) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("rate_learning_button")
                ) {
                    Text(
                        text = "Learning",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Reviewing
                Button(
                    onClick = { onRate(WordStatus.REVIEWING) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("rate_reviewing_button")
                ) {
                    Text(
                        text = "Reviewing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Mastered
                Button(
                    onClick = { onRate(WordStatus.MASTERED) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("rate_mastered_button")
                ) {
                    Text(
                        text = "Mastered",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: WordStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (status) {
        WordStatus.LEARNING -> Quad(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.onErrorContainer,
            "Learning",
            Icons.Default.Bookmark
        )
        WordStatus.REVIEWING -> Quad(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Reviewing",
            Icons.Default.Replay
        )
        WordStatus.MASTERED, WordStatus.KNOWN -> Quad(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Mastered",
            Icons.Default.Check
        )
        else -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Saved",
            Icons.Default.Bookmark
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
