package com.manjul.genai.videogenerator.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.local.VideoCacheEntity
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.player.VideoPreviewCache
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.ui.viewmodel.AIModelsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PLAYER_PREFETCH_DISTANCE = 0 // Only play visible items to save memory
private const val MAX_CONCURRENT_PLAYERS = 2 // Limit concurrent players


@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier,
    viewModel: AIModelsViewModel = viewModel(factory = AIModelsViewModel.Factory),
    onModelClick: (String) -> Unit = {},
    highlightModelId: String? = null,
    onHighlightCleared: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var fullscreenVideoUrl by remember { mutableStateOf<String?>(null) }

    when {
        state.isLoading -> LoadingState(modifier)
        state.errorMessage != null -> ErrorState(modifier, state.errorMessage!!)
        else -> ModelsList(
            modifier = modifier,
            models = state.models,
            onVideoClick = { fullscreenVideoUrl = it },
            onModelClick = onModelClick,
            highlightModelId = highlightModelId,
            onHighlightCleared = onHighlightCleared,
            viewModel = viewModel
        )
    }

    fullscreenVideoUrl?.let { url ->
        FullscreenVideoDialog(
            videoUrl = url,
            onDismiss = { fullscreenVideoUrl = null }
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
        horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading AI Models",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        Text(
                text = "Discovering the latest video generation models...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        }
    }
}

@Composable
private fun ErrorState(modifier: Modifier, message: String) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }
            Text(
                text = "Unable to Load Models",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
        )
        }
    }
}

