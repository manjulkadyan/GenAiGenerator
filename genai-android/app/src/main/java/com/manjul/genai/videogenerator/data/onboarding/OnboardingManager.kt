package com.manjul.genai.videogenerator.data.onboarding

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages onboarding state - whether user has completed onboarding or not.
 */
object OnboardingManager {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    
    private var prefs: SharedPreferences? = null
    
    /**
     * Initialize the manager with application context.
     * Should be called from Application.onCreate()
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if user has completed onboarding.
     */
    fun hasCompletedOnboarding(): Boolean {
        return prefs?.getBoolean(KEY_ONBOARDING_COMPLETED, false) ?: false
    }
    
    /**
     * Mark onboarding as completed.
     */
    fun setOnboardingCompleted() {
        prefs?.edit()?.putBoolean(KEY_ONBOARDING_COMPLETED, true)?.apply()
    }
    
    /**
     * Reset onboarding state (for testing/debugging).
     */
    fun resetOnboarding() {
        prefs?.edit()?.putBoolean(KEY_ONBOARDING_COMPLETED, false)?.apply()
    }
}


