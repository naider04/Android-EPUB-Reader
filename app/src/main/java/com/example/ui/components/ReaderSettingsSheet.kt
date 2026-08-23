package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderPageMargin
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChanged: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }

    if (showVoiceSettingsDialog) {
        VoiceSettingsDialog(
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(settings.themeChoice.surfaceColorHex),
        contentColor = Color(settings.themeChoice.textColorHex),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        val textColor = Color(settings.themeChoice.textColorHex)
        val accentColor = Color(settings.themeChoice.accentColorHex)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Sheet & Reader Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
            }

            // 1. Font Size Slider
            Text(
                text = "Font Size: ${settings.fontSizeSp.roundToInt()} sp",
                style = MaterialTheme.typography.labelLarge.copy(color = textColor),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = { newSize ->
                        onSettingsChanged { it.copy(fontSizeSp = newSize) }
                    },
                    valueRange = 12f..30f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_font_size_slider")
                )
                Text(
                    text = "A",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Theme Presets
            Text(
                text = "Theme Palette",
                style = MaterialTheme.typography.labelLarge.copy(color = textColor),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReaderTheme.entries.forEach { theme ->
                    val isSelected = settings.themeChoice == theme
                    val bg = Color(theme.backgroundColorHex)
                    val textCol = Color(theme.textColorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) accentColor else textColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSettingsChanged { it.copy(themeChoice = theme) }
                            }
                            .padding(vertical = 12.dp)
                            .testTag("theme_button_${theme.name.lowercase()}")
                    ) {
                        Text(
                            text = "Aa",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = theme.title.split(" ").first(),
                            fontSize = 11.sp,
                            color = textCol.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Font Family Selector
            Text(
                text = "Typeface",
                style = MaterialTheme.typography.labelLarge.copy(color = textColor),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReaderFontFamily.entries.forEach { font ->
                    val isSelected = settings.fontChoice == font
                    val fontPreviewFamily = when (font) {
                        ReaderFontFamily.SERIF -> FontFamily.Serif
                        ReaderFontFamily.SANS -> FontFamily.SansSerif
                        ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
                        ReaderFontFamily.CURSIVE -> FontFamily.Cursive
                    }

                    Surface(
                        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentColor else textColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onSettingsChanged { it.copy(fontChoice = font) }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = font.name.take(5),
                                fontFamily = fontPreviewFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else textColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Line Spacing & Margins
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Line spacing
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Line Height",
                        style = MaterialTheme.typography.labelLarge.copy(color = textColor),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.3f to "Tight", 1.55f to "Normal", 1.85f to "Wide").forEach { (mult, label) ->
                            val isSelected = (settings.lineHeightMultiplier - mult) in -0.05f..0.05f
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSettingsChanged { it.copy(lineHeightMultiplier = mult) } },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }

                // Margins
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Page Margins",
                        style = MaterialTheme.typography.labelLarge.copy(color = textColor),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReaderPageMargin.entries.forEach { margin ->
                            val isSelected = settings.marginChoice == margin
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSettingsChanged { it.copy(marginChoice = margin) } },
                                label = { Text(margin.title, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Voice Source Settings Button
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showVoiceSettingsDialog = true }
                    .testTag("open_voice_settings_from_reader")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pronunciation Voice Settings",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Change audio engine for UK & US voices",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Voice",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Header/Footer Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Show Header & Sheet Number",
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                )
                Switch(
                    checked = settings.showHeaderFooter,
                    onCheckedChange = { checked ->
                        onSettingsChanged { it.copy(showHeaderFooter = checked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
