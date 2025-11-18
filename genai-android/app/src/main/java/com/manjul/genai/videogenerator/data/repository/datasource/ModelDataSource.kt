package com.manjul.genai.videogenerator.data.repository.datasource

import com.manjul.genai.videogenerator.data.model.AIModel

/**
 * Local data source for AI models (Room DB cache)
 */
interface LocalModelDataSource {
    suspend fun getCachedModels(): List<AIModel>?
    suspend fun saveModels(models: List<AIModel>)
    suspend fun clearCache()
    suspend fun deleteOldCache(timestamp: Long)
}

/**
 * Remote data source for AI models (Firebase Firestore)
 */
interface RemoteModelDataSource {
    suspend fun fetchModels(): List<AIModel>
}

