package com.example.data.model

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
    SERIF("Serif (Classic Book)", "Georgia / Serif"),
    SANS("Sans-Serif (Modern Clean)", "Helvetica / Sans-serif"),
    MONOSPACE("Monospace (Terminal)", "Courier New"),
    CURSIVE("Cursive (Handwritten)", "Segoe Script")
}

enum class ReaderPageMargin(
    val title: String,
    val horizontalPaddingDp: Int,
    val maxInlineSizePx: Int
) {
    COMPACT("Compact", 16, 860),
    NORMAL("Normal", 24, 720),
    WIDE("Spacious", 36, 600)
}

data class ReaderSettings(
    val fontSizeSp: Float = 17f,
    val lineHeightMultiplier: Float = 1.55f,
    val fontChoice: ReaderFontFamily = ReaderFontFamily.SERIF,
    val themeChoice: ReaderTheme = ReaderTheme.WARM_PAPER,
    val marginChoice: ReaderPageMargin = ReaderPageMargin.NORMAL,
    val brightnessPercent: Float = 1.0f,
    val showHeaderFooter: Boolean = true,
    val tapToTurnPage: Boolean = true,
    val justifyText: Boolean = true,
    val hyphenation: Boolean = true
)

/**
 * A text selection reported by the embedded rendering engine.
 * [selectedText] is the selected run of text, [cfi] is the EPUB CFI locator for
 * the selection (used to re-highlight and save quotes), and [sentenceContext] is
 * the surrounding sentence used by the dictionary/Google lookup.
 */
data class TextSelection(
    val selectedText: String,
    val cfi: String = "",
    val index: Int = 0,
    val sentenceContext: String = ""
)
