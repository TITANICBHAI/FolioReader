package com.tbtechsdev.lexiread.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbtechsdev.lexiread.data.db.entities.CachedDefinition
import com.tbtechsdev.lexiread.data.db.entities.UserWordEntity
import com.tbtechsdev.lexiread.data.dictionary.DictionaryRepository
import com.tbtechsdev.lexiread.data.vocabulary.UserWordRepository
import com.tbtechsdev.lexiread.domain.model.WordStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val learningWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllByStatus(WordStatus.LEARNING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knownWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllByStatus(WordStatus.KNOWN)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWords: StateFlow<List<UserWordEntity>> = userWordRepository
        .getAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWord = MutableStateFlow<UserWordEntity?>(null)
    val selectedWord: StateFlow<UserWordEntity?> = _selectedWord.asStateFlow()

    private val _selectedWordEntry = MutableStateFlow<CachedDefinition?>(null)
    val selectedWordEntry: StateFlow<CachedDefinition?> = _selectedWordEntry.asStateFlow()

    private val _wordToEdit = MutableStateFlow<UserWordEntity?>(null)
    val wordToEdit: StateFlow<UserWordEntity?> = _wordToEdit.asStateFlow()

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectWord(entity: UserWordEntity?) {
        _selectedWord.value = entity
        if (entity != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val entry = dictionaryRepository.getDefinition(entity.word)
                _selectedWordEntry.value = entry
            }
        } else {
            _selectedWordEntry.value = null
        }
    }

    fun saveWord(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.saveWord(entity.word, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun markKnown(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.markAs(entity.word, WordStatus.KNOWN, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun markIgnored(entity: UserWordEntity) {
        viewModelScope.launch {
            userWordRepository.markAs(entity.word, WordStatus.IGNORED, entity.sourceBookPath)
            _selectedWord.value = null
        }
    }

    fun dismissWordPanel() {
        _selectedWord.value = null
        _selectedWordEntry.value = null
    }

    fun showEditDialog(entity: UserWordEntity) {
        _wordToEdit.value = entity
    }

    fun dismissEditDialog() {
        _wordToEdit.value = null
    }

    fun updateWordStatus(word: String, newStatus: WordStatus) {
        viewModelScope.launch {
            userWordRepository.markAs(word, newStatus, null)
            _wordToEdit.value = null
        }
    }

    fun deleteWord(word: String) {
        viewModelScope.launch {
            userWordRepository.deleteWord(word)
            _wordToEdit.value = null
        }
    }
}
