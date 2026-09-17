package com.tbtechs.folioreader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

data class ReaderSavedState(
    val uriString: String? = null,
    val fileName: String? = null,
    val pageIndex: Int = 0
)

@Singleton
class ReaderPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_LAST_URI = stringPreferencesKey("last_pdf_uri")
        private val KEY_LAST_NAME = stringPreferencesKey("last_pdf_name")
        private val KEY_LAST_PAGE = intPreferencesKey("last_read_page")
        private val KEY_VOCAB_ASSISTANCE = booleanPreferencesKey("vocab_assistance_enabled")
        private val KEY_MAX_HIGHLIGHTS = intPreferencesKey("max_highlights_per_page")
        private val KEY_HINDI_MODEL_DOWNLOADED = booleanPreferencesKey("hindi_model_downloaded")
        private val KEY_TRANSLATION_PROMPT_SHOWN = booleanPreferencesKey("translation_prompt_shown")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode_preference")
        private val KEY_SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation_preference")
        private val KEY_TARGET_LANGUAGE = stringPreferencesKey("target_translation_language")
    }

    val targetLanguage: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_TARGET_LANGUAGE] ?: "hi"
        }

    suspend fun setTargetLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_TARGET_LANGUAGE] = languageCode
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    val darkModePreference: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: "system"
        }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDarkModePreference(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = mode
        }
    }

    val screenOrientationPreference: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SCREEN_ORIENTATION] ?: "sensor"
        }

    suspend fun setScreenOrientationPreference(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SCREEN_ORIENTATION] = mode
        }
    }

    val isHindiModelDownloaded: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_HINDI_MODEL_DOWNLOADED] ?: false
        }

    val isTranslationPromptShown: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_TRANSLATION_PROMPT_SHOWN] ?: false
        }

    suspend fun setHindiModelDownloaded(downloaded: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HINDI_MODEL_DOWNLOADED] = downloaded
        }
    }

    suspend fun setTranslationPromptShown(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TRANSLATION_PROMPT_SHOWN] = shown
        }
    }

    val readerStateFlow: Flow<ReaderSavedState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ReaderSavedState(
                uriString = preferences[KEY_LAST_URI],
                fileName = preferences[KEY_LAST_NAME],
                pageIndex = preferences[KEY_LAST_PAGE] ?: 0
            )
        }

    val isVocabAssistanceEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_VOCAB_ASSISTANCE] ?: true
        }

    val maxHighlightsPerPage: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_MAX_HIGHLIGHTS] ?: 6
        }

    suspend fun setVocabAssistanceEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_VOCAB_ASSISTANCE] = enabled
        }
    }

    suspend fun setMaxHighlightsPerPage(count: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_MAX_HIGHLIGHTS] = count
        }
    }

    suspend fun saveLastPdf(uriString: String, fileName: String, pageIndex: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_URI] = uriString
            preferences[KEY_LAST_NAME] = fileName
            preferences[KEY_LAST_PAGE] = pageIndex
        }
    }

    suspend fun saveLastPage(pageIndex: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_PAGE] = pageIndex
        }
    }

    suspend fun clearLastPdf() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_URI)
            preferences.remove(KEY_LAST_NAME)
            preferences.remove(KEY_LAST_PAGE)
        }
    }
}
