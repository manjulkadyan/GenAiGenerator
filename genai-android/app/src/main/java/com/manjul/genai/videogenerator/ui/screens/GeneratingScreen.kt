package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.ui.components.NotificationPermissionDialog
import com.manjul.genai.videogenerator.data.notification.NotificationManager

@Composable
fun GeneratingScreen(
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    errorMessage: String? = null,
    onCancel: () -> Unit = {},
    onRetry: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showNotificationDialog by remember { mutableStateOf(false) }
    var hasCheckedPermission by remember { mutableStateOf(false) }
    
    // Show notification dialog on first appearance (if not already asked)
    // Use a key to ensure this only runs once per screen appearance
    LaunchedEffect(hasCheckedPermission) {
        if (!hasCheckedPermission) {
            hasCheckedPermission = true
            // Check if we should show the dialog
            if (!NotificationManager.hasAskedForPermission(context) && 
                !NotificationManager.isNotificationEnabled(context)) {
                // Small delay to let the generating screen appear first
                kotlinx.coroutines.delay(500)
                showNotificationDialog = true
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show error state or normal generating state
            if (errorMessage != null) {
                // Error state
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.size(24.dp))
                
                Text(
                    text = "Generation Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.size(16.dp))
                
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.size(24.dp))
                
                // Retry button if available
                if (onRetry != null) {
                    AppTextButton(
                        text = "Retry",
                        onClick = onRetry
                    )
                }
            } else {
                // Normal generating state
                // Animated sparkles
                SparkleAnimation()
                
                Spacer(modifier = Modifier.size(32.dp))
                
                // Progress text
                Text(
                    text = "Generating Video...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.size(16.dp))
                
                // Status message (e.g., "Uploading first frame...", "Submitting generation request...")
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                
                // Subtitle
                Text(
                    text = "This will only take a moment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.size(4.dp))
                
                Text(
                    text = "Please wait..",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Notification Permission Dialog - Show on top
        if (showNotificationDialog) {
            NotificationPermissionDialog(
                onDismiss = {
                    showNotificationDialog = false
                    NotificationManager.setPermissionAsked(context)
                },
                onPermissionGranted = {
                    showNotificationDialog = false
                    NotificationManager.setPermissionAsked(context)
                }
            )
        }
    }
}

@Composable
private fun SparkleAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    
    // First sparkle (large, left)
    val sparkle1Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle1_rotation"
    )
    
    val sparkle1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle1_alpha"
    )
    
    // Second sparkle (medium, top right)
    val sparkle2Rotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle2_rotation"
    )
    
    val sparkle2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle2_alpha"
    )
    
    // Third sparkle (small, bottom right)
    val sparkle3Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle3_rotation"
    )
    
    val sparkle3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle3_alpha"
    )
    
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Large sparkle (left)
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha(sparkle1Alpha)
                .rotate(sparkle1Rotation),
            tint = MaterialTheme.colorScheme.primary
        )
        
        // Medium sparkle (top right)
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .alpha(sparkle2Alpha)
                .rotate(sparkle2Rotation)
                .align(Alignment.TopEnd),
            tint = MaterialTheme.colorScheme.secondary
        )
        
        // Small sparkle (bottom right)
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .alpha(sparkle3Alpha)
                .rotate(sparkle3Rotation)
                .align(Alignment.BottomEnd),
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

// ==================== Previews ====================

@Preview(
    name = "Generating Screen",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun GeneratingScreenPreview() {
    GenAiVideoTheme {
        GeneratingScreen(
            statusMessage = "Uploading first frame..."
        )
    }
}

@Preview(
    name = "Generating Screen - Error",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun GeneratingScreenErrorPreview() {
    GenAiVideoTheme {
        GeneratingScreen(
            errorMessage = "Failed to generate video. Please try again.",
            onRetry = {}
        )
    }
}

