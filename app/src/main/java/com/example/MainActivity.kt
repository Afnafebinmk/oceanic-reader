package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.books.BookDetailScreen
import com.example.ui.books.BookViewModel
import com.example.ui.books.ReaderScreen
import com.example.ui.books.PdfReaderScreen
import com.example.ui.books.SearchScreen
import com.example.ui.books.ShelfScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BookViewModel = viewModel()
            val isDarkThemeGlobal by viewModel.isDarkThemeGlobal.collectAsState()
            val darkTheme = isDarkThemeGlobal ?: isSystemInDarkTheme()
            MyApplicationTheme(darkTheme = darkTheme) {
                MainNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: BookViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "shelf",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("shelf") {
            ShelfScreen(
                viewModel = viewModel,
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToBookDetail = { book -> navController.navigate("book_detail/${book.id}") }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookDetail = { book -> navController.navigate("book_detail/${book.id}") }
            )
        }

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                viewModel = viewModel,
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReader = { navController.navigate("reader") },
                onNavigateToPdfReader = { navController.navigate("pdf_reader") }
            )
        }

        composable("reader") {
            ReaderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_reader") {
            PdfReaderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
