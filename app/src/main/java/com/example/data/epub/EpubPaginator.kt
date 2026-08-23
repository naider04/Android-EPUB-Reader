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
                    val layout = buildStaticLayout(block.text, titlePaint, textWidthPx, settings.lineHeightMultiplier)
                    val totalBlockHeight = layout.height + titleSpacingPx

                    if (totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else {
                        // Title doesn't fit on this page, push to next page
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
                    val layout = buildStaticLayout(block.text, quotePaint, quoteWidth, settings.lineHeightMultiplier)
                    val totalBlockHeight = layout.height + paragraphSpacingPx

                    if (totalBlockHeight <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf(block)
                        remainingHeightPx = availablePageHeightPx - totalBlockHeight
                    }
                }

                is PageBlock.ImagePlaceholder -> {
                    val imgHeight = (180 * density).toInt()
                    if (imgHeight <= remainingHeightPx || currentPageBlocks.isEmpty()) {
                        currentPageBlocks.add(block)
                        remainingHeightPx -= imgHeight
                    } else {
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf(block)
                        remainingHeightPx = availablePageHeightPx - imgHeight
                    }
                }

                is PageBlock.Paragraph -> {
                    val layout = buildStaticLayout(block.text, bodyPaint, textWidthPx, settings.lineHeightMultiplier)
                    val totalBlockHeight = layout.height + paragraphSpacingPx

                    if (totalBlockHeight <= remainingHeightPx) {
                        // Entire paragraph fits on this page
                        currentPageBlocks.add(block)
                        remainingHeightPx -= totalBlockHeight
                    } else if (remainingHeightPx < (bodyPaint.fontSpacing * 1.8f) && currentPageBlocks.isNotEmpty()) {
                        // Less than 2 lines fit and page already has content -> start fresh page
                        pages.add(currentPageBlocks)
                        currentPageBlocks = mutableListOf()
                        remainingHeightPx = availablePageHeightPx
                        blockQueue.addFirst(block)
                    } else {
                        // Paragraph needs to be cleanly split across page boundary
                        val targetHeightForSplit = remainingHeightPx - paragraphSpacingPx
                        var lineBreakIndex = layout.getLineForVertical(targetHeightForSplit)

                        if (lineBreakIndex <= 0 && currentPageBlocks.isEmpty()) {
                            // At least put 1 line if the page is empty
                            lineBreakIndex = 1
                        }

                        if (lineBreakIndex > 0 && lineBreakIndex < layout.lineCount) {
                            val splitCharOffset = layout.getLineEnd(lineBreakIndex - 1)
                            val firstPartText = block.text.substring(0, splitCharOffset).trimEnd()
                            val secondPartText = block.text.substring(splitCharOffset).trimStart()

                            if (firstPartText.isNotBlank()) {
                                currentPageBlocks.add(block.copy(text = firstPartText))
                            }
                            pages.add(currentPageBlocks)

                            // Remainder starts new page
                            currentPageBlocks = mutableListOf()
                            remainingHeightPx = availablePageHeightPx
                            if (secondPartText.isNotBlank()) {
                                blockQueue.addFirst(block.copy(text = secondPartText))
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
        lineSpacingMultiplier: Float
    ): StaticLayout {
        val safeText = if (text.isBlank()) " " else text
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
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

    fun findMatchingPage(pages: List<BookPage>, previousCharOffset: Int): Int {
        if (pages.isEmpty()) return 0
        val match = pages.indexOfFirst { previousCharOffset in it.characterStartOffset..it.characterEndOffset }
        return if (match != -1) match else 0
    }
}
