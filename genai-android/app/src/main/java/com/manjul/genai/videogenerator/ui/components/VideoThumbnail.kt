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
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.utils.ThumbnailCache
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
 * Optimized to check database cache first, then file cache, then extract if needed
 */
@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    jobId: String? = null // Optional job ID to check/update DB cache
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val jobDao = remember { database.videoJobDao() }
    
    var thumbnail by remember(videoUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }

    LaunchedEffect(videoUrl, jobId) {
        isLoading = true
        thumbnail = null
        
        // Step 1: Check if thumbnail path is stored in DB (if jobId provided)
        var cachedThumbnail: Bitmap? = null
        if (jobId != null) {
            try {
                val job = jobDao.getJobById(jobId)
                if (job?.thumbnailPath != null) {
                    val file = java.io.File(job.thumbnailPath)
                    if (file.exists() && file.length() > 0) {
                        cachedThumbnail = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoThumbnail", "Error loading thumbnail from DB", e)
            }
        }
        
        // Step 2: If not in DB, check file cache
        if (cachedThumbnail == null) {
            cachedThumbnail = ThumbnailCache.loadThumbnail(context, videoUrl)
        }
        
        // Step 3: If still not found, extract from video
        if (cachedThumbnail == null) {
            cachedThumbnail = extractVideoThumbnail(context, videoUrl)
            
            // Step 4: Save extracted thumbnail to cache
            if (cachedThumbnail != null) {
                val thumbnailPath = ThumbnailCache.saveThumbnail(context, videoUrl, cachedThumbnail)
                
                // Step 5: Update DB with thumbnail path (if jobId provided)
                if (thumbnailPath != null && jobId != null) {
                    try {
                        jobDao.updateThumbnailPath(jobId, thumbnailPath)
                    } catch (e: Exception) {
                        android.util.Log.e("VideoThumbnail", "Error updating thumbnail path in DB", e)
                    }
                }
            }
        }
        
        thumbnail = cachedThumbnail
        isLoading = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
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

