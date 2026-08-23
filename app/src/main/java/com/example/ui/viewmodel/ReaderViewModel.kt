package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dictionary.DictionaryService
import com.example.data.epub.EpubPaginator
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.BookPage
import com.example.data.model.DictionaryEntry
import com.example.data.model.EpubBook
import com.example.data.model.ReaderSettings
import com.example.data.repository.BookRepository
import com.example.data.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val isLoading: Boolean = true,
    val book: EpubBook? = null,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val pages: List<BookPage> = emptyList(),
    val totalPagesInChapter: Int = 1,
    val settings: ReaderSettings = ReaderSettings(),
    val isControlsVisible: Boolean = false,
    val isSettingsSheetVisible: Boolean = false,
    val isTocSheetVisible: Boolean = false,
    val isBookmarksSheetVisible: Boolean = false,
    val isCurrentPageBookmarked: Boolean = false,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val selectedDictionaryEntry: DictionaryEntry? = null,
    val lookupWordContext: String = "",
    val isWordSavedInVocabulary: Boolean = false,
    val isDictionarySheetVisible: Boolean = false,
    val isLookingUpWord: Boolean = false,
    val toastMessage: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val vocabularyRepository = VocabularyRepository(application)
    private val dictionaryService = DictionaryService.getInstance(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentViewportWidthPx: Int = 0
    private var currentViewportHeightPx: Int = 0
    private var currentDensity: Float = 2.5f

    init {
        viewModelScope.launch {
            dictionaryService.initialize()
        }
    }

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val book = bookRepository.getEpubBook(bookId)
            val bookEntity = bookRepository.getBookEntity(bookId)

            if (book != null) {
                val initialChapterIndex = bookEntity?.currentChapterIndex ?: 0
                val initialPageIndex = bookEntity?.currentPageInChapter ?: 0

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    book = book,
                    currentChapterIndex = initialChapterIndex.coerceIn(0, (book.chapters.size - 1).coerceAtLeast(0)),
                    currentPageIndex = initialPageIndex
                )

                loadBookmarks(bookId)
                repaginate()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    toastMessage = "Could not load book"
                )
            }
        }
    }

    private fun loadBookmarks(bookId: String) {
        viewModelScope.launch {
            bookRepository.getBookmarks(bookId).collect { list ->
                _uiState.value = _uiState.value.copy(bookmarks = list)
                checkIfCurrentPageIsBookmarked()
            }
        }
    }

    fun onViewportDimensionsChanged(widthPx: Int, heightPx: Int, density: Float) {
        if (widthPx > 100 && heightPx > 100 &&
            (widthPx != currentViewportWidthPx || heightPx != currentViewportHeightPx || density != currentDensity)
        ) {
            currentViewportWidthPx = widthPx
            currentViewportHeightPx = heightPx
            currentDensity = density
            repaginate()
        }
    }

    fun repaginate() {
        val state = _uiState.value
        val book = state.book ?: return
        if (book.chapters.isEmpty()) return

        val chapterIndex = state.currentChapterIndex.coerceIn(0, book.chapters.size - 1)
        val chapter = book.chapters[chapterIndex]

        val currentCharOffset = state.pages.getOrNull(state.currentPageIndex)?.characterStartOffset ?: 0

        val newPages = EpubPaginator.paginateChapter(
            chapter = chapter,
            chapterIndex = chapterIndex,
            viewportWidthPx = currentViewportWidthPx,
            viewportHeightPx = currentViewportHeightPx,
            density = currentDensity,
            settings = state.settings
        )

        val newPageIndex = EpubPaginator.findMatchingPage(newPages, currentCharOffset)
            .coerceIn(0, (newPages.size - 1).coerceAtLeast(0))

        _uiState.value = _uiState.value.copy(
            pages = newPages,
            currentPageIndex = newPageIndex,
            totalPagesInChapter = newPages.size
        )

        checkIfCurrentPageIsBookmarked()
        saveReadingProgress()
    }

    fun onPageChanged(pageIndex: Int) {
        val state = _uiState.value
        if (pageIndex in state.pages.indices && pageIndex != state.currentPageIndex) {
            _uiState.value = _uiState.value.copy(currentPageIndex = pageIndex)
            checkIfCurrentPageIsBookmarked()
            saveReadingProgress()
        }
    }

    fun nextPage(): Boolean {
        val state = _uiState.value
        if (state.currentPageIndex < state.pages.size - 1) {
            onPageChanged(state.currentPageIndex + 1)
            return true
        } else if (state.currentChapterIndex < (state.book?.chapters?.size ?: 0) - 1) {
            // Advance to next chapter
            goToChapter(state.currentChapterIndex + 1, pageIndex = 0)
            return true
        }
        return false
    }

    fun prevPage(): Boolean {
        val state = _uiState.value
        if (state.currentPageIndex > 0) {
            onPageChanged(state.currentPageIndex - 1)
            return true
        } else if (state.currentChapterIndex > 0) {
            // Go to previous chapter's last page
            goToChapter(state.currentChapterIndex - 1, pageIndex = Int.MAX_VALUE)
            return true
        }
        return false
    }

    fun goToChapter(chapterIndex: Int, pageIndex: Int = 0) {
        val book = _uiState.value.book ?: return
        if (chapterIndex in book.chapters.indices) {
            val chapter = book.chapters[chapterIndex]
            val newPages = EpubPaginator.paginateChapter(
                chapter = chapter,
                chapterIndex = chapterIndex,
                viewportWidthPx = currentViewportWidthPx,
                viewportHeightPx = currentViewportHeightPx,
                density = currentDensity,
                settings = _uiState.value.settings
            )

            val clampedPage = pageIndex.coerceIn(0, (newPages.size - 1).coerceAtLeast(0))

            _uiState.value = _uiState.value.copy(
                currentChapterIndex = chapterIndex,
                currentPageIndex = clampedPage,
                pages = newPages,
                totalPagesInChapter = newPages.size,
                isTocSheetVisible = false,
                isBookmarksSheetVisible = false
            )

            checkIfCurrentPageIsBookmarked()
            saveReadingProgress()
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(
            isControlsVisible = false,
            isSettingsSheetVisible = false,
            isTocSheetVisible = false,
            isBookmarksSheetVisible = false
        )
    }

    fun toggleSettingsSheet() {
        _uiState.value = _uiState.value.copy(
            isSettingsSheetVisible = !_uiState.value.isSettingsSheetVisible,
            isTocSheetVisible = false,
            isBookmarksSheetVisible = false
        )
    }

    fun toggleTocSheet() {
        _uiState.value = _uiState.value.copy(
            isTocSheetVisible = !_uiState.value.isTocSheetVisible,
            isSettingsSheetVisible = false,
            isBookmarksSheetVisible = false
        )
    }

    fun toggleBookmarksSheet() {
        _uiState.value = _uiState.value.copy(
            isBookmarksSheetVisible = !_uiState.value.isBookmarksSheetVisible,
            isTocSheetVisible = false,
            isSettingsSheetVisible = false
        )
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.value = _uiState.value.copy(settings = updated)
        repaginate()
    }

    fun toggleBookmarkCurrentPage() {
        val state = _uiState.value
        val book = state.book ?: return
        val currentPage = state.pages.getOrNull(state.currentPageIndex) ?: return

        val excerpt = currentPage.blocks.filterIsInstance<com.example.data.model.PageBlock.Paragraph>()
            .firstOrNull()?.text ?: currentPage.chapterTitle

        viewModelScope.launch {
            val isNowBookmarked = bookRepository.toggleBookmark(
                bookId = book.id,
                chapterIndex = state.currentChapterIndex,
                chapterTitle = currentPage.chapterTitle,
                pageIndex = state.currentPageIndex,
                excerpt = excerpt
            )
            _uiState.value = _uiState.value.copy(
                isCurrentPageBookmarked = isNowBookmarked,
                toastMessage = if (isNowBookmarked) "Page bookmarked" else "Bookmark removed"
            )
        }
    }

    private fun checkIfCurrentPageIsBookmarked() {
        val state = _uiState.value
        val bookId = state.book?.id ?: return
        viewModelScope.launch {
            val isBookmarked = bookRepository.isPageBookmarked(bookId, state.currentChapterIndex, state.currentPageIndex)
            _uiState.value = _uiState.value.copy(isCurrentPageBookmarked = isBookmarked)
        }
    }

    fun lookupWord(rawWord: String, contextSnippet: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLookingUpWord = true,
                isDictionarySheetVisible = true,
                lookupWordContext = contextSnippet
            )

            val entry = dictionaryService.lookupWord(rawWord)
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
                    toastMessage = "No definition found for '$rawWord'"
                )
            }
        }
    }

    fun openDictionarySheet() {
        _uiState.value = _uiState.value.copy(isDictionarySheetVisible = true)
    }

    fun closeDictionarySheet() {
        _uiState.value = _uiState.value.copy(isDictionarySheetVisible = false, selectedDictionaryEntry = null)
    }

    fun saveCurrentWordToVocabulary() {
        val state = _uiState.value
        val entry = state.selectedDictionaryEntry ?: return
        val bookTitle = state.book?.title ?: ""

        viewModelScope.launch {
            vocabularyRepository.saveWordFromDictionary(
                entry = entry,
                bookTitle = bookTitle,
                contextSentence = state.lookupWordContext
            )
            _uiState.value = _uiState.value.copy(
                isWordSavedInVocabulary = true,
                toastMessage = "Saved '${entry.word}' to your Vocabulary List!"
            )
        }
    }

    private fun saveReadingProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val totalChapters = book.chapters.size
        if (totalChapters == 0) return

        val chapterWeight = 1f / totalChapters
        val pageWeightInChapter = if (state.totalPagesInChapter > 0) {
            (state.currentPageIndex.toFloat() / state.totalPagesInChapter) * chapterWeight
        } else 0f

        val overallProgress = (state.currentChapterIndex.toFloat() / totalChapters) + pageWeightInChapter
        val charOffset = state.pages.getOrNull(state.currentPageIndex)?.characterStartOffset ?: 0

        viewModelScope.launch {
            bookRepository.saveReadingProgress(
                bookId = book.id,
                chapterIndex = state.currentChapterIndex,
                pageIndex = state.currentPageIndex,
                charOffset = charOffset,
                progress = overallProgress.coerceIn(0f, 1f)
            )
        }
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
