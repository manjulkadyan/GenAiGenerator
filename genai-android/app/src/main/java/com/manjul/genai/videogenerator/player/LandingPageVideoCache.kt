package com.manjul.genai.videogenerator.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.local.VideoCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Pre-caches the landing page background video on app start.
 * Downloads video to ExoPlayer cache so it plays instantly without buffering.
 * Uses ExoPlayer's cache system (stored in cacheDir, not heap/stack).
 */
@OptIn(UnstableApi::class)
object LandingPageVideoCache {
    private const val TAG = "LandingPageVideoCache"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPrecaching = false
    private var precachePlayer: ExoPlayer? = null

    /**
     * Start pre-caching the landing page video.
     * Fetches video URL from Firestore and caches it in background.
     * Only caches if video is not already cached (persistent across app launches).
     */
    fun startPrecaching(context: Context) {
        if (isPrecaching) {
            Log.d(TAG, "Pre-caching already in progress")
            return
        }

        scope.launch {
            try {
                isPrecaching = true
                Log.d(TAG, "Starting landing page video pre-caching...")

                // Fetch video URL from Firestore
                val videoUrl = fetchVideoUrlFromFirestore()
                if (videoUrl.isNullOrEmpty()) {
                    Log.w(TAG, "No video URL found in Firestore, skipping pre-cache")
                    isPrecaching = false
                    return@launch
                }

                Log.d(TAG, "Video URL: $videoUrl")
                
                // Check Room DB first for cache status (more reliable than ExoPlayer cache check)
                val database = AppDatabase.getDatabase(context)
                val cacheEntry = database.videoCacheDao().getCacheEntry(videoUrl)
                
                // Check if video is already cached (using both Room DB and ExoPlayer cache)
                val isCachedInExoPlayer = isCachedSuspend(context, videoUrl)
                val isCachedInDB = cacheEntry?.isCached == true
                
                if (isCachedInDB && isCachedInExoPlayer) {
                    Log.d(TAG, "✅ Video already cached (verified in Room DB and ExoPlayer cache)")
                    Log.d(TAG, "Cache stored in filesDir - persists even when cache is cleared")
                    // Update access time
                    database.videoCacheDao().updateAccess(videoUrl)
                    isPrecaching = false
                    return@launch
                } else if (isCachedInDB && !isCachedInExoPlayer) {
                    // DB says cached but ExoPlayer cache missing - mark as not cached
                    Log.w(TAG, "Cache entry in DB but ExoPlayer cache missing - will re-cache")
                    database.videoCacheDao().updateCacheStatus(videoUrl, false, 0L)
                }
                
                Log.d(TAG, "Video not cached, starting pre-cache in background...")
                Log.d(TAG, "Cache will be stored in filesDir (persistent storage)")
                
                // Pre-cache the video (only if not already cached)
                precacheVideo(context, videoUrl)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error pre-caching landing page video", e)
            } finally {
                isPrecaching = false
            }
        }
    }

    /**
     * Fetch background video URL from Firestore.
     */
    private suspend fun fetchVideoUrlFromFirestore(): String? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("app")
                .document("landingPage")
                .get()
                .await()

