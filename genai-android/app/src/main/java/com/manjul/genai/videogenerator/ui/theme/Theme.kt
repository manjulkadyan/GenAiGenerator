package com.manjul.genai.videogenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepSpace = Color(0xFF05060A)
private val Obsidian = Color(0xFF0C0D15)
private val Panel = Color(0xFF141524)
private val ElevatedPanel = Color(0xFF1D1E2F)
private val SoftOutline = Color(0xFF2C2D3F)
private val AccentPurple = Color(0xFFC6A5FF)
private val AccentPurpleDark = Color(0xFF502D7F)
private val AccentPurpleLight = Color(0xFFF3E8FF)
private val AmberGlow = Color(0xFFF6C177)
private val AmberContainer = Color(0xFF583516)
private val AmberOnContainer = Color(0xFFFFEDD5)
private val TealSpark = Color(0xFF7CF4E7)
private val TealContainer = Color(0xFF0B3E3A)
private val TealOnContainer = Color(0xFFB7FFF6)
private val CrimsonAlert = Color(0xFFFF8E8E)
private val CrimsonDark = Color(0xFF4F1010)
private val CrimsonLight = Color(0xFFFFE4E4)

private val DarkColors = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color(0xFF1F102F),
    primaryContainer = AccentPurpleDark,
    onPrimaryContainer = AccentPurpleLight,

    secondary = AmberGlow,
    onSecondary = Color(0xFF1B1206),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,

    tertiary = TealSpark,
    onTertiary = Color(0xFF00201E),
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealOnContainer,

    background = DeepSpace,
    onBackground = Color(0xFFE9E7F5),
    surface = Obsidian,
    onSurface = Color(0xFFF7F4FF),
    surfaceVariant = Panel,
    onSurfaceVariant = Color(0xFFBCB7D0),
    inverseSurface = ElevatedPanel,
    inverseOnSurface = Color(0xFFE5E1F0),

    error = CrimsonAlert,
    onError = Color(0xFF45070D),
    errorContainer = CrimsonDark,
    onErrorContainer = CrimsonLight,

    outline = SoftOutline,
    outlineVariant = ElevatedPanel,
    scrim = Color.Black
)

@Composable
fun GenAiVideoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
