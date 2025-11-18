package com.manjul.genai.videogenerator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_models_cache")
data class AIModelCacheEntity(
    @PrimaryKey
    val cacheKey: String = "models_cache", // Single entry for all models
    val modelsJson: String, // Serialized List<AIModel> as JSON
    val cachedAt: Long = System.currentTimeMillis() // Timestamp when cached
)

