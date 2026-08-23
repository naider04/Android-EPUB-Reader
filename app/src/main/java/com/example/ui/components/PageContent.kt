package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookPage
import com.example.data.model.PageBlock
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings

@Composable
fun PageContent(
    page: BookPage,
    settings: ReaderSettings,
    onWordTapped: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = settings.themeChoice
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)

    val fontFamily = when (settings.fontChoice) {
        ReaderFontFamily.SERIF -> FontFamily.Serif
        ReaderFontFamily.SANS -> FontFamily.SansSerif
        ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
        ReaderFontFamily.CURSIVE -> FontFamily.Cursive
    }

    val bodyFontSize = settings.fontSizeSp.sp
    val bodyLineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp
    val titleFontSize = (settings.fontSizeSp * 1.35f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = settings.marginChoice.horizontalPaddingDp.dp,
                vertical = 12.dp
            )
    ) {
        page.blocks.forEach { block ->
            when (block) {
                is PageBlock.Title -> {
                    Text(
                        text = block.text,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                            lineHeight = (settings.fontSizeSp * 1.6f).sp,
                            color = accentColor,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                is PageBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(IntrinsicSize.Min)
                                .background(accentColor, shape = RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        InteractiveWordText(
                            text = block.text,
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = (settings.fontSizeSp * 0.95f).sp,
                                lineHeight = bodyLineHeight,
                                color = textColor.copy(alpha = 0.9f)
                            ),
                            onWordClick = { word -> onWordTapped(word, block.text) }
                        )
                    }
                }

                is PageBlock.Divider -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "❖  ❖  ❖",
                            style = TextStyle(
                                color = accentColor.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                letterSpacing = 4.sp
                            )
                        )
                    }
                }

                is PageBlock.ImagePlaceholder -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(textColor.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "[Illustration: ${block.alt}]",
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                is PageBlock.Paragraph -> {
                    InteractiveWordText(
                        text = block.text,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight,
                            color = textColor
                        ),
                        onWordClick = { word -> onWordTapped(word, block.text) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveWordText(
    text: String,
    style: TextStyle,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text) { buildAnnotatedString { append(text) } }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(text) {
            detectTapGestures { pos ->
                val layout = layoutResult ?: return@detectTapGestures
                val offset = layout.getOffsetForPosition(pos)
                if (offset in 0 until text.length) {
                    val word = extractWordAt(text, offset)
                    if (word.isNotBlank()) {
                        onWordClick(word)
                    }
                }
            }
        }
    )
}

private fun extractWordAt(text: String, offset: Int): String {
    if (offset !in 0 until text.length) return ""

    var start = offset
    var end = offset

    while (start > 0 && text[start - 1].isLetterOrDigit()) {
        start--
    }
    while (end < text.length && text[end].isLetterOrDigit()) {
        end++
    }

    return text.substring(start, end).trim()
}
