package com.example.data.model

import androidx.compose.ui.text.font.FontFamily

enum class ReaderTheme(
    val title: String,
    val backgroundColorHex: Long,
    val textColorHex: Long,
    val surfaceColorHex: Long,
    val accentColorHex: Long,
    val secondaryTextColorHex: Long
) {
    WARM_PAPER("Warm Paper", 0xFFFDF7EB, 0xFF2D241E, 0xFFF3E7D3, 0xFFC07028, 0xFF7A6B5F),
    SEPIA("Sepia", 0xFFF4ECD8, 0xFF3E3024, 0xFFE8DCC0, 0xFF9E6534, 0xFF83705C),
    LIGHT("Clean Light", 0xFFFAFAFC, 0xFF191D24, 0xFFEEF0F4, 0xFF3D5AFE, 0xFF656F7D),
    SLATE("Slate Gray", 0xFF262C36, 0xFFE1E5EA, 0xFF1E232B, 0xFF64B5F6, 0xFF9DA8B6),
    DARK("OLED Black", 0xFF0A0C10, 0xFFD8DCE3, 0xFF141820, 0xFFE5A952, 0xFF858E9B)
}

enum class ReaderFontFamily(val title: String, val fontDisplayName: String) {
    SERIF("Serif (Classic Book)", "Literata / Serif"),
    SANS("Sans-Serif (Modern Clean)", "Inter / Sans-serif"),
    MONOSPACE("Monospace (Terminal)", "Roboto Mono"),
    CURSIVE("Cursive (Handwritten)", "Casual / Cursive")
}

enum class ReaderPageMargin(val title: String, val horizontalPaddingDp: Int) {
    COMPACT("Compact", 16),
    NORMAL("Normal", 24),
    WIDE("Spacious", 36)
}

data class ReaderSettings(
    val fontSizeSp: Float = 17f,
    val lineHeightMultiplier: Float = 1.55f,
    val fontChoice: ReaderFontFamily = ReaderFontFamily.SERIF,
    val themeChoice: ReaderTheme = ReaderTheme.WARM_PAPER,
    val marginChoice: ReaderPageMargin = ReaderPageMargin.NORMAL,
    val brightnessPercent: Float = 1.0f,
    val showHeaderFooter: Boolean = true,
    val tapToTurnPage: Boolean = true
)

sealed class PageBlock {
    data class Title(val text: String, val level: Int = 1) : PageBlock()
    data class Paragraph(val text: String, val isItalic: Boolean = false, val isBold: Boolean = false) : PageBlock()
    data class Blockquote(val text: String, val citation: String? = null) : PageBlock()
    data class Divider(val style: String = "asterisks") : PageBlock()
    data class ImagePlaceholder(val alt: String, val path: String? = null) : PageBlock()
}

data class BookPage(
    val pageIndex: Int,
    val totalPagesInChapter: Int,
    val chapterIndex: Int,
    val chapterTitle: String,
    val blocks: List<PageBlock>,
    val characterStartOffset: Int = 0,
    val characterEndOffset: Int = 0
)

data class EpubChapter(
    val id: String,
    val title: String,
    val rawHtml: String,
    val blocks: List<PageBlock>,
    val fullPlainText: String,
    val href: String = ""
)

data class EpubBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverResId: Int? = null,
    val coverFilePath: String? = null,
    val chapters: List<EpubChapter>,
    val isAsset: Boolean = false,
    val fileUriString: String? = null
)

data class TextSelection(
    val blockIndex: Int,
    val selectedText: String,
    val startOffset: Int,
    val endOffset: Int,
    val sentenceContext: String = ""
)

data class BookSearchResult(
    val id: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val query: String,
    val charOffset: Int
)

