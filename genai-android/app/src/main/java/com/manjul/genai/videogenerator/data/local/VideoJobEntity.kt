package com.manjul.genai.videogenerator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import java.time.Instant

/**
 * Room entity to store all video generation parameters and metadata
 * This allows us to regenerate videos with the same parameters
 */
@Entity(tableName = "video_jobs")
data class VideoJobEntity(
    @PrimaryKey
    val id: String, // Replicate prediction ID
    val prompt: String,
    val negativePrompt: String? = null,
    val modelId: String?,
    val modelName: String,
    val durationSeconds: Int,
    val aspectRatio: String,
    val enableAudio: Boolean = false,
    val firstFrameUri: String? = null, // URI as string
    val lastFrameUri: String? = null, // URI as string
    val seed: Int? = null,
    val status: VideoJobStatus,
    val previewUrl: String? = null,
    val storageUrl: String? = null,
    val localFilePath: String? = null, // Local file path after download
    val errorMessage: String? = null,
    val replicatePredictionId: String? = null,
    val cost: Int = 0,
    val createdAt: Long = System.currentTimeMillis(), // Stored as timestamp
    val completedAt: Long? = null,
    val failedAt: Long? = null
)

