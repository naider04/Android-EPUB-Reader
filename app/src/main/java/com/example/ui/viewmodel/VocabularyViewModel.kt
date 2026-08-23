package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dictionary.DictionaryService
import com.example.data.local.entity.VocabularyEntity
import com.example.data.model.ActiveDictionaryInfo
import com.example.data.model.DictionaryEntry
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VocabularyUiState(
    val searchQuery: String = "",
    val dictionarySearchQuery: String = "",
    val dictionarySearchResult: DictionaryEntry? = null,
    val isSearchingDictionary: Boolean = false,
    val importMessage: String? = null,
    val isImportingDictionary: Boolean = false,
    val showCustomJsonDialog: Boolean = false,
    val customJsonInput: String = "",
    val customJsonName: String = ""
)

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val vocabularyRepository = VocabularyRepository(application)
    private val dictionaryService = DictionaryService.getInstance(application)

    val allVocabulary: StateFlow<List<VocabularyEntity>> = vocabularyRepository.allVocabulary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDictionaryInfo: StateFlow<ActiveDictionaryInfo> = dictionaryService.activeDictionaryInfo

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateDictionarySearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(dictionarySearchQuery = query)
        if (query.isNotBlank()) {
            searchWordInDictionary(query)
        } else {
            _uiState.value = _uiState.value.copy(dictionarySearchResult = null)
        }
    }

    private fun searchWordInDictionary(word: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingDictionary = true)
            val result = dictionaryService.lookupWord(word)
            _uiState.value = _uiState.value.copy(
                isSearchingDictionary = false,
                dictionarySearchResult = result
            )
        }
    }

    fun toggleLearned(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            vocabularyRepository.toggleLearned(id, currentStatus)
        }
    }

    fun deleteWord(id: Long) {
        viewModelScope.launch {
            vocabularyRepository.deleteWord(id)
        }
    }

    fun saveEntryToVocabulary(entry: DictionaryEntry) {
        viewModelScope.launch {
            vocabularyRepository.saveWordFromDictionary(entry, "Manual Search", "")
            _uiState.value = _uiState.value.copy(
                importMessage = "Added '${entry.word}' to your Vocabulary List!"
            )
        }
    }

    fun importDictionaryFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImportingDictionary = true, importMessage = null)
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val fileName = uri.lastPathSegment ?: "Custom Dictionary.json"
                val result = dictionaryService.loadCustomDictionaryFromStream(inputStream, fileName)
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _uiState.value = _uiState.value.copy(
                        isImportingDictionary = false,
                        importMessage = "Successfully loaded custom dictionary with $count words!"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isImportingDictionary = false,
                        importMessage = "Error loading JSON dictionary: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid JSON"}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isImportingDictionary = false,
                    importMessage = "Cannot access the selected file."
                )
            }
        }
    }

    fun importDictionaryFromJsonString(jsonString: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImportingDictionary = true, importMessage = null, showCustomJsonDialog = false)
            val result = dictionaryService.loadCustomDictionaryFromJson(jsonString, name)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    isImportingDictionary = false,
                    importMessage = "Successfully installed dictionary '$name' ($count words)!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isImportingDictionary = false,
                    importMessage = "Could not parse dictionary JSON: ${result.exceptionOrNull()?.localizedMessage ?: "Format error"}"
                )
            }
        }
    }

    fun resetToDefaultDictionary() {
        viewModelScope.launch {
            dictionaryService.resetToDefaultDictionary()
            _uiState.value = _uiState.value.copy(importMessage = "Switched to Default Literary English Dictionary.")
        }
    }

    fun showCustomJsonDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCustomJsonDialog = show)
    }

    fun updateCustomJsonInput(json: String, name: String) {
        _uiState.value = _uiState.value.copy(customJsonInput = json, customJsonName = name)
    }

    fun clearImportMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }
}
