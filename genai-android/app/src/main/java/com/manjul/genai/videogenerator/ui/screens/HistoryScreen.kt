package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.local.VideoCacheEntity
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    onVideoClick: ((com.manjul.genai.videogenerator.data.model.VideoJob) -> Unit)? = null
) {
    val jobs by viewModel.jobs.collectAsState()
    var fullscreenVideoUrl by remember { mutableStateOf<String?>(null) }

    if (jobs.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No videos yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Start generating to see your history here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobs, key = { it.id }) { job ->
                JobCard(
                    job = job,
                    onVideoClick = { url -> 
                        fullscreenVideoUrl = url
                        onVideoClick?.invoke(job)
                    }
                )
            }
        }
    }

    fullscreenVideoUrl?.let { url ->
        FullscreenVideoDialog(
            videoUrl = url,
            onDismiss = { fullscreenVideoUrl = null }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun JobCard(
    job: VideoJob,
    onVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val cacheDao = remember { database.videoCacheDao() }
    // Display model name - prefer model_id if available, otherwise use model_name
    val displayModelName = job.modelId?.replace("-", " ")?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName
    
    // Show timestamp for completed jobs
    val timeInfo = if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE && job.completedAt != null) {
        val duration = java.time.Duration.between(job.createdAt, job.completedAt)
        val minutes = duration.toMinutes()
        val seconds = duration.seconds % 60
        "Completed in ${if (minutes > 0) "${minutes}m " else ""}${seconds}s"
    } else if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.PROCESSING) {
        val duration = java.time.Duration.between(job.createdAt, java.time.Instant.now())
        val minutes = duration.toMinutes()
        val seconds = duration.seconds % 60
        "Processing for ${if (minutes > 0) "${minutes}m " else ""}${seconds}s"
    } else {
        null
    }
    
    // Status configuration
    val (statusIcon, statusColor, statusBgColor) = when (job.status) {
        com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE -> 
            Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        com.manjul.genai.videogenerator.data.model.VideoJobStatus.FAILED -> 
            Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        com.manjul.genai.videogenerator.data.model.VideoJobStatus.PROCESSING -> 
            Triple(Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        com.manjul.genai.videogenerator.data.model.VideoJobStatus.QUEUED -> 
            Triple(Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }
    
    // Show video if available
    val videoUrl = when {
        job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE -> {
            job.storageUrl?.takeIf { it.isNotBlank() } 
                ?: job.previewUrl?.takeIf { it.isNotBlank() }
        }
        job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.PROCESSING -> {
            job.previewUrl?.takeIf { it.isNotBlank() }
        }
        else -> null
    }
    
    // Load cache entry for this video (async)
    var cacheEntry by remember(videoUrl) { mutableStateOf<VideoCacheEntity?>(null) }
    LaunchedEffect(videoUrl) {
        if (videoUrl != null) {
            cacheEntry = withContext(Dispatchers.IO) {
                cacheDao.getCacheEntry(videoUrl)
            }
            // Update access count and timestamp
            if (cacheEntry != null) {
                withContext(Dispatchers.IO) {
                    cacheDao.updateAccess(videoUrl)
                }
            } else if (videoUrl != null) {
                // Create new cache entry
                withContext(Dispatchers.IO) {
                    cacheDao.insertOrUpdate(
                        VideoCacheEntity(
                            videoUrl = videoUrl,
                            modelId = job.modelId ?: "",
                            modelName = job.modelName,
                            lastPlayedPosition = 0L,
                            isCached = false,
                            cacheSize = 0L
                        )
                    )
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBgColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = statusColor
                                )
                                Text(
                                    text = job.status.name.lowercase().replaceFirstChar { it.titlecase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip("${job.durationSeconds}s", MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                InfoChip(job.aspectRatio, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                InfoChip("${job.cost} credits", MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
            }
            
            // Model and time info
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayModelName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                timeInfo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Error message if failed
            job.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Video preview
            if (videoUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                var isVideoPlaying by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onVideoClick(videoUrl) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black
                ) {
                    Box {
                        ModelVideoPlayer(
                            videoUrl = videoUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            playbackEnabled = true,
                            onVideoClick = { onVideoClick(videoUrl) },
                            onPlayingStateChanged = { playing ->
                                isVideoPlaying = playing
                                // Update cache access when video starts playing
                                if (playing && videoUrl != null) {
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        cacheDao.updateAccess(videoUrl)
                                    }
                                }
                            }
                        )
                        // Overlay with play icon - only show when video is not playing
                        if (!isVideoPlaying) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play fullscreen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Processing video...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, backgroundColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
