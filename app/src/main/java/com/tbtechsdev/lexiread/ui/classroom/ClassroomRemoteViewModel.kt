package com.tbtechsdev.lexiread.ui.classroom

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbtechsdev.lexiread.data.classroom.NearbyConnectionManager
import com.tbtechsdev.lexiread.data.classroom.VoiceWordTracker
import com.tbtechsdev.lexiread.data.classroom.WordMatchEngine
import com.tbtechsdev.lexiread.domain.model.PdfWord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

sealed class RemoteUiState {
    object Searching : RemoteUiState()
    data class SessionsFound(val sessions: List<Pair<String, String>>) : RemoteUiState()
    object Connecting : RemoteUiState()
    object Connected : RemoteUiState()
}

@HiltViewModel
class ClassroomRemoteViewModel @Inject constructor() : ViewModel() {

    private val nearbyManager = NearbyConnectionManager()
    private val voiceTracker = VoiceWordTracker()
    private val matchEngine = WordMatchEngine()

    private val _uiState = MutableStateFlow<RemoteUiState>(RemoteUiState.Searching)
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _currentWord = MutableStateFlow("")
    val currentWord: StateFlow<String> = _currentWord.asStateFlow()

    private val _currentWordIndex = MutableStateFlow<Int?>(null)
    val currentWordIndex: StateFlow<Int?> = _currentWordIndex.asStateFlow()

    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords.asStateFlow()

    private val _isVoiceOn = MutableStateFlow(false)
    val isVoiceOn: StateFlow<Boolean> = _isVoiceOn.asStateFlow()

    private val _connectedSessionName = MutableStateFlow("")
    val connectedSessionName: StateFlow<String> = _connectedSessionName.asStateFlow()

    init {
        voiceTracker.onWordMatched = { index, _ ->
            nearbyManager.sendCommand("""{"action":"SET_WORD","index":$index}""")
        }

        nearbyManager.onCommandReceived = { json ->
            try {
                val obj = JSONObject(json)
                if (obj.optString("event") == "WORD_CHANGED") {
                    _currentWord.value = obj.optString("word", "")
                    if (obj.has("index")) {
                        _currentWordIndex.value = obj.getInt("index")
                    }
                    if (obj.has("total")) {
                        _totalWords.value = obj.getInt("total")
                    }
                }
            } catch (e: Exception) {
                Log.w("ClassroomRemoteVM", "Error parsing command: $json", e)
            }
        }

        viewModelScope.launch {
            nearbyManager.connectionState.collect { state ->
                when (state) {
                    is NearbyConnectionManager.ConnectionState.Idle,
                    is NearbyConnectionManager.ConnectionState.Discovering -> {
                        _uiState.value = RemoteUiState.Searching
                    }
                    is NearbyConnectionManager.ConnectionState.SessionsFound -> {
                        _uiState.value = if (state.list.isEmpty()) {
                            RemoteUiState.Searching
                        } else {
                            RemoteUiState.SessionsFound(state.list)
                        }
                    }
                    is NearbyConnectionManager.ConnectionState.Connected -> {
                        _connectedSessionName.value = state.name
                        _uiState.value = RemoteUiState.Connected
                    }
                    is NearbyConnectionManager.ConnectionState.Disconnected -> {
                        if (_uiState.value is RemoteUiState.Connected) {
                            if (_isVoiceOn.value) {
                                voiceTracker.stop()
                                _isVoiceOn.value = false
                            }
                            _uiState.value = RemoteUiState.Searching
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadPdfWords(words: List<PdfWord>) = matchEngine.loadWords(words)

    fun startDiscovery(context: Context) {
        _uiState.value = RemoteUiState.Searching
        nearbyManager.startDiscovery(context)
    }

    fun connect(endpointId: String, context: Context) {
        _uiState.value = RemoteUiState.Connecting
        nearbyManager.requestConnection(endpointId, context)
    }

    fun disconnect(context: Context) {
        if (_isVoiceOn.value) {
            voiceTracker.stop()
            _isVoiceOn.value = false
        }
        nearbyManager.stop()
        _currentWord.value = ""
        startDiscovery(context)
    }

    fun sendNext() = nearbyManager.sendCommand("""{"action":"NEXT"}""")

    fun sendBack() = nearbyManager.sendCommand("""{"action":"BACK"}""")

    fun toggleVoice(context: Context) {
        if (_isVoiceOn.value) {
            voiceTracker.stop()
            _isVoiceOn.value = false
        } else {
            voiceTracker.start(context, matchEngine)
            _isVoiceOn.value = true
        }
    }

    fun isVoiceAvailable(context: Context): Boolean {
        if (!voiceTracker.isAvailable(context)) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        voiceTracker.stop()
        nearbyManager.stop()
    }
}
