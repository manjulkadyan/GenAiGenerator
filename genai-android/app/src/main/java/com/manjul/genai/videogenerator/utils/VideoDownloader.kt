package com.manjul.genai.videogenerator.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for downloading videos to the device gallery
 */
object VideoDownloader {
    private const val TAG = "VideoDownloader"

    /**
     * Downloads a video file to the gallery using MediaStore
     * Returns the URI of the saved video, or null if failed
     */
    suspend fun saveVideoToGallery(
        context: Context,
        sourceFile: File,
        displayName: String = "AI_Video_${System.currentTimeMillis()}.mp4"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                Log.e(TAG, "Source file does not exist or cannot be read: ${sourceFile.absolutePath}")
                return@withContext null
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GenAI Videos")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: run {
                    Log.e(TAG, "Failed to create MediaStore entry")
                    return@withContext null
                }

            // Copy file content
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Log.e(TAG, "Failed to open output stream")
                resolver.delete(uri, null, null)
                return@withContext null
            }

            // Mark as not pending (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Log.d(TAG, "Video saved to gallery: $uri")
            return@withContext uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video to gallery", e)
            return@withContext null
        }
    }

    /**
     * Downloads a video from URL to gallery
     * First caches the video, then saves to gallery
     */
    suspend fun downloadVideoToGallery(
        context: Context,
        videoUrl: String,
        displayName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // First, ensure video is cached
            val cachedFileUri = com.manjul.genai.videogenerator.player.VideoFileCache.getCachedFileUri(context, videoUrl)
            
            val sourceFile = if (cachedFileUri != null) {
                // Remove "file://" prefix if present
                val filePath = cachedFileUri.removePrefix("file://")
                File(filePath)
            } else {
                // Download video first
                com.manjul.genai.videogenerator.player.VideoFileCache.downloadVideo(context, videoUrl)
                val newCachedUri = com.manjul.genai.videogenerator.player.VideoFileCache.getCachedFileUri(context, videoUrl)
                if (newCachedUri != null) {
                    val filePath = newCachedUri.removePrefix("file://")
                    File(filePath)
                } else {
                    Log.e(TAG, "Failed to cache video")
                    return@withContext null
                }
            }

            val name = displayName ?: "AI_Video_${System.currentTimeMillis()}.mp4"
            return@withContext saveVideoToGallery(context, sourceFile, name)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video to gallery", e)
            return@withContext null
        }
    }
}

