package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BlockAlignment
import com.example.data.model.BookPage
import com.example.data.model.InlineSpan
import com.example.data.model.PageBlock
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.TextSelection
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PageContent(
    page: BookPage,
    settings: ReaderSettings,
    activeSelection: TextSelection? = null,
    onTextSelected: (TextSelection) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onTap: () -> Unit = {},
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

    val bodyAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
            .padding(
                horizontal = settings.marginChoice.horizontalPaddingDp.dp,
                vertical = 12.dp
            )
    ) {
        page.blocks.forEachIndexed { blockIndex, block ->
            val selectionRange = if (activeSelection?.blockIndex == blockIndex) {
                Pair(activeSelection.startOffset, activeSelection.endOffset)
            } else null

            when (block) {
                is PageBlock.Title -> {
                    InteractiveWordText(
                        spans = listOf(InlineSpan(block.text)),
                        blockIndex = blockIndex,
                        selectionRange = selectionRange,
                        selectionHighlightColor = accentColor.copy(alpha = 0.35f),
                        accentColor = accentColor,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                            lineHeight = (settings.fontSizeSp * 1.6f).sp,
                            color = accentColor,
                            textAlign = TextAlign.Start
                        ),
                        onTextSelected = onTextSelected,
                        onLinkClick = onLinkClick,
                        onTap = onTap,
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
                            spans = block.spans,
                            blockIndex = blockIndex,
                            selectionRange = selectionRange,
                            selectionHighlightColor = accentColor.copy(alpha = 0.35f),
                            accentColor = accentColor,
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = (settings.fontSizeSp * 0.95f).sp,
                                lineHeight = bodyLineHeight,
                                textAlign = TextAlign.Start,
                                color = textColor.copy(alpha = 0.9f)
                            ),
                            onTextSelected = onTextSelected,
                            onLinkClick = onLinkClick,
                            onTap = onTap
                        )
                    }
                }

                is PageBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = block.marker,
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(20.dp)
                        )
                        InteractiveWordText(
                            spans = block.spans,
                            blockIndex = blockIndex,
                            selectionRange = selectionRange,
                            selectionHighlightColor = accentColor.copy(alpha = 0.35f),
                            accentColor = accentColor,
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight,
                                textAlign = bodyAlign,
                                color = textColor
                            ),
                            onTextSelected = onTextSelected,
                            onLinkClick = onLinkClick,
                            onTap = onTap
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

                is PageBlock.Image -> {
                    val imagePath = block.path
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (imagePath != null && File(imagePath).exists()) {
                            AsyncImage(
                                model = File(imagePath),
                                contentDescription = block.alt,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(bottom = 4.dp)
                                    .testTag("reader_image_${blockIndex}")
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
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
                    }
                }

                is PageBlock.Paragraph -> {
                    val align = when (block.alignment) {
                        BlockAlignment.CENTER -> TextAlign.Center
                        BlockAlignment.RIGHT -> TextAlign.Right
                        BlockAlignment.LEFT -> TextAlign.Start
                        BlockAlignment.JUSTIFY -> if (settings.justifyText) TextAlign.Justify else TextAlign.Start
                    }
                    InteractiveWordText(
                        spans = block.spans,
                        blockIndex = blockIndex,
                        selectionRange = selectionRange,
                        selectionHighlightColor = accentColor.copy(alpha = 0.35f),
                        accentColor = accentColor,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight,
                            textAlign = align,
                            color = textColor
                        ),
                        onTextSelected = onTextSelected,
                        onLinkClick = onLinkClick,
                        onTap = onTap,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveWordText(
    spans: List<InlineSpan>,
    blockIndex: Int,
    selectionRange: Pair<Int, Int>?,
    selectionHighlightColor: Color,
    accentColor: Color,
    style: TextStyle,
    onTextSelected: (TextSelection) -> Unit,
    onLinkClick: (String) -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val text = remember(spans) { spans.joinToString("") { it.text } }

    val annotatedString = remember(text, spans, selectionRange, selectionHighlightColor, accentColor, style) {
        buildAnnotatedString {
            for (span in spans) {
                withStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.Bold else style.fontWeight ?: FontWeight.Normal,
                        fontStyle = if (span.italic) FontStyle.Italic else style.fontStyle ?: FontStyle.Normal,
                        textDecoration = if (span.underline || span.linkTarget != null) TextDecoration.Underline else TextDecoration.None,
                        color = if (span.linkTarget != null) accentColor else style.color
                    )
                ) {
                    append(span.text)
                }
            }
            if (selectionRange != null && selectionRange.first < selectionRange.second && length > 0) {
                val clampedStart = selectionRange.first.coerceIn(0, length)
                val clampedEnd = selectionRange.second.coerceIn(0, length)
                if (clampedStart < clampedEnd) {
                    addStyle(
                        SpanStyle(background = selectionHighlightColor),
                        start = clampedStart,
                        end = clampedEnd
                    )
                }
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    val isJustified = style.textAlign == TextAlign.Justify

    Box(modifier = modifier) {
        Text(
            text = annotatedString,
            style = style,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(text, blockIndex, isJustified) {
                    detectTapGestures(
                        onTap = { pos ->
                            val layout = layoutResult
                            if (layout != null) {
                                val offset = getJustifiedOffsetForPosition(layout, text, pos, isJustified)
                                val link = findLinkAt(offset, spans)
                                if (link != null) {
                                    onLinkClick(link)
                                } else {
                                    onTap()
                                }
                            } else {
                                onTap()
                            }
                        },
                        onLongPress = { pos ->
                            val layout = layoutResult ?: return@detectTapGestures
                            val offset = getJustifiedOffsetForPosition(layout, text, pos, isJustified)
                            if (offset in 0 until text.length) {
                                val wordRange = extractWordRange(text, offset)
                                if (wordRange != null) {
                                    val (wordStart, wordEnd) = wordRange
                                    val wordText = text.substring(wordStart, wordEnd)
                                    val sentenceRange = extractSentenceRange(text, offset)
                                    val (sentStart, sentEnd) = sentenceRange
                                    val sentenceText = text.substring(sentStart, sentEnd).trim()

                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                    onTextSelected(
                                        TextSelection(
                                            blockIndex = blockIndex,
                                            selectedText = wordText,
                                            startOffset = wordStart,
                                            endOffset = wordEnd,
                                            sentenceContext = sentenceText
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
        )

        val layout = layoutResult
        if (selectionRange != null && layout != null &&
            selectionRange.first in 0..text.length &&
            selectionRange.second in 0..text.length &&
            selectionRange.first < selectionRange.second
        ) {
            val startRect = getSelectionStartRect(layout, text, selectionRange.first, isJustified)
            val endRect = getSelectionEndRect(layout, text, selectionRange.second, isJustified)

            var startDragAnchor by remember { mutableStateOf(Offset.Zero) }
            var endDragAnchor by remember { mutableStateOf(Offset.Zero) }

            DraggableSelectionHandle(
                cursorRect = startRect,
                isStartHandle = true,
                accentColor = accentColor,
                onDragStart = {
                    val curLayout = layoutResult
                    if (curLayout != null) {
                        val rect = getSelectionStartRect(curLayout, text, selectionRange.first, isJustified)
                        startDragAnchor = Offset(rect.left, (rect.top + rect.bottom) / 2f)
                    }
                },
                onDrag = { totalDragDelta ->
                    val curLayout = layoutResult ?: return@DraggableSelectionHandle
                    val targetPos = startDragAnchor + totalDragDelta
                    val newOffset = getJustifiedOffsetForPosition(curLayout, text, targetPos, isJustified)
                    val endBound = selectionRange.second
                    val clampedStart = newOffset.coerceIn(0, (endBound - 1).coerceAtLeast(0))

                    if (clampedStart != selectionRange.first) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newSelectedText = text.substring(clampedStart, endBound)
                        val sentRange = extractSentenceRange(text, clampedStart)
                        val sentText = text.substring(sentRange.first, sentRange.second).trim()
                        onTextSelected(
                            TextSelection(
                                blockIndex = blockIndex,
                                selectedText = newSelectedText,
                                startOffset = clampedStart,
                                endOffset = endBound,
                                sentenceContext = sentText
                            )
                        )
                    }
                }
            )

            DraggableSelectionHandle(
                cursorRect = endRect,
                isStartHandle = false,
                accentColor = accentColor,
                onDragStart = {
                    val curLayout = layoutResult
                    if (curLayout != null) {
                        val rect = getSelectionEndRect(curLayout, text, selectionRange.second, isJustified)
                        endDragAnchor = Offset(rect.left, (rect.top + rect.bottom) / 2f)
                    }
                },
                onDrag = { totalDragDelta ->
                    val curLayout = layoutResult ?: return@DraggableSelectionHandle
                    val targetPos = endDragAnchor + totalDragDelta
                    val newOffset = getJustifiedOffsetForPosition(curLayout, text, targetPos, isJustified)
                    val startBound = selectionRange.first
                    val clampedEnd = newOffset.coerceIn(startBound + 1, text.length)

                    if (clampedEnd != selectionRange.second) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newSelectedText = text.substring(startBound, clampedEnd)
                        val sentRange = extractSentenceRange(text, startBound)
                        val sentText = text.substring(sentRange.first, sentRange.second).trim()
                        onTextSelected(
                            TextSelection(
                                blockIndex = blockIndex,
                                selectedText = newSelectedText,
                                startOffset = startBound,
                                endOffset = clampedEnd,
                                sentenceContext = sentText
                            )
                        )
                    }
                }
            )
        }
    }
}

/**
 * Computes the horizontal visual position of a character offset, correctly accounting for
 * inter-word spacing expansion when text is justified (TextAlign.Justify).
 */
fun getJustifiedHorizontal(
    layout: TextLayoutResult,
    text: String,
    offset: Int,
    isJustified: Boolean
): Float {
    if (text.isEmpty()) return 0f
    val clampedOffset = offset.coerceIn(0, text.length)
    val charIndex = clampedOffset.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val line = layout.getLineForOffset(charIndex)
    val basePos = layout.getHorizontalPosition(clampedOffset, usePrimaryDirection = true)

    if (!isJustified) return basePos

    val lineStart = layout.getLineStart(line)
    val lineVisibleEnd = layout.getLineEnd(line, visibleEnd = true)
    val isLastLine = line == layout.lineCount - 1 ||
        (lineVisibleEnd in 1..text.length && text[lineVisibleEnd - 1] == '\n') ||
        (layout.getLineEnd(line) in 1..text.length && text[layout.getLineEnd(line) - 1] == '\n')

    if (isLastLine || lineVisibleEnd <= lineStart) {
        return basePos
    }

    val xLastChar = layout.getHorizontalPosition(lineVisibleEnd, usePrimaryDirection = true)
    val lineRight = layout.getLineRight(line)
    val extraTotal = lineRight - xLastChar
    if (extraTotal <= 0f) {
        return basePos
    }

    var totalSpaces = 0
    var spacesBeforeOffset = 0
    for (i in lineStart until lineVisibleEnd) {
        if (text[i] == ' ') {
            totalSpaces++
            if (i < clampedOffset) {
                spacesBeforeOffset++
            }
        }
    }

    if (totalSpaces == 0) return basePos

    val extraPerSpace = extraTotal / totalSpaces
    return basePos + (spacesBeforeOffset * extraPerSpace)
}

/**
 * Maps a screen pixel position to a character offset, accurately accounting for inter-word
 * spacing expansion when text is justified (TextAlign.Justify).
 */
fun getJustifiedOffsetForPosition(
    layout: TextLayoutResult,
    text: String,
    pos: Offset,
    isJustified: Boolean
): Int {
    if (!isJustified || text.isEmpty()) {
        return layout.getOffsetForPosition(pos)
    }

    val boundedY = pos.y.coerceIn(0f, (layout.size.height - 1).toFloat().coerceAtLeast(0f))
    val line = layout.getLineForVerticalPosition(boundedY)
    val lineStart = layout.getLineStart(line)
    val lineVisibleEnd = layout.getLineEnd(line, visibleEnd = true)
    val isLastLine = line == layout.lineCount - 1 ||
        (lineVisibleEnd in 1..text.length && text[lineVisibleEnd - 1] == '\n') ||
        (layout.getLineEnd(line) in 1..text.length && text[layout.getLineEnd(line) - 1] == '\n')

    if (isLastLine || lineVisibleEnd <= lineStart) {
        return layout.getOffsetForPosition(pos)
    }

    val xLastChar = layout.getHorizontalPosition(lineVisibleEnd, usePrimaryDirection = true)
    val lineRight = layout.getLineRight(line)
    val extraTotal = lineRight - xLastChar
    if (extraTotal <= 0f) {
        return layout.getOffsetForPosition(pos)
    }

    var totalSpaces = 0
    for (i in lineStart until lineVisibleEnd) {
        if (text[i] == ' ') totalSpaces++
    }
    if (totalSpaces == 0) {
        return layout.getOffsetForPosition(pos)
    }

    val extraPerSpace = extraTotal / totalSpaces

    val firstX = layout.getHorizontalPosition(lineStart, usePrimaryDirection = true)
    if (pos.x <= firstX) {
        return lineStart
    }
    if (pos.x >= lineRight) {
        return lineVisibleEnd
    }

    var currentSpaces = 0
    var curLeft = firstX
    for (i in lineStart until lineVisibleEnd) {
        val nextSpaces = if (text[i] == ' ') currentSpaces + 1 else currentSpaces
        val nextX = layout.getHorizontalPosition(i + 1, usePrimaryDirection = true) + (nextSpaces * extraPerSpace)
        val midPoint = (curLeft + nextX) / 2f
        if (pos.x < midPoint) {
            return i
        }
        currentSpaces = nextSpaces
        curLeft = nextX
    }

    return lineVisibleEnd
}

/**
 * Calculates the exact pixel bounding rect for the start selection handle.
 * Bounded to the left edge of the first selected character using layout horizontal positions.
 */
fun getSelectionStartRect(
    layout: TextLayoutResult,
    text: String,
    startOffset: Int,
    isJustified: Boolean = false
): Rect {
    if (text.isEmpty()) return Rect.Zero
    val clampedStart = startOffset.coerceIn(0, text.length)
    val charIndex = clampedStart.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val line = layout.getLineForOffset(charIndex)
    val top = layout.getLineTop(line)
    val bottom = layout.getLineBottom(line)

    val lineStart = layout.getLineStart(line)
    val x = if (clampedStart <= lineStart) {
        layout.getLineLeft(line)
    } else {
        getJustifiedHorizontal(layout, text, clampedStart, isJustified)
    }
    return Rect(left = x, top = top, right = x, bottom = bottom)
}

/**
 * Calculates the exact pixel bounding rect for the end selection handle.
 * Bounded to the right edge of the last selected character using layout horizontal positions.
 */
fun getSelectionEndRect(
    layout: TextLayoutResult,
    text: String,
    endOffset: Int,
    isJustified: Boolean = false
): Rect {
    if (text.isEmpty()) return Rect.Zero
    val clampedEnd = endOffset.coerceIn(0, text.length)
    val charIndex = (clampedEnd - 1).coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val line = layout.getLineForOffset(charIndex)
    val top = layout.getLineTop(line)
    val bottom = layout.getLineBottom(line)

    val lineVisibleEnd = layout.getLineEnd(line, visibleEnd = true)
    val x = if (clampedEnd >= lineVisibleEnd) {
        layout.getLineRight(line)
    } else {
        getJustifiedHorizontal(layout, text, clampedEnd, isJustified)
    }
    return Rect(left = x, top = top, right = x, bottom = bottom)
}

private fun findLinkAt(offset: Int, spans: List<InlineSpan>): String? {
    var pos = 0
    for (span in spans) {
        val end = pos + span.text.length
        if (span.linkTarget != null && offset in pos until end) {
            return span.linkTarget
        }
        pos = end
    }
    return null
}

@Composable
fun DraggableSelectionHandle(
    cursorRect: Rect,
    isStartHandle: Boolean,
    accentColor: Color,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (dragDelta: Offset) -> Unit
) {
    val density = LocalDensity.current
    val touchTargetSizePx = with(density) { 48.dp.toPx() }
    val handleRadiusPx = with(density) { 9.dp.toPx() }
    val cursorLineWidthPx = with(density) { 2.5.dp.toPx() }

    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentCursorRect by rememberUpdatedState(cursorRect)

    var accumulatedDrag by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset {
                val rect = currentCursorRect
                val left = rect.left - (touchTargetSizePx / 2f)
                val top = rect.top
                IntOffset(left.roundToInt(), top.roundToInt())
            }
            .size(
                width = 48.dp,
                height = with(density) { (cursorRect.height + handleRadiusPx * 2.5f + 8f).toDp() }
            )
            .testTag(if (isStartHandle) "selection_start_handle" else "selection_end_handle")
            .pointerInput(isStartHandle) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDrag = Offset.Zero
                        currentOnDragStart()
                    },
                    onDragEnd = {
                        accumulatedDrag = Offset.Zero
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        accumulatedDrag = Offset.Zero
                        currentOnDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        currentOnDrag(accumulatedDrag)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val lineHeight = currentCursorRect.height

            // 1. Vertical cursor stick exactly aligned to the text boundary (centerX -> cursorRect.left)
            drawLine(
                color = accentColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, lineHeight),
                strokeWidth = cursorLineWidthPx,
                cap = StrokeCap.Round
            )

            // 2. Selection handle pin knob at the base of the stick
            val knobCenterY = lineHeight + handleRadiusPx
            val knobCenterX = if (isStartHandle) {
                centerX - (handleRadiusPx * 0.45f)
            } else {
                centerX + (handleRadiusPx * 0.45f)
            }

            // Drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = handleRadiusPx + 1.5f,
                center = Offset(knobCenterX, knobCenterY + 1f)
            )

            // Pin / teardrop neck & body
            val path = Path().apply {
                moveTo(centerX, lineHeight)
                if (isStartHandle) {
                    lineTo(knobCenterX, knobCenterY - handleRadiusPx)
                    arcTo(
                        rect = Rect(
                            left = knobCenterX - handleRadiusPx,
                            top = knobCenterY - handleRadiusPx,
                            right = knobCenterX + handleRadiusPx,
                            bottom = knobCenterY + handleRadiusPx
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = -270f,
                        forceMoveTo = false
                    )
                    close()
                } else {
                    lineTo(knobCenterX, knobCenterY - handleRadiusPx)
                    arcTo(
                        rect = Rect(
                            left = knobCenterX - handleRadiusPx,
                            top = knobCenterY - handleRadiusPx,
                            right = knobCenterX + handleRadiusPx,
                            bottom = knobCenterY + handleRadiusPx
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 270f,
                        forceMoveTo = false
                    )
                    close()
                }
            }

            drawPath(
                path = path,
                color = accentColor,
                style = Fill
            )

            // Inner white grip dot
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = handleRadiusPx * 0.35f,
                center = Offset(knobCenterX, knobCenterY)
            )
        }
    }
}

fun extractWordRange(text: String, offset: Int): Pair<Int, Int>? {
    if (text.isEmpty() || offset !in 0..text.length) return null
    val clampedOffset = offset.coerceIn(0, (text.length - 1).coerceAtLeast(0))

    var start = clampedOffset
    if (!text[start].isLetterOrDigit() && start > 0 && text[start - 1].isLetterOrDigit()) {
        start--
    }

    if (!text[start].isLetterOrDigit()) {
        var f = start
        while (f < text.length && !text[f].isLetterOrDigit()) f++
        if (f < text.length) start = f else {
            var b = start
            while (b >= 0 && !text[b].isLetterOrDigit()) b--
            if (b >= 0) start = b else return null
        }
    }

    var wordStart = start
    var wordEnd = start

    while (wordStart > 0 && (text[wordStart - 1].isLetterOrDigit() || text[wordStart - 1] == '\'' || text[wordStart - 1] == '-')) {
        wordStart--
    }
    while (wordEnd < text.length && (text[wordEnd].isLetterOrDigit() || text[wordEnd] == '\'' || text[wordEnd] == '-')) {
        wordEnd++
    }

    while (wordStart < wordEnd && (text[wordStart] == '\'' || text[wordStart] == '-')) {
        wordStart++
    }
    while (wordEnd > wordStart && (text[wordEnd - 1] == '\'' || text[wordEnd - 1] == '-')) {
        wordEnd--
    }

    if (wordStart >= wordEnd) return null
    return Pair(wordStart, wordEnd)
}

fun extractSentenceRange(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return Pair(0, 0)
    val clampedOffset = offset.coerceIn(0, text.length - 1)

    var start = clampedOffset
    while (start > 0) {
        val prev = text[start - 1]
        if (prev == '.' || prev == '!' || prev == '?' || prev == '\n') {
            break
        }
        start--
    }
    while (start < text.length && (text[start].isWhitespace() || text[start] == '"' || text[start] == '“' || text[start] == '‘')) {
        start++
    }

    var end = clampedOffset
    while (end < text.length) {
        val ch = text[end]
        if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') {
            end++
            while (end < text.length && (text[end] == '"' || text[end] == '”' || text[end] == '’' || text[end] == ')')) {
                end++
            }
            break
        }
        end++
    }

    return Pair(start.coerceAtMost(text.length), end.coerceAtMost(text.length))
}
