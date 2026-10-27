package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppThemeMode

private val DarkOledColorScheme = darkColorScheme(
    primary = PrimaryBronzeDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF422812),
    onPrimaryContainer = Color(0xFFFFDCC1),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = DarkOledBg,
    onBackground = DarkOledText,
    surface = DarkOledSurface,
    onSurface = DarkOledText,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = SlateText
)

private val SlateColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0F1E2A),
    primaryContainer = Color(0xFF263238),
    onPrimaryContainer = Color(0xFFE1E5EA),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF1E232B),
    background = SlateBg,
    onBackground = SlateText,
    surface = SlateSurface,
    onSurface = SlateText,
    surfaceVariant = Color(0xFF343D4B),
    onSurfaceVariant = Color(0xFFC4CBD4)
)

private val WarmPaperColorScheme = lightColorScheme(
    primary = PrimaryBronze,
    onPrimary = Color.White,
    primaryContainer = WarmPaperSurface,
    onPrimaryContainer = WarmPaperText,
    secondary = SecondaryCoffee,
    onSecondary = Color.White,
    background = WarmPaperBg,
    onBackground = WarmPaperText,
    surface = WarmPaperSurface,
    onSurface = WarmPaperText,
    surfaceVariant = Color(0xFFECE0CA),
    onSurfaceVariant = Color(0xFF534335)
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF9E6534),
    onPrimary = Color.White,
    primaryContainer = SepiaSurface,
    onPrimaryContainer = SepiaText,
    secondary = SecondaryCoffee,
    onSecondary = Color.White,
    background = SepiaBg,
    onBackground = SepiaText,
    surface = SepiaSurface,
    onSurface = SepiaText,
    surfaceVariant = Color(0xFFDECDB0),
    onSurfaceVariant = Color(0xFF4F3E30)
)

private val CleanLightColorScheme = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8ECFC),
    onPrimaryContainer = Color(0xFF191D24),
    secondary = Color(0xFF4A6572),
    onSecondary = Color.White,
    background = CleanLightBg,
    onBackground = CleanLightText,
    surface = CleanLightSurface,
    onSurface = CleanLightText,
    surfaceVariant = Color(0xFFE2E4EB),
    onSurfaceVariant = Color(0xFF44474F)
)

@Composable
fun EpubReaderTheme(
    appTheme: AppThemeMode = AppThemeMode.WARM_PAPER,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkOledColorScheme
            } else {
                WarmPaperColorScheme
            }
        }
        AppThemeMode.WARM_PAPER -> WarmPaperColorScheme
        AppThemeMode.SEPIA -> SepiaColorScheme
        AppThemeMode.LIGHT -> CleanLightColorScheme
        AppThemeMode.SLATE -> SlateColorScheme
        AppThemeMode.DARK -> DarkOledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
