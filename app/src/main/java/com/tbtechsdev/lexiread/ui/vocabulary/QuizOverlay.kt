package com.tbtechsdev.lexiread.ui.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtechsdev.lexiread.domain.model.WordStatus

@Composable
fun QuizOverlay(
    mode: PracticeMode,
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedIndex: Int?,
    isSubmitted: Boolean,
    answers: List<QuizAnswer>,
    aiExplanation: String?,
    isAiExplaining: Boolean,
    isGeminiConfigured: Boolean,
    onSubmitOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onRetakeQuiz: () -> Unit,
    onStartFlashcards: () -> Unit,
    onPronounce: (String) -> Unit,
    onRequestAiExplanation: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz_overlay")
    ) {
        if (mode == PracticeMode.QUIZ_RESULTS) {
            QuizResultsView(
                answers = answers,
                onRetakeQuiz = onRetakeQuiz,
                onStartFlashcards = onStartFlashcards,
                onClose = onClose
            )
        } else {
            QuizActiveView(
                questions = questions,
                currentIndex = currentIndex,
                selectedIndex = selectedIndex,
                isSubmitted = isSubmitted,
                answers = answers,
                aiExplanation = aiExplanation,
                isAiExplaining = isAiExplaining,
                isGeminiConfigured = isGeminiConfigured,
                onSubmitOption = onSubmitOption,
                onNextQuestion = onNextQuestion,
                onPronounce = onPronounce,
                onRequestAiExplanation = onRequestAiExplanation,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun QuizActiveView(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedIndex: Int?,
    isSubmitted: Boolean,
    answers: List<QuizAnswer>,
    aiExplanation: String?,
    isAiExplaining: Boolean,
    isGeminiConfigured: Boolean,
    onSubmitOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPronounce: (String) -> Unit,
    onRequestAiExplanation: (String, String) -> Unit,
    onClose: () -> Unit
) {
    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No quiz questions available", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) { Text("Back") }
            }
        }
        return
    }

    val question = questions[currentIndex.coerceIn(questions.indices)]
    val progress = (currentIndex + 1).toFloat() / questions.size.toFloat()
    val correctCount = answers.count { it.isCorrect }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("exit_quiz_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Quick Quiz",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "Score: $correctCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .testTag("quiz_progress_bar"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Question Target Word Card
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "What is the meaning of:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = question.targetWord.word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onPronounce(question.targetWord.word) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("quiz_word_pronounce_btn")
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (question.targetWord.phonetic.isNotBlank() || question.targetWord.partOfSpeech.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (question.targetWord.partOfSpeech.isNotBlank()) {
                            Text(
                                text = question.targetWord.partOfSpeech,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (question.targetWord.phonetic.isNotBlank()) {
                            Text(
                                text = "[${question.targetWord.phonetic}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Multiple Choice Options
        val letters = listOf("A", "B", "C", "D")
        question.options.forEachIndexed { optIndex, optionText ->
            val isOptionSelected = selectedIndex == optIndex
            val isOptionCorrect = optIndex == question.correctIndex

            // Color logic after submission
            val (containerColor, borderColor, contentColor) = when {
                !isSubmitted -> {
                    Triple(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onSurface
                    )
                }
                isOptionSelected && isOptionCorrect -> {
                    Triple(
                        Color(0xFFE8F5E9), // Light green
                        Color(0xFF2E7D32),
                        Color(0xFF1B5E20)
                    )
                }
                isOptionSelected && !isOptionCorrect -> {
                    Triple(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                !isOptionSelected && isOptionCorrect -> {
                    Triple(
                        Color(0xFFE8F5E9),
                        Color(0xFF2E7D32),
                        Color(0xFF1B5E20)
                    )
                }
                else -> {
                    Triple(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Surface(
                onClick = {
                    if (!isSubmitted) {
                        onSubmitOption(optIndex)
                    }
                },
                enabled = !isSubmitted,
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(if (isOptionSelected || (isSubmitted && isOptionCorrect)) 2.dp else 1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .testTag("quiz_option_$optIndex")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSubmitted && isOptionCorrect) Color(0xFF2E7D32)
                                else if (isSubmitted && isOptionSelected) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitted && isOptionCorrect) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Correct",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isSubmitted && isOptionSelected) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Incorrect",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = letters.getOrElse(optIndex) { "${optIndex + 1}" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isOptionSelected || (isSubmitted && isOptionCorrect)) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Post-answer Explanation Card
        AnimatedVisibility(
            visible = isSubmitted,
            enter = fadeIn() + expandVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                val isAnswerCorrect = selectedIndex == question.correctIndex
                val currentStatus = question.targetWord.status

                val (feedbackText, feedbackColor) = if (isAnswerCorrect) {
                    val promotedTo = when (currentStatus) {
                        WordStatus.LEARNING -> "Promoted to Reviewing"
                        WordStatus.REVIEWING -> "Mastered! 🌟"
                        else -> "Mastery Maintained"
                    }
                    Pair("Correct! 🎉 $promotedTo", Color(0xFF2E7D32))
                } else {
                    Pair("Incorrect. Marked for Learning review", MaterialTheme.colorScheme.error)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnswerCorrect) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = feedbackText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = feedbackColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // AI Explanation trigger if BYOK is active
                        if (isGeminiConfigured) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (aiExplanation == null && !isAiExplaining) {
                                OutlinedButton(
                                    onClick = { onRequestAiExplanation(question.targetWord.word, question.targetWord.example) },
                                    modifier = Modifier.testTag("quiz_ai_explain_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ask Gemini AI Tutor", style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (isAiExplaining) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetching AI explanation...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else if (aiExplanation != null) {
                                Text(
                                    text = aiExplanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Next Question / Finish Quiz button
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quiz_next_question_button")
                ) {
                    Text(
                        text = if (currentIndex < questions.size - 1) "Next Question" else "View Results",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizResultsView(
    answers: List<QuizAnswer>,
    onRetakeQuiz: () -> Unit,
    onStartFlashcards: () -> Unit,
    onClose: () -> Unit
) {
    val total = answers.size
    val correct = answers.count { it.isCorrect }
    val percentage = if (total > 0) (correct * 100) / total else 0

    val (title, subtitle) = when {
        percentage >= 80 -> Pair("Vocabulary Master! 🏆", "Outstanding retention and mastery of reading vocabulary.")
        percentage >= 60 -> Pair("Great Job! 👍", "Solid progress! Review the remaining words to achieve mastery.")
        else -> Pair("Keep Practicing! 📚", "Good effort! Flip through the flashcards to strengthen memory.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Hero Score Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$correct / $total ($percentage%)",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "WORD MASTERY UPDATES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Words breakdown list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(answers) { answer ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (answer.isCorrect) Color(0xFF2E7D32).copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (answer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = if (answer.isCorrect) "Correct" else "Incorrect",
                            tint = if (answer.isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = answer.question.targetWord.word,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = answer.question.targetWord.definition,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        StatusBadge(status = answer.updatedStatus)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onStartFlashcards,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quiz_review_flashcards_btn")
            ) {
                Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flashcards", fontSize = 13.sp)
            }

            FilledTonalButton(
                onClick = onRetakeQuiz,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quiz_retake_btn")
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retake", fontSize = 13.sp)
            }

            Button(
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quiz_done_btn")
            ) {
                Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
