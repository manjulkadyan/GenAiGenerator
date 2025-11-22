package com.manjul.genai.videogenerator.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.manjul.genai.videogenerator.data.notification.NotificationManager
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    android.util.Log.d("NotificationPermissionDialog", "=== NotificationPermissionDialog Composed ===")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSavingToken by remember { mutableStateOf(false) }
    
    // Track if permission was requested by user (to distinguish from initial state)
    var permissionRequested by remember { mutableStateOf(false) }
    
    android.util.Log.d("NotificationPermissionDialog", "Android version: ${Build.VERSION.SDK_INT}, TIRAMISU: ${Build.VERSION_CODES.TIRAMISU}")
    
    // Request notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        android.util.Log.d("NotificationPermissionDialog", "Android < 13, permission state is null")
        null
    }
    
    // Check if permission is already granted when dialog appears - auto-enable notifications
    LaunchedEffect(Unit) {
        if (notificationPermissionState != null) {
            val status = notificationPermissionState.status
            android.util.Log.d("NotificationPermissionDialog", "Initial permission status: $status")
            android.util.Log.d("NotificationPermissionDialog", "  - isGranted: ${status.isGranted}")
            android.util.Log.d("NotificationPermissionDialog", "  - shouldShowRationale: ${status.shouldShowRationale}")
            
            // If permission is already granted, automatically enable notifications and dismiss
            if (status.isGranted) {
                android.util.Log.d("NotificationPermissionDialog", "Permission already granted! Auto-enabling notifications...")
                isSavingToken = true
                val result = NotificationManager.enableNotifications()
                isSavingToken = false
                
                if (result.isSuccess) {
                    android.util.Log.d("NotificationPermissionDialog", "FCM token saved successfully (auto-enabled)")
                    NotificationManager.setNotificationEnabled(context, true)
                    onPermissionGranted()
                } else {
                    android.util.Log.e("NotificationPermissionDialog", "Failed to save FCM token (auto-enable): ${result.exceptionOrNull()}")
                }
                onDismiss()
            }
        } else {
            // Android < 13 - permission is granted by default
            android.util.Log.d("NotificationPermissionDialog", "Android < 13, auto-enabling notifications...")
            isSavingToken = true
            val result = NotificationManager.enableNotifications()
            isSavingToken = false
            
            if (result.isSuccess) {
                android.util.Log.d("NotificationPermissionDialog", "FCM token saved successfully (Android < 13)")
                NotificationManager.setNotificationEnabled(context, true)
                onPermissionGranted()
            } else {
                android.util.Log.e("NotificationPermissionDialog", "Failed to save FCM token (Android < 13): ${result.exceptionOrNull()}")
            }
            onDismiss()
        }
    }
    
    val handleEnableNotifications: () -> Unit = {
        android.util.Log.d("NotificationPermissionDialog", "handleEnableNotifications called")
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.util.Log.d("NotificationPermissionDialog", "Android 13+, requesting permission...")
                // Mark that user requested permission
                permissionRequested = true
                android.util.Log.d("NotificationPermissionDialog", "permissionRequested set to true")
                // Request permission first
                notificationPermissionState?.launchPermissionRequest()
                android.util.Log.d("NotificationPermissionDialog", "Permission request launched")
            } else {
                android.util.Log.d("NotificationPermissionDialog", "Android < 13, permission granted by default")
                // For Android 12 and below, permission is granted by default
                isSavingToken = true
                val result = NotificationManager.enableNotifications()
                isSavingToken = false
                
                if (result.isSuccess) {
                    android.util.Log.d("NotificationPermissionDialog", "FCM token saved for Android < 13")
                    NotificationManager.setNotificationEnabled(context, true)
                    onPermissionGranted()
                } else {
                    android.util.Log.e("NotificationPermissionDialog", "Failed to save FCM token for Android < 13")
                }
                onDismiss()
            }
        }
    }
    
    // Handle permission result - only react when user actually requests permission
    LaunchedEffect(notificationPermissionState?.status) {
        android.util.Log.d("NotificationPermissionDialog", "=== Permission Status LaunchedEffect Triggered ===")
        android.util.Log.d("NotificationPermissionDialog", "permissionRequested: $permissionRequested")
        
        val status = notificationPermissionState?.status
        android.util.Log.d("NotificationPermissionDialog", "Permission status: $status")
        
        if (status != null) {
            android.util.Log.d("NotificationPermissionDialog", "  - isGranted: ${status.isGranted}")
            android.util.Log.d("NotificationPermissionDialog", "  - shouldShowRationale: ${status.shouldShowRationale}")
        }
        
        // Only process if user actually requested permission (clicked "Enable Notifications")
        if (!permissionRequested) {
            android.util.Log.d("NotificationPermissionDialog", "Permission not requested by user yet, skipping")
            return@LaunchedEffect
        }
        
        android.util.Log.d("NotificationPermissionDialog", "Processing permission result...")
        
        if (status != null) {
            if (status.isGranted) {
                android.util.Log.d("NotificationPermissionDialog", "Permission granted! Saving FCM token...")
                // Permission granted by user, save FCM token
                isSavingToken = true
                val result = NotificationManager.enableNotifications()
                isSavingToken = false
                
                if (result.isSuccess) {
                    android.util.Log.d("NotificationPermissionDialog", "FCM token saved successfully")
                    NotificationManager.setNotificationEnabled(context, true)
                    onPermissionGranted()
                } else {
                    android.util.Log.e("NotificationPermissionDialog", "Failed to save FCM token: ${result.exceptionOrNull()}")
                }
                onDismiss()
            } else if (status.shouldShowRationale) {
                android.util.Log.d("NotificationPermissionDialog", "Permission denied, but can show rationale. Keeping dialog open.")
                // User denied, but we can show rationale
                // Keep dialog open - user can try again
                permissionRequested = false // Reset so they can try again
            } else {
                android.util.Log.d("NotificationPermissionDialog", "Permission permanently denied")
                // User permanently denied
                NotificationManager.setNotificationEnabled(context, false)
                // Don't auto-dismiss - let user dismiss manually with "Not Now"
            }
        }
        android.util.Log.d("NotificationPermissionDialog", "=== Permission Status LaunchedEffect Completed ===")
    }
    
    AppDialog(
        onDismissRequest = onDismiss,
        title = "Get Notified When Your Video is Ready"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(48.dp),
                    tint = AppColors.PrimaryPurple
                )
            }
            
            // Description
            Text(
                text = "Video generation can take 5-10 minutes. Enable notifications to get alerted when your video is ready!",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppSecondaryButton(
                    text = "Not Now",
                    onClick = {
                        NotificationManager.setPermissionAsked(context)
                        onDismiss()
                    },
                    fullWidth = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppPrimaryButton(
                    text = if (isSavingToken) "Enabling..." else "Enable Notifications",
                    onClick = handleEnableNotifications,
                    enabled = !isSavingToken,
                    isLoading = isSavingToken,
                    fullWidth = false
                )
            }
        }
    }
}

