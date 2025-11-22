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
        Log.d(TAG, "Message received: ${message.messageId}")
        
        // Handle notification
        message.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}, body: ${notification.body}")
            // You can show a custom notification here if needed
            // For now, Firebase will show the notification automatically
        }
        
        // Handle data payload
        message.data.let { data ->
            Log.d(TAG, "Data payload: $data")
            val type = data["type"]
            val jobId = data["job_id"]
            val videoUrl = data["video_url"]
            
            if (type == "video_complete" && jobId != null) {
                Log.d(TAG, "Video complete notification for job: $jobId")
                // The app will automatically update when Firestore listener detects the change
            }
        }
    }
}

