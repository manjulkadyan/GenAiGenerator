package com.manjul.genai.videogenerator.data.repository.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.local.AIModelCacheEntity
import com.manjul.genai.videogenerator.data.model.AIModel

class RoomModelDataSource(
    private val context: Context
) : LocalModelDataSource {
    private val gson = Gson()
    private val cacheKey = "models_cache"
    private val cacheValidityHours = 24L
    
    private val database by lazy {
        AppDatabase.getDatabase(context)
    }
    
    private val cacheDao by lazy {
        database.aiModelCacheDao()
    }
    
    override suspend fun getCachedModels(): List<AIModel>? {
        return try {
            val cacheEntry = cacheDao.getCacheEntry(cacheKey) ?: return null
            
            // Check if cache is still valid (less than 24 hours old)
            val cacheAge = System.currentTimeMillis() - cacheEntry.cachedAt
            val cacheValidityMs = cacheValidityHours * 60 * 60 * 1000
            if (cacheAge > cacheValidityMs) {
                // Cache expired - delete it
                cacheDao.deleteCache(cacheKey)
                return null
            }
            
            // Parse cached models
            val type = object : TypeToken<List<AIModel>>() {}.type
            gson.fromJson<List<AIModel>>(cacheEntry.modelsJson, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("RoomModelDataSource", "Failed to parse cached models", e)
            // Delete corrupted cache
            try {
                cacheDao.deleteCache(cacheKey)
            } catch (deleteError: Exception) {
                android.util.Log.e("RoomModelDataSource", "Failed to delete corrupted cache", deleteError)
            }
            null
        }
    }
    
    override suspend fun saveModels(models: List<AIModel>) {
        try {
            val json = gson.toJson(models)
            val cacheEntity = AIModelCacheEntity(
                cacheKey = cacheKey,
                modelsJson = json,
                cachedAt = System.currentTimeMillis()
            )
            cacheDao.insertOrUpdate(cacheEntity)
        } catch (e: Exception) {
            android.util.Log.e("RoomModelDataSource", "Failed to save models to cache", e)
        }
    }
    
    override suspend fun clearCache() {
        try {
            cacheDao.deleteCache(cacheKey)
        } catch (e: Exception) {
            android.util.Log.e("RoomModelDataSource", "Failed to clear cache", e)
        }
    }
    
    override suspend fun deleteOldCache(timestamp: Long) {
        try {
            cacheDao.deleteOldCache(timestamp)
        } catch (e: Exception) {
            android.util.Log.e("RoomModelDataSource", "Failed to delete old cache", e)
        }
    }
}

