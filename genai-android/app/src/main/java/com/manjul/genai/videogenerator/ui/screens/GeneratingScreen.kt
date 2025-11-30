package com.manjul.genai.videogenerator.ui.screens

import android.util.Log
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
import com.manjul.genai.videogenerator.utils.AnalyticsManager

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
    
    Log.d("GeneratingScreen", "=== GeneratingScreen Composed ===")
    Log.d("GeneratingScreen", "statusMessage: $statusMessage")
    Log.d("GeneratingScreen", "errorMessage: $errorMessage")
    Log.d("GeneratingScreen", "showNotificationDialog initial: $showNotificationDialog")
    
    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("Generating")
    }
    
    // Show notification dialog on first appearance if notifications are not enabled
    // Use Unit as key to ensure this only runs once per screen composition
    LaunchedEffect(Unit) {
        Log.d("GeneratingScreen", "=== LaunchedEffect Started ===")
        
        // Check if we should show the dialog
        // Only show if we haven't asked before AND notifications are not enabled
        // This prevents showing the dialog repeatedly if user dismissed it
        val isEnabled = NotificationManager.isNotificationEnabled(context)
        val hasAsked = NotificationManager.hasAskedForPermission(context)
        
        Log.d("GeneratingScreen", "Permission check results:")
        Log.d("GeneratingScreen", "  - hasAskedForPermission: $hasAsked")
        Log.d("GeneratingScreen", "  - isNotificationEnabled: $isEnabled")
        Log.d("GeneratingScreen", "  - Should show dialog: ${!hasAsked && !isEnabled}")
        
        // Show dialog only if we haven't asked before AND notifications are not enabled
        // This ensures the dialog shows once per user, not on every generation
        if (!hasAsked && !isEnabled) {
            Log.d("GeneratingScreen", "First time - notifications not enabled. Will show dialog after delay...")
            // Small delay to let the generating screen appear first
            kotlinx.coroutines.delay(500)
            Log.d("GeneratingScreen", "Delay complete. Setting showNotificationDialog = true")
            showNotificationDialog = true
            Log.d("GeneratingScreen", "showNotificationDialog set to: $showNotificationDialog")
        } else {
            if (hasAsked) {
                Log.d("GeneratingScreen", "Skipping notification dialog: Already asked before")
            }
            if (isEnabled) {
                Log.d("GeneratingScreen", "Skipping notification dialog: Notifications already enabled")
            }
        }
        Log.d("GeneratingScreen", "=== LaunchedEffect Completed ===")
    }
    
    // Log when showNotificationDialog changes
    LaunchedEffect(showNotificationDialog) {
        Log.d("GeneratingScreen", "showNotificationDialog changed to: $showNotificationDialog")
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
                
                // Status message with better formatting and emojis for clarity
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }
                
                // Subtitle - context-aware based on status with helpful tips
                val subtitleText = when {
                    statusMessage?.contains("Uploading", ignoreCase = true) == true -> 
                        "Preparing your reference images..."
                    statusMessage?.contains("Submitting", ignoreCase = true) == true ->
                        "Checking credits and queuing your request..."
                    statusMessage?.contains("AI is generating", ignoreCase = true) == true || 
                    statusMessage?.contains("Processing", ignoreCase = true) == true ->
                        "Your video is being created by AI. You'll get a notification when ready!"
                    else -> "This will only take a moment."
                }
                
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
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
            Log.d("GeneratingScreen", "Rendering NotificationPermissionDialog")
            NotificationPermissionDialog(
                onDismiss = {
                    Log.d("GeneratingScreen", "NotificationPermissionDialog onDismiss called")
                    showNotificationDialog = false
                    NotificationManager.setPermissionAsked(context)
                    Log.d("GeneratingScreen", "Permission asked flag set")
                },
                onPermissionGranted = {
                    Log.d("GeneratingScreen", "NotificationPermissionDialog onPermissionGranted called")
                    showNotificationDialog = false
                    NotificationManager.setPermissionAsked(context)
                    Log.d("GeneratingScreen", "Permission granted and flag set")
                }
            )
        } else {
            Log.d("GeneratingScreen", "NotificationPermissionDialog NOT rendered (showNotificationDialog = false)")
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

