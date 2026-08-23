package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.EpubReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EpubReaderTheme {
                EpubReaderApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun EpubReaderApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "library",
        modifier = modifier
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
}
