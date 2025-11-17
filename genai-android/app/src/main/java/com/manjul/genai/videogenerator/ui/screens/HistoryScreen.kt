package com.manjul.genai.videogenerator.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import com.manjul.genai.videogenerator.ui.screens.ModelsScreen.ModelVideoPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val jobs by viewModel.jobs.collectAsState()
    var fullscreenVideoUrl by remember { mutableStateOf<String?>(null) }

    if (jobs.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("No videos yet", style = MaterialTheme.typography.titleMedium)
            Text("Start generating to see your history here.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(jobs, key = { it.id }) { job ->
                JobCard(
                    job = job,
                    onVideoClick = { url -> fullscreenVideoUrl = url }
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

@Composable
private fun JobCard(
    job: VideoJob,
    onVideoClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.prompt,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            
            Text(
                text = "Model: ${job.modelName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = "Duration: ${job.durationSeconds}s • ${job.aspectRatio} • ${job.cost} credits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            // Status with color coding
            val statusColor = when (job.status) {
                com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE -> 
                    MaterialTheme.colorScheme.primary
                com.manjul.genai.videogenerator.data.model.VideoJobStatus.FAILED -> 
                    MaterialTheme.colorScheme.error
                com.manjul.genai.videogenerator.data.model.VideoJobStatus.PROCESSING -> 
                    MaterialTheme.colorScheme.secondary
                com.manjul.genai.videogenerator.data.model.VideoJobStatus.QUEUED -> 
                    MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Text(
                text = job.status.name.lowercase().replaceFirstChar { it.titlecase() },
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Show error message if failed
            job.errorMessage?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Show video if available (complete or has preview)
            val videoUrl = when {
                job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE && job.storageUrl != null -> {
                    job.storageUrl
                }
                job.previewUrl != null && job.previewUrl.isNotBlank() -> {
                    job.previewUrl
                }
                else -> null
            }
            
            if (videoUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE) {
                        "✓ Video ready - Tap to view"
                    } else {
                        "Preview available - Tap to view"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ModelVideoPlayer(
                    videoUrl = videoUrl,
                    playbackEnabled = true,
                    onVideoClick = { onVideoClick(videoUrl) }
                )
            } else if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Video ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
