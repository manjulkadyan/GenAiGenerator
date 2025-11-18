package com.manjul.genai.videogenerator.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility class for sharing video files
 */
object VideoSharer {
    private const val TAG = "VideoSharer"
    private const val FILE_PROVIDER_AUTHORITY = "com.manjul.genai.videogenerator.fileprovider"

    /**
     * Shares a video file using FileProvider
     * Returns true if sharing was initiated, false otherwise
     */
    suspend fun shareVideo(
        context: Context,
        videoFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists() || !videoFile.canRead()) {
                Log.e(TAG, "Video file does not exist or cannot be read: ${videoFile.absolutePath}")
                return@withContext false
            }

            val uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                videoFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Video")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing video", e)
            return@withContext false
        }
    }

    /**
     * Shares a video from URL
     * First caches the video, then shares it
     */
    suspend fun shareVideoFromUrl(
        context: Context,
        videoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // First, ensure video is cached
            var cachedFileUri = com.manjul.genai.videogenerator.player.VideoFileCache.getCachedFileUri(context, videoUrl)
            
            if (cachedFileUri == null) {
                // Download video first
                com.manjul.genai.videogenerator.player.VideoFileCache.downloadVideo(context, videoUrl)
                cachedFileUri = com.manjul.genai.videogenerator.player.VideoFileCache.getCachedFileUri(context, videoUrl)
            }

            if (cachedFileUri == null) {
                Log.e(TAG, "Failed to cache video for sharing")
                return@withContext false
            }

            // Remove "file://" prefix if present
            val filePath = cachedFileUri.removePrefix("file://")
            val videoFile = File(filePath)

            return@withContext shareVideo(context, videoFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing video from URL", e)
            return@withContext false
        }
    }
}

