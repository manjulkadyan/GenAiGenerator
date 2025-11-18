package com.manjul.genai.videogenerator.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.player.VideoFileCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun ResultsScreen(
    modifier: Modifier = Modifier,
    job: VideoJob,
    onClose: () -> Unit,
    onRegenerate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val videoUrl = job.storageUrl ?: job.previewUrl ?: return
    
    // Parse aspect ratio to determine video aspect
    val aspectRatio = parseAspectRatio(job.aspectRatio)
    var showFullscreen by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Video Player
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFullscreen = true },
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box {
                    ModelVideoPlayer(
                        videoUrl = videoUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio),
                        playbackEnabled = true,
                        showControls = true,
                        initialVolume = 1f,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        onVideoClick = { showFullscreen = true }
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Refresh,
                    label = "Regenerate",
                    onClick = onRegenerate,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = { shareVideo(context, videoUrl) },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Download,
                    label = "Download",
                    onClick = { downloadVideo(context, videoUrl) },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    isDestructive = true
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

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isDestructive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
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
            // Close button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
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
            
            // Video player - centered with proper aspect ratio
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
                    showControls = true,
                    initialVolume = 1f,
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    onVideoClick = null
                )
            }
        }
    }
}

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

private fun shareVideo(context: Context, videoUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // For now, share the URL directly
            // TODO: Implement FileProvider for sharing cached files
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this AI-generated video!\n$videoUrl")
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
        } catch (e: Exception) {
            android.util.Log.e("ResultsScreen", "Failed to share video", e)
        }
    }
}

private fun downloadVideo(context: Context, videoUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Download to file cache if not already cached
            // Ensure video is cached
            val cachedFile = VideoFileCache.getCachedFileUri(context, videoUrl)
            if (cachedFile == null) {
                VideoFileCache.downloadVideo(context, videoUrl)
            }
            
            // TODO: Full download to Downloads folder requires FileProvider setup
            // For now, video is cached and can be accessed from cache
            
            android.util.Log.d("ResultsScreen", "Video cached for download")
        } catch (e: Exception) {
            android.util.Log.e("ResultsScreen", "Failed to download video", e)
        }
    }
}

