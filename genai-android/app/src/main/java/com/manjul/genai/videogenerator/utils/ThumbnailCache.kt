package com.manjul.genai.videogenerator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Utility for caching video thumbnails to local storage
 */
object ThumbnailCache {
    private const val TAG = "ThumbnailCache"
    private const val THUMBNAIL_DIR_NAME = "thumbnails"
    private const val THUMBNAIL_QUALITY = 85

    /**
     * Get the thumbnail cache directory
     */
    private fun getThumbnailDir(context: Context): File {
        val dir = File(context.cacheDir, THUMBNAIL_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generate a filename from video URL using MD5 hash
     */
    private fun getThumbnailFileName(videoUrl: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(videoUrl.toByteArray())
        val hashString = hash.joinToString("") { "%02x".format(it) }
        return "thumb_$hashString.jpg"
    }

    /**
     * Get the thumbnail file path for a video URL
     */
    fun getThumbnailFilePath(context: Context, videoUrl: String): File {
        val fileName = getThumbnailFileName(videoUrl)
        return File(getThumbnailDir(context), fileName)
    }

    /**
     * Save a bitmap thumbnail to cache
     * Returns the file path if successful, null otherwise
     */
    suspend fun saveThumbnail(
        context: Context,
        videoUrl: String,
        bitmap: Bitmap
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = getThumbnailFilePath(context, videoUrl)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }
            Log.d(TAG, "Thumbnail saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thumbnail", e)
            null
        }
    }

    /**
     * Load a cached thumbnail from file
     * Returns the bitmap if found, null otherwise
     */
    suspend fun loadThumbnail(
        context: Context,
        videoUrl: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = getThumbnailFilePath(context, videoUrl)
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading thumbnail", e)
            null
        }
    }

    /**
     * Check if a thumbnail is cached
     */
    suspend fun isThumbnailCached(
        context: Context,
        videoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getThumbnailFilePath(context, videoUrl)
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a cached thumbnail
     */
    suspend fun deleteThumbnail(
        context: Context,
        videoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getThumbnailFilePath(context, videoUrl)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting thumbnail", e)
            false
        }
    }
}

