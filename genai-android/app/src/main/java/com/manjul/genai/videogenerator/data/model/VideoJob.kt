package com.manjul.genai.videogenerator.data.model

import java.time.Instant

data class VideoJob(
    val id: String,
    val prompt: String,
    val modelName: String,
    val durationSeconds: Int,
    val aspectRatio: String,
    val status: VideoJobStatus,
    val previewUrl: String?,
    val createdAt: Instant,
    // Additional fields from Firestore
    val storageUrl: String? = null,
    val errorMessage: String? = null,
    val replicatePredictionId: String? = null,
    val completedAt: Instant? = null,
    val failedAt: Instant? = null,
    val cost: Int = 0,
    val modelId: String? = null,
)

enum class VideoJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETE,
    FAILED,
}
