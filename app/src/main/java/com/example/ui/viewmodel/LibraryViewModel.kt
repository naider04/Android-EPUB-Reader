package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BookEntity
import com.example.data.repository.BookRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val importMessage: String? = null,
    val isImporting: Boolean = false,
    val searchQuery: String = ""
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val vocabularyRepository = VocabularyRepository(application)

    val books: StateFlow<List<BookEntity>> = bookRepository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vocabularyCount: StateFlow<Int> = vocabularyRepository.vocabularyCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.initializeLibrary()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun importEpub(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            val result = bookRepository.importEpubFile(uri)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "Successfully imported '${result.getOrNull()?.title}'"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "Could not import EPUB: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid file"}"
                )
            }
        }
    }

    fun clearImportMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }
}
