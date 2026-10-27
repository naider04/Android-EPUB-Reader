package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderPageMargin
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System Default", "Follows device dark/light setting"),
    WARM_PAPER("Warm Paper", "Cream background with espresso text"),
    SEPIA("Sepia", "Warm antique page tone"),
    LIGHT("Clean Light", "Bright modern clean aesthetic"),
    SLATE("Slate Gray", "Cool modern dark gray"),
    DARK("OLED Black", "True deep black for reading in the dark");

    fun toReaderTheme(): ReaderTheme {
        return when (this) {
            SYSTEM -> ReaderTheme.WARM_PAPER
            WARM_PAPER -> ReaderTheme.WARM_PAPER
            SEPIA -> ReaderTheme.SEPIA
            LIGHT -> ReaderTheme.LIGHT
            SLATE -> ReaderTheme.SLATE
            DARK -> ReaderTheme.DARK
        }
    }

    companion object {
        fun fromReaderTheme(readerTheme: ReaderTheme): AppThemeMode {
            return when (readerTheme) {
                ReaderTheme.WARM_PAPER -> WARM_PAPER
                ReaderTheme.SEPIA -> SEPIA
                ReaderTheme.LIGHT -> LIGHT
                ReaderTheme.SLATE -> SLATE
                ReaderTheme.DARK -> DARK
            }
        }
    }
}

object ThemePreferences {
    private const val PREFS_NAME = "app_theme_preferences"

    // App Theme keys
    private const val KEY_APP_THEME = "key_app_theme_mode"

    // Reader Settings keys
    private const val KEY_READER_FONT_SIZE = "key_reader_font_size"
    private const val KEY_READER_LINE_HEIGHT = "key_reader_line_height"
    private const val KEY_READER_FONT_FAMILY = "key_reader_font_family"
    private const val KEY_READER_THEME = "key_reader_theme"
    private const val KEY_READER_MARGIN = "key_reader_margin"
    private const val KEY_READER_BRIGHTNESS = "key_reader_brightness"
    private const val KEY_READER_SHOW_HEADER_FOOTER = "key_reader_show_header_footer"
    private const val KEY_READER_TAP_TURN = "key_reader_tap_turn"
    private const val KEY_READER_JUSTIFY = "key_reader_justify"
    private const val KEY_READER_HYPHENATION = "key_reader_hyphenation"

    private var _appThemeFlow: MutableStateFlow<AppThemeMode>? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns an observable StateFlow of the active AppThemeMode.
     */
    fun getAppThemeFlow(context: Context): StateFlow<AppThemeMode> {
        if (_appThemeFlow == null) {
            _appThemeFlow = MutableStateFlow(getAppTheme(context))
        }
        return _appThemeFlow!!.asStateFlow()
    }

