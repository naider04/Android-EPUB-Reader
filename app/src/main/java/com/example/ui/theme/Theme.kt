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

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
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

@Composable
fun EpubReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
