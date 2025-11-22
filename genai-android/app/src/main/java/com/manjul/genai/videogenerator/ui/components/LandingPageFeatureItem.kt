package com.manjul.genai.videogenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Feature item component for landing page.
 * Displays an icon, title, and description with purple accent color.
 */
@Composable
fun LandingPageFeatureItem(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Purple icon - exact match from design
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF4B3FFF) // Exact purple from design
        )
        
        // Title and description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF), // Gray-400
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Get icon for feature based on icon identifier string.
 */
@Composable
fun getFeatureIcon(iconId: String): ImageVector {
    return when (iconId.lowercase()) {
        "gear", "settings" -> Icons.Default.Settings
        "flame", "fire" -> Icons.Default.LocalFireDepartment
        "sound", "audio", "wave" -> Icons.Default.GraphicEq
        "house", "home", "building" -> Icons.Default.Home
        "screen", "display" -> Icons.Default.SmartDisplay
        "quote", "prompt" -> Icons.Default.FormatQuote
        else -> Icons.Default.Settings // Default icon
    }
}

