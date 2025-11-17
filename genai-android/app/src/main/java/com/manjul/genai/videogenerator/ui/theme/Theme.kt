package com.manjul.genai.videogenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern, vibrant color palette inspired by AI/tech aesthetics
// Primary: Deep purple-blue gradient
// Accent: Vibrant cyan/teal
// Background: Clean whites and soft grays

private val LightColors = lightColorScheme(
    // Primary colors - Deep purple-blue
    primary = Color(0xFF6366F1), // Indigo-500
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo-100
    onPrimaryContainer = Color(0xFF312E81), // Indigo-800
    
    // Secondary colors - Vibrant cyan
    secondary = Color(0xFF06B6D4), // Cyan-500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE), // Cyan-100
    onSecondaryContainer = Color(0xFF164E63), // Cyan-800
    
    // Tertiary colors - Warm pink
    tertiary = Color(0xFFEC4899), // Pink-500
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3), // Pink-100
    onTertiaryContainer = Color(0xFF831843), // Pink-800
    
    // Background colors
    background = Color(0xFFFAFBFC), // Almost white with slight gray
    onBackground = Color(0xFF1F2937), // Gray-800
    surface = Color.White,
    onSurface = Color(0xFF111827), // Gray-900
    surfaceVariant = Color(0xFFF3F4F6), // Gray-100
    onSurfaceVariant = Color(0xFF6B7280), // Gray-500
    
    // Error colors
    error = Color(0xFFEF4444), // Red-500
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), // Red-100
    onErrorContainer = Color(0xFF991B1B), // Red-800
    
    // Outline colors
    outline = Color(0xFFE5E7EB), // Gray-200
    outlineVariant = Color(0xFFF3F4F6) // Gray-100
)

private val DarkColors = darkColorScheme(
    // Primary colors - Lighter indigo for dark mode
    primary = Color(0xFF818CF8), // Indigo-400
    onPrimary = Color(0xFF1E1B4B), // Indigo-900
    primaryContainer = Color(0xFF4338CA), // Indigo-700
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo-100
    
    // Secondary colors - Lighter cyan
    secondary = Color(0xFF22D3EE), // Cyan-400
    onSecondary = Color(0xFF083344), // Cyan-900
    secondaryContainer = Color(0xFF0891B2), // Cyan-600
    onSecondaryContainer = Color(0xFFCFFAFE), // Cyan-100
    
    // Tertiary colors - Lighter pink
    tertiary = Color(0xFFF472B6), // Pink-400
    onTertiary = Color(0xFF831843), // Pink-800
    tertiaryContainer = Color(0xFFDB2777), // Pink-600
    onTertiaryContainer = Color(0xFFFCE7F3), // Pink-100
    
    // Background colors
    background = Color(0xFF0F172A), // Slate-900
    onBackground = Color(0xFFF1F5F9), // Slate-100
    surface = Color(0xFF1E293B), // Slate-800
    onSurface = Color(0xFFF8FAFC), // Slate-50
    surfaceVariant = Color(0xFF334155), // Slate-700
    onSurfaceVariant = Color(0xFFCBD5E1), // Slate-300
    
    // Error colors
    error = Color(0xFFF87171), // Red-400
    onError = Color(0xFF7F1D1D), // Red-900
    errorContainer = Color(0xFFDC2626), // Red-600
    onErrorContainer = Color(0xFFFEE2E2), // Red-100
    
    // Outline colors
    outline = Color(0xFF475569), // Slate-600
    outlineVariant = Color(0xFF334155) // Slate-700
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
