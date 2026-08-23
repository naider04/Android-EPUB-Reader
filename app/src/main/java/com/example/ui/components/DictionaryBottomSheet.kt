package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DictionaryEntry
import com.example.data.model.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryBottomSheet(
    entry: DictionaryEntry?,
    isLoading: Boolean,
    isSavedInVocabulary: Boolean,
    theme: ReaderTheme,
    contextSnippet: String = "",
    onSaveToVocabulary: () -> Unit,
    onSearchWord: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)
    val surfaceColor = Color(theme.surfaceColorHex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = surfaceColor,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Look up any word in dictionary...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor
                    )
                },
                trailingIcon = {
                    if (searchInput.isNotBlank()) {
                        Button(
                            onClick = { onSearchWord(searchInput.trim()) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Define", fontSize = 12.sp, color = Color.White)
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
                    .testTag("dict_search_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (entry != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Word & Phonetics Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.word,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            if (!entry.phonetic.isNullOrBlank()) {
                                Text(
                                    text = entry.phonetic,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        color = accentColor,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }

                        // Save to Vocabulary button
                        Button(
                            onClick = onSaveToVocabulary,
                            enabled = !isSavedInVocabulary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSavedInVocabulary) accentColor.copy(alpha = 0.2f) else accentColor,
                                contentColor = if (isSavedInVocabulary) accentColor else Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("dict_save_vocab_button")
                        ) {
                            Icon(
                                imageVector = if (isSavedInVocabulary) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = if (isSavedInVocabulary) "Saved" else "Save Word",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Meanings
                    entry.meanings.forEachIndexed { mIdx, meaning ->
                        Surface(
                            color = textColor.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Part of Speech Badge
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = meaning.partOfSpeech.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                // Definitions
                                meaning.definitions.forEachIndexed { dIdx, def ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                        Text(
                                            text = "${dIdx + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            fontSize = 13.sp,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Text(
                                            text = def,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textColor,
                                                lineHeight = 20.sp,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }

                                // Examples
                                if (meaning.examples.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    meaning.examples.forEach { example ->
                                        Text(
                                            text = "• \"$example\"",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = FontStyle.Italic,
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Origin / Etymology
                    if (!entry.origin.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Origin: ${entry.origin}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Source label
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Source: ${entry.source}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = "Press and hold any word in the book, or type above, to look up its definition.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
