package com.example.data.epub

import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.BookPage
import com.example.data.model.EpubChapter
import com.example.data.model.PageBlock
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import kotlin.math.max

object EpubPaginator {

    fun paginateChapter(
        chapter: EpubChapter,
        chapterIndex: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        density: Float,
        settings: ReaderSettings
    ): List<BookPage> {
        if (viewportWidthPx <= 100 || viewportHeightPx <= 100) {
            // Viewport not measured yet, provide single fallback page
            return listOf(
                BookPage(
                    pageIndex = 0,
                    totalPagesInChapter = 1,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapter.title,
                    blocks = chapter.blocks
                )
            )
        }

        val horizontalPaddingPx = (settings.marginChoice.horizontalPaddingDp * density).toInt() * 2
        val textWidthPx = max(100, viewportWidthPx - horizontalPaddingPx)
        val availablePageHeightPx = viewportHeightPx

        val typeface = when (settings.fontChoice) {
            ReaderFontFamily.SERIF -> Typeface.SERIF
            ReaderFontFamily.SANS -> Typeface.SANS_SERIF
            ReaderFontFamily.MONOSPACE -> Typeface.MONOSPACE
            ReaderFontFamily.CURSIVE -> Typeface.create("casual", Typeface.NORMAL)
        }

        val bodyPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = settings.fontSizeSp * density
            this.typeface = typeface
        }

        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = (settings.fontSizeSp * 1.35f) * density
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }

        val quotePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = (settings.fontSizeSp * 0.95f) * density
            this.typeface = Typeface.create(typeface, Typeface.ITALIC)
        }

        val paragraphSpacingPx = (14 * density).toInt()
        val titleSpacingPx = (20 * density).toInt()
        val dividerHeightPx = (28 * density).toInt()

        val pages = mutableListOf<MutableList<PageBlock>>()
        var currentPageBlocks = mutableListOf<PageBlock>()
        var remainingHeightPx = availablePageHeightPx

        val blockQueue = ArrayDeque<PageBlock>(chapter.blocks)

        while (blockQueue.isNotEmpty()) {
            val block = blockQueue.removeFirst()

            when (block) {
                is PageBlock.Title -> {
                    val layout = buildStaticLayout(block.text, titlePaint, textWidthPx, settings.lineHeightMultiplier, justify = false)
                    val totalBlockHeight = layout.height + titleSpacingPx

                    if (totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf(block)
                        remainingHeightPx = availablePageHeightPx - totalBlockHeight
                    }
                }

                is PageBlock.Divider -> {
                    if (dividerHeightPx <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= dividerHeightPx
                    } else {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf(block)
                        remainingHeightPx = availablePageHeightPx - dividerHeightPx
                    }
                }

                is PageBlock.Blockquote -> {
                    val quoteWidth = max(80, textWidthPx - (32 * density).toInt())
                    val layout = buildStaticLayout(block.text, quotePaint, quoteWidth, settings.lineHeightMultiplier, justify = false)
                    val totalBlockHeight = layout.height + paragraphSpacingPx

                    when {
                        totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty() -> {
                            currentPageBlocks.add(block)
                            remainingHeightPx -= totalBlockHeight
                        }
                        layout.height > availablePageHeightPx -> {
                            splitTextOverflow(
                                block.text, layout,
                                { text -> PageBlock.Blockquote(text.toString()) }
                            ) { first, second ->
                                if (first != null) currentPageBlocks.add(first)
                                pages.add(currentPageBlocks)
                                currentPageBlocks = mutableListOf()
                                remainingHeightPx = availablePageHeightPx
                                if (second != null) blockQueue.addFirst(second)
                            }
                        }
                        else -> {
                            pages.add(currentPageBlocks)
                            currentPageBlocks = mutableListOf(block)
                            remainingHeightPx = availablePageHeightPx - totalBlockHeight
                        }
                    }
                }

                is PageBlock.ListItem -> {
                    // Indent the list item body and hang the marker via left padding in rendering.
                    val itemWidth = max(80, textWidthPx - (24 * density).toInt())
                    val layout = buildStaticLayout(block.text, bodyPaint, itemWidth, settings.lineHeightMultiplier, justify = settings.justifyText)
                    val totalBlockHeight = layout.height + paragraphSpacingPx

                    when {
                        totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty() -> {
                            currentPageBlocks.add(block)
                            remainingHeightPx -= totalBlockHeight
                        }
                        layout.height > availablePageHeightPx -> {
                            splitTextOverflow(
                                block.text, layout,
                                { text -> PageBlock.ListItem(listOf(com.example.data.model.InlineSpan(text.toString())), block.marker, block.indentLevel) }
                            ) { first, second ->
                                if (first != null) currentPageBlocks.add(first)
                                pages.add(currentPageBlocks)
                                currentPageBlocks = mutableListOf()
                                remainingHeightPx = availablePageHeightPx
                                if (second != null) blockQueue.addFirst(second)
                            }
                        }
                        else -> {
                            pages.add(currentPageBlocks)
                            currentPageBlocks = mutableListOf(block)
                            remainingHeightPx = availablePageHeightPx - totalBlockHeight
                        }
                    }
                }

                is PageBlock.Image -> {
                    val imageWidthPx = max(120, (textWidthPx * 0.7f).toInt())
                    val imgHeight = (imageWidthPx * 0.62f).toInt().coerceAtLeast((120 * density).toInt())
                    val totalBlockHeight = imgHeight + paragraphSpacingPx

                    if (totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf(block)
                        remainingHeightPx = availablePageHeightPx - totalBlockHeight
                    }
                }

                is PageBlock.Paragraph -> {
                    val layout = buildStaticLayout(block.text, bodyPaint, textWidthPx, settings.lineHeightMultiplier, justify = settings.justifyText)
                    val totalBlockHeight = layout.height + paragraphSpacingPx

                    if (totalBlockHeight <= remainingHeightPx) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else if (remainingHeightPx < (bodyPaint.fontSpacing * 1.8f) && currentPageBlocks.isNotEmpty()) {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf()
                        remainingHeightPx = availablePageHeightPx
                        blockQueue.addFirst(block)
                    } else {
                        val targetHeightForSplit = remainingHeightPx - paragraphSpacingPx
                        var lineBreakIndex = layout.getLineForVertical(targetHeightForSplit)

                        if (lineBreakIndex <= 0 && currentPageBlocks.isEmpty()) {
                            lineBreakIndex = 1
                        }

                        if (lineBreakIndex > 0 && lineBreakIndex < layout.lineCount) {
                            val splitCharOffset = layout.getLineEnd(lineBreakIndex - 1)
                            val (firstBlock, secondBlock) = splitParagraph(block, splitCharOffset)

                            if (firstBlock != null) currentPageBlocks.add(firstBlock)
                            pages.add(currentPageBlocks)

                            currentPageBlocks = mutableListOf()
                            remainingHeightPx = availablePageHeightPx
                            if (secondBlock != null) {
                                blockQueue.addFirst(secondBlock)
                            }
                        } else {
                            if (currentPageBlocks.isNotEmpty()) {
                                pages.add(currentPageBlocks)
                                currentPageBlocks = mutableListOf()
                                remainingHeightPx = availablePageHeightPx
                            }
                            currentPageBlocks.add(block)
                            remainingHeightPx -= totalBlockHeight
                        }
                    }
                }
            }
        }

        if (currentPageBlocks.isNotEmpty()) {
            pages.add(currentPageBlocks)
        }

        if (pages.isEmpty()) {
            pages.add(mutableListOf(PageBlock.Paragraph("End of chapter.")))
        }

        val totalPages = pages.size
        var cumulativeCharOffset = 0

        return pages.mapIndexed { index, blocksList ->
            val pageCharCount = blocksList.sumOf {
                when (it) {
                    is PageBlock.Paragraph -> it.text.length
                    is PageBlock.Title -> it.text.length
                    is PageBlock.Blockquote -> it.text.length
                    is PageBlock.ListItem -> it.text.length
                    else -> 0
                }
            }
            val start = cumulativeCharOffset
            val end = start + pageCharCount
            cumulativeCharOffset = end

            BookPage(
                pageIndex = index,
                totalPagesInChapter = totalPages,
                chapterIndex = chapterIndex,
                chapterTitle = chapter.title,
                blocks = blocksList,
                characterStartOffset = start,
                characterEndOffset = end
            )
        }
    }

    private fun buildStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        lineSpacingMultiplier: Float,
        justify: Boolean = false,
        hyphenate: Boolean = true
    ): StaticLayout {
        val safeText = if (text.isBlank()) " " else text
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .setHyphenationFrequency(if (hyphenate) Layout.HYPHENATION_FREQUENCY_NORMAL else Layout.HYPHENATION_FREQUENCY_NONE)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            if (Build.VERSION.SDK_INT >= 26 && justify) {
                builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
            builder.build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                safeText,
                paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                lineSpacingMultiplier,
                0f,
                false
            )
        }
    }

    /**
     * Splits a paragraph block at [splitCharOffset] into two new paragraphs.
     * Inline formatting spans are clamped so the split never throws on bad offsets.
     */
    private fun splitParagraph(block: PageBlock.Paragraph, splitCharOffset: Int): Pair<PageBlock.Paragraph?, PageBlock.Paragraph?> {
        val fullText = block.text
        val offset = splitCharOffset.coerceIn(0, fullText.length)
        val first = fullText.substring(0, offset).trimEnd()
        val second = fullText.substring(offset).trimStart()

        val firstBlock = if (first.isNotBlank()) {
            PageBlock.Paragraph(listOf(com.example.data.model.InlineSpan(first)), block.alignment)
        } else null
        val secondBlock = if (second.isNotBlank()) {
            PageBlock.Paragraph(listOf(com.example.data.model.InlineSpan(second)), block.alignment)
        } else null
        return Pair(firstBlock, secondBlock)
    }

    /**
     * Splits a text block that is taller than a single page into two halves so the
     * content is never dropped or clipped. Used for Blockquote and ListItem blocks.
     * [makeBlock] rebuilds a block of the same type from a chunk of text.
     */
    private inline fun splitTextOverflow(
        text: String,
        layout: StaticLayout,
        makeBlock: (CharSequence) -> com.example.data.model.PageBlock,
        emit: (first: com.example.data.model.PageBlock?, second: com.example.data.model.PageBlock?) -> Unit
    ) {
        val half = layout.lineCount / 2
        if (half <= 0) {
            emit(null, null)
            return
        }
        val offset = layout.getLineEnd(half - 1).coerceIn(0, text.length)
        val first = text.substring(0, offset).trimEnd()
        val second = text.substring(offset).trimStart()
        emit(
            if (first.isNotBlank()) makeBlock(first) else null,
            if (second.isNotBlank()) makeBlock(second) else null
        )
    }

    fun findMatchingPage(pages: List<BookPage>, previousCharOffset: Int): Int {
        if (pages.isEmpty()) return 0
        val match = pages.indexOfFirst { previousCharOffset in it.characterStartOffset..it.characterEndOffset }
        return if (match != -1) match else 0
    }
}
