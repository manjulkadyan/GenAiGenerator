package com.manjul.genai.videogenerator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for caching video preview metadata and playback state
 */
@Entity(tableName = "video_cache")
data class VideoCacheEntity(
    @PrimaryKey
    val videoUrl: String,
    val modelId: String,
    val modelName: String,
    val lastPlayedPosition: Long = 0L, // Last playback position in milliseconds
    val isCached: Boolean = false, // Whether video is fully cached by ExoPlayer
    val cacheSize: Long = 0L, // Size of cached video in bytes
    val lastAccessed: Long = System.currentTimeMillis(), // Last time this video was accessed
    val accessCount: Int = 1 // Number of times this video has been accessed
)

