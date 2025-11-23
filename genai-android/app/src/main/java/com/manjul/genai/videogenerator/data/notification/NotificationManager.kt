package com.manjul.genai.videogenerator.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.manjul.genai.videogenerator.MainActivity
import com.manjul.genai.videogenerator.R
import kotlinx.coroutines.tasks.await

object NotificationManager {
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val KEY_UNREAD_NOTIFICATION_COUNT = "unread_notification_count"
    
    // Notification channel constants
    private const val CHANNEL_ID = "video_complete_channel"
    private const val CHANNEL_NAME = "Video Completion Notifications"
    private const val NOTIFICATION_ID_BASE = 1000
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if we've already asked for notification permission
     */
    fun hasAskedForPermission(context: Context): Boolean {
        val result = getSharedPreferences(context).getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)
        android.util.Log.d("NotificationManager", "hasAskedForPermission: $result")
        return result
    }
    
    /**
     * Mark that we've asked for notification permission
     */
    fun setPermissionAsked(context: Context) {
        android.util.Log.d("NotificationManager", "setPermissionAsked called")
        getSharedPreferences(context).edit()
            .putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true)
            .apply()
        android.util.Log.d("NotificationManager", "Permission asked flag saved")
    }
    
    /**
     * Check if notifications are enabled
     */
    fun isNotificationEnabled(context: Context): Boolean {
        val result = getSharedPreferences(context).getBoolean(KEY_NOTIFICATION_ENABLED, false)
        android.util.Log.d("NotificationManager", "isNotificationEnabled: $result")
        return result
    }
    
    /**
     * Set notification enabled status
     */
    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        android.util.Log.d("NotificationManager", "setNotificationEnabled: $enabled")
        getSharedPreferences(context).edit()
            .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            .apply()
        android.util.Log.d("NotificationManager", "Notification enabled flag saved: $enabled")
    }
    
    /**
     * Get FCM token and save it to Firestore
     */
    suspend fun saveFCMTokenToFirestore(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
            
            // Use set with merge to create document if it doesn't exist
            userRef.set(mapOf("fcm_token" to token), SetOptions.merge())
                .await()
            
            android.util.Log.d("NotificationManager", "FCM token saved for user $userId")
            Result.success(token)
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to save FCM token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Request FCM token and save it (call this after user grants permission)
     */
    suspend fun enableNotifications(): Result<String> {
        return try {
            val token = saveFCMTokenToFirestore()
            token.onSuccess {
                android.util.Log.d("NotificationManager", "Notifications enabled")
            }
            token
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Failed to enable notifications", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get unread notification count
     */
    fun getUnreadNotificationCount(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_UNREAD_NOTIFICATION_COUNT, 0)
    }
    
    /**
     * Increment unread notification count (call when notification is received)
     */
    fun incrementUnreadCount(context: Context) {
        val current = getUnreadNotificationCount(context)
        getSharedPreferences(context).edit()
            .putInt(KEY_UNREAD_NOTIFICATION_COUNT, current + 1)
            .apply()
        android.util.Log.d("NotificationManager", "Unread count incremented to: ${current + 1}")
    }
    
    /**
     * Clear unread notification count (call when History screen is viewed)
     */
    fun clearUnreadCount(context: Context) {
        getSharedPreferences(context).edit()
            .putInt(KEY_UNREAD_NOTIFICATION_COUNT, 0)
            .apply()
        android.util.Log.d("NotificationManager", "Unread count cleared")
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Check if channel already exists
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH shows heads-up notifications
                ).apply {
                    description = "Notifications for completed video generations"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    // Set sound to default
                    setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
                }
                notificationManager.createNotificationChannel(channel)
                android.util.Log.d("NotificationManager", "Notification channel created with IMPORTANCE_HIGH")
            } else {
                android.util.Log.d("NotificationManager", "Notification channel already exists with importance: ${existingChannel.importance}")
            }
        }
    }
    
    /**
     * Check if notifications are enabled for the app
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.areNotificationsEnabled()
        } else {
            // For older versions, assume enabled
            true
        }
    }
    
    /**
     * Check if the notification channel is enabled
     */
    fun isNotificationChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }
    
    /**
     * Show local notification that opens HistoryScreen when clicked
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param jobId Optional job ID for tracking
     */
    fun showVideoCompleteNotification(
        context: Context,
        title: String,
        message: String,
        jobId: String? = null
    ) {
        android.util.Log.d("NotificationManager", "=== Attempting to show notification ===")
        android.util.Log.d("NotificationManager", "Title: $title, Message: $message, JobId: $jobId")
        
        // Check if notifications are enabled
        val notificationsEnabled = areNotificationsEnabled(context)
        android.util.Log.d("NotificationManager", "Notifications enabled: $notificationsEnabled")
        
        if (!notificationsEnabled) {
            android.util.Log.w("NotificationManager", "Notifications are disabled for this app")
            return
        }
        
        // Create notification channel if needed
        createNotificationChannel(context)
        
        // Check if channel is enabled
        val channelEnabled = isNotificationChannelEnabled(context)
        android.util.Log.d("NotificationManager", "Notification channel enabled: $channelEnabled")
        
        if (!channelEnabled) {
            android.util.Log.w("NotificationManager", "Notification channel is disabled")
            return
        }
        
        // Create intent to open MainActivity and navigate to History
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "history")
            jobId?.let { putExtra("job_id", it) }
        }
        
        // Create pending intent with FLAG_IMMUTABLE for Android 12+
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            jobId?.hashCode() ?: System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlags
        )
        
        // Build notification
        // Use a reliable system icon - ic_dialog_info is more reliable than ic_media_play
        // For production, you should create a custom notification icon (white icon on transparent background)
        val smallIcon = R.mipmap.ic_launcher
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setFullScreenIntent(pendingIntent, false) // This helps show notification even in foreground
            .build()
        
        // Show notification using NotificationManagerCompat for better compatibility
        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            val notificationId = jobId?.hashCode() ?: (NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt())
            
            // Check if we can show notifications
            if (notificationManagerCompat.areNotificationsEnabled()) {
                // Use notify() which should show the notification even in foreground
                notificationManagerCompat.notify(notificationId, notification)
                android.util.Log.d("NotificationManager", "✅ Notification posted with ID: $notificationId")
                
                // Verify notification was actually posted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNotifications = notificationManagerCompat.activeNotifications
                    val wasPosted = activeNotifications.any { it.id == notificationId }
                    android.util.Log.d("NotificationManager", "Notification verification: posted=$wasPosted, active count=${activeNotifications.size}")
                }
            } else {
                android.util.Log.w("NotificationManager", "❌ Cannot show notification - notifications disabled")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationManager", "❌ SecurityException when showing notification", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "❌ Error showing notification", e)
        }
        
        android.util.Log.d("NotificationManager", "=== Notification attempt complete ===")
    }
}

