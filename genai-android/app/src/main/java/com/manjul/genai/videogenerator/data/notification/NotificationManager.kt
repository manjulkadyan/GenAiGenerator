package com.manjul.genai.videogenerator.data.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object NotificationManager {
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val KEY_UNREAD_NOTIFICATION_COUNT = "unread_notification_count"
    
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
}

