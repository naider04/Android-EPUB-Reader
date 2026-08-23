package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookSearchResult
import com.example.data.model.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchSheet(
    query: String,
    results: List<BookSearchResult>,
    isSearching: Boolean,
    theme: ReaderTheme,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectResult: (BookSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)
    val surfaceColor = Color(theme.surfaceColorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surfaceColor,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search in Book",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }

            // Search Text Field
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    onSearch(it)
                },
                placeholder = { Text("Type word or phrase...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                onSearch("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.25f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Results count or status
            if (isSearching) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (query.isBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                ) {
                    Text(
                        text = "Search for characters, quotes, or themes across all chapters.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.55f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            } else if (results.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                ) {
                    Text(
                        text = "No matches found for \"$query\".",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            } else {
                Text(
                    text = "${results.size} matches found",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(results, key = { it.id }) { item ->
                        Surface(
                            color = textColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectResult(item)
                                    onDismiss()
                                }
                                .testTag("search_result_item_${item.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.chapterTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Highlight query occurrences in snippet
                                val annotatedSnippet = buildAnnotatedString {
                                    val lowerSnippet = item.snippet.lowercase()
                                    val lowerQuery = item.query.lowercase()
                                    var startIndex = 0
                                    while (startIndex < item.snippet.length) {
                                        val matchIndex = lowerSnippet.indexOf(lowerQuery, startIndex)
                                        if (matchIndex == -1 || lowerQuery.isEmpty()) {
                                            append(item.snippet.substring(startIndex))
                                            break
                                        }
                                        if (matchIndex > startIndex) {
                                            append(item.snippet.substring(startIndex, matchIndex))
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                background = accentColor.copy(alpha = 0.18f)
                                            )
                                        ) {
                                            append(item.snippet.substring(matchIndex, matchIndex + lowerQuery.length))
                                        }
                                        startIndex = matchIndex + lowerQuery.length
                                    }
                                }

                                Text(
                                    text = annotatedSnippet,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = textColor,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