            snapshot.getString("backgroundVideoUrl") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching video URL from Firestore", e)
            null
        }
    }

    /**
     * Pre-cache video using ExoPlayer's cache system.
     * This downloads the video to filesDir (not heap/stack) for instant playback.
     */
    @OptIn(UnstableApi::class)
    private suspend fun precacheVideo(context: Context, videoUrl: String) {
        try {
            val appContext = context.applicationContext
            
            // Get cache instance
            val cache = VideoPreviewCache.get(appContext)
            
            // Create HttpDataSource
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("GenAiVideoPlayer/1.0")
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(30_000)
            
            // Create CacheDataSource factory
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            
            // Create DataSource factory
            val dataSourceFactory = DefaultDataSource.Factory(appContext, cacheDataSourceFactory)
            
            // Create MediaItem
            val mediaItem = MediaItem.fromUri(videoUrl)
            
            // Create MediaSource based on URL type
            val mediaSource = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            
            // Create a temporary ExoPlayer instance for pre-caching
            val player = ExoPlayer.Builder(appContext)
                .build()
            
            precachePlayer = player
            
            // Set media source and prepare (this triggers caching)
            player.setMediaSource(mediaSource)
            player.prepare()
            
            // Wait for video to be ready (cached)
            var attempts = 0
            val maxAttempts = 60 // Wait up to 60 seconds
            
            while (player.playbackState != ExoPlayer.STATE_READY && attempts < maxAttempts) {
                Thread.sleep(1000) // Wait 1 second
                attempts++
                
                // Log progress
                if (attempts % 10 == 0) {
                    val cachedBytes = cache.cacheSpace
                    Log.d(TAG, "Pre-caching progress: ${cachedBytes / 1024 / 1024} MB cached")
                }
            }
            
            if (player.playbackState == ExoPlayer.STATE_READY) {
                val cachedBytes = cache.cacheSpace
                val cacheSizeMB = cachedBytes / 1024 / 1024
                Log.d(TAG, "✅ Landing page video pre-cached successfully! ($cacheSizeMB MB)")
                
                // Update Room DB with cache status (in coroutine scope)
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(context)
                    val cacheEntry = database.videoCacheDao().getCacheEntry(videoUrl)
                    if (cacheEntry != null) {
                        database.videoCacheDao().updateCacheStatus(videoUrl, true, cachedBytes)
                    } else {
                        // Create new cache entry in Room DB
                        database.videoCacheDao().insertOrUpdate(
                            VideoCacheEntity(
                                videoUrl = videoUrl,
                                modelId = "landing_page",
                                modelName = "Landing Page Background Video",
                                isCached = true,
                                cacheSize = cachedBytes
                            )
                        )
                    }
                    Log.d(TAG, "Cache status saved to Room DB (persistent)")
                }
            } else {
                Log.w(TAG, "⚠️ Pre-caching timed out, but some data may be cached")
            }
            
            // Release player (cache persists in filesDir)
            player.release()
            precachePlayer = null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-caching video", e)
            precachePlayer?.release()
            precachePlayer = null
        }
    }

    /**
     * Check if video is already cached (suspend version for coroutines).
     * Uses Room DB for reliable cache status tracking.
     * Cache files stored in filesDir (persistent, only cleared on app uninstall).
     */
    @OptIn(UnstableApi::class)
    private suspend fun isCachedSuspend(context: Context, videoUrl: String): Boolean {
        return try {
            // First check Room DB (most reliable)
            val database = AppDatabase.getDatabase(context)
            val cacheEntry = database.videoCacheDao().getCacheEntry(videoUrl)
            
            if (cacheEntry?.isCached == true) {
                Log.d(TAG, "✅ Video cached (verified in Room DB, ${cacheEntry.cacheSize / 1024 / 1024} MB)")
                
                // Also verify ExoPlayer cache exists
                val cache = VideoPreviewCache.get(context)
                val keys = cache.keys
                val hasExoPlayerCache = keys.isNotEmpty() && keys.any { key ->
                    key == videoUrl || key.contains(videoUrl) || videoUrl.contains(key)
                }
                
                if (!hasExoPlayerCache) {
                    Log.w(TAG, "Room DB says cached but ExoPlayer cache missing - marking as not cached")
                    database.videoCacheDao().updateCacheStatus(videoUrl, false, 0L)
                    return false
                }
                
                return true
            }
            
            // Fallback: Check ExoPlayer cache directly (for backwards compatibility)
            val cache = VideoPreviewCache.get(context)
            val keys = cache.keys
            
            if (keys.isEmpty()) {
                Log.d(TAG, "Cache is empty - video not cached")
                return false
            }
            
            // For HLS (m3u8), check if master playlist and at least some segments are cached
            val isCachedInExoPlayer = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                val masterPlaylistCached = keys.any { key ->
                    key == videoUrl || 
                    key.contains(videoUrl) || 
                    videoUrl.contains(key) ||
                    (key.contains(".m3u8") && videoUrl.contains(".m3u8"))
                }
                val hasSegments = keys.count { key ->
                    !key.contains(".m3u8") && (key.contains(videoUrl.substringBeforeLast("/")) || videoUrl.contains(key.substringBeforeLast("/")))
                } > 0
                masterPlaylistCached || hasSegments
            } else {
                keys.any { key -> key == videoUrl }
            }
            
            if (isCachedInExoPlayer) {
                Log.d(TAG, "✅ Video cached in ExoPlayer (${keys.size} cache entries)")
            } else {
                Log.d(TAG, "Video not cached (${keys.size} other cache entries exist)")
            }
            
            isCachedInExoPlayer
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cache", e)
            false
        }
    }
    
    /**
     * Check if video is already cached (non-suspend version for backwards compatibility).
     * Note: This only checks ExoPlayer cache, not Room DB.
     * For full cache check, use isCachedSuspend() from a coroutine.
     */
    @OptIn(UnstableApi::class)
    fun isCached(context: Context, videoUrl: String): Boolean {
        return try {
            // Only check ExoPlayer cache (non-blocking)
            val cache = VideoPreviewCache.get(context)
            val keys = cache.keys
            
            if (keys.isEmpty()) {
                return false
            }
            
            // For HLS (m3u8), check if master playlist and at least some segments are cached
            val isCachedInExoPlayer = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                val masterPlaylistCached = keys.any { key ->
                    key == videoUrl || 
                    key.contains(videoUrl) || 
                    videoUrl.contains(key) ||
                    (key.contains(".m3u8") && videoUrl.contains(".m3u8"))
                }
                val hasSegments = keys.count { key ->
                    !key.contains(".m3u8") && (key.contains(videoUrl.substringBeforeLast("/")) || videoUrl.contains(key.substringBeforeLast("/")))
                } > 0
                masterPlaylistCached || hasSegments
            } else {
                keys.any { key -> key == videoUrl }
            }
            
            isCachedInExoPlayer
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cache", e)
            false
        }
    }

    /**
     * Cancel pre-caching if in progress.
     */
    fun cancelPrecaching() {
        precachePlayer?.release()
        precachePlayer = null
        isPrecaching = false
    }
}

