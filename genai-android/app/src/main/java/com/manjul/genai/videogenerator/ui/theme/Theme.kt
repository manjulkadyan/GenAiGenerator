package com.manjul.genai.videogenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5)
)

@Composable
fun GenAiVideoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
