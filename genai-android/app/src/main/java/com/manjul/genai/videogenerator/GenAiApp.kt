package com.manjul.genai.videogenerator

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.StrictMode
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.manjul.genai.videogenerator.data.local.AppDatabase
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.player.VideoPreviewCache
import com.manjul.genai.videogenerator.player.VideoFileCache
import com.manjul.genai.videogenerator.player.LandingPageVideoCache
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.launch

class GenAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // StrictMode for debug builds to surface performance and resource issues early
        /*if (BuildConfig.STRICT_MODE_ENABLED || false) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }*/

        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this)
        
        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Initialize AnalyticsManager
        AnalyticsManager.initialize(this)
        
        // Set app version for Crashlytics
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", versionName)
            FirebaseCrashlytics.getInstance().setCustomKey("version_code", packageInfo.versionCode)
            AnalyticsManager.setAppVersion(versionName)
        } catch (e: Exception) {
            android.util.Log.e("GenAiApp", "Error getting package info", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        
        // Initialize RepositoryProvider with context for caching
        RepositoryProvider.initialize(this)
        
        // Initialize Room database for video caching
        AppDatabase.getDatabase(this)
        
        // Pre-cache landing page video on app start (background, non-blocking)
        // This downloads video to ExoPlayer cache (cacheDir) for instant playback
        LandingPageVideoCache.startPrecaching(this)
        Log.d("GenAiApp", "Started landing page video pre-caching")
        
        // Clean up old cache entries on app start (older than 7 days)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(this@GenAiApp)
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                // Clean up old video cache entries
                database.videoCacheDao().deleteOldEntries(sevenDaysAgo)
                // Clean up old model cache entries (older than 7 days)
                database.aiModelCacheDao().deleteOldCache(sevenDaysAgo)
                // Clean up old file cache entries (older than 7 days)
                VideoFileCache.deleteOldCache(this@GenAiApp, 7)
            } catch (e: Exception) {
                android.util.Log.e("GenAiApp", "Error cleaning old cache entries", e)
                FirebaseCrashlytics.getInstance().recordException(e)
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

    @OptIn(UnstableApi::class)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Release players when system requests memory trim
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
            VideoPlayerManager.releaseAllPlayers()
        }
        // Aggressively clear cache on high memory pressure
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            VideoPreviewCache.clearCache()
            // Also clear file cache on critical memory pressure
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                VideoFileCache.clearCache(this@GenAiApp)
            }
        }
    }
}
