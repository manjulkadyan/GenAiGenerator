package com.manjul.genai.videogenerator.data.local

import com.manjul.genai.videogenerator.data.model.VideoJob
import java.time.Instant
import java.time.ZoneId

/**
 * Extension functions to convert between VideoJob and VideoJobEntity
 */

fun VideoJob.toEntity(): VideoJobEntity {
    return VideoJobEntity(
        id = id,
        prompt = prompt,
        negativePrompt = negativePrompt,
        modelId = modelId,
        modelName = modelName,
        durationSeconds = durationSeconds,
        aspectRatio = aspectRatio,
        enableAudio = enableAudio,
        firstFrameUri = firstFrameUri,
        lastFrameUri = lastFrameUri,
        seed = seed,
        status = status,
        previewUrl = previewUrl,
        storageUrl = storageUrl,
        localFilePath = null, // Will be set when video is downloaded
        errorMessage = errorMessage,
        replicatePredictionId = replicatePredictionId,
        cost = cost,
        createdAt = createdAt.toEpochMilli(),
        completedAt = completedAt?.toEpochMilli(),
        failedAt = failedAt?.toEpochMilli()
    )
}

fun VideoJobEntity.toVideoJob(): VideoJob {
    return VideoJob(
        id = id,
        prompt = prompt,
        modelName = modelName,
        durationSeconds = durationSeconds,
        aspectRatio = aspectRatio,
        status = status,
        previewUrl = previewUrl,
        createdAt = Instant.ofEpochMilli(createdAt),
        storageUrl = storageUrl,
        errorMessage = errorMessage,
        replicatePredictionId = replicatePredictionId,
        completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
        failedAt = failedAt?.let { Instant.ofEpochMilli(it) },
        cost = cost,
        modelId = modelId,
        negativePrompt = negativePrompt,
        enableAudio = enableAudio,
        firstFrameUri = firstFrameUri,
        lastFrameUri = lastFrameUri,
        seed = seed
    )
}

