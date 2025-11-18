package com.manjul.genai.videogenerator.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * File-based video cache for persistent storage.
 * Downloads videos to local files for offline playback and faster subsequent loads.
 * 
 * This complements ExoPlayer's cache by providing:
 * - Persistent storage (survives app restarts)
 * - Full video files (not just cached chunks)
 * - Offline playback capability
 */
object VideoFileCache {
    private const val CACHE_DIR_NAME = "video_cache"
    
    /**
     * Get the cache directory for video files
     */
    private fun getCacheDir(context: Context): File {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }
    
    /**
     * Generate a filename from video URL using MD5 hash
     * Format: video_{md5_hash}.mp4
     */
    fun getFileNameFromUrl(videoUrl: String): String {
        return try {
            // Try to extract filename from URL
            val uri = android.net.Uri.parse(videoUrl)
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null && lastSegment.contains(".")) {
                // Use sanitized filename from URL
                lastSegment.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            } else {
                // Generate MD5 hash of URL
                val md = MessageDigest.getInstance("MD5")
                val hashBytes = md.digest(videoUrl.toByteArray())
                val hashString = hashBytes.joinToString("") { "%02x".format(it) }
                "video_$hashString.mp4"
            }
        } catch (e: Exception) {
            // Fallback to MD5 hash
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(videoUrl.toByteArray())
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            "video_$hashString.mp4"
        }
    }
    
    /**
     * Get the cached file for a video URL
     */
    fun getCachedFile(context: Context, videoUrl: String): File {
        val fileName = getFileNameFromUrl(videoUrl)
        return File(getCacheDir(context), fileName)
    }
    
    /**
     * Check if a video is cached locally
     */
    suspend fun isCached(context: Context, videoUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = getCachedFile(context, videoUrl)
            file.exists() && file.length() > 0
        }
    }
    
    /**
     * Get the cached file URI if it exists, null otherwise
     * Returns file path as URI string for ExoPlayer
     * Validates that the file is actually readable and has content
     */
    suspend fun getCachedFileUri(context: Context, videoUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = getCachedFile(context, videoUrl)
                // Validate file exists, has content, and is readable
                if (file.exists() && file.length() > 1024 && file.canRead()) {
                    // Return file path - ExoPlayer can handle file paths directly
                    // Format: file:///absolute/path/to/file.mp4
                    "file://${file.absolutePath}"
                } else {
                    // File is invalid, delete it
                    if (file.exists()) {
                        android.util.Log.w("VideoFileCache", "Invalid cached file detected, deleting: ${file.absolutePath}")
                        file.delete()
                    }
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFileCache", "Error getting cached file URI", e)
                null
            }
        }
    }
    
    /**
     * Download video to local cache
     * Returns the cached file path on success, null on failure
     */
    suspend fun downloadVideo(
        context: Context,
        videoUrl: String,
        onProgress: ((Long, Long) -> Unit)? = null
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val cachedFile = getCachedFile(context, videoUrl)
                
                // If already cached and valid, return it
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return@withContext cachedFile
                }
                
                // Download to temp file first
                val tempFile = File(cachedFile.parent, "temp_${cachedFile.name}")
                
                val connection = java.net.URL(videoUrl).openConnection()
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                val inputStream = connection.getInputStream()
                val outputStream = java.io.FileOutputStream(tempFile)
                
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                val contentLength = connection.contentLength.toLong()
                
                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    // Report progress
                    if (contentLength > 0) {
                        onProgress?.invoke(totalBytes, contentLength)
                    }
                }
                
                inputStream.close()
                outputStream.close()
                
                // Validate download
                if (totalBytes == 0L) {
                    tempFile.delete()
                    throw Exception("Download failed: no data received")
                }
                
                // Move temp file to final location
                if (tempFile.renameTo(cachedFile)) {
                    cachedFile
                } else {
                    // Fallback: copy if rename fails
                    tempFile.copyTo(cachedFile, overwrite = true)
                    tempFile.delete()
                    cachedFile
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFileCache", "Failed to download video: $videoUrl", e)
                // Clean up temp file on error
                try {
                    val cachedFile = getCachedFile(context, videoUrl)
                    val tempFile = File(cachedFile.parent, "temp_${cachedFile.name}")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                } catch (cleanupError: Exception) {
                    // Ignore cleanup errors
                }
                null
            }
        }
    }
    
    /**
     * Delete a cached video file
     */
    suspend fun deleteCachedFile(context: Context, videoUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = getCachedFile(context, videoUrl)
                if (file.exists()) {
                    file.delete()
                }else{
                    android.util.Log.w("VideoFileCache", "No cached file to delete for URL: $videoUrl")
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFileCache", "Failed to delete cached file", e)
            }
        }
    }
    
    /**
     * Clear all cached video files
     */
    suspend fun clearCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = getCacheDir(context)
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFileCache", "Failed to clear cache", e)
            }
        }
    }
    
    /**
     * Get total cache size in bytes
     */
    suspend fun getCacheSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = getCacheDir(context)
                cacheDir.listFiles()?.sumOf { file ->
                    if (file.isFile) file.length() else 0L
                } ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    /**
     * Delete old cache files (older than specified days)
     */
    suspend fun deleteOldCache(context: Context, olderThanDays: Int) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = getCacheDir(context)
                val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
                
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoFileCache", "Failed to delete old cache", e)
            }
        }
    }
}

