package com.manjul.genai.videogenerator.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object VideoPreviewCache {
    // Increased cache size to prevent rebuffering - 200 MB for better caching
    private const val CACHE_SIZE_BYTES = 200L * 1024 * 1024 // 200 MB
    @Volatile
    private var cache: Cache? = null

    @OptIn(UnstableApi::class)
    fun get(context: Context): Cache {
        val appContext = context.applicationContext
        return cache ?: synchronized(this) {
            cache ?: run {
                // Use filesDir instead of cacheDir for more persistent storage
                // filesDir persists even when cache is cleared (only cleared on app uninstall)
                // This is better for video caching that should persist across app launches
                val cacheDir = File(appContext.filesDir, "video_cache")
                //Log.d("VideoPreviewCache", "Cache directory: ${cacheDir.absolutePath}")
                //Log.d("VideoPreviewCache", "Cache directory exists: ${cacheDir.exists()}")
                //Log.d("VideoPreviewCache", "Using filesDir (more persistent than cacheDir)")
                
                SimpleCache(
                    cacheDir,
                    LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
                    StandaloneDatabaseProvider(appContext)
                ).also { cache = it }
            }
        }
    }
    
    /**
     * Check if a video URL is cached
     * Returns true if the video exists in cache (simplified check)
     */
    @OptIn(UnstableApi::class)
    fun isCached(videoUrl: String, startPosition: Long, endPosition: Long): Boolean {
        return synchronized(this) {
            cache?.let {
                try {
                    // Check if the URL exists in cache keys
                    // ExoPlayer uses the URL as the cache key
                    val keys = it.keys
                    val isInCache = keys.any { key -> 
                        key == videoUrl || key.contains(videoUrl) || videoUrl.contains(key)
                    }
                    isInCache
                } catch (e: Exception) {
                    android.util.Log.e("VideoPreviewCache", "Error checking cache", e)
                    false
                }
            } ?: false
        }
    }
    
    /**
     * Clear cache to free memory if needed
     */
    @OptIn(UnstableApi::class)
    fun clearCache() {
        synchronized(this) {
            cache?.let {
                try {
                    // Clear all cached data by evicting everything
                    val keys = it.keys
                    keys.forEach { key ->
                        try {
                            it.removeResource(key)
                        } catch (e: Exception) {
                            // Ignore individual errors
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoPreviewCache", "Error clearing cache", e)
                }
            }
        }
    }
}
