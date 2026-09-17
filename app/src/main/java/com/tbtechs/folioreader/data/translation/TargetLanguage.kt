package com.tbtechs.folioreader.data.translation

import com.google.mlkit.nl.translate.TranslateLanguage

data class TargetLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
)

object SupportedLanguages {
    val ALL: List<TargetLanguage> = listOf(
        TargetLanguage(TranslateLanguage.HINDI, "Hindi", "हिन्दी", "🇮🇳"),
        TargetLanguage(TranslateLanguage.SPANISH, "Spanish", "Español", "🇪🇸"),
        TargetLanguage(TranslateLanguage.FRENCH, "French", "Français", "🇫🇷"),
        TargetLanguage(TranslateLanguage.GERMAN, "German", "Deutsch", "🇩🇪"),
        TargetLanguage(TranslateLanguage.ARABIC, "Arabic", "العربية", "🇸🇦"),
        TargetLanguage(TranslateLanguage.JAPANESE, "Japanese", "日本語", "🇯🇵"),
        TargetLanguage(TranslateLanguage.RUSSIAN, "Russian", "Русский", "🇷🇺"),
        TargetLanguage(TranslateLanguage.BENGALI, "Bengali", "বাংলা", "🇧🇩"),
        TargetLanguage(TranslateLanguage.MARATHI, "Marathi", "मराठी", "🇮🇳")
    )

    fun getByCode(code: String): TargetLanguage {
        return ALL.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ALL.first()
    }
}
