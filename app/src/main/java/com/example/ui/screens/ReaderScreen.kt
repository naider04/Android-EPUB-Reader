package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.viewmodel.ReaderViewModel
import com.example.util.PronunciationAccent
import com.example.util.rememberPronunciationPlayer
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pronunciationPlayer = rememberPronunciationPlayer()

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastMessage()
        }
    }

    val theme = uiState.settings.themeChoice
    val bgColor = Color(theme.backgroundColorHex)
    val textColor = Color(theme.textColorHex)

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { uiState.pages.size.coerceAtLeast(1) }
    )

    // Sync ViewModel page to PagerState (initial load, TOC jump, chapter transition)
    LaunchedEffect(uiState.currentPageIndex, uiState.pages.size) {
        if (uiState.pages.isNotEmpty() &&
            uiState.currentPageIndex in uiState.pages.indices &&
            pagerState.currentPage != uiState.currentPageIndex
        ) {
            pagerState.scrollToPage(uiState.currentPageIndex)
        }
    }

    // Sync swipe gesture from PagerState to ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (uiState.pages.isNotEmpty() &&
                settledPage in uiState.pages.indices &&
                settledPage != uiState.currentPageIndex
            ) {
                viewModel.onPageChanged(settledPage)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) {
            // Reserve space for top header and bottom footer indicators
            val topInset = if (uiState.settings.showHeaderFooter) 56.dp else 24.dp
            val bottomInset = if (uiState.settings.showHeaderFooter) 56.dp else 24.dp
            (maxHeight - topInset - bottomInset).roundToPx()
        }

        LaunchedEffect(widthPx, heightPx, density.density, uiState.settings) {
            viewModel.onViewportDimensionsChanged(widthPx, heightPx, density.density)
        }

        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(color = Color(theme.accentColorHex))
            }
        } else {
            val currentPage = uiState.pages.getOrNull(uiState.currentPageIndex)

            // Main Reader Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (uiState.activeSelection != null) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.toggleControls()
                                }
                            }
                        )
                    }
            ) {
                // Paginated Sheets Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (uiState.settings.showHeaderFooter) 48.dp else 16.dp,
                            bottom = if (uiState.settings.showHeaderFooter) 44.dp else 16.dp
                        )
                ) {
                    if (uiState.pages.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_horizontal_pager")
                        ) { pageIdx ->
                            val page = uiState.pages.getOrNull(pageIdx)
                            if (page != null) {
                                PageContent(
                                    page = page,
                                    settings = uiState.settings,
                                    activeSelection = uiState.activeSelection,
                                    onTextSelected = { selection ->
                                        viewModel.selectText(selection)
                                    },
                                    onTap = {
                                        if (uiState.activeSelection != null) {
                                            viewModel.clearSelection()
                                        } else {
                                            viewModel.toggleControls()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Minimalist Passive Sheet Header & Footer (when controls are hidden)
                if (uiState.settings.showHeaderFooter && !uiState.isControlsVisible) {
                    // Top Chapter indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPage?.chapterTitle ?: uiState.book?.title ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }

                    // Bottom Sheet number indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sheet ${uiState.currentPageIndex + 1} of ${uiState.totalPagesInChapter.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                        val totalChapters = uiState.book?.chapters?.size ?: 1
                        Text(
                            text = "Ch. ${uiState.currentChapterIndex + 1}/$totalChapters",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Interactive Overlays: Top Header & Bottom Bar
                ReaderHeader(
                    visible = uiState.isControlsVisible && uiState.activeSelection == null,
                    bookTitle = uiState.book?.title ?: "EPUB Reader",
                    chapterTitle = currentPage?.chapterTitle ?: "Chapter",
                    theme = theme,
                    isBookmarked = uiState.isCurrentPageBookmarked,
                    onBackClick = onBack,
                    onSearchClick = { viewModel.openSearchSheet() },
                    onBookmarkClick = { viewModel.toggleBookmarkCurrentPage() },
                    onSettingsClick = { viewModel.toggleSettingsSheet() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ReaderFooter(
                    visible = uiState.isControlsVisible && uiState.activeSelection == null,
                    currentPageIndex = uiState.currentPageIndex,
                    totalPagesInChapter = uiState.totalPagesInChapter,
                    currentChapterIndex = uiState.currentChapterIndex,
                    totalChapters = uiState.book?.chapters?.size ?: 1,
                    theme = theme,
                    onPreviousPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onPageSliderChange = { newPageIndex -> viewModel.onPageChanged(newPageIndex) },
                    onTocClick = { viewModel.toggleTocSheet() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Floating Text Selection Action Toolbar (Dictionary & Pronounce options)
                TextSelectionToolbar(
                    selection = uiState.activeSelection,
                    theme = theme,
                    onDictionaryClick = { term, sentence ->
                        viewModel.lookupWord(term, sentence)
                    },
                    onPronounceUkClick = { textToSpeak ->
                        pronunciationPlayer.speak(textToSpeak, PronunciationAccent.UK)
                    },
                    onPronounceUsClick = { textToSpeak ->
                        pronunciationPlayer.speak(textToSpeak, PronunciationAccent.US)
                    },
                    onDismiss = {
                        viewModel.clearSelection()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                )

                // Snackbar Host
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (uiState.activeSelection != null) 140.dp else 64.dp)
                )
            }

            // Bottom Sheets
            if (uiState.isSearchSheetVisible) {
                BookSearchSheet(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    theme = theme,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { viewModel.searchBook(it) },
                    onSelectResult = { viewModel.selectSearchResult(it) },
                    onDismiss = { viewModel.closeSearchSheet() }
                )
            }

            if (uiState.isSettingsSheetVisible) {
                ReaderSettingsSheet(
                    settings = uiState.settings,
                    onSettingsChanged = { viewModel.updateSettings(it) },
                    onDismiss = { viewModel.toggleSettingsSheet() }
                )
            }

            if (uiState.isTocSheetVisible) {
                TableOfContentsSheet(
                    chapters = uiState.book?.chapters ?: emptyList(),
                    currentChapterIndex = uiState.currentChapterIndex,
                    bookmarks = uiState.bookmarks,
                    theme = theme,
                    onSelectChapter = { chapterIndex -> viewModel.goToChapter(chapterIndex) },
                    onSelectBookmark = { bm -> viewModel.goToChapter(bm.chapterIndex, bm.pageIndex) },
                    onDeleteBookmark = { bmId ->
                        coroutineScope.launch {
                            // Handled in repository via viewModel if needed
                        }
                    },
                    onDismiss = { viewModel.toggleTocSheet() }
                )
            }

            if (uiState.isDictionarySheetVisible) {
                DictionaryBottomSheet(
                    entry = uiState.selectedDictionaryEntry,
                    isLoading = uiState.isLookingUpWord,
                    isSavedInVocabulary = uiState.isWordSavedInVocabulary,
                    theme = theme,
                    contextSnippet = uiState.lookupWordContext,
                    onSaveToVocabulary = { viewModel.saveCurrentWordToVocabulary() },
                    onSearchWord = { word -> viewModel.lookupWord(word) },
                    onDismiss = { viewModel.closeDictionarySheet() }
                )
            }
        }
    }
}
