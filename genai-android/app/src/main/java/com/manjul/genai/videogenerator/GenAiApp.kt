package com.manjul.genai.videogenerator

import android.app.Application
import android.content.ComponentCallbacks2
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.player.VideoPreviewCache
import kotlinx.coroutines.launch

class GenAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        
        // Initialize RepositoryProvider with context for caching
        RepositoryProvider.initialize(this)
        
        // Initialize Room database for video caching
        AppDatabase.getDatabase(this)
        
        // Clean up old cache entries on app start (older than 7 days)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                AppDatabase.getDatabase(this@GenAiApp).videoCacheDao().deleteOldEntries(sevenDaysAgo)
            } catch (e: Exception) {
                android.util.Log.e("GenAiApp", "Error cleaning old cache entries", e)
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Release all video players when system is low on memory
        VideoPlayerManager.releaseAllPlayers()
        // Optionally clear video cache (LRU eviction will handle it automatically)
        // VideoPreviewCache.clearCache()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Release players when system requests memory trim
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
            VideoPlayerManager.releaseAllPlayers()
        }
        // Aggressively clear cache on high memory pressure
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            VideoPreviewCache.clearCache()
        }
    }
}
