package com.tbtechs.folioreader.domain.model

/**
 * Status representing a user's familiarity and mastery of a word.
 * Spaced repetition progression: LEARNING -> REVIEWING -> MASTERED.
 */
enum class WordStatus {
    UNKNOWN,
    LEARNING,
    REVIEWING,
    MASTERED,
    KNOWN, // Backwards-compatible alias for MASTERED
    IGNORED;

    val displayName: String
        get() = when (this) {
            UNKNOWN -> "Unknown"
            LEARNING -> "Learning"
            REVIEWING -> "Reviewing"
            MASTERED, KNOWN -> "Mastered"
            IGNORED -> "Ignored"
        }
}
