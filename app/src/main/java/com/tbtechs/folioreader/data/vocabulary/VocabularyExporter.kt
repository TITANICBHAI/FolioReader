package com.tbtechs.folioreader.data.vocabulary

import com.tbtechs.folioreader.data.db.entities.CachedDefinition
import com.tbtechs.folioreader.data.db.entities.UserWordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VocabularyExportFormat {
    ANKI_CSV,
    PLAIN_TEXT
}

object VocabularyExporter {

    private fun escapeCsv(text: String?): String {
        if (text == null) return "\"\""
        val escaped = text.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Generates an Anki-compatible CSV formatted string with headers and escaped fields.
     */
    fun toAnkiCsv(
        words: List<UserWordEntity>,
        definitions: Map<String, CachedDefinition> = emptyMap()
    ): String {
        val sb = StringBuilder()
        // Header row
        sb.append("Word,PartOfSpeech,Phonetic,Definition,Translation,Example,Status,Synonyms,Lookups\n")

        for (item in words) {
            val def = definitions[item.word.lowercase()]
            val word = escapeCsv(item.word)
            val pos = escapeCsv(def?.partOfSpeech.orEmpty())
            val phonetic = escapeCsv(def?.phonetic.orEmpty())
            val definition = escapeCsv(def?.englishDefinition.orEmpty())
            val translation = escapeCsv(def?.hindiMeaning.orEmpty())
            val example = escapeCsv(def?.example.orEmpty())
            val status = escapeCsv(item.status)
            val synonyms = escapeCsv(def?.synonyms.orEmpty())
            val lookups = escapeCsv(item.lookupCount.toString())

            sb.append("$word,$pos,$phonetic,$definition,$translation,$example,$status,$synonyms,$lookups\n")
        }

        return sb.toString()
    }

    /**
     * Generates a clean human-readable study text guide suitable for reading or printing.
     */
    fun toPlainText(
        words: List<UserWordEntity>,
        definitions: Map<String, CachedDefinition> = emptyMap()
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("============================================================\n")
        sb.append("  FOLIO READER VOCABULARY STUDY LIST\n")
        sb.append("  Total Words: ${words.size}  |  Exported: $dateStr\n")
        sb.append("============================================================\n\n")

        words.forEachIndexed { index, item ->
            val def = definitions[item.word.lowercase()]
            val num = index + 1
            val phoneticStr = if (!def?.phonetic.isNullOrBlank()) " [${def?.phonetic}]" else ""
            sb.append("$num. ${item.word.uppercase()}$phoneticStr\n")

            val posInfo = buildList {
                if (!def?.partOfSpeech.isNullOrBlank()) add(def.partOfSpeech)
                add("Status: ${item.status}")
                if (item.lookupCount > 0) add("Lookups: ${item.lookupCount}")
            }.joinToString(" • ")

            if (posInfo.isNotBlank()) {
                sb.append("   $posInfo\n")
            }

            if (!def?.englishDefinition.isNullOrBlank()) {
                sb.append("   • Definition: ${def.englishDefinition}\n")
            }

            if (!def?.hindiMeaning.isNullOrBlank()) {
                sb.append("   • Translation: ${def.hindiMeaning}\n")
            }

            if (!def?.example.isNullOrBlank()) {
                sb.append("   • Example: \"${def.example}\"\n")
            }

            if (!def?.synonyms.isNullOrBlank()) {
                sb.append("   • Synonyms: ${def.synonyms}\n")
            }

            sb.append("\n------------------------------------------------------------\n\n")
        }

        return sb.toString()
    }
}
