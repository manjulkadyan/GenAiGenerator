package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.components.VideoThumbnail
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import java.time.Duration
import java.time.Instant

enum class HistoryFilter {
    ALL,
    COMPLETED,
    RUNNING,
    FAILED
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    onVideoClick: ((VideoJob) -> Unit)? = null
) {
    val jobs by viewModel.jobs.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }

    // Filter jobs based on selected filter
    val filteredJobs = when (selectedFilter) {
        HistoryFilter.ALL -> jobs
        HistoryFilter.COMPLETED -> jobs.filter { it.status == VideoJobStatus.COMPLETE }
        HistoryFilter.RUNNING -> jobs.filter { it.status == VideoJobStatus.PROCESSING || it.status == VideoJobStatus.QUEUED }
        HistoryFilter.FAILED -> jobs.filter { it.status == VideoJobStatus.FAILED }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section
        AppToolbar(
            title = "Video History",
            subtitle = "Your Creations",
            showBorder = true
        )

        // Filter Chips Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                ),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FilterChip(
                        text = "All",
                        isSelected = selectedFilter == HistoryFilter.ALL,
                        onClick = { selectedFilter = HistoryFilter.ALL }
                    )
                }
                item {
                    FilterChip(
                        text = "Completed",
                        isSelected = selectedFilter == HistoryFilter.COMPLETED,
                        onClick = { selectedFilter = HistoryFilter.COMPLETED }
                    )
                }
                item {
                    FilterChip(
                        text = "Running",
                        isSelected = selectedFilter == HistoryFilter.RUNNING,
                        onClick = { selectedFilter = HistoryFilter.RUNNING }
                    )
                }
                item {
                    FilterChip(
                        text = "Failed",
                        isSelected = selectedFilter == HistoryFilter.FAILED,
                        onClick = { selectedFilter = HistoryFilter.FAILED }
                    )
                }
            }
        }

        // Content
        if (filteredJobs.isEmpty()) {
            Column(
                modifier = Modifier
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredJobs, key = { it.id }) { job ->
                    HistoryJobCard(
                        job = job,
                        onCardClick = {
                            // Open ResultsScreen for completed videos when clicking anywhere on card
                            if (job.status == VideoJobStatus.COMPLETE) {
                                onVideoClick?.invoke(job)
                            }
                        },
                        onMoreClick = {
                            // TODO: Show options menu
                        }
                    )
                }
            }
        }
    }

    // Removed fullscreen dialog - clicking videos now opens ResultsScreen instead
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(1000.dp), // Fully rounded
        color = if (isSelected) {
            Color(0xFF6C5CE7) // Purple
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        } else null,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HistoryJobCard(
    job: VideoJob,
    onCardClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val cacheDao = remember { database.videoCacheDao() }

    // Display model name
    val displayModelName = job.modelId?.replace("-", " ")?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName.split("/").lastOrNull()?.replace("-", " ")
            ?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName

    // Format time ago
    val timeAgo = formatTimeAgo(job.createdAt)

    // Status configuration
    val (statusText, statusColor, statusBgColor) = when (job.status) {
        VideoJobStatus.COMPLETE -> Triple(
            "completed",
            Color(0xFF10B981), // Emerald-500
            Color(0xFF10B981).copy(alpha = 0.1f)
        )

        VideoJobStatus.FAILED -> Triple(
            "failed",
            Color(0xFFEF4444), // Red-500
            Color(0xFFEF4444).copy(alpha = 0.1f)
        )

        VideoJobStatus.PROCESSING -> Triple(
            "running",
            Color(0xFF3B82F6), // Blue-500
            Color(0xFF3B82F6).copy(alpha = 0.1f)
        )

        VideoJobStatus.QUEUED -> Triple(
            "running",
            Color(0xFF3B82F6), // Blue-500
            Color(0xFF3B82F6).copy(alpha = 0.1f)
        )
    }

    // Show video if available
    val videoUrl = when {
        job.status == VideoJobStatus.COMPLETE -> {
            job.storageUrl?.takeIf { it.isNotBlank() }
                ?: job.previewUrl?.takeIf { it.isNotBlank() }
        }

        job.status == VideoJobStatus.PROCESSING -> {
            job.previewUrl?.takeIf { it.isNotBlank() }
        }

        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = job.status == VideoJobStatus.COMPLETE, onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thumbnail on left
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (videoUrl != null) Color.Black else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (videoUrl != null) {
                    // Video thumbnail extracted from video (with DB cache optimization)
                    VideoThumbnail(
                        videoUrl = videoUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        jobId = job.id
                    )
                } else if (job.status == VideoJobStatus.PROCESSING) {
                    // Processing state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                } else {
                    // Failed or no video
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        // Empty state
                    }
                }
            }

            // Details on right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and more button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = job.prompt,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onMoreClick),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Model name
                Text(
                    text = displayModelName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status, duration, and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBgColor,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            statusColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }

                    // Duration
                    if (job.status == VideoJobStatus.COMPLETE || job.status == VideoJobStatus.PROCESSING) {
                        Text(
                            text = "${job.durationSeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Time ago
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
private fun formatTimeAgo(createdAt: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(createdAt, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1 -> "${duration.toMinutes()}m ago"
        duration.toDays() < 1 -> "${duration.toHours()}h ago"
        duration.toDays() == 1L -> "Yesterday"
        duration.toDays() < 7 -> "${duration.toDays()} days ago"
        duration.toDays() < 30 -> "${duration.toDays() / 7} weeks ago"
        duration.toDays() < 365 -> "${duration.toDays() / 30} months ago"
        else -> "${duration.toDays() / 365} years ago"
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    // Stop video player when dialog is dismissed
    DisposableEffect(videoUrl) {
        onDispose {
            // Release video player when fullscreen dialog is dismissed
            com.manjul.genai.videogenerator.player.VideoPlayerManager.unregisterPlayer(videoUrl)
        }
    }

    Dialog(
        onDismissRequest = {
            // Stop video player before closing
            com.manjul.genai.videogenerator.player.VideoPlayerManager.unregisterPlayer(videoUrl)
            onDismiss()
        },
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
                showControls = false, // No controls anywhere in the app
                initialVolume = 1f,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                playbackEnabled = true,
                onVideoClick = null
            )
        }
    }
}
