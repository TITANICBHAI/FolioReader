package com.tbtechsdev.lexiread.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbtechsdev.lexiread.data.ai.IGeminiRepository
import com.tbtechsdev.lexiread.data.preferences.ReaderPreferencesRepository
import com.tbtechsdev.lexiread.data.translation.ITranslationRepository
import com.tbtechsdev.lexiread.data.vocabulary.IUserWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GeminiValidationStatus {
    NONE,
    VALID,
    INVALID
}

data class SettingsUiState(
    val isModelDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val vocabAssistanceEnabled: Boolean = true,
    val maxHighlightsPerPage: Int = 6,
    val darkMode: String = "system",
    val showClearVocabDialog: Boolean = false,
    val isVocabCleared: Boolean = false,
    val isGeminiConfigured: Boolean = false,
    val geminiApiKeyInput: String = "",
    val isTestingGeminiKey: Boolean = false,
    val geminiValidationStatus: GeminiValidationStatus = GeminiValidationStatus.NONE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val translationRepository: ITranslationRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val geminiRepository: IGeminiRepository,
    private val userWordRepository: IUserWordRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.isHindiModelDownloaded.collect { downloaded ->
                _uiState.value = _uiState.value.copy(isModelDownloaded = downloaded)
            }
        }
        viewModelScope.launch {
            preferencesRepository.isVocabAssistanceEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(vocabAssistanceEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.maxHighlightsPerPage.collect { count ->
                _uiState.value = _uiState.value.copy(maxHighlightsPerPage = count)
            }
        }
        viewModelScope.launch {
            preferencesRepository.darkModePreference.collect { mode ->
                _uiState.value = _uiState.value.copy(darkMode = mode)
                applyNightMode(mode)
            }
        }
        checkModelStatus()
        checkGeminiStatus()
    }

    fun checkGeminiStatus() {
        val configured = geminiRepository.isConfigured()
        val storedKey = geminiRepository.getStoredApiKey() ?: ""
        _uiState.value = _uiState.value.copy(
            isGeminiConfigured = configured,
            geminiApiKeyInput = if (configured && _uiState.value.geminiApiKeyInput.isBlank()) storedKey else _uiState.value.geminiApiKeyInput
        )
    }

    fun checkModelStatus(): Job = viewModelScope.launch {
        val downloaded = translationRepository.isModelDownloaded()
        _uiState.value = _uiState.value.copy(isModelDownloaded = downloaded)
        preferencesRepository.setHindiModelDownloaded(downloaded)
    }

    fun downloadModel(): Job {
        if (_uiState.value.isDownloading) return Job()
        _uiState.value = _uiState.value.copy(
            isDownloading = true,
            downloadProgress = 0f,
            errorMessage = null
        )
        return viewModelScope.launch {
            try {
                translationRepository.downloadModel { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    isModelDownloaded = true
                )
                preferencesRepository.setHindiModelDownloaded(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 0f,
                    errorMessage = "Failed to download model. Please check your internet connection."
                )
            }
        }
    }

    fun deleteModel(): Job = viewModelScope.launch {
        try {
            translationRepository.deleteModel()
            _uiState.value = _uiState.value.copy(
                isModelDownloaded = false,
                errorMessage = null
            )
            preferencesRepository.setHindiModelDownloaded(false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to delete model."
            )
        }
    }

    fun setVocabAssistanceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVocabAssistanceEnabled(enabled)
        }
    }

    fun setMaxHighlightsPerPage(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setMaxHighlightsPerPage(count)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setDarkModePreference(mode)
            applyNightMode(mode)
        }
    }

    private fun applyNightMode(mode: String) {
        val nightMode = when (mode.lowercase()) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun showClearVocabDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearVocabDialog = show)
    }

    fun clearVocabulary(): Job = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(showClearVocabDialog = false)
        try {
            userWordRepository?.deleteAll()
            _uiState.value = _uiState.value.copy(isVocabCleared = true)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to clear vocabulary: ${e.message}")
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun onGeminiApiKeyInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(
            geminiApiKeyInput = input,
            geminiValidationStatus = GeminiValidationStatus.NONE
        )
    }

    fun saveAndTestGeminiKey(): Job = viewModelScope.launch {
        val key = _uiState.value.geminiApiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(
                geminiValidationStatus = GeminiValidationStatus.INVALID
            )
            return@launch
        }

        _uiState.value = _uiState.value.copy(
            isTestingGeminiKey = true,
            geminiValidationStatus = GeminiValidationStatus.NONE
        )

        val result = geminiRepository.saveAndTestKey(key)
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isTestingGeminiKey = false,
                isGeminiConfigured = true,
                geminiValidationStatus = GeminiValidationStatus.VALID
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isTestingGeminiKey = false,
                geminiValidationStatus = GeminiValidationStatus.INVALID
            )
        }
    }

    fun removeGeminiKey() {
        geminiRepository.removeKey()
        _uiState.value = _uiState.value.copy(
            isGeminiConfigured = false,
            geminiApiKeyInput = "",
            geminiValidationStatus = GeminiValidationStatus.NONE
        )
    }
}
