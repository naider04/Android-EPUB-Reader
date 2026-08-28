package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DictionaryEntry
import com.example.data.model.ReaderTheme
import com.example.util.PronunciationAccent
import com.example.util.rememberPronunciationPlayer
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryBottomSheet(
    entry: DictionaryEntry?,
    selectedWord: String = "",
    isLoading: Boolean,
    theme: ReaderTheme,
    bookId: String? = null,
    contextSnippet: String = "",
    onSearchWord: (String) -> Unit,
    onDismiss: () -> Unit,
    isSavedInVocabulary: Boolean = false,
    onSaveToVocabulary: () -> Unit = {}
) {
    var searchInput by remember { mutableStateOf("") }
    val textColor = Color(theme.textColorHex)
    val accentColor = Color(theme.accentColorHex)
    val surfaceColor = Color(theme.surfaceColorHex)
    val pronunciationPlayer = rememberPronunciationPlayer()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = surfaceColor,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Look up any word in dictionary...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor
                    )
                },
                trailingIcon = {
                    if (searchInput.isNotBlank()) {
                        Button(
                            onClick = { onSearchWord(searchInput.trim()) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Define", fontSize = 12.sp, color = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.25f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dict_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (entry != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Word, Phonetics & UK/US Pronunciation Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.word,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            if (!entry.phonetic.isNullOrBlank()) {
                                Text(
                                    text = entry.phonetic,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        color = accentColor,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }

                        // Pronunciation Buttons: UK and US
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    pronunciationPlayer.speak(entry.word, PronunciationAccent.UK)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.14f),
                                    contentColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("pronounce_uk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "UK Pronunciation",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UK",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    pronunciationPlayer.speak(entry.word, PronunciationAccent.US)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.14f),
                                    contentColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("pronounce_us_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "US Pronunciation",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "US",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // DEFINITIONS SECTION
                    Text(
                        text = "Definitions & Meaning",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    entry.meanings.forEachIndexed { mIdx, meaning ->
                        Surface(
                            color = textColor.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = meaning.partOfSpeech.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                meaning.definitions.forEachIndexed { dIdx, def ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                        Text(
                                            text = "${dIdx + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            fontSize = 13.sp,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Text(
                                            text = def,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = textColor,
                                                lineHeight = 20.sp,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }

                                if (meaning.examples.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    meaning.examples.forEach { example ->
                                        Text(
                                            text = "• \"$example\"",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontStyle = FontStyle.Italic,
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Origin / Etymology
                    if (!entry.origin.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Origin: ${entry.origin}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // GOOGLE EMBEDDED SEARCH VIEW (Below dictionary definition)
                    val effectiveWord = selectedWord.ifBlank { entry.word }
                    GoogleImagesWebView(
                        word = effectiveWord,
                        accentColor = accentColor,
                        textColor = textColor,
                        surfaceColor = surfaceColor
                    )

                    // Source label
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Source: ${entry.source}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else if (selectedWord.isNotBlank()) {
                // Word not in offline dictionary, but available for Google Search & pronunciation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedWord,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Web & Image Search",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = accentColor,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // Pronunciation Buttons: UK and US
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    pronunciationPlayer.speak(selectedWord, PronunciationAccent.UK)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.14f),
                                    contentColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("pronounce_uk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "UK Pronunciation",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "UK",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    pronunciationPlayer.speak(selectedWord, PronunciationAccent.US)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.14f),
                                    contentColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("pronounce_us_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "US Pronunciation",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "US",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // GOOGLE EMBEDDED SEARCH VIEW (Search, AI Mode, All, Images, Pronunciation)
                    GoogleImagesWebView(
                        word = selectedWord,
                        accentColor = accentColor,
                        textColor = textColor,
                        surfaceColor = surfaceColor
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = "Press and hold any word in the book, or type above, to look up its definition and images.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

enum class GoogleSearchTab(val label: String) {
    IMAGES("Images"),
    ALL("All"),
    AI_MODE("AI Mode"),
    PRONUNCIATION("Pronunciation")
}

/**
 * Embedded Google Search Browser with Mode Selection Bar (AI Mode, All, Images, Pronunciation)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleImagesWebView(
    word: String,
    accentColor: Color,
    textColor: Color,
    surfaceColor: Color
) {
    val context = LocalContext.current
    var selectedTab by remember(word) { mutableStateOf(GoogleSearchTab.IMAGES) }
    var isWebLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Strip dictionary root annotations or wrapping quotes/punctuation to search exact word as selected
    val cleanSelectedWord = remember(word) {
        word.substringBefore(" (root:").trim().trim('"', '\'', '.', ',', ';', ':', '!', '?', '(', ')', '[', ']', '“', '”', '‘', '’')
    }
    val encodedWord = remember(cleanSelectedWord) { URLEncoder.encode(cleanSelectedWord, "UTF-8") }
    val encodedPronunciationQuery = remember(cleanSelectedWord) {
        URLEncoder.encode("$cleanSelectedWord pronunciation", "UTF-8")
    }

    val currentUrl = remember(cleanSelectedWord, selectedTab) {
        when (selectedTab) {
            GoogleSearchTab.IMAGES -> "https://www.google.com/search?tbm=isch&q=$encodedWord&hl=en"
            GoogleSearchTab.ALL -> "https://www.google.com/search?q=$encodedWord&hl=en"
            GoogleSearchTab.AI_MODE -> "https://www.google.com/search?q=$encodedWord&hl=en"
            GoogleSearchTab.PRONUNCIATION -> "https://www.google.com/search?q=$encodedPronunciationQuery&hl=en"
        }
    }

    // JavaScript snippet that comprehensively hides the Google header, search bar, GOOGLE logo,
    // Google's internal filter tabs/chips (since we have our own native buttons), and sign-in button.
    val cleanCssScript = """
        javascript:(function() {
            var css = `
                header, #header, #searchform, form[role="search"], div[role="search"],
                #topstuff, #logo, .logo, [aria-label="Google"], img[alt*="Google"],
                a[href*="accounts.google.com"], .gb_Xa, .gb_Wa, .gb_Ta, .gb_d, #tsf, #sfcnt,
                .mJ7ve, .yg51vc, .RNNXgb, header div.o3IK9e, #easter-egg, .minidiv,
                div[role="navigation"], .hdtb-mbtc, .sfsh, .O3DeR, .appbar,
                .gG5yvd, .crJ18e, .T75of, .f5r5Fd, .EDblX, .q2X4bd,
                div[data-ved*="2ahUKE"] > div > a[role="tab"],
                div[role="tablist"], .T4LgNb, .X6j8Ze, .zWegpe {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    min-height: 0px !important;
                    max-height: 0px !important;
                    padding: 0px !important;
                    margin: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    overflow: hidden !important;
                }
                body, html, #main, #cnt, #rcnt, #center_col, .main {
                    padding-top: 0px !important;
                    margin-top: 0px !important;
                }
            `;
            var head = document.head || document.getElementsByTagName('head')[0];
            if (head) {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                head.appendChild(style);
            }
        })()
    """.trimIndent()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = textColor.copy(alpha = 0.035f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mode selection bar: AI Mode, All, Images + Reload & Open in Browser actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented tabs: AI Mode | All | Images | Pronunciation
                Row(
                    modifier = Modifier
                        .background(textColor.copy(alpha = 0.08f), shape = RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // AI Mode Tab
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == GoogleSearchTab.AI_MODE) accentColor else Color.Transparent,
                        modifier = Modifier.clickable {
                            selectedTab = GoogleSearchTab.AI_MODE
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Mode",
                                tint = if (selectedTab == GoogleSearchTab.AI_MODE) Color.White else textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "AI Mode",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == GoogleSearchTab.AI_MODE) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == GoogleSearchTab.AI_MODE) Color.White else textColor.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // All Tab
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == GoogleSearchTab.ALL) accentColor else Color.Transparent,
                        modifier = Modifier.clickable {
                            selectedTab = GoogleSearchTab.ALL
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "All",
                                tint = if (selectedTab == GoogleSearchTab.ALL) Color.White else textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "All",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == GoogleSearchTab.ALL) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == GoogleSearchTab.ALL) Color.White else textColor.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Images Tab
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == GoogleSearchTab.IMAGES) accentColor else Color.Transparent,
                        modifier = Modifier.clickable {
                            selectedTab = GoogleSearchTab.IMAGES
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Images",
                                tint = if (selectedTab == GoogleSearchTab.IMAGES) Color.White else textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Images",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == GoogleSearchTab.IMAGES) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == GoogleSearchTab.IMAGES) Color.White else textColor.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Pronunciation Tab
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == GoogleSearchTab.PRONUNCIATION) accentColor else Color.Transparent,
                        modifier = Modifier.clickable {
                            selectedTab = GoogleSearchTab.PRONUNCIATION
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronunciation",
                                tint = if (selectedTab == GoogleSearchTab.PRONUNCIATION) Color.White else textColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Pronunciation",
                                fontSize = 10.5.sp,
                                fontWeight = if (selectedTab == GoogleSearchTab.PRONUNCIATION) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == GoogleSearchTab.PRONUNCIATION) Color.White else textColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Action buttons: Loading indicator, Reload & Open in external browser
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWebLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Browser",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Embedded Browser Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, textColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Setup persistent cookies so CAPTCHAs & Google verification state persist across lookups
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Standard Mobile Chrome User-Agent to avoid generic bot/WebView CAPTCHA flags
                            val defaultUa = settings.userAgentString
                            val chromeMobileUa = defaultUa.replace("; wv", "")
                                .ifBlank { "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36" }
                            settings.userAgentString = chromeMobileUa

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isWebLoading = true
                                    view?.evaluateJavascript(cleanCssScript, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isWebLoading = false
                                    view?.evaluateJavascript(cleanCssScript, null)
                                    cookieManager.flush()
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    // Allow internal navigation through search results and tabs
                                    return false
                                }
                            }

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { webView ->
                        if (webView.url != currentUrl) {
                            webView.loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
