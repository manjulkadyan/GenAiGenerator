package com.manjul.genai.videogenerator.ui.components

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.manjul.genai.videogenerator.player.VideoPreviewCache

/**
 * Background video player component with overlay for landing page.
 * Plays video in background with dark overlay for content readability.
 */
@OptIn(UnstableApi::class)
@Composable
fun BackgroundVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.7f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // Track loading/buffering state
    
    LaunchedEffect(videoUrl) {
        //Log.d("BackgroundVideoPlayer", "=== LaunchedEffect triggered ===")
        //Log.d("BackgroundVideoPlayer", "Video URL received: $videoUrl")
        //Log.d("BackgroundVideoPlayer", "Video URL is empty: ${videoUrl.isEmpty()}")
        
        if (videoUrl.isNotEmpty()) {
            //Log.d("BackgroundVideoPlayer", "=== Starting video setup ===")
            hasError = false
            errorMessage = null
            isLoading = true // Show loading indicator while setting up video
            
            // Release previous player if exists
            exoPlayer?.release()
            exoPlayer = null
            
            try {
                // Get cache instance for video caching
                val cache = VideoPreviewCache.get(context)
                //Log.d("BackgroundVideoPlayer", "Using video cache for playback")
                
                // Create HttpDataSource with User-Agent to avoid 403 errors
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("GenAiVideoPlayer/1.0")
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(15_000)
                    .setAllowCrossProtocolRedirects(true)
                
                // Create CacheDataSource factory - wraps HTTP data source with cache
                // Allow both reading from cache and writing to cache during playback
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    // Don't set setCacheWriteDataSinkFactory(null) - allow writing to cache during playback
                
                // Create DataSource factory with cache support
                val dataSourceFactory = DefaultDataSource.Factory(context, cacheDataSourceFactory)
                //Log.d("BackgroundVideoPlayer", "CacheDataSource factory created with User-Agent")
                
                // Since this is HLS-only, always use HLS MediaSource
                //Log.d("BackgroundVideoPlayer", "Creating HLS MediaSource for URL: $videoUrl")
                
                val player = ExoPlayer.Builder(context)
                    .setHandleAudioBecomingNoisy(true)
                    .build()
                
                //Log.d("BackgroundVideoPlayer", "ExoPlayer instance created")
                
                // Create MediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUrl)
                    .build()
                //Log.d("BackgroundVideoPlayer", "MediaItem created with URI: ${mediaItem.localConfiguration?.uri}")
                
                // For HLS master playlists, ExoPlayer will auto-detect and use HLS
                // However, if the master playlist points to regular MP4 files (not fragmented),
                // HLS may fail. In that case, we should use the best quality MP4 directly.
                // For now, let ExoPlayer auto-detect the format from the URL
                val mediaSource: MediaSource = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                    //Log.d("BackgroundVideoPlayer", "Creating HLS MediaSource for m3u8 URL")
                    HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .createMediaSource(mediaItem)
                } else {
                    //Log.d("BackgroundVideoPlayer", "Creating Progressive MediaSource for direct video URL")
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                //Log.d("BackgroundVideoPlayer", "MediaSource created successfully")
                
                // Configure player
                player.apply {
                    setMediaSource(mediaSource)
                    //Log.d("BackgroundVideoPlayer", "MediaSource set on player")
                    
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = 1f // Enable audio - full volume
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT // Show full video without cropping
                    //Log.d("BackgroundVideoPlayer", "Player configured: repeatMode=ONE, playWhenReady=true, volume=${volume}")
                        
                    // Add comprehensive listener for debugging
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            val errorMsg = "❌ Playback error: ${error.message} (errorCode: ${error.errorCode})"
                            android.util.Log.e("BackgroundVideoPlayer", errorMsg, error)
                            
                            // Log detailed error info
                            android.util.Log.e("BackgroundVideoPlayer", "Error code: ${error.errorCode}")
                            android.util.Log.e("BackgroundVideoPlayer", "Error cause: ${error.cause?.message}")
                            
                            // Log the underlying HTTP error if available
                            if (error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                                val httpError = error.cause as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                                android.util.Log.e("BackgroundVideoPlayer", "HTTP Response Code: ${httpError.responseCode}")
                                android.util.Log.e("BackgroundVideoPlayer", "HTTP Response Message: ${httpError.responseMessage}")
                                android.util.Log.e("BackgroundVideoPlayer", "Request URL: ${httpError.dataSpec?.uri}")
                            }
                            
                            android.util.Log.e("BackgroundVideoPlayer", "Error stack trace:", error)
                            
                            hasError = true
                            errorMessage = error.message ?: "Unknown playback error"
                        }
                        
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateName = when (playbackState) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> "UNKNOWN($playbackState)"
                            }
                            //Log.d("BackgroundVideoPlayer", "📊 Playback state changed: $stateName")
                            
                            // Update loading state based on playback state
                            isLoading = playbackState == Player.STATE_BUFFERING
                            
                            if (playbackState == Player.STATE_READY) {
                                //Log.d("BackgroundVideoPlayer", "✅ Player is READY - video should be visible")
                                isLoading = false // Video is ready, stop showing loading
                                // Ensure volume is set to 1.0 when ready
                                if (player.volume != 1f) {
                                    android.util.Log.w("BackgroundVideoPlayer", "⚠️ Volume is ${player.volume}, setting to 1.0")
                                    player.volume = 1f
                                } else {
                                    //Log.d("BackgroundVideoPlayer", "✅ Volume is correctly set to 1.0")
                                }
                                hasError = false
                                errorMessage = null
                            }
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            //Log.d("BackgroundVideoPlayer", "▶️ Is playing changed: $isPlaying")
                        }
                        
                        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                            //Log.d("BackgroundVideoPlayer", "🎬 Tracks changed. Groups: ${tracks.groups.size}")
                            var hasAudioTrack = false
                            var audioTrackSelected = false
                            tracks.groups.forEachIndexed { index, group ->
                                val trackType = group.mediaTrackGroup.getFormat(0).sampleMimeType
                                val isAudio = trackType?.startsWith("audio/") == true
                                //Log.d("BackgroundVideoPlayer", "  Track group $index: ${group.mediaTrackGroup.length} tracks, selected: ${group.isSelected}, type: $trackType")
                                if (isAudio) {
                                    hasAudioTrack = true
                                    if (group.isSelected) {
                                        audioTrackSelected = true
                                        //Log.d("BackgroundVideoPlayer", "✅ Audio track found and selected!")
                                    } else {
                                        android.util.Log.w("BackgroundVideoPlayer", "⚠️ Audio track found but NOT selected")
                                    }
                                }
                            }
                            if (!hasAudioTrack) {
                                android.util.Log.w("BackgroundVideoPlayer", "⚠️ No audio track found in video")
                            }
                            // Log current volume
                            //Log.d("BackgroundVideoPlayer", "Current player volume: ${player.volume}")
                        }
                        
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            //Log.d("BackgroundVideoPlayer", "🔄 Media item transition: ${mediaItem?.localConfiguration?.uri}, reason: $reason")
                        }
                    })
                    
                    //Log.d("BackgroundVideoPlayer", "Listener added, calling prepare()...")
                    prepare()
                    //Log.d("BackgroundVideoPlayer", "prepare() called")
                }
                
                exoPlayer = player
                //Log.d("BackgroundVideoPlayer", "✅ ExoPlayer assigned to state variable")
                //Log.d("BackgroundVideoPlayer", "Player state: ${player.playbackState}, isPlaying: ${player.isPlaying}, playWhenReady: ${player.playWhenReady}")
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize player: ${e.message}"
                android.util.Log.e("BackgroundVideoPlayer", errorMsg, e)
                hasError = true
                errorMessage = e.message ?: "Failed to initialize video player"
            }
        } else {
            // Release player if URL is empty
            exoPlayer?.release()
            exoPlayer = null
        }
    }
    
    DisposableEffect(lifecycleOwner, videoUrl) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer?.play()
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.release()
                    exoPlayer = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Debug: Log current state
        LaunchedEffect(videoUrl, exoPlayer, hasError) {
            //Log.d("BackgroundVideoPlayer", "=== Render State ===")
            //Log.d("BackgroundVideoPlayer", "videoUrl.isEmpty: ${videoUrl.isEmpty()}")
            //Log.d("BackgroundVideoPlayer", "exoPlayer is null: ${exoPlayer == null}")
            //Log.d("BackgroundVideoPlayer", "hasError: $hasError")
            if (exoPlayer != null) {
                //Log.d("BackgroundVideoPlayer", "Player state: ${exoPlayer?.playbackState}, isPlaying: ${exoPlayer?.isPlaying}")
            }
        }
        
        // Video player - show immediately when player exists, even if not ready yet
        if (videoUrl.isNotEmpty() && exoPlayer != null) {
            if (hasError) {
                // Show error state (black background)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    android.util.Log.w("BackgroundVideoPlayer", "Showing error state: $errorMessage")
                }
            } else {
                //Log.d("BackgroundVideoPlayer", "✅ Rendering PlayerView")
                AndroidView(
                    factory = { ctx ->
                        //Log.d("BackgroundVideoPlayer", "🏭 Creating PlayerView")
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT // Show full video without cropping
                            // Don't show buffering overlay - show video surface immediately
                            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                            // Ensure view is visible immediately
                            visibility = android.view.View.VISIBLE
                            //Log.d("BackgroundVideoPlayer", "PlayerView configured with player")
                            // Force layout to ensure video surface is attached and visible
                            post {
                                visibility = android.view.View.VISIBLE
                                requestLayout()
                                invalidate()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        //Log.d("BackgroundVideoPlayer", "🔄 Updating PlayerView with player")
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                        // Force view to be visible and request layout
                        // This ensures video surface is attached and visible immediately
                        view.visibility = android.view.View.VISIBLE
                        view.requestLayout()
                        // Post to ensure layout happens after view is attached
                        view.post {
                            view.visibility = android.view.View.VISIBLE
                            view.requestLayout()
                            view.invalidate()
                        }
                    }
                )
            }
        } else if (hasError) {
            // Show error state (black background with overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                android.util.Log.w("BackgroundVideoPlayer", "Showing error state: $errorMessage")
            }
        } else if (videoUrl.isEmpty()) {
            // No video URL - show black background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        
        // Loading indicator overlay (shown while buffering/caching)
        if (isLoading && !hasError && videoUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This may take a moment on first launch",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Dark overlay for content readability (always show, even on error)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = overlayAlpha),
                            Color.Black.copy(alpha = overlayAlpha * 0.9f),
                            Color.Black.copy(alpha = overlayAlpha)
                        )
                    )
                )
        )
    }
}

