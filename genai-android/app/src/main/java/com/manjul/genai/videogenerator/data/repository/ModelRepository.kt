package com.manjul.genai.videogenerator.data.repository

import com.manjul.genai.videogenerator.data.model.AIModel

/**
 * Repository interface for AI models.
 * Abstracts the data source (Firebase + Room DB cache).
 */
interface ModelRepository {
    suspend fun fetchModels(): List<AIModel>
}

