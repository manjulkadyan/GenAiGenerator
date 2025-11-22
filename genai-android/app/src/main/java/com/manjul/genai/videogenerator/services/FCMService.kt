package com.manjul.genai.videogenerator.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.manjul.genai.videogenerator.data.notification.NotificationManager

class FCMService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Save token to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            NotificationManager.saveFCMTokenToFirestore()
        }
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "=== FCM Message Received ===")
        Log.d(TAG, "Message ID: ${message.messageId}")
        
        // Handle notification
        message.notification?.let { notification ->
            Log.d(TAG, "Notification - Title: ${notification.title}, Body: ${notification.body}")
            // Firebase will show the notification automatically
        }
        
        // Handle data payload
        val data = message.data
        Log.d(TAG, "Data payload: $data")
        
        val type = data["type"]
        val jobId = data["job_id"]
        val videoUrl = data["video_url"]
        
        if (type == "video_complete" && jobId != null) {
            Log.d(TAG, "✅ Video complete notification received for job: $jobId")
            Log.d(TAG, "Video URL: $videoUrl")
            
            // Increment unread notification count (badge will update automatically)
            val oldCount = NotificationManager.getUnreadNotificationCount(this)
            NotificationManager.incrementUnreadCount(this)
            val newCount = NotificationManager.getUnreadNotificationCount(this)
            Log.d(TAG, "Badge count updated: $oldCount -> $newCount")
            
            // Show local notification that opens HistoryScreen when clicked
            val notificationTitle = message.notification?.title ?: "Video Ready!"
            val notificationBody = message.notification?.body ?: "Your video generation is complete."
            NotificationManager.showVideoCompleteNotification(
                context = this,
                title = notificationTitle,
                message = notificationBody,
                jobId = jobId
            )
            Log.d(TAG, "Local notification shown for job: $jobId")
            
            // The badge in GenAiRoot will automatically update via polling
            // The app will also update when Firestore listener detects the job status change
        } else {
            Log.d(TAG, "Unknown notification type: $type")
        }
        Log.d(TAG, "=== FCM Message Processing Complete ===")
    }
}

