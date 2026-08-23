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
        initialPage = uiState.currentPageIndex,
        pageCount = { uiState.pages.size.coerceAtLeast(1) }
    )

    // Sync pager with ViewModel state
    LaunchedEffect(uiState.currentPageIndex) {
        if (uiState.currentPageIndex in 0 until pagerState.pageCount &&
            pagerState.currentPage != uiState.currentPageIndex
        ) {
            pagerState.scrollToPage(uiState.currentPageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPageIndex &&
            pagerState.currentPage in uiState.pages.indices
        ) {
            viewModel.onPageChanged(pagerState.currentPage)
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
                            onTap = { offset ->
                                val screenWidth = size.width
                                val tapX = offset.x
                                when {
                                    tapX < screenWidth * 0.22f -> {
                                        // Left tap zone: previous sheet
                                        if (!viewModel.prevPage() && pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    }
                                    tapX > screenWidth * 0.78f -> {
                                        // Right tap zone: next sheet
                                        if (!viewModel.nextPage() && pagerState.currentPage < pagerState.pageCount - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    }
                                    else -> {
                                        // Center zone: toggle controls
                                        viewModel.toggleControls()
                                    }
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
                                    onWordTapped = { word, snippet ->
                                        viewModel.lookupWord(word, snippet)
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
                    visible = uiState.isControlsVisible,
                    bookTitle = uiState.book?.title ?: "EPUB Reader",
                    chapterTitle = currentPage?.chapterTitle ?: "Chapter",
                    theme = theme,
                    isBookmarked = uiState.isCurrentPageBookmarked,
                    onBackClick = onBack,
                    onTocClick = { viewModel.toggleTocSheet() },
                    onBookmarkClick = { viewModel.toggleBookmarkCurrentPage() },
                    onSettingsClick = { viewModel.toggleSettingsSheet() },
                    onDictionaryClick = { viewModel.lookupWord("read", "") },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                ReaderFooter(
                    visible = uiState.isControlsVisible,
                    currentPageIndex = uiState.currentPageIndex,
                    totalPagesInChapter = uiState.totalPagesInChapter,
                    currentChapterIndex = uiState.currentChapterIndex,
                    totalChapters = uiState.book?.chapters?.size ?: 1,
                    theme = theme,
                    onPreviousPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onPageSliderChange = { newPageIndex -> viewModel.onPageChanged(newPageIndex) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Snackbar Host
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                )
            }

            // Bottom Sheets
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
