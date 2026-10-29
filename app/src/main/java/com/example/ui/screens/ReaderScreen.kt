package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ReaderTheme
import com.example.ui.components.*
import com.example.ui.viewmodel.ReaderViewModel
import com.example.util.PronunciationAccent
import com.example.util.rememberPronunciationPlayer

@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    val theme: ReaderTheme = uiState.settings.themeChoice
    val bgColor = Color(theme.backgroundColorHex)
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // The foliate-js rendering engine lives in a WebView that fills the screen.
        ReaderWebView(
            engine = viewModel.engine,
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        }

        // Minimal always-visible top bar (back button + title).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(bgColor.copy(alpha = 0.85f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reader_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Library",
                    tint = textColor
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.bookTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiState.currentTocLabel.isNotBlank()) {
                    Text(
                        text = uiState.currentTocLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Bottom control bar (settings, bookmark, TOC, prev/next) when controls visible.
        AnimatedVisibility(
            visible = uiState.isControlsVisible && uiState.activeSelection == null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color(theme.surfaceColorHex).copy(alpha = 0.97f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { viewModel.prevPage() },
                        modifier = Modifier.testTag("footer_prev_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleBookmark() },
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (uiState.isCurrentBookmarked) accentColor else textColor
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleTocSheet() },
                        modifier = Modifier.testTag("footer_chapters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Contents & Quotes",
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleSettingsSheet() },
                        modifier = Modifier.testTag("reader_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font & Display Settings",
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextPage() },
                        modifier = Modifier.testTag("footer_next_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = textColor
                        )
                    }
                }
            }
        }

        // Progress indicator (fraction) shown at the bottom.
        if (!uiState.isControlsVisible && uiState.fraction > 0f) {
            Text(
                text = "${(uiState.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.45f),
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }

        // Text selection action toolbar (Google search, dictionary, pronunciation, save quote).
        TextSelectionToolbar(
            selection = uiState.activeSelection,
            theme = theme,
            onDictionaryClick = { term, sentence ->
                viewModel.lookupWord(term, sentence)
                pronunciationPlayer.speak(term, PronunciationAccent.US)
            },
            onPronounceUkClick = { text -> pronunciationPlayer.speak(text, PronunciationAccent.UK) },
            onPronounceUsClick = { text -> pronunciationPlayer.speak(text, PronunciationAccent.US) },
            onSaveQuoteClick = { viewModel.saveQuote() },
            onDismiss = { viewModel.clearSelection() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (uiState.activeSelection != null) 140.dp else 64.dp)
        )

        if (uiState.isSettingsSheetVisible) {
            ReaderSettingsSheet(
                settings = uiState.settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onDismiss = { viewModel.toggleSettingsSheet() }
            )
        }

        if (uiState.isTocSheetVisible) {
            TableOfContentsSheet(
                tocEntries = uiState.tocEntries,
                currentTocLabel = uiState.currentTocLabel,
                bookmarks = uiState.bookmarks,
                theme = theme,
                onSelectEntry = { href -> viewModel.goToHref(href) },
                onSelectBookmark = { bm -> viewModel.selectBookmark(bm) },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                onDismiss = { viewModel.toggleTocSheet() }
            )
        }

        if (uiState.isDictionarySheetVisible) {
            DictionaryBottomSheet(
                entry = uiState.selectedDictionaryEntry,
                selectedWord = uiState.selectedLookupWord,
                isLoading = uiState.isLookingUpWord,
                isSavedInVocabulary = uiState.isWordSavedInVocabulary,
                theme = theme,
                bookId = uiState.bookId.ifBlank { null },
                contextSnippet = uiState.lookupWordContext,
                onSaveToVocabulary = { viewModel.saveCurrentWordToVocabulary() },
                onSearchWord = { word -> viewModel.lookupWord(word) },
                onDismiss = { viewModel.closeDictionarySheet() }
            )
        }
    }
}
