package com.manjul.genai.videogenerator.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

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
    
    LaunchedEffect(videoUrl) {
        android.util.Log.d("BackgroundVideoPlayer", "=== LaunchedEffect triggered ===")
        android.util.Log.d("BackgroundVideoPlayer", "Video URL received: $videoUrl")
        android.util.Log.d("BackgroundVideoPlayer", "Video URL is empty: ${videoUrl.isEmpty()}")
        
        if (videoUrl.isNotEmpty()) {
            android.util.Log.d("BackgroundVideoPlayer", "=== Starting video setup ===")
            hasError = false
            errorMessage = null
            
            // Release previous player if exists
            exoPlayer?.release()
            exoPlayer = null
            
            try {
                // Create HttpDataSource with User-Agent to avoid 403 errors
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("GenAiVideoPlayer/1.0")
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(15_000)
                    .setAllowCrossProtocolRedirects(true)
                
                // Create DataSource factory with custom HttpDataSource
                val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                android.util.Log.d("BackgroundVideoPlayer", "DataSource factory created with User-Agent")
                
                // Since this is HLS-only, always use HLS MediaSource
                android.util.Log.d("BackgroundVideoPlayer", "Creating HLS MediaSource for URL: $videoUrl")
                
                val player = ExoPlayer.Builder(context)
                    .setHandleAudioBecomingNoisy(true)
                    .build()
                
                android.util.Log.d("BackgroundVideoPlayer", "ExoPlayer instance created")
                
                // Create MediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUrl)
                    .build()
                android.util.Log.d("BackgroundVideoPlayer", "MediaItem created with URI: ${mediaItem.localConfiguration?.uri}")
                
                // For HLS master playlists, ExoPlayer will auto-detect and use HLS
                // However, if the master playlist points to regular MP4 files (not fragmented),
                // HLS may fail. In that case, we should use the best quality MP4 directly.
                // For now, let ExoPlayer auto-detect the format from the URL
                val mediaSource: MediaSource = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                    android.util.Log.d("BackgroundVideoPlayer", "Creating HLS MediaSource for m3u8 URL")
                    HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .createMediaSource(mediaItem)
                } else {
                    android.util.Log.d("BackgroundVideoPlayer", "Creating Progressive MediaSource for direct video URL")
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                android.util.Log.d("BackgroundVideoPlayer", "MediaSource created successfully")
                
                // Configure player
                player.apply {
                    setMediaSource(mediaSource)
                    android.util.Log.d("BackgroundVideoPlayer", "MediaSource set on player")
                    
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = 1f // Enable audio
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT // Show full video without cropping
                    android.util.Log.d("BackgroundVideoPlayer", "Player configured: repeatMode=ONE, playWhenReady=true, volume=0")
                        
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
                            android.util.Log.d("BackgroundVideoPlayer", "📊 Playback state changed: $stateName")
                            
                            if (playbackState == Player.STATE_READY) {
                                android.util.Log.d("BackgroundVideoPlayer", "✅ Player is READY - video should be visible")
                                hasError = false
                                errorMessage = null
                            }
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            android.util.Log.d("BackgroundVideoPlayer", "▶️ Is playing changed: $isPlaying")
                        }
                        
                        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                            android.util.Log.d("BackgroundVideoPlayer", "🎬 Tracks changed. Groups: ${tracks.groups.size}")
                            tracks.groups.forEachIndexed { index, group ->
                                android.util.Log.d("BackgroundVideoPlayer", "  Track group $index: ${group.mediaTrackGroup.length} tracks, selected: ${group.isSelected}")
                            }
                        }
                        
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            android.util.Log.d("BackgroundVideoPlayer", "🔄 Media item transition: ${mediaItem?.localConfiguration?.uri}, reason: $reason")
                        }
                    })
                    
                    android.util.Log.d("BackgroundVideoPlayer", "Listener added, calling prepare()...")
                    prepare()
                    android.util.Log.d("BackgroundVideoPlayer", "prepare() called")
                }
                
                exoPlayer = player
                android.util.Log.d("BackgroundVideoPlayer", "✅ ExoPlayer assigned to state variable")
                android.util.Log.d("BackgroundVideoPlayer", "Player state: ${player.playbackState}, isPlaying: ${player.isPlaying}, playWhenReady: ${player.playWhenReady}")
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
            android.util.Log.d("BackgroundVideoPlayer", "=== Render State ===")
            android.util.Log.d("BackgroundVideoPlayer", "videoUrl.isEmpty: ${videoUrl.isEmpty()}")
            android.util.Log.d("BackgroundVideoPlayer", "exoPlayer is null: ${exoPlayer == null}")
            android.util.Log.d("BackgroundVideoPlayer", "hasError: $hasError")
            if (exoPlayer != null) {
                android.util.Log.d("BackgroundVideoPlayer", "Player state: ${exoPlayer?.playbackState}, isPlaying: ${exoPlayer?.isPlaying}")
            }
        }
        
        // Video player
        if (videoUrl.isNotEmpty() && exoPlayer != null && !hasError) {
            android.util.Log.d("BackgroundVideoPlayer", "✅ Rendering PlayerView")
            AndroidView(
                factory = { ctx ->
                    android.util.Log.d("BackgroundVideoPlayer", "🏭 Creating PlayerView")
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT // Show full video without cropping
                        android.util.Log.d("BackgroundVideoPlayer", "PlayerView configured with player")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    android.util.Log.d("BackgroundVideoPlayer", "🔄 Updating PlayerView with player")
                    view.player = exoPlayer
                }
            )
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

