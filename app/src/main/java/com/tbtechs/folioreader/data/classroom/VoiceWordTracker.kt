package com.tbtechs.folioreader.data.classroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceWordTracker @Inject constructor() {

    companion object {
        private const val TAG = "VoiceWordTracker"
        private const val RESTART_DELAY_MS = 300L
    }

    var onWordMatched: ((index: Int, word: String) -> Unit)? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private var matchEngine: WordMatchEngine? = null
    private var appContext: Context? = null

    private var isRestartPending = false

    private val restartRunnable = Runnable {
        isRestartPending = false
        if (_isListening.value) {
            safeStartListening()
        }
    }

    fun isAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun start(context: Context, engine: WordMatchEngine) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { start(context, engine) }
            return
        }

        stopInternal()

        appContext = context.applicationContext
        matchEngine = engine
        _isListening.value = true

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        initRecognizer()
        safeStartListening()
    }

    fun stop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stop() }
            return
        }
        stopInternal()
    }

    private fun stopInternal() {
        _isListening.value = false
        mainHandler.removeCallbacks(restartRunnable)
        isRestartPending = false

        speechRecognizer?.let { recognizer ->
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying recognizer: ${e.message}")
            }
        }
        speechRecognizer = null
        matchEngine = null
    }

    private fun initRecognizer() {
        val ctx = appContext ?: return
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    scheduleRestart(RESTART_DELAY_MS)
                }

                override fun onError(error: Int) {
                    Log.d(TAG, "SpeechRecognizer error: $error")
                    scheduleRestart(RESTART_DELAY_MS)
                }

                override fun onResults(results: Bundle?) {
                    handleSpeechResults(results)
                    scheduleRestart(0L)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    handleSpeechResults(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleSpeechResults(bundle: Bundle?): Boolean {
        val engine = matchEngine ?: return false
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val topResult = matches?.firstOrNull() ?: return false

        val matchedIndex = engine.processSpokenText(topResult)
        if (matchedIndex != null) {
            val wordText = engine.getWord(matchedIndex)?.text ?: ""
            Log.d(TAG, "Matched word at index $matchedIndex: '$wordText' for speech: '$topResult'")
            onWordMatched?.invoke(matchedIndex, wordText)
            return true
        }
        return false
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!_isListening.value) return
        if (isRestartPending && delayMs > 0) return

        mainHandler.removeCallbacks(restartRunnable)
        isRestartPending = true

        if (delayMs <= 0) {
            mainHandler.post(restartRunnable)
        } else {
            mainHandler.postDelayed(restartRunnable, delayMs)
        }
    }

    private fun safeStartListening() {
        if (!_isListening.value) return
        val intent = speechIntent ?: return

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed startListening, re-initializing recognizer", e)
            initRecognizer()
            try {
                speechRecognizer?.startListening(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed second startListening attempt", e2)
                scheduleRestart(RESTART_DELAY_MS)
            }
        }
    }
}
