package com.tbtechs.folioreader.ui.vocabulary

import android.net.Uri
import com.tbtechs.folioreader.domain.model.WordStatus

/**
 * Source of vocabulary for practicing with Flashcards or Quizzes.
 */
sealed interface StudySource {
    data class SavedWords(val statusFilter: WordStatus? = null) : StudySource {
        val label: String
            get() = when (statusFilter) {
                null -> "All Saved Words"
                WordStatus.LEARNING -> "Learning Words"
                WordStatus.REVIEWING -> "Reviewing Words"
                WordStatus.MASTERED, WordStatus.KNOWN -> "Mastered Words"
                else -> "Saved Words"
            }
    }

    data class FromPdf(val uri: Uri, val title: String, val isRecent: Boolean = false) : StudySource
}

/**
 * Individual practice word with definition, translation, and metadata for flashcards and quiz.
 */
data class PracticeWordItem(
    val word: String,
    val definition: String,
    val hindiMeaning: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val example: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val status: WordStatus = WordStatus.LEARNING,
    val sourceBook: String? = null
)

/**
 * Multiple-choice question for the Quick Quiz.
 */
data class QuizQuestion(
    val id: Int,
    val targetWord: PracticeWordItem,
    val questionPrompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Recorded answer for quiz grading and mastery updates.
 */
data class QuizAnswer(
    val question: QuizQuestion,
    val selectedIndex: Int,
    val isCorrect: Boolean,
    val updatedStatus: WordStatus
)

/**
 * Practice active mode.
 */
enum class PracticeMode {
    NONE,
    FLASHCARDS,
    QUIZ,
    QUIZ_RESULTS
}
