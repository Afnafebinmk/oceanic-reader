package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BookshelfGold,
    secondary = LibrarySecondaryLight,
    background = DeepBookshelfBg,
    surface = BookshelfSurface,
    onPrimary = Color(0xFF131722),
    onSecondary = Color.White,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    secondaryContainer = Color(0xFF2B3047),
    onSecondaryContainer = BookshelfGold
)

private val LightColorScheme = lightColorScheme(
    primary = LibraryPrimaryLight,
    secondary = LibrarySecondaryLight,
    background = Color(0xFFFCFBF9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2C251C),
    onSurface = Color(0xFF2C251C),
    secondaryContainer = Color(0xFFEFEBE9),
    onSecondaryContainer = LibraryPrimaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
