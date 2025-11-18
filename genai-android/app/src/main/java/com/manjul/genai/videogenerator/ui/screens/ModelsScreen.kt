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
import androidx.media3.datasource.DefaultDataSource
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
import com.manjul.genai.videogenerator.player.VideoFileCache
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
    // Use a unique key to ensure fullscreen player is separate from thumbnail
    // This prevents state conflicts but we'll use actual URL for loading
    val fullscreenKey = remember(videoUrl) { "fullscreen_${System.currentTimeMillis()}_$videoUrl" }
    
    Dialog(
        onDismissRequest = {
            // Release player before dismissing dialog
            VideoPlayerManager.unregisterPlayer(fullscreenKey)
            VideoPlayerManager.unregisterPlayer(videoUrl) // Also release by actual URL
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Use Box with black background to ensure proper rendering context
        // Surface might be causing rendering issues with PlayerView in Dialog
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Use unique key to create separate player instance
            // ModelVideoPlayer will extract actual URL from the key
            ModelVideoPlayer(
                videoUrl = fullscreenKey, // Unique key for separate player instance
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
    
    // CRITICAL: Ensure player is immediately released when dialog is dismissed
    // This prevents audio from continuing to play in the background (memory leak prevention)
    DisposableEffect(videoUrl) {
        onDispose {
            // When dialog is dismissed, immediately release the player
            VideoPlayerManager.unregisterPlayer(fullscreenKey)
            VideoPlayerManager.unregisterPlayer(videoUrl) // Fallback
            android.util.Log.d("FullscreenVideoDialog", "Released fullscreen player for: $videoUrl")
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
    onVideoClick: (() -> Unit)? = null,
    onPlayingStateChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mediaSourceFactory = rememberPreviewMediaSourceFactory()

    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var isPlaying by remember(videoUrl) { mutableStateOf(false) }
    var exoPlayer by remember(videoUrl) { mutableStateOf<ExoPlayer?>(null) }
    
    // Room DB cache for video metadata
    val database = remember { AppDatabase.getDatabase(context) }
    val cacheDao = remember { database.videoCacheDao() }
    
    // Coroutine scope for async operations
    val coroutineScope = remember { kotlinx.coroutines.CoroutineScope(Dispatchers.IO) }
    
    // Store video source URI for error handling (accessible in both LaunchedEffect and DisposableEffect)
    var currentVideoSourceUri by remember(videoUrl) { mutableStateOf<String?>(null) }
    var currentActualVideoUrl by remember(videoUrl) { mutableStateOf<String?>(null) }

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
            // Extract actual video URL if this is a fullscreen player key
            // Format: "fullscreen_<timestamp>_<actualUrl>" or "fullscreen_<actualUrl>"
            val actualVideoUrl = if (videoUrl.startsWith("fullscreen_")) {
                // Remove "fullscreen_" prefix, then remove timestamp if present
                val withoutPrefix = videoUrl.removePrefix("fullscreen_")
                // If it contains underscore after timestamp, extract the actual URL
                val parts = withoutPrefix.split("_", limit = 2)
                if (parts.size > 1 && parts[0].all { it.isDigit() }) {
                    // First part is timestamp, second part is URL
                    parts[1]
                } else {
                    // No timestamp, the whole thing is the URL
                    withoutPrefix
                }
            } else {
                videoUrl
            }
            
            // Check all caches in parallel: File cache, Room DB, and ExoPlayer cache
            val (cacheEntry, isExoCached, fileCacheUri) = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val entry = cacheDao.getCacheEntry(actualVideoUrl)
                val cache = VideoPreviewCache.get(context)
                val exoCached = cache.isCached(actualVideoUrl, 0, Long.MAX_VALUE)
                // Check file cache (persistent storage)
                val fileUri = VideoFileCache.getCachedFileUri(context, actualVideoUrl)
                Triple(entry, exoCached, fileUri)
            }
            val lastPosition = cacheEntry?.lastPlayedPosition ?: 0L
            
            // Determine which source to use (priority: ExoPlayer cache > file cache > network)
            // Prefer ExoPlayer cache first (faster, already validated), then file cache, then network
            val videoSourceUri = when {
                isExoCached -> {
                    // ExoPlayer cache is fastest and most reliable
                    android.util.Log.d("ModelVideoPlayer", "Using ExoPlayer cache: $actualVideoUrl")
                    actualVideoUrl
                }
                fileCacheUri != null -> {
                    // File cache is persistent but may have issues, use with fallback
                    android.util.Log.d("ModelVideoPlayer", "Using file cache: $fileCacheUri")
                    fileCacheUri
                }
                else -> {
                    // Use network URL (ExoPlayer will cache it)
                    android.util.Log.d("ModelVideoPlayer", "Using network: $actualVideoUrl")
                    actualVideoUrl
                }
            }
            val isCached = fileCacheUri != null || isExoCached
            
            // Store for error handling
            currentVideoSourceUri = videoSourceUri
            currentActualVideoUrl = actualVideoUrl
            
            // For fullscreen players in Dialog, add a small delay to ensure Dialog is fully laid out
            // This prevents black screen issue where video surface isn't attached yet
            val isFullscreen = videoUrl.startsWith("fullscreen_")
            if (isFullscreen) {
                kotlinx.coroutines.delay(100) // Small delay for Dialog to fully render
            } else if (!isCached) {
                kotlinx.coroutines.delay(50) // Small delay only for non-cached videos
            }
            // No delay for cached videos (unless fullscreen) - they should appear immediately
            
            // Double-check playback is still enabled after delay (if any)
        if (playbackEnabled) {
                // Create player with optimized settings for caching
                // For file:// URIs, we don't need CacheDataSource (it's already a local file)
                // For HTTP/HTTPS URIs, use the cache-enabled media source factory
                val factory = if (videoSourceUri.startsWith("file://")) {
                    // Use DefaultDataSource for local files (handles file:// URIs properly)
                    // DefaultDataSource automatically handles file://, http://, https://, etc.
                    ProgressiveMediaSource.Factory(
                        DefaultDataSource.Factory(context)
                    )
                } else {
                    // Use cache-enabled factory for network URLs
                    mediaSourceFactory
                }
                
                val player = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(factory)
                    .setHandleAudioBecomingNoisy(true)
                    .build().apply {
                    // Use file cache if available, otherwise use URL (ExoPlayer will cache it)
                    // Parse URI properly to handle file:// paths
                    val uri = android.net.Uri.parse(videoSourceUri)
                    val mediaItem = MediaItem.fromUri(uri)
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true // Start playing immediately
                    volume = initialVolume
                        videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                        // Prepare immediately - ExoPlayer will use cache if available
                    prepare()
                    }
                exoPlayer = player
                
                // If video is not in file cache but is playing from network,
                // download it to file cache in background for future use
                if (fileCacheUri == null && !actualVideoUrl.startsWith("file://")) {
                    // Download to file cache in background (non-blocking)
                    // Use the existing coroutine scope for async operations
                    coroutineScope.launch {
                        try {
                            VideoFileCache.downloadVideo(context, actualVideoUrl)
                            android.util.Log.d("ModelVideoPlayer", "Video downloaded to file cache: $actualVideoUrl")
                        } catch (e: Exception) {
                            android.util.Log.e("ModelVideoPlayer", "Failed to download video to file cache", e)
                        }
                    }
                }
                
                // Register with manager to track and limit concurrent players
                // Use the videoUrl key (which might be fullscreen_ prefixed) for manager tracking
                VideoPlayerManager.registerPlayer(videoUrl, player)
                
                // Update Room DB cache - mark as accessed (we're already in a coroutine via LaunchedEffect)
                // Use withContext since we're already in a coroutine
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        if (cacheEntry != null) {
                            cacheDao.updateAccess(actualVideoUrl) // Use actual URL for cache
                        } else {
                            // Create new cache entry (we'll need model info - for now use placeholder)
                            cacheDao.insertOrUpdate(
                                VideoCacheEntity(
                                    videoUrl = actualVideoUrl, // Use actual URL for cache
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
            // Extract actual video URL if this is a fullscreen player key
            // Format: "fullscreen_<timestamp>_<actualUrl>" or "fullscreen_<actualUrl>"
            val actualVideoUrl = if (videoUrl.startsWith("fullscreen_")) {
                // Remove "fullscreen_" prefix, then remove timestamp if present
                val withoutPrefix = videoUrl.removePrefix("fullscreen_")
                // If it contains underscore after timestamp, extract the actual URL
                val parts = withoutPrefix.split("_", limit = 2)
                if (parts.size > 1 && parts[0].all { it.isDigit() }) {
                    // First part is timestamp, second part is URL
                    parts[1]
                } else {
                    // No timestamp, the whole thing is the URL
                    withoutPrefix
                }
            } else {
                videoUrl
            }
            // Check Room DB for last position (async)
            val cacheEntry = kotlinx.coroutines.withContext(Dispatchers.IO) {
                cacheDao.getCacheEntry(actualVideoUrl)
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
            // Extract actual video URL if this is a fullscreen player key
            // Format: "fullscreen_<timestamp>_<actualUrl>" or "fullscreen_<actualUrl>"
            val actualVideoUrl = if (videoUrl.startsWith("fullscreen_")) {
                // Remove "fullscreen_" prefix, then remove timestamp if present
                val withoutPrefix = videoUrl.removePrefix("fullscreen_")
                // If it contains underscore after timestamp, extract the actual URL
                val parts = withoutPrefix.split("_", limit = 2)
                if (parts.size > 1 && parts[0].all { it.isDigit() }) {
                    // First part is timestamp, second part is URL
                    parts[1]
                } else {
                    // No timestamp, the whole thing is the URL
                    withoutPrefix
                }
        } else {
                videoUrl
            }
            // Use withContext since we're already in a coroutine (LaunchedEffect)
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                try {
                    cacheDao.updatePlaybackPosition(actualVideoUrl, currentPosition)
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
    // This ensures players are properly cleaned up when composable is removed
    DisposableEffect(videoUrl) {
        val player = exoPlayer
        onDispose {
            player?.let {
                try {
                    // Immediately pause and stop playback
                    it.pause()
                    it.stop()
                    // Unregister from manager
                    VideoPlayerManager.unregisterPlayer(videoUrl)
                    // Release player resources
                    it.release()
                    android.util.Log.d("ModelVideoPlayer", "Released player for: $videoUrl")
                } catch (e: Exception) {
                    android.util.Log.e("ModelVideoPlayer", "Error releasing player for $videoUrl", e)
                }
            }
            exoPlayer = null
            isBuffering = false
            hasError = false
            isPlaying = false
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Only set buffering if actually buffering (not ready or idle)
                // This prevents blocking overlay for cached videos that load instantly
                isBuffering = playbackState == Player.STATE_BUFFERING
                isPlaying = player.isPlaying && playbackState == Player.STATE_READY
                onPlayingStateChanged?.invoke(isPlaying)
                
                // Update cache status when video is ready
                if (playbackState == Player.STATE_READY) {
                    // Use coroutine scope for async operation (Player.Listener is not a coroutine)
                    coroutineScope.launch {
                        try {
                            // Extract actual video URL if this is a fullscreen player key
                            val actualVideoUrl = if (videoUrl.startsWith("fullscreen_")) {
                                videoUrl.removePrefix("fullscreen_")
                            } else {
                                videoUrl
                            }
                            // Check if video is cached by ExoPlayer
                            val cache = VideoPreviewCache.get(context)
                            val isCached = cache.isCached(actualVideoUrl, 0, Long.MAX_VALUE)
                            val cacheSize = if (isCached) {
                                // Estimate cache size (this is approximate)
                                player.duration * 1000 // Rough estimate
                            } else 0L
                            
                            cacheDao.updateCacheStatus(actualVideoUrl, isCached, cacheSize)
                        } catch (e: Exception) {
                            android.util.Log.e("ModelVideoPlayer", "Error updating cache status", e)
                        }
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow && player.playbackState == Player.STATE_READY
                onPlayingStateChanged?.invoke(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isPlaying = false
                android.util.Log.e("ModelVideoPlayer", "Player error for $videoUrl", error)
                android.util.Log.e("ModelVideoPlayer", "Error message: ${error.message}")
                android.util.Log.e("ModelVideoPlayer", "Error cause: ${error.cause}")
                
                // If error is with file:// URI, try falling back to original URL
                val sourceUri = currentVideoSourceUri
                val actualUrl = currentActualVideoUrl
                if (sourceUri != null && sourceUri.startsWith("file://") && actualUrl != null) {
                    android.util.Log.w("ModelVideoPlayer", "File cache failed, trying original URL: $actualUrl")
                    // Try to reload with original URL and cache-enabled factory
                    coroutineScope.launch(Dispatchers.Main) {
                        try {
                            val mediaItem = MediaItem.fromUri(actualUrl)
                            // Use cache-enabled factory for network URL
                            val factory = mediaSourceFactory
                            val newPlayer = ExoPlayer.Builder(context)
                                .setMediaSourceFactory(factory)
                                .setHandleAudioBecomingNoisy(true)
                                .build().apply {
                                    setMediaItem(mediaItem)
                                    repeatMode = Player.REPEAT_MODE_ONE
                                    playWhenReady = true
                                    volume = initialVolume
                                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                                    prepare()
                                }
                            // Release old player and use new one
                            player.release()
                            exoPlayer = newPlayer
                            VideoPlayerManager.registerPlayer(videoUrl, newPlayer)
                            hasError = false
                        } catch (e: Exception) {
                            android.util.Log.e("ModelVideoPlayer", "Failed to fallback to original URL", e)
                        }
                    }
                }
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
                        // Don't show buffering overlay - let ExoPlayer show video immediately
                        // This allows cached videos to display instantly without blocking overlay
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        // Ensure view is visible and properly initialized
                        visibility = android.view.View.VISIBLE
                        // Set player after view is created to ensure proper attachment
                        this.player = player
                        // Force layout to ensure video surface is attached
                        post {
                            requestLayout()
                            invalidate()
                            // Ensure video surface is visible
                            visibility = android.view.View.VISIBLE
                        }
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
                    // Force view to be visible and request layout
                    // This is critical for Dialog rendering - ensures video surface is attached
                    view.visibility = android.view.View.VISIBLE
                    view.requestLayout()
                    // Post to ensure layout happens after view is attached (fixes black screen in Dialog)
                    view.post {
                        view.requestLayout()
                        view.invalidate()
                        // Force video surface to be visible
                        view.visibility = android.view.View.VISIBLE
                    }
                },
                onRelease = {
                    // Clean up when view is released
                    it.player = null
                }
            )

            // Only show error overlay - ExoPlayer will handle video display
            // This allows cached videos to show immediately without blocking overlay
            if (hasError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                    text = "Unable to load preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                }
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


