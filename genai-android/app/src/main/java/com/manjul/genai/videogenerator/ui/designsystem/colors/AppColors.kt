package com.manjul.genai.videogenerator.ui.designsystem.colors

import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for the design system
 * Matches the reference design with purple accents and dark theme
 */
object AppColors {
    // Primary Colors - Purple accent (matching reference images)
    val PrimaryPurple = Color(0xFF6C5CE7) // Main purple for selected states
    val PrimaryPurpleLight = Color(0xFF9B8AFF)
    val PrimaryPurpleDark = Color(0xFF4A3DB8)
    val OnPrimaryPurple = Color.White

    // Background Colors - Dark theme
    val BackgroundDark = Color(0xFF000000) // Pure black background
    val BackgroundDarkGray = Color(0xFF0C0D15) // Slightly lighter dark gray
    val SurfaceDark = Color(0xFF141524) // Card surface
    val SurfaceElevated = Color(0xFF1D1E2F) // Elevated surfaces

    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF) // White for primary text
    val TextSecondary = Color(0xFFBCB7D0) // Light gray for secondary text
    val TextTertiary = Color(0xFF8E8E93) // Muted gray

    // Selection States
    val SelectedBackground = PrimaryPurple
    val SelectedText = OnPrimaryPurple
    val UnselectedBackground = Color(0xFF1D1E2F) // Dark gray
    val UnselectedText = TextSecondary
    val UnselectedBorder = Color(0xFF2C2D3F) // Soft outline

    // Status Colors
    val StatusSuccess = Color(0xFF10B981) // Green
    val StatusError = Color(0xFFEF4444) // Red
    val StatusWarning = Color(0xFFF59E0B) // Amber
    val StatusInfo = Color(0xFF3B82F6) // Blue

    // Status Background Colors (with alpha)
    val StatusSuccessBackground = StatusSuccess.copy(alpha = 0.1f)
    val StatusErrorBackground = StatusError.copy(alpha = 0.1f)
    val StatusWarningBackground = StatusWarning.copy(alpha = 0.1f)
    val StatusInfoBackground = StatusInfo.copy(alpha = 0.1f)

    // Border Colors
    val BorderDefault = Color(0xFF2C2D3F)
    val BorderLight = Color(0xFF2C2D3F).copy(alpha = 0.3f)
    val BorderSelected = PrimaryPurple

    // Button Colors
    val ButtonPrimary = Color.White // White button like "Continue" in reference
    val ButtonPrimaryText = Color.Black
    val ButtonSecondary = SurfaceDark
    val ButtonSecondaryText = TextPrimary
    val ButtonDisabled = SurfaceDark.copy(alpha = 0.5f)
    val ButtonDisabledText = TextTertiary

    // Card Colors
    val CardBackground = SurfaceDark
    val CardBackgroundElevated = SurfaceElevated
    val CardBorder = BorderLight

    // Badge Colors
    val BadgeRequired = StatusError.copy(alpha = 0.2f)
    val BadgeRequiredText = StatusError
    val BadgeOptional = StatusInfo.copy(alpha = 0.2f)
    val BadgeOptionalText = StatusInfo
}
