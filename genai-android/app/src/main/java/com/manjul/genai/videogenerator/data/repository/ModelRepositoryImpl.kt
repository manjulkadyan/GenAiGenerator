package com.manjul.genai.videogenerator.data.repository

import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.repository.datasource.LocalModelDataSource
import com.manjul.genai.videogenerator.data.repository.datasource.RemoteModelDataSource

/**
 * Implementation of ModelRepository.
 * Handles caching logic: checks local cache first, then fetches from remote, then saves to cache.
 */
class ModelRepositoryImpl(
    private val localDataSource: LocalModelDataSource,
    private val remoteDataSource: RemoteModelDataSource
) : ModelRepository {
    
    override suspend fun fetchModels(): List<AIModel> {
        // Try to load from cache first
        val cachedModels = localDataSource.getCachedModels()
        if (cachedModels != null && cachedModels.isNotEmpty()) {
            return cachedModels
        }
        
        // If cache is invalid or missing, fetch from remote
        val models = remoteDataSource.fetchModels()
        
        // Save to cache for next time
        if (models.isNotEmpty()) {
            localDataSource.saveModels(models)
        }
        
        return models
    }
}

