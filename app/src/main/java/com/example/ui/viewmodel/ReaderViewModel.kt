package com.example.ui.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dictionary.DictionaryService
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.DictionaryEntry
import com.example.data.model.ReaderSettings
import com.example.data.model.TextSelection
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.BookRepository
import com.example.data.repository.VocabularyRepository
import com.example.ui.components.ReaderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val isLoading: Boolean = true,
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val settings: ReaderSettings = ReaderSettings(),
    val isControlsVisible: Boolean = false,
    val isSettingsSheetVisible: Boolean = false,
    val isTocSheetVisible: Boolean = false,
    val isDictionarySheetVisible: Boolean = false,
    val fraction: Float = 0f,
    val currentCfi: String = "",
    val currentTocLabel: String = "",
    val tocEntries: List<ReaderEngine.TocEntry> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val isCurrentBookmarked: Boolean = false,
    val activeSelection: TextSelection? = null,
    val selectedDictionaryEntry: DictionaryEntry? = null,
    val selectedLookupWord: String = "",
    val lookupWordContext: String = "",
    val isWordSavedInVocabulary: Boolean = false,
    val isLookingUpWord: Boolean = false,
    val toastMessage: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val vocabularyRepository = VocabularyRepository(application)
    private val dictionaryService = DictionaryService.getInstance(application)

    val engine = ReaderEngine()

    private val _uiState = MutableStateFlow(
        ReaderUiState(settings = ThemePreferences.loadReaderSettings(application))
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var pendingRestoreCfi: String = ""
    private var loaded = false
    private var cachedBookmarks: List<BookmarkEntity> = emptyList()

    init {
        engine.onReady = { /* reader.html loaded */ }
        engine.onLoaded = { title, author ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                bookTitle = title.ifBlank { _uiState.value.bookTitle },
                bookAuthor = author
            )
            loaded = true
            if (pendingRestoreCfi.isNotBlank()) {
                engine.goTo(pendingRestoreCfi)
                pendingRestoreCfi = ""
            }
        }
        engine.onRelocate = { fraction, cfi, _, tocLabel ->
            val state = _uiState.value
            _uiState.value = _uiState.value.copy(
                fraction = fraction,
                currentCfi = cfi,
                currentTocLabel = tocLabel
            )
            refreshBookmarkState()
            if (state.bookId.isNotBlank() && loaded) {
                viewModelScope.launch {
                    bookRepository.saveReadingProgress(state.bookId, fraction, cfi)
                }
            }
        }
        engine.onSelection = { text, cfi, index ->
            _uiState.value = _uiState.value.copy(
                activeSelection = TextSelection(selectedText = text, cfi = cfi, index = index, sentenceContext = text),
                isControlsVisible = false
            )
        }
        engine.onToc = { entries ->
            _uiState.value = _uiState.value.copy(tocEntries = entries)
        }
        engine.onTap = { x, width ->
            val ratio = if (width > 0f) x / width else 0.5f
            when {
                _uiState.value.activeSelection != null -> clearSelection()
                ratio < 0.33f -> prevPage()
                ratio > 0.67f -> nextPage()
                else -> toggleControls()
            }
        }

        viewModelScope.launch { dictionaryService.initialize() }
    }

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, bookId = bookId)
            val entity = bookRepository.getBookEntity(bookId)
            val file = bookRepository.getEpubFile(bookId)

            _uiState.value = _uiState.value.copy(
                bookTitle = entity?.title ?: "EPUB Reader",
                bookAuthor = entity?.author ?: ""
            )
            pendingRestoreCfi = entity?.currentCfi ?: ""

            loadBookmarks(bookId)

            if (file == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    toastMessage = "Could not open this book"
                )
                return@launch
            }

            val base64 = withContext(Dispatchers.IO) {
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            }
            engine.load(base64)
        }
    }

    private fun loadBookmarks(bookId: String) {
        viewModelScope.launch {
            bookRepository.getBookmarks(bookId).collect { list ->
                cachedBookmarks = list
                _uiState.value = _uiState.value.copy(bookmarks = list)
                refreshBookmarkState()
            }
        }
    }

    private fun refreshBookmarkState() {
        val cfi = _uiState.value.currentCfi
        val marked = cfi.isNotBlank() && cachedBookmarks.any { it.cfi == cfi }
        if (_uiState.value.isCurrentBookmarked != marked) {
            _uiState.value = _uiState.value.copy(isCurrentBookmarked = marked)
        }
    }

    fun toggleBookmark() {
        val state = _uiState.value
        if (state.currentCfi.isBlank()) return
        viewModelScope.launch {
            val now = bookRepository.toggleBookmark(
                bookId = state.bookId,
                chapterTitle = state.currentTocLabel.ifBlank { state.bookTitle },
                cfi = state.currentCfi,
                excerpt = state.currentTocLabel
            )
            _uiState.value = _uiState.value.copy(
                toastMessage = if (now) "Page bookmarked" else "Bookmark removed"
            )
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(
            isControlsVisible = false,
            isSettingsSheetVisible = false,
            isTocSheetVisible = false
        )
    }

    fun nextPage() {
        clearSelection()
        engine.next()
    }

    fun prevPage() {
        clearSelection()
        engine.prev()
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.value = _uiState.value.copy(settings = updated)
        ThemePreferences.saveReaderSettings(getApplication(), updated, syncAppTheme = true)
        engine.setSettings(updated)
    }

    fun toggleSettingsSheet() {
        _uiState.value = _uiState.value.copy(
            isSettingsSheetVisible = !_uiState.value.isSettingsSheetVisible,
            isTocSheetVisible = false
        )
    }

    fun toggleTocSheet() {
        _uiState.value = _uiState.value.copy(
            isTocSheetVisible = !_uiState.value.isTocSheetVisible,
            isSettingsSheetVisible = false
        )
        engine.requestToc()
    }

    fun goToHref(href: String) {
        engine.goTo(href)
        _uiState.value = _uiState.value.copy(isTocSheetVisible = false)
    }

    fun selectBookmark(bookmark: BookmarkEntity) {
        if (bookmark.cfi.isNotBlank()) {
            engine.goTo(bookmark.cfi)
        }
        _uiState.value = _uiState.value.copy(isTocSheetVisible = false)
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookRepository.removeBookmark(bookmarkId)
        }
    }

    fun saveQuote() {
        val state = _uiState.value
        val selection = state.activeSelection ?: return
        val quote = selection.selectedText.trim()
        if (quote.isBlank()) return

        viewModelScope.launch {
            bookRepository.saveQuote(
                bookId = state.bookId,
                chapterTitle = state.currentTocLabel.ifBlank { state.bookTitle },
                quote = quote,
                cfi = selection.cfi
            )
            if (selection.cfi.isNotBlank()) {
                engine.addAnnotation(selection.cfi, "#f6d365", quote)
            }
            _uiState.value = _uiState.value.copy(
                activeSelection = null,
                toastMessage = "Quote saved and highlighted"
            )
        }
    }

    fun lookupWord(rawWord: String, contextSnippet: String = "") {
        val trimmed = rawWord.trim()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLookingUpWord = true,
                isDictionarySheetVisible = true,
                selectedLookupWord = trimmed,
                lookupWordContext = contextSnippet
            )

            val entry = dictionaryService.lookupWord(trimmed)
            if (entry != null) {
                val isSaved = vocabularyRepository.isWordSaved(entry.word)
                _uiState.value = _uiState.value.copy(
                    isLookingUpWord = false,
                    selectedDictionaryEntry = entry,
                    isWordSavedInVocabulary = isSaved
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLookingUpWord = false,
                    selectedDictionaryEntry = null
                )
            }
        }
    }

    fun closeDictionarySheet() {
        _uiState.value = _uiState.value.copy(isDictionarySheetVisible = false, selectedDictionaryEntry = null)
    }

    fun saveCurrentWordToVocabulary() {
        val state = _uiState.value
        val entry = state.selectedDictionaryEntry ?: return
        viewModelScope.launch {
            vocabularyRepository.saveWordFromDictionary(
                entry = entry,
                bookTitle = state.bookTitle,
                contextSentence = state.lookupWordContext
            )
            _uiState.value = _uiState.value.copy(
                isWordSavedInVocabulary = true,
                toastMessage = "Saved '${entry.word}' to your Vocabulary List!"
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(activeSelection = null)
        engine.clearSelection()
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
