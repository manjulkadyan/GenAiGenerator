package com.manjul.genai.videogenerator.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
class ResultsActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_JOB_ID = "job_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val jobId = intent.getStringExtra(EXTRA_JOB_ID)
        if (jobId == null) {
            finish()
            return
        }
        
        // Load job from Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            android.util.Log.e("ResultsActivity", "User not authenticated")
            finish()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Jobs are stored in users/{userId}/jobs collection
                val jobDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("jobs")
                    .document(jobId)
                    .get()
                    .await()
                
                if (jobDoc.exists()) {
                    // Firestore uses snake_case field names
                    val prompt = jobDoc.getString("prompt") ?: return@launch
                    val modelName = jobDoc.getString("model_name") ?: ""
                    val durationSeconds = jobDoc.getLong("duration_seconds")?.toInt() ?: 5
                    val aspectRatio = jobDoc.getString("aspect_ratio") ?: "16:9"
                    val statusRaw = jobDoc.getString("status") ?: "PROCESSING"
                    val storageUrl = jobDoc.getString("storage_url")
                    val previewUrl = jobDoc.getString("preview_url")
                    val modelId = jobDoc.getString("model_id")
                    val negativePrompt = jobDoc.getString("negative_prompt")
                    val cost = jobDoc.getLong("cost")?.toInt() ?: 0
                    val createdAt = (jobDoc.getTimestamp("created_at") ?: Timestamp.now())
                        .toDate()
                        .toInstant()
                    val completedAt = jobDoc.getTimestamp("completed_at")?.toDate()?.toInstant()
                    
                    val job = VideoJob(
                        id = jobDoc.id,
                        modelId = modelId,
                        modelName = modelName,
                        prompt = prompt,
                        negativePrompt = negativePrompt,
                        durationSeconds = durationSeconds,
                        aspectRatio = aspectRatio,
                        cost = cost,
                        status = VideoJobStatus.valueOf(statusRaw.uppercase()),
                        storageUrl = storageUrl,
                        previewUrl = previewUrl,
                        createdAt = createdAt,
                        completedAt = completedAt
                    )
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        setContent {
                            GenAiVideoTheme {
                                ResultsScreenDialog(
                                    job = job,
                                    onClose = { finish() },
                                    onRegenerate = {
                                        // Navigate to Generate screen with same prompt
                                        val intent = Intent(this@ResultsActivity, com.manjul.genai.videogenerator.MainActivity::class.java).apply {
                                            putExtra("preselected_prompt", job.prompt)
                                        }
                                        startActivity(intent)
                                        finish()
                                    },
                                    onDelete = {
                                        // Delete job from Firestore
                                        CoroutineScope(Dispatchers.IO).launch {
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(userId)
                                                .collection("jobs")
                                                .document(jobId)
                                                .delete()
                                                .await()
                                        }
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    finish()
                }
            } catch (e: Exception) {
                android.util.Log.e("ResultsActivity", "Failed to load job", e)
                finish()
            }
        }
    }
}

