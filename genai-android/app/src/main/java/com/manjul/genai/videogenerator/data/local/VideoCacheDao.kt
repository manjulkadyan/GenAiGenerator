package com.manjul.genai.videogenerator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoCacheDao {
    /**
     * Get cache entry for a video URL
     */
    @Query("SELECT * FROM video_cache WHERE videoUrl = :videoUrl LIMIT 1")
    suspend fun getCacheEntry(videoUrl: String): VideoCacheEntity?
    
    /**
     * Get all cache entries, ordered by last accessed (most recent first)
     */
    @Query("SELECT * FROM video_cache ORDER BY lastAccessed DESC")
    fun getAllCacheEntries(): Flow<List<VideoCacheEntity>>
    
    /**
     * Insert or update cache entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: VideoCacheEntity)
    
    /**
     * Update last accessed time and increment access count
     */
    @Query("UPDATE video_cache SET lastAccessed = :timestamp, accessCount = accessCount + 1 WHERE videoUrl = :videoUrl")
    suspend fun updateAccess(videoUrl: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update playback position
     */
    @Query("UPDATE video_cache SET lastPlayedPosition = :position WHERE videoUrl = :videoUrl")
    suspend fun updatePlaybackPosition(videoUrl: String, position: Long)
    
    /**
     * Update cache status
     */
    @Query("UPDATE video_cache SET isCached = :isCached, cacheSize = :cacheSize WHERE videoUrl = :videoUrl")
    suspend fun updateCacheStatus(videoUrl: String, isCached: Boolean, cacheSize: Long)
    
    /**
     * Delete old cache entries (older than specified timestamp)
     */
    @Query("DELETE FROM video_cache WHERE lastAccessed < :timestamp")
    suspend fun deleteOldEntries(timestamp: Long)
    
    /**
     * Delete cache entry for a specific video
     */
    @Query("DELETE FROM video_cache WHERE videoUrl = :videoUrl")
    suspend fun deleteEntry(videoUrl: String)
    
    /**
     * Get total cache size
     */
    @Query("SELECT SUM(cacheSize) FROM video_cache")
    suspend fun getTotalCacheSize(): Long?
    
    /**
     * Get count of cached videos
     */
    @Query("SELECT COUNT(*) FROM video_cache WHERE isCached = 1")
    suspend fun getCachedVideoCount(): Int
}

