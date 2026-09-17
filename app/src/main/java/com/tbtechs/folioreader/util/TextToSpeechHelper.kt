package com.tbtechs.folioreader.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("TextToSpeechHelper", "US English TTS language not supported or missing data")
                    } else {
                        isInitialized = true
                    }
                } else {
                    Log.w("TextToSpeechHelper", "Failed to initialize TextToSpeech: status $status")
                }
            }
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Error creating TextToSpeech: ${e.message}", e)
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            // Attempt to re-initialize if needed
            tts?.setLanguage(Locale.US)
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexiread_word_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Error speaking text: ${e.message}", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w("TextToSpeechHelper", "Error shutting down TTS: ${e.message}")
        }
    }
}
