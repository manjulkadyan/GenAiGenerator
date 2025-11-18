package com.manjul.genai.videogenerator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AIModelCacheDao {
    @Query("SELECT * FROM ai_models_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCacheEntry(key: String = "models_cache"): AIModelCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: AIModelCacheEntity)
    
    @Query("DELETE FROM ai_models_cache WHERE cacheKey = :key")
    suspend fun deleteCache(key: String = "models_cache")
    
    @Query("DELETE FROM ai_models_cache WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}