    /**
     * Reads the persisted AppThemeMode. Defaults to WARM_PAPER.
     */
    fun getAppTheme(context: Context): AppThemeMode {
        val prefs = getPrefs(context)
        val name = prefs.getString(KEY_APP_THEME, AppThemeMode.WARM_PAPER.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.WARM_PAPER.name)
        } catch (_: Exception) {
            AppThemeMode.WARM_PAPER
        }
    }

    /**
     * Persists the AppThemeMode and notifies active observers immediately.
     */
    fun setAppTheme(context: Context, mode: AppThemeMode) {
        val prefs = getPrefs(context)
        prefs.edit {
            putString(KEY_APP_THEME, mode.name)
        }
        if (_appThemeFlow != null) {
            _appThemeFlow?.value = mode
        } else {
            _appThemeFlow = MutableStateFlow(mode)
        }

        // Also update the ReaderSettings theme choice to keep them cohesive
        if (mode != AppThemeMode.SYSTEM) {
            val currentSettings = loadReaderSettings(context)
            val mappedReaderTheme = mode.toReaderTheme()
            if (currentSettings.themeChoice != mappedReaderTheme) {
                saveReaderSettings(context, currentSettings.copy(themeChoice = mappedReaderTheme), syncAppTheme = false)
            }
        }
    }

    /**
     * Loads the persisted ReaderSettings.
     */
    fun loadReaderSettings(context: Context): ReaderSettings {
        val prefs = getPrefs(context)
        val defaultSettings = ReaderSettings()

        val themeName = prefs.getString(KEY_READER_THEME, null)
        val themeChoice = if (themeName != null) {
            try {
                ReaderTheme.valueOf(themeName)
            } catch (_: Exception) {
                defaultSettings.themeChoice
            }
        } else {
            // Check if app theme was set
            val appTheme = getAppTheme(context)
            if (appTheme != AppThemeMode.SYSTEM) {
                appTheme.toReaderTheme()
            } else {
                defaultSettings.themeChoice
            }
        }

        val fontName = prefs.getString(KEY_READER_FONT_FAMILY, defaultSettings.fontChoice.name)
        val fontChoice = try {
            ReaderFontFamily.valueOf(fontName ?: defaultSettings.fontChoice.name)
        } catch (_: Exception) {
            defaultSettings.fontChoice
        }

        val marginName = prefs.getString(KEY_READER_MARGIN, defaultSettings.marginChoice.name)
        val marginChoice = try {
            ReaderPageMargin.valueOf(marginName ?: defaultSettings.marginChoice.name)
        } catch (_: Exception) {
            defaultSettings.marginChoice
        }

        return ReaderSettings(
            fontSizeSp = prefs.getFloat(KEY_READER_FONT_SIZE, defaultSettings.fontSizeSp),
            lineHeightMultiplier = prefs.getFloat(KEY_READER_LINE_HEIGHT, defaultSettings.lineHeightMultiplier),
            fontChoice = fontChoice,
            themeChoice = themeChoice,
            marginChoice = marginChoice,
            brightnessPercent = prefs.getFloat(KEY_READER_BRIGHTNESS, defaultSettings.brightnessPercent),
            showHeaderFooter = prefs.getBoolean(KEY_READER_SHOW_HEADER_FOOTER, defaultSettings.showHeaderFooter),
            tapToTurnPage = prefs.getBoolean(KEY_READER_TAP_TURN, defaultSettings.tapToTurnPage),
            justifyText = prefs.getBoolean(KEY_READER_JUSTIFY, defaultSettings.justifyText),
            hyphenation = prefs.getBoolean(KEY_READER_HYPHENATION, defaultSettings.hyphenation)
        )
    }

    /**
     * Persists ReaderSettings and optionally synchronizes the AppThemeMode.
     */
    fun saveReaderSettings(context: Context, settings: ReaderSettings, syncAppTheme: Boolean = true) {
        val prefs = getPrefs(context)
        prefs.edit {
            putFloat(KEY_READER_FONT_SIZE, settings.fontSizeSp)
            putFloat(KEY_READER_LINE_HEIGHT, settings.lineHeightMultiplier)
            putString(KEY_READER_FONT_FAMILY, settings.fontChoice.name)
            putString(KEY_READER_THEME, settings.themeChoice.name)
            putString(KEY_READER_MARGIN, settings.marginChoice.name)
            putFloat(KEY_READER_BRIGHTNESS, settings.brightnessPercent)
            putBoolean(KEY_READER_SHOW_HEADER_FOOTER, settings.showHeaderFooter)
            putBoolean(KEY_READER_TAP_TURN, settings.tapToTurnPage)
            putBoolean(KEY_READER_JUSTIFY, settings.justifyText)
            putBoolean(KEY_READER_HYPHENATION, settings.hyphenation)
        }

        if (syncAppTheme) {
            val appThemeMode = AppThemeMode.fromReaderTheme(settings.themeChoice)
            putStringInternal(context, appThemeMode)
        }
    }

    private fun putStringInternal(context: Context, mode: AppThemeMode) {
        val prefs = getPrefs(context)
        prefs.edit {
            putString(KEY_APP_THEME, mode.name)
        }
        if (_appThemeFlow != null) {
            _appThemeFlow?.value = mode
        } else {
            _appThemeFlow = MutableStateFlow(mode)
        }
    }
}
