package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.BookRepository
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.EpubReaderTheme

class MainActivity : ComponentActivity() {

    private val incomingUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val appTheme by ThemePreferences.getAppThemeFlow(this)
                .collectAsStateWithLifecycle(initialValue = ThemePreferences.getAppTheme(this))

            EpubReaderTheme(appTheme = appTheme) {
                EpubReaderApp(
                    incomingUri = incomingUriState.value,
                    onConsumeIncomingUri = { incomingUriState.value = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
        if (uri != null) {
            incomingUriState.value = uri
        }
    }
}

@Composable
fun EpubReaderApp(
    incomingUri: Uri? = null,
    onConsumeIncomingUri: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isOpeningExternalEpub by remember { mutableStateOf(false) }

    LaunchedEffect(incomingUri) {
        val uri = incomingUri ?: return@LaunchedEffect
        isOpeningExternalEpub = true
        try {
            val repository = BookRepository(context)
            val result = repository.importEpubFile(uri)
            if (result.isSuccess) {
                val book = result.getOrThrow()
                Toast.makeText(context, "Opening \"${book.title}\"", Toast.LENGTH_SHORT).show()
                navController.navigate("reader/${book.id}") {
                    launchSingleTop = true
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Invalid or unreadable EPUB file"
                Toast.makeText(context, "Could not open EPUB: $errorMsg", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening EPUB: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isOpeningExternalEpub = false
            onConsumeIncomingUri()
        }
    }

    Box(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("library") {
                LibraryScreen(
                    onOpenBook = { bookId ->
                        navController.navigate("reader/$bookId")
                    },
                    onOpenVocabulary = {
                        navController.navigate("vocabulary")
                    }
                )
            }

            composable(
                route = "reader/{bookId}",
                arguments = listOf(
                    navArgument("bookId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                ReaderScreen(
                    bookId = bookId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("vocabulary") {
                VocabularyScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        if (isOpeningExternalEpub) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Opening EPUB book...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
