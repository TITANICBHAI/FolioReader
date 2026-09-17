package com.tbtechs.folioreader

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.tbtechs.folioreader.data.ai.IGeminiRepository
import com.tbtechs.folioreader.data.preferences.ReaderPreferencesRepository
import com.tbtechs.folioreader.data.preferences.readerDataStore
import com.tbtechs.folioreader.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var preferencesRepository: ReaderPreferencesRepository
    private lateinit var fakeTranslationRepo: FakeTranslationRepository
    private lateinit var fakeGeminiRepo: FakeGeminiRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            context.readerDataStore.edit { it.clear() }
        }
        preferencesRepository = ReaderPreferencesRepository(context, context.readerDataStore)
        fakeTranslationRepo = FakeTranslationRepository(isDownloaded = false)
        fakeGeminiRepo = FakeGeminiRepository()
        viewModel = SettingsViewModel(fakeTranslationRepo, preferencesRepository, fakeGeminiRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_notDownloaded() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isModelDownloaded)
        assertFalse(state.isDownloading)
        assertEquals(0f, state.downloadProgress)
    }

    @Test
    fun downloadModel_success_updatesStateAndPersists() = runTest {
        viewModel.downloadModel().join()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isModelDownloaded)
        assertFalse(state.isDownloading)
        assertEquals(1f, state.downloadProgress)
        assertEquals(1, fakeTranslationRepo.downloadCallCount)

        // Check preference
        val pref = preferencesRepository.isHindiModelDownloaded.first()
        assertTrue(pref)
    }

    @Test
    fun deleteModel_success_updatesStateAndPersists() = runTest {
        // Setup downloaded
        fakeTranslationRepo.isDownloaded = true
        preferencesRepository.setHindiModelDownloaded(true)
        viewModel.checkModelStatus().join()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isModelDownloaded)

        viewModel.deleteModel().join()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isModelDownloaded)
        assertEquals(1, fakeTranslationRepo.deleteCallCount)

        val pref = preferencesRepository.isHindiModelDownloaded.first()
        assertFalse(pref)
    }
}
