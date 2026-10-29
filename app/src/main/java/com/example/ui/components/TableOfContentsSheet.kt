package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.ReaderTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsSheet(
    tocEntries: List<ReaderEngine.TocEntry>,
    currentTocLabel: String,
    bookmarks: List<BookmarkEntity>,
    theme: ReaderTheme,
    onSelectEntry: (String) -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)
    val surfaceColor = Color(theme.surfaceColorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surfaceColor,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = surfaceColor,
                contentColor = accentColor,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Chapters (${tocEntries.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) accentColor else textColor.copy(alpha = 0.7f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Bookmarks & Quotes (${bookmarks.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) accentColor else textColor.copy(alpha = 0.7f)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                if (tocEntries.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "No table of contents.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.6f))
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(tocEntries) { index, entry ->
                            val isCurrent = entry.label == currentTocLabel
                            Surface(
                                color = if (isCurrent) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectEntry(entry.href)
                                        onDismiss()
                                    }
                                    .testTag("toc_chapter_item_$index")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = entry.label,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) accentColor else textColor,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (bookmarks.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "No bookmarks or quotes yet.\nSelect text and tap 'Save Quote' to keep a highlight!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                } else {
                    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(bookmarks) { _, bm ->
                            val isQuote = bm.note == "Quote"
                            Surface(
                                color = accentColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectBookmark(bm)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isQuote) Icons.Default.FormatQuote else Icons.Default.Bookmark,
                                        contentDescription = if (isQuote) "Quote" else "Bookmark",
                                        tint = accentColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isQuote) "QUOTE" else "BOOKMARK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        if (bm.chapterTitle.isNotBlank()) {
                                            Text(
                                                text = bm.chapterTitle,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = dateFormat.format(Date(bm.timestamp)),
                                            fontSize = 11.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                        if (bm.excerpt.isNotBlank()) {
                                            Text(
                                                text = "“${bm.excerpt}”",
                                                fontSize = 12.sp,
                                                color = textColor.copy(alpha = 0.85f),
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteBookmark(bm.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = textColor.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
