package com.manjul.genai.videogenerator.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

/**
 * Manager class for handling Google Play in-app reviews.
 * Shows review dialog to users on their second visit to the app.
 */
object InAppReviewManager {
    private const val TAG = "InAppReviewManager"
    private const val PREFS_NAME = "in_app_review_prefs"
    private const val KEY_APP_OPEN_COUNT = "app_open_count"
    private const val KEY_REVIEW_SHOWN = "review_shown"
    private const val KEY_REQUIRED_OPENS = "required_opens" // Dynamic threshold
    
    private const val DEFAULT_REQUIRED_OPENS = 2 // Show on 2nd open by default
    private const val MAYBE_LATER_INCREMENT = 4 // Add 4 more opens if user clicks "Maybe Later"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Increment the app open count.
     * Call this when the app is opened.
     */
    fun incrementAppOpenCount(context: Context) {
        val prefs = getPreferences(context)
        val currentCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, currentCount + 1).apply()
        Log.d(TAG, "App open count incremented to ${currentCount + 1}")
    }
    
    /**
     * Get the current app open count.
     */
    fun getAppOpenCount(context: Context): Int {
        return getPreferences(context).getInt(KEY_APP_OPEN_COUNT, 0)
    }
    
    /**
     * Get the required number of opens to show review.
     * This increases if user clicks "Maybe Later".
     */
    private fun getRequiredOpens(context: Context): Int {
        return getPreferences(context).getInt(KEY_REQUIRED_OPENS, DEFAULT_REQUIRED_OPENS)
    }
    
    /**
     * Increase the required opens threshold (called when user clicks "Maybe Later").
     */
    fun postponeReview(context: Context) {
        val currentRequired = getRequiredOpens(context)
        val newRequired = currentRequired + MAYBE_LATER_INCREMENT
        getPreferences(context).edit().putInt(KEY_REQUIRED_OPENS, newRequired).apply()
        Log.d(TAG, "Review postponed. Now requires $newRequired app opens (was $currentRequired)")
    }
    
    /**
     * Check if the review has already been shown.
     */
    private fun isReviewShown(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_REVIEW_SHOWN, false)
    }
    
    /**
     * Mark the review as shown.
     * Call this to prevent showing the review again.
     */
    fun markReviewAsShown(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_REVIEW_SHOWN, true).apply()
        Log.d(TAG, "Review marked as shown")
    }
    
    /**
     * Check if we should show the review dialog.
     * Shows when app open count reaches the required threshold and hasn't been shown yet.
     */
    fun shouldShowReview(context: Context): Boolean {
        val openCount = getAppOpenCount(context)
        val requiredOpens = getRequiredOpens(context)
        val reviewShown = isReviewShown(context)
        val shouldShow = openCount >= requiredOpens && !reviewShown
        Log.d(TAG, "Should show review: $shouldShow (openCount=$openCount, requiredOpens=$requiredOpens, reviewShown=$reviewShown)")
        return shouldShow
    }
    
    /**
     * Request in-app review from Google Play.
     * This method handles the review flow asynchronously.
     * 
     * @param activity The current activity (required for showing the review dialog)
     * @return true if review was shown successfully, false otherwise
     * 
     * NOTE: This method does NOT mark review as shown. 
     * Call markReviewAsShown() separately after user completes the review.
     */
    suspend fun requestReview(activity: Activity): Boolean {
        return try {
            val appOpenCount = getAppOpenCount(activity)
            Log.d(TAG, "Requesting in-app review (app opened $appOpenCount times)")
            
            val reviewManager = ReviewManagerFactory.create(activity)
            
            // Request review info
            val reviewInfo = reviewManager.requestReviewFlow().await()
            Log.d(TAG, "Review info obtained successfully")
            
            // Launch the review flow
            reviewManager.launchReviewFlow(activity, reviewInfo).await()
            Log.d(TAG, "Review flow launched successfully")
            
            // Track analytics events
            AnalyticsManager.trackInAppReviewShown(appOpenCount)
            AnalyticsManager.setCustomKey("in_app_review_shown", true)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show in-app review", e)
            AnalyticsManager.trackInAppReviewFailed(e.message ?: "Unknown error")
            AnalyticsManager.recordException(e)
            false
        }
    }
    
    /**
     * Reset the review state (for testing purposes only).
     * This allows showing the review again.
     */
    fun resetForTesting(context: Context) {
        getPreferences(context).edit().clear().apply()
        Log.d(TAG, "Review state reset for testing")
    }
}

