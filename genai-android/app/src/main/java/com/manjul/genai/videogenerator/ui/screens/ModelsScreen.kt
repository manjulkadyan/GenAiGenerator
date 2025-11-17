package com.manjul.genai.videogenerator.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.player.VideoPreviewCache
import com.manjul.genai.videogenerator.ui.viewmodel.AIModelsViewModel

private const val PLAYER_PREFETCH_DISTANCE = 1

@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier,
    viewModel: AIModelsViewModel = viewModel(factory = AIModelsViewModel.Factory),
    onModelClick: (String) -> Unit = {}
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
            onModelClick = onModelClick
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "Fetching AI models..."
        )
    }
}

@Composable
private fun ErrorState(modifier: Modifier, message: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun ModelsList(
    modifier: Modifier,
    models: List<AIModel>,
    onVideoClick: (String) -> Unit,
    onModelClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(models, key = { _, item -> item.id }) { index, model ->
            val isActive by remember(model.id, listState) {
                derivedStateOf {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) {
                        index == 0
                    } else {
                        val firstVisibleIndex = (visibleItems.first().index - PLAYER_PREFETCH_DISTANCE)
                            .coerceAtLeast(0)
                        val lastVisibleIndex = visibleItems.last().index + PLAYER_PREFETCH_DISTANCE
                        index in firstVisibleIndex..lastVisibleIndex
                    }
                }
            }
            ModelCard(
                model = model,
                playbackEnabled = isActive,
                onVideoClick = onVideoClick,
                onModelClick = { onModelClick(model.id) }
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
    onModelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onModelClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = model.name, style = MaterialTheme.typography.titleMedium)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = model.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Price: ${model.pricePerSecond} credits/sec",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Durations: ${model.durationOptions.joinToString("s, ", postfix = "s")}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Aspect ratios: ${model.aspectRatios.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )

            val exampleVideoUrl = model.exampleVideoUrl
            if (!exampleVideoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ModelVideoPlayer(
                    videoUrl = exampleVideoUrl,
                    playbackEnabled = playbackEnabled,
                    onVideoClick = { onVideoClick(exampleVideoUrl) }
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Preview video coming soon.",
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

    LaunchedEffect(playbackEnabled, videoUrl, mediaSourceFactory) {
        if (playbackEnabled) {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build().apply {
                    val mediaItem = MediaItem.fromUri(videoUrl)
                    setMediaItem(mediaItem)
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = initialVolume
                    prepare()
                }
            } else {
                exoPlayer?.playWhenReady = true
            }
            isBuffering = true
            hasError = false
        } else {
            exoPlayer?.pause()
            exoPlayer = null
            isBuffering = false
            hasError = false
        }
    }

    LaunchedEffect(initialVolume, exoPlayer) {
        exoPlayer?.volume = initialVolume
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (!hasError) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
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

@Composable
private fun rememberPreviewMediaSourceFactory(): ProgressiveMediaSource.Factory {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val cache = VideoPreviewCache.get(context)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setUserAgent("GenAiVideoPreview/1.0")
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        ProgressiveMediaSource.Factory(cacheFactory)
    }
}
