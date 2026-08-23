package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReaderTheme

@Composable
fun ReaderFooter(
    visible: Boolean,
    currentPageIndex: Int,
    totalPagesInChapter: Int,
    currentChapterIndex: Int,
    totalChapters: Int,
    theme: ReaderTheme,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageSliderChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = Color(theme.surfaceColorHex).copy(alpha = 0.95f)
    val contentColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)

    val pageNumber = currentPageIndex + 1
    val totalPages = totalPagesInChapter.coerceAtLeast(1)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = barBg,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Page slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = currentPageIndex > 0 || currentChapterIndex > 0,
                        modifier = Modifier.testTag("footer_prev_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0 || currentChapterIndex > 0) contentColor else contentColor.copy(alpha = 0.3f)
                        )
                    }

                    Slider(
                        value = pageNumber.toFloat(),
                        onValueChange = { onPageSliderChange((it.toInt() - 1).coerceIn(0, totalPages - 1)) },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = contentColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("page_scrubber_slider")
                    )

                    IconButton(
                        onClick = onNextPage,
                        enabled = currentPageIndex < totalPages - 1 || currentChapterIndex < totalChapters - 1,
                        modifier = Modifier.testTag("footer_next_page")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < totalPages - 1 || currentChapterIndex < totalChapters - 1) contentColor else contentColor.copy(alpha = 0.3f)
                        )
                    }
                }

                // Info label
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Chapter ${currentChapterIndex + 1} of $totalChapters",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "Sheet $pageNumber of $totalPages",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = contentColor,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
