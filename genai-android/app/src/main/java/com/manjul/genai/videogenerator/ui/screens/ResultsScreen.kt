package com.manjul.genai.videogenerator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.local.toEntity
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.player.VideoFileCache
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.ui.components.VideoThumbnail
import com.manjul.genai.videogenerator.utils.VideoDownloader
import com.manjul.genai.videogenerator.utils.VideoSharer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Helper function to parse aspect ratio
private fun parseAspectRatio(ratioString: String?): Float {
    return when (ratioString) {
        "16:9" -> 16f / 9f
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        "3:2" -> 3f / 2f
        "2:3" -> 2f / 3f
        "4:3" -> 4f / 3f
        "3:4" -> 3f / 4f
        else -> 16f / 9f // Default
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@Composable
fun ResultsScreenDialog(
    job: VideoJob,
    onClose: () -> Unit,
    onRegenerate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val videoUrl = job.storageUrl ?: job.previewUrl ?: return
    val database = remember { AppDatabase.getDatabase(context) }
    val jobDao = remember { database.videoJobDao() }

    // Parse aspect ratio to determine video aspect
    val aspectRatio = parseAspectRatio(job.aspectRatio)
    var showFullscreen by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    // Save job to Room DB when viewing (for regeneration and local storage)
    LaunchedEffect(job.id) {
        try {
            val entity = job.toEntity()
            jobDao.insertJob(entity)
        } catch (e: Exception) {
            android.util.Log.e("ResultsScreen", "Failed to save job to Room DB", e)
        }
    }

    // Stop video player when dialog is dismissed
    DisposableEffect(videoUrl) {
        onDispose {
            VideoPlayerManager.unregisterPlayer(videoUrl)
        }
    }

    Dialog(
        onDismissRequest = {
            VideoPlayerManager.unregisterPlayer(videoUrl)
            onClose()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Header with back button, title, and status badge
                HeaderSection(
                    status = job.status,
                    onBackClick = {
                        VideoPlayerManager.unregisterPlayer(videoUrl)
                        onClose()
                    }
                )

                // Scrollable content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Video Player Card
                    VideoPlayerCard(
                            videoUrl = videoUrl,
                        aspectRatio = aspectRatio,
                        onFullscreenClick = { showFullscreen = true }
                    )

                    // Prompt Card
                    PromptCard(
                        prompt = job.prompt,
                        context = context
                    )

                    // Details Card
                    DetailsCard(job = job)

                    // Action Buttons
                    ActionButtonsSection(
                        context = context,
                        videoUrl = videoUrl,
                        isDownloading = isDownloading,
                        isSharing = isSharing,
                        onDownload = {
                            isDownloading = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val uri = VideoDownloader.downloadVideoToGallery(
                                        context,
                                        videoUrl,
                                        "AI_Video_${job.id}.mp4"
                                    )
                                    if (uri != null) {
                                        // Update local file path in Room DB
                                        val cachedFileUri = VideoFileCache.getCachedFileUri(context, videoUrl)
                                        if (cachedFileUri != null) {
                                            val filePath = cachedFileUri.removePrefix("file://")
                                            jobDao.updateLocalFilePath(job.id, filePath)
                                        }
                                        // Show success toast
                                        CoroutineScope(Dispatchers.Main).launch {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Video saved to gallery",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to save video",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ResultsScreen", "Failed to download video", e)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to download video",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    isDownloading = false
                                }
                            }
                        },
                        onShare = {
                            isSharing = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val success = VideoSharer.shareVideoFromUrl(context, videoUrl)
                                    if (!success) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to share video",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ResultsScreen", "Failed to share video", e)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to share video",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    isSharing = false
                                }
                            }
                        },
                        onRegenerate = onRegenerate
                    )
                }
            }
        }

        // Fullscreen Video Dialog
        if (showFullscreen) {
            FullscreenVideoDialog(
                videoUrl = videoUrl,
                aspectRatio = aspectRatio,
                onDismiss = { showFullscreen = false }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    status: VideoJobStatus,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title
            Text(
                text = "Video Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (status) {
                    VideoJobStatus.COMPLETE -> Color(0xFFD1FAE5) // green-100
                    VideoJobStatus.FAILED -> Color(0xFFFEE2E2) // red-100
                    VideoJobStatus.PROCESSING, VideoJobStatus.QUEUED -> Color(0xFFDBEAFE) // blue-100
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = Color.Transparent
                )
            ) {
                Text(
                    text = when (status) {
                        VideoJobStatus.COMPLETE -> "completed"
                        VideoJobStatus.FAILED -> "failed"
                        VideoJobStatus.PROCESSING -> "running"
                        VideoJobStatus.QUEUED -> "queued"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = when (status) {
                        VideoJobStatus.COMPLETE -> Color(0xFF008236) // green-700
                        VideoJobStatus.FAILED -> Color(0xFFDC2626) // red-600
                        VideoJobStatus.PROCESSING, VideoJobStatus.QUEUED -> Color(0xFF2563EB) // blue-600
                    }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerCard(
    videoUrl: String,
    aspectRatio: Float,
    onFullscreenClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable(onClick = onFullscreenClick)
        ) {
            // Video thumbnail
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Extract and show thumbnail from video
                VideoThumbnail(
                    videoUrl = videoUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                // Play button overlay (centered)
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Progress bar overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Progress bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        // Progress indicator (full for now, would be dynamic in real implementation)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Color.White,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    // Time indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0:01",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = "0:04",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptCard(
    prompt: String,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header with title and copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prompt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            // Copy prompt to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Prompt", prompt)
                            clipboard.setPrimaryClip(clip)
                            // Show toast
                            android.widget.Toast.makeText(
                                context,
                                "Prompt copied to clipboard",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy prompt",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DetailsCard(job: VideoJob) {
    // Format model name
    val displayModelName = job.modelId?.replace("-", " ")?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercaseChar() }
        ?: job.modelName

    // Format cost as dollars
    val costInDollars = String.format(Locale.US, "$%.2f", job.cost / 100.0)

    // Format created date
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US)
    val formattedDate = job.createdAt.atZone(ZoneId.systemDefault()).format(dateFormatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "Model", value = displayModelName)
                DetailRow(label = "Duration", value = "${job.durationSeconds}s")
                DetailRow(label = "Aspect Ratio", value = job.aspectRatio)
                // Seed removed as requested
                DetailRow(
                    label = "Credits Spent",
                    value = costInDollars,
                    valueColor = MaterialTheme.colorScheme.primary
                )
                DetailRow(label = "Created", value = formattedDate)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )
    }
}

@Composable
private fun ActionButtonsSection(
    context: Context,
    videoUrl: String,
    isDownloading: Boolean = false,
    isSharing: Boolean = false,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Download button
        TextButton(
            onClick = onDownload,
            enabled = !isDownloading && !isSharing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            if (isDownloading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDownloading) "Downloading..." else "Download Video",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        // Secondary buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share button
            TextButton(
                onClick = onShare,
                enabled = !isDownloading && !isSharing,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                if (isSharing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSharing) "Sharing..." else "Share",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Regenerate button
            TextButton(
                onClick = onRegenerate,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Regenerate",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoDialog(
        videoUrl: String,
        aspectRatio: Float,
        onDismiss: () -> Unit
    ) {
    DisposableEffect(videoUrl) {
        onDispose {
            VideoPlayerManager.unregisterPlayer(videoUrl)
        }
    }

        Dialog(
        onDismissRequest = {
            VideoPlayerManager.unregisterPlayer(videoUrl)
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                    .clickable {
                        VideoPlayerManager.unregisterPlayer(videoUrl)
                        onDismiss()
                    },
                    color = Color.White.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                ModelVideoPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio),
                    playbackEnabled = true,
                    showControls = false, // No controls anywhere in the app
                    initialVolume = 1f,
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    onVideoClick = null
                )
                }
            }
        }
    }

