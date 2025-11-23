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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
                
                // Pre-cache the video
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
     * This downloads the video to cacheDir (not heap/stack) for instant playback.
     */
    @OptIn(UnstableApi::class)
    private fun precacheVideo(context: Context, videoUrl: String) {
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
                Log.d(TAG, "✅ Landing page video pre-cached successfully! (${cachedBytes / 1024 / 1024} MB)")
            } else {
                Log.w(TAG, "⚠️ Pre-caching timed out, but some data may be cached")
            }
            
            // Release player (cache persists)
            player.release()
            precachePlayer = null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-caching video", e)
            precachePlayer?.release()
            precachePlayer = null
        }
    }

    /**
     * Check if video is already cached.
     */
    @OptIn(UnstableApi::class)
    fun isCached(context: Context, videoUrl: String): Boolean {
        return try {
            val cache = VideoPreviewCache.get(context)
            val keys = cache.keys
            keys.any { key ->
                key == videoUrl || key.contains(videoUrl) || videoUrl.contains(key)
            }
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

