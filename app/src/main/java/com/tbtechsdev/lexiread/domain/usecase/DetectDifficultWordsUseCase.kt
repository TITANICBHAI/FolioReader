package com.tbtechsdev.lexiread.domain.usecase

import com.tbtechsdev.lexiread.data.dictionary.DictionaryRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepository
import com.tbtechsdev.lexiread.domain.model.PdfWord
import com.tbtechsdev.lexiread.domain.model.WordStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DetectDifficultWordsUseCase(
    private val dictionaryRepository: DictionaryRepository,
    private val userWordRepository: UserWordRepository,
    private val defaultDispatcher: CoroutineDispatcher
) {
    @Inject
    constructor(
        dictionaryRepository: DictionaryRepository,
        userWordRepository: UserWordRepository
    ) : this(dictionaryRepository, userWordRepository, Dispatchers.Default)

    suspend operator fun invoke(
        words: List<PdfWord>,
        maxCount: Int = 6
    ): List<PdfWord> = withContext(defaultDispatcher) {
        if (words.isEmpty() || maxCount <= 0) return@withContext emptyList()

        val candidates = mutableListOf<PdfWord>()
        val frequencyMap = mutableMapOf<String, Int>()

        var previousWordEndedSentence = true

        for (i in words.indices) {
            val word = words[i]
            val norm = word.normalizedText
            val raw = word.text.trim()

            val isFirstWordOfSentence = (i == 0) || previousWordEndedSentence
            previousWordEndedSentence = raw.endsWith('.') || raw.endsWith('?') || raw.endsWith('!') ||
                raw.endsWith(".\"") || raw.endsWith("?\"") || raw.endsWith("!\"")

            // 1. isCommonWord() = true -> skip
            if (dictionaryRepository.isCommonWord(norm)) {
                continue
            }

            // 2. frequency rank < 3000 -> skip
            val freq = dictionaryRepository.getFrequency(norm)
            if (freq < 3000) {
                continue
            }
            frequencyMap[norm] = freq

            // 3. UserWordRepository.getStatus(word) = KNOWN or IGNORED -> skip
            val status = userWordRepository.getStatus(norm)
            if (status == WordStatus.KNOWN || status == WordStatus.MASTERED || status == WordStatus.IGNORED) {
                continue
            }

            // 4. word length < 4 characters -> skip
            if (norm.length < 4) {
                continue
            }

            // 5. Likely proper noun: starts with uppercase and is not the first word of a detected sentence -> skip
            val startsWithUpper = raw.firstOrNull()?.isUpperCase() == true
            if (startsWithUpper && !isFirstWordOfSentence) {
                continue
            }

            candidates.add(word)
        }

        // Sort remaining words: highest frequency number first (rarest first)
        // Deduplicate distinct word forms while limiting to top N
        val sortedCandidates = candidates.sortedByDescending {
            frequencyMap[it.normalizedText] ?: 0
        }

        val topWordNorms = sortedCandidates
            .map { it.normalizedText }
            .distinct()
            .take(maxCount)
            .toSet()

        sortedCandidates
            .filter { it.normalizedText in topWordNorms }
            .take(maxCount)
    }
}
