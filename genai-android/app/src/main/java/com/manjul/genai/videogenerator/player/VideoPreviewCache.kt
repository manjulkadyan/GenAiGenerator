package com.manjul.genai.videogenerator.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object VideoPreviewCache {
    private const val CACHE_SIZE_BYTES = 200L * 1024 * 1024 // 200 MB
    @Volatile
    private var cache: Cache? = null

    @OptIn(UnstableApi::class)
    fun get(context: Context): Cache {
        val appContext = context.applicationContext
        return cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(appContext.cacheDir, "preview_videos"),
                LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(appContext)
            ).also { cache = it }
        }
    }
}