@Composable
private fun ModelsList(
    modifier: Modifier,
    models: List<AIModel>,
    onVideoClick: (String) -> Unit,
    onModelClick: (String) -> Unit,
    highlightModelId: String? = null,
    onHighlightCleared: () -> Unit = {},
    viewModel: AIModelsViewModel
) {
    val state by viewModel.state.collectAsState()

    // Use saved scroll position from ViewModel
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = state.savedScrollOffset
    )

    // Restore scroll position after models are loaded and list is laid out
    // Only restore if we have a saved position and we're not highlighting
    LaunchedEffect(models.isNotEmpty(), highlightModelId, state.savedScrollIndex) {
        if (models.isNotEmpty() &&
            highlightModelId == null &&
            state.savedScrollIndex > 0) {
            // Check if we need to restore (if we're at the top or near it)
            val needsRestore = listState.firstVisibleItemIndex == 0 ||
                    (listState.firstVisibleItemIndex < state.savedScrollIndex - 2)
            if (needsRestore) {
                // Small delay to ensure layout is complete
                kotlinx.coroutines.delay(150)
                // Restore scroll position
                listState.scrollToItem(
                    index = state.savedScrollIndex,
                    scrollOffset = state.savedScrollOffset
                )
            }
        }
    }

    // Save scroll position when it changes (but not when highlighting or scrolling to highlight)
    // Use a debounced approach to avoid saving too frequently during scrolling
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (highlightModelId == null && models.isNotEmpty()) {
            // Debounce to avoid saving too frequently during fast scrolling
            kotlinx.coroutines.delay(300)
            // Save the current position (highlightModelId check is in the condition above)
            viewModel.saveScrollPosition(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    // Also save scroll position when leaving the screen (DisposableEffect)
    DisposableEffect(listState, highlightModelId) {
        onDispose {
            // Save the current scroll position when the composable is disposed
            // Only save if we're not highlighting (to avoid overwriting with highlight position)
            if (highlightModelId == null) {
                viewModel.saveScrollPosition(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    // Scroll to highlighted model when it's set (only when highlightModelId changes)
    // This takes priority over scroll restoration
    LaunchedEffect(highlightModelId) {
        if (highlightModelId != null && models.isNotEmpty()) {
            val index = models.indexOfFirst { it.id == highlightModelId }
            if (index >= 0) {
                // Smooth scroll to the highlighted model
                kotlinx.coroutines.delay(100) // Small delay to ensure layout is ready
                listState.animateScrollToItem(
                    index = index,
                    scrollOffset = 0
                )
                // Clear highlight after scrolling animation completes
                kotlinx.coroutines.delay(2500)
                onHighlightCleared()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AI Video Models",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${models.size} models available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(models, key = { _, item -> item.id }) { index, model ->
            // Enable playback for ALL visible items + prefetch nearby items
            // This allows smooth playback without loading delays
            val isActive by remember(model.id, listState) {
                derivedStateOf {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) {
                        index == 0 // Play first item if list isn't laid out yet
                    } else {
                        // Check if this item is visible
                        val isVisible = visibleItems.any { it.index == index }
                        
                        // Also prefetch items that are about to become visible (1 item ahead/behind)
                        val firstVisibleIndex = visibleItems.first().index
                        val lastVisibleIndex = visibleItems.last().index
                        val prefetchRange = 1 // Prefetch 1 item ahead/behind
                        
                        val isInPrefetchRange = index in (firstVisibleIndex - prefetchRange)..(lastVisibleIndex + prefetchRange)
                        
                        isVisible || isInPrefetchRange
                    }
                }
            }
            val isHighlighted = model.id == highlightModelId
            ModelCard(
                model = model,
                playbackEnabled = isActive,
                onVideoClick = onVideoClick,
                onModelClick = { onModelClick(model.id) },
                isHighlighted = isHighlighted
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ModelVideoPlayer(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                showControls = true,
                initialVolume = 1f,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                playbackEnabled = true,
                onVideoClick = null
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: AIModel,
    playbackEnabled: Boolean,
    onVideoClick: (String) -> Unit,
    onModelClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = tween(300), label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onModelClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Video Preview Section
            val exampleVideoUrl = model.exampleVideoUrl
            if (!exampleVideoUrl.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Always create player for visible items and prefetch nearby items
                    // This allows smooth playback without loading delays
                ModelVideoPlayer(
                    videoUrl = exampleVideoUrl,
                    playbackEnabled = playbackEnabled,
                        onVideoClick = { onVideoClick(exampleVideoUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                }
            } else {
                // Placeholder for models without preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Preview coming soon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Content Section
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Description
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Divider
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ) {}

                // Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        label = "Price",
                        value = "${model.pricePerSecond}",
                        unit = "/sec",
                        icon = null
                    )
                    InfoChip(
                        label = "Duration",
                        value = model.durationOptions.joinToString("s, ", postfix = "s"),
                        unit = null,
                        icon = null
                    )
                }

                // Aspect Ratios
                if (model.aspectRatios.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Aspect Ratios:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        model.aspectRatios.take(3).forEach { ratio ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = ratio,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    unit: String?,
    icon: ImageVector?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            unit?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun ModelVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(360.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    showControls: Boolean = false,
    initialVolume: Float = 0f,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    playbackEnabled: Boolean = true,
    onVideoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mediaSourceFactory = rememberPreviewMediaSourceFactory()

    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var exoPlayer by remember(videoUrl) { mutableStateOf<ExoPlayer?>(null) }
    
    // Room DB cache for video metadata
    val database = remember { AppDatabase.getDatabase(context) }
    val cacheDao = remember { database.videoCacheDao() }
    
    // Coroutine scope for async operations
    val coroutineScope = remember { kotlinx.coroutines.CoroutineScope(Dispatchers.IO) }

    // Keep players in memory much longer to prevent rebuffering when scrolling back
    // Only release when item is far off-screen for an extended period
    LaunchedEffect(playbackEnabled) {
        if (!playbackEnabled) {
            // Much longer delay - keep players in memory for 30 seconds
            // This prevents rebuffering when user scrolls back up/down
            kotlinx.coroutines.delay(30000) // 30 seconds delay before releasing
            
            // Double-check playback is still disabled after delay
            if (!playbackEnabled) {
                exoPlayer?.let { player ->
                    try {
                        player.pause()
                        player.stop()
                        VideoPlayerManager.unregisterPlayer(videoUrl)
                        player.release()
                    } catch (e: Exception) {
                        android.util.Log.e("ModelVideoPlayer", "Error releasing player", e)
                    }
                    exoPlayer = null
                }
                isBuffering = false
                hasError = false
            }
        } else {
            // Cancel any pending release when playback is re-enabled
            // This ensures players stay alive when scrolling back
        }
    }

    LaunchedEffect(playbackEnabled, videoUrl, mediaSourceFactory) {
        if (playbackEnabled && exoPlayer == null) {
            // Check Room DB cache for this video (async)
            val cacheEntry = kotlinx.coroutines.withContext(Dispatchers.IO) {
                cacheDao.getCacheEntry(videoUrl)
            }
            val lastPosition = cacheEntry?.lastPlayedPosition ?: 0L
            
            // Very short delay for prefetching - we want videos ready quickly
            kotlinx.coroutines.delay(50)
            
            // Double-check playback is still enabled after delay
        if (playbackEnabled) {
                // Create player with optimized settings for caching
                val player = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setHandleAudioBecomingNoisy(true)
                    .build().apply {
                    val mediaItem = MediaItem.fromUri(videoUrl)
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true // Start playing immediately
                    volume = initialVolume
                        videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                        // Prepare immediately - ExoPlayer will use cache if available
                    prepare()
                    }
                exoPlayer = player
                
                // Register with manager to track and limit concurrent players
                VideoPlayerManager.registerPlayer(videoUrl, player)
                
                // Update Room DB cache - mark as accessed (we're already in a coroutine via LaunchedEffect)
                // Use withContext since we're already in a coroutine
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        if (cacheEntry != null) {
                            cacheDao.updateAccess(videoUrl)
                        } else {
                            // Create new cache entry (we'll need model info - for now use placeholder)
                            cacheDao.insertOrUpdate(
                                VideoCacheEntity(
                                    videoUrl = videoUrl,
                                    modelId = "",
                                    modelName = "",
                                    lastPlayedPosition = 0L,
                                    isCached = false,
                                    cacheSize = 0L
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ModelVideoPlayer", "Error updating cache", e)
                    }
                }
                
                isBuffering = true
                hasError = false
            }
        } else if (playbackEnabled && exoPlayer != null) {
            // Resume playback immediately if player exists - no rebuffering needed
            // Check Room DB for last position (async)
            val cacheEntry = kotlinx.coroutines.withContext(Dispatchers.IO) {
                cacheDao.getCacheEntry(videoUrl)
            }
            val lastPosition = cacheEntry?.lastPlayedPosition ?: 0L
            
                exoPlayer?.playWhenReady = true
            // Seek to last known position if available (within reasonable range)
            if (lastPosition > 0 && lastPosition < (exoPlayer?.duration ?: Long.MAX_VALUE)) {
                exoPlayer?.seekTo(lastPosition)
            }
        } else if (!playbackEnabled && exoPlayer != null) {
            // Save playback position to Room DB before pausing
            val currentPosition = exoPlayer?.currentPosition ?: 0L
            // Use withContext since we're already in a coroutine (LaunchedEffect)
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                try {
                    cacheDao.updatePlaybackPosition(videoUrl, currentPosition)
                } catch (e: Exception) {
                    android.util.Log.e("ModelVideoPlayer", "Error saving playback position", e)
                }
            }
            
            // Pause (but don't release) if playback is disabled - keep in memory
            // Player stays in memory for 30 seconds to prevent rebuffering
            exoPlayer?.playWhenReady = false
        }
    }

    LaunchedEffect(initialVolume, exoPlayer) {
        exoPlayer?.volume = initialVolume
    }

    // CRITICAL: Always release player on dispose
    DisposableEffect(videoUrl, playbackEnabled) {
        val player = exoPlayer
        onDispose {
            player?.let {
                try {
                    it.pause()
                    it.stop()
                    VideoPlayerManager.unregisterPlayer(videoUrl)
                    it.release()
                } catch (e: Exception) {
                    android.util.Log.e("ModelVideoPlayer", "Error releasing player", e)
                }
            }
            exoPlayer = null
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                
                // Update cache status when video is ready
                if (playbackState == Player.STATE_READY) {
                    // Use coroutine scope for async operation (Player.Listener is not a coroutine)
                    coroutineScope.launch {
                        try {
                            // Check if video is cached by ExoPlayer
                            val cache = VideoPreviewCache.get(context)
                            val isCached = cache.isCached(videoUrl, 0, Long.MAX_VALUE)
                            val cacheSize = if (isCached) {
                                // Estimate cache size (this is approximate)
                                player.duration * 1000 // Rough estimate
                            } else 0L
                            
                            cacheDao.updateCacheStatus(videoUrl, isCached, cacheSize)
                        } catch (e: Exception) {
                            android.util.Log.e("ModelVideoPlayer", "Error updating cache status", e)
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                android.util.Log.e("ModelVideoPlayer", "Player error for $videoUrl", error)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!hasError && playbackEnabled) {
                        player.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { 
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val clickableModifier = if (onVideoClick != null) {
        Modifier.clickable(onClick = onVideoClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        val player = exoPlayer
        if (player != null && playbackEnabled) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = showControls
                        controllerAutoShow = showControls
                        this.resizeMode = resizeMode
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        this.player = player
                    }
                },
                update = { view ->
                    view.useController = showControls
                    view.controllerAutoShow = showControls
                    view.resizeMode = resizeMode
                    if (view.player !== player) {
                        view.player = player
                    }
                    // Ensure player is paused if playback is disabled
                    if (!playbackEnabled) {
                        view.player?.pause()
                    }
                },
                onRelease = {
                    // Clean up when view is released
                    it.player = null
                }
            )

            when {
                hasError -> Text(
                    text = "Unable to load preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                isBuffering -> CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        } else {
            if (!playbackEnabled) {
                Text(
                    text = "Scroll to play preview",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun rememberPreviewMediaSourceFactory(): ProgressiveMediaSource.Factory {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val cache = VideoPreviewCache.get(context)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000) // Increased timeout for better reliability
            .setReadTimeoutMs(15_000)
            .setUserAgent("GenAiVideoPreview/1.0")
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            // Remove FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS to allow better caching
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        ProgressiveMediaSource.Factory(cacheFactory)
    }
}


