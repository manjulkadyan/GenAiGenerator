package com.manjul.genai.videogenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.typography.AppTypography

private val DarkColors = darkColorScheme(
    // Primary - Purple accent matching reference images
    primary = AppColors.PrimaryPurple,
    onPrimary = AppColors.OnPrimaryPurple,
    primaryContainer = AppColors.PrimaryPurpleDark,
    onPrimaryContainer = AppColors.OnPrimaryPurple,

    // Secondary - Amber for variety
    secondary = Color(0xFFF6C177),
    onSecondary = Color(0xFF1B1206),
    secondaryContainer = Color(0xFF583516),
    onSecondaryContainer = Color(0xFFFFEDD5),

    // Tertiary - Teal for accents
    tertiary = Color(0xFF7CF4E7),
    onTertiary = Color(0xFF00201E),
    tertiaryContainer = Color(0xFF0B3E3A),
    onTertiaryContainer = Color(0xFFB7FFF6),

    // Background and Surface - Dark theme
    background = AppColors.BackgroundDark,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.SurfaceDark,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    inverseSurface = AppColors.SurfaceElevated,
    inverseOnSurface = AppColors.TextPrimary,

    // Error colors
    error = AppColors.StatusError,
    onError = Color.White,
    errorContainer = AppColors.StatusErrorBackground,
    onErrorContainer = AppColors.StatusError,

    // Outlines
    outline = AppColors.BorderDefault,
    outlineVariant = AppColors.BorderLight,
    scrim = Color.Black
)

@Composable
fun GenAiVideoTheme(
    darkTheme: Boolean = true, // Force dark mode only
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors, // Always use dark colors
        typography = AppTypography,
        content = content
    )
}
