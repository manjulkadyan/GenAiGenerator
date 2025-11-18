package com.manjul.genai.videogenerator.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a thumbnail from a video URL
 * Handles both remote URLs and local cached files
 */
suspend fun extractVideoThumbnail(context: Context, videoUrl: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            
            // Check if video is cached locally first (prefer cached file for faster thumbnail extraction)
            val cachedFileUri = com.manjul.genai.videogenerator.player.VideoFileCache.getCachedFileUri(context, videoUrl)
            val source = cachedFileUri ?: videoUrl
            
            // Set data source - works with both file paths and URLs
            if (source.startsWith("http://") || source.startsWith("https://")) {
                // Remote URL - MediaMetadataRetriever can handle HTTP/HTTPS URLs
                retriever.setDataSource(source, HashMap())
            } else {
                // Local file path
                retriever.setDataSource(source)
            }
            
            // Get frame at 1 second (or first frame if video is shorter)
            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("VideoThumbnail", "Error extracting thumbnail from $videoUrl", e)
            null
        }
    }
}

/**
 * Composable that displays a video thumbnail extracted from a video URL
 */
@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var thumbnail by remember(videoUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }

    LaunchedEffect(videoUrl) {
        isLoading = true
        thumbnail = extractVideoThumbnail(context, videoUrl)
        isLoading = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.dp
            )
        } else if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Fallback to black background if thumbnail extraction fails
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Empty - parent will handle background
            }
        }
    }
}

