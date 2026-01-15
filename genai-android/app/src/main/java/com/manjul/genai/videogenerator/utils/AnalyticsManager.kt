package com.manjul.genai.videogenerator.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized utility class for Firebase Analytics and Crashlytics.
 * Provides methods for tracking events, screen views, user properties, and error logging.
 */
object AnalyticsManager {
    private var analytics: FirebaseAnalytics? = null
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    /**
     * Initialize AnalyticsManager with application context.
     * Should be called in Application.onCreate()
     */
    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    // ==================== Screen View Tracking ====================

    /**
     * Track a screen view event.
     * @param screenName Name of the screen being viewed
     */
    fun trackScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // ==================== Authentication Events ====================

    /**
     * Track anonymous sign-in event.
     */
    fun trackSignInAnonymous() {
        val bundle = Bundle().apply {
            putString("method", "anonymous")
        }
        analytics?.logEvent("sign_in_anonymous", bundle)
    }

    /**
     * Track Google sign-in event.
     */
    fun trackSignInGoogle() {
        val bundle = Bundle().apply {
            putString("method", "google")
        }
        analytics?.logEvent("sign_in_google", bundle)
    }
    
    /**
     * Track email sign-in event.
     */
    fun trackSignInEmail() {
        val bundle = Bundle().apply {
            putString("method", "email")
        }
        analytics?.logEvent("sign_in_email", bundle)
    }
    
    /**
     * Track email sign-up event.
     */
    fun trackSignUpEmail() {
        val bundle = Bundle().apply {
            putString("method", "email")
        }
        analytics?.logEvent("sign_up_email", bundle)
    }

    /**
     * Track account linking event.
     * @param method The linking method (e.g., "google")
     */
    fun trackLinkAccount(method: String) {
        val bundle = Bundle().apply {
            putString("method", method)
        }
        analytics?.logEvent("link_account_google", bundle)
    }

    /**
     * Track account merge event.
     * @param fromUserId The user ID being merged from
     * @param toUserId The user ID being merged to
     */
    fun trackAccountMerge(fromUserId: String, toUserId: String) {
        val bundle = Bundle().apply {
            putString("from_user_id", fromUserId)
            putString("to_user_id", toUserId)
        }
        analytics?.logEvent("account_merge", bundle)
    }

    /**
     * Track failed sign-in attempt.
     * @param method The sign-in method attempted
     * @param errorCode Error code if available
     * @param errorMessage Error message
     */
    fun trackSignInFailed(method: String, errorCode: String? = null, errorMessage: String? = null) {
        val bundle = Bundle().apply {
            putString("method", method)
            errorCode?.let { putString("error_code", it) }
            errorMessage?.let { putString("error_message", it) }
        }
        analytics?.logEvent("sign_in_failed", bundle)
    }

    // ==================== Video Generation Events ====================

    /**
     * Track video generation started event.
     * @param modelId The AI model ID being used
     * @param modelName The AI model name
     * @param durationSeconds Video duration in seconds
     * @param aspectRatio Selected aspect ratio
     * @param cost Credits cost for generation
     * @param hasAudio Whether audio is enabled
     * @param usePromptOptimizer Whether prompt optimizer is enabled
     */
    fun trackGenerateVideoStarted(
        modelId: String,
        modelName: String,
        durationSeconds: Int,
        aspectRatio: String,
        cost: Int,
        hasAudio: Boolean,
        usePromptOptimizer: Boolean
    ) {
        val bundle = Bundle().apply {
            putString("model_id", modelId)
            putString("model_name", modelName)
            putLong("duration_seconds", durationSeconds.toLong())
            putString("aspect_ratio", aspectRatio)
            putLong("cost", cost.toLong())
            putLong("has_audio", if (hasAudio) 1L else 0L)
            putLong("use_prompt_optimizer", if (usePromptOptimizer) 1L else 0L)
        }
        analytics?.logEvent("generate_video_started", bundle)
    }

    /**
     * Track video generation completed event.
     * @param modelId The AI model ID used
     * @param jobId The job ID
     * @param cost Credits cost
     */
    fun trackGenerateVideoCompleted(modelId: String, jobId: String, cost: Int) {
        val bundle = Bundle().apply {
            putString("model_id", modelId)
            putString("job_id", jobId)
            putLong("cost", cost.toLong())
        }
        analytics?.logEvent("generate_video_completed", bundle)
    }

    /**
     * Track video generation failed event.
     * @param modelId The AI model ID used
     * @param errorMessage Error message
     * @param errorCode Error code if available
     */
    fun trackGenerateVideoFailed(modelId: String, errorMessage: String, errorCode: String? = null) {
        val bundle = Bundle().apply {
            putString("model_id", modelId)
            putString("error_message", errorMessage)
            errorCode?.let { putString("error_code", it) }
        }
        analytics?.logEvent("generate_video_failed", bundle)
    }

    /**
     * Track insufficient credits event.
     * @param requiredCredits Credits required
     * @param availableCredits Credits available
     */
    fun trackGenerateVideoInsufficientCredits(requiredCredits: Int, availableCredits: Int) {
        val bundle = Bundle().apply {
            putLong("required_credits", requiredCredits.toLong())
            putLong("available_credits", availableCredits.toLong())
        }
        analytics?.logEvent("generate_video_insufficient_credits", bundle)
    }

    /**
     * Track model selection event.
     * @param modelId Selected model ID
     * @param modelName Selected model name
     */
    fun trackModelSelected(modelId: String, modelName: String) {
        val bundle = Bundle().apply {
            putString("model_id", modelId)
            putString("model_name", modelName)
        }
        analytics?.logEvent("model_selected", bundle)
    }

    /**
     * Track aspect ratio selection event.
     * @param aspectRatio Selected aspect ratio
     */
    fun trackAspectRatioSelected(aspectRatio: String) {
        val bundle = Bundle().apply {
            putString("aspect_ratio", aspectRatio)
        }
        analytics?.logEvent("aspect_ratio_selected", bundle)
    }

    /**
     * Track duration selection event.
     * @param durationSeconds Selected duration in seconds
     */
    fun trackDurationSelected(durationSeconds: Int) {
        val bundle = Bundle().apply {
            putLong("duration_seconds", durationSeconds.toLong())
        }
        analytics?.logEvent("duration_selected", bundle)
    }

    /**
     * Track prompt optimizer toggle event.
     * @param enabled Whether prompt optimizer is enabled
     */
    fun trackPromptOptimizerToggled(enabled: Boolean) {
        val bundle = Bundle().apply {
            putLong("enabled", if (enabled) 1L else 0L)
        }
        analytics?.logEvent("prompt_optimizer_toggled", bundle)
    }

    /**
     * Track audio enabled event.
     * @param enabled Whether audio is enabled
     */
    fun trackAudioEnabled(enabled: Boolean) {
        val bundle = Bundle().apply {
            putLong("enabled", if (enabled) 1L else 0L)
        }
        analytics?.logEvent("audio_enabled", bundle)
    }

    /**
     * Track reference frame upload event.
     * @param frameType Type of frame ("first" or "last")
     * @param success Whether upload was successful
     */
    fun trackReferenceFrameUploaded(frameType: String, success: Boolean) {
        val bundle = Bundle().apply {
            putString("frame_type", frameType)
            putLong("success", if (success) 1L else 0L)
        }
        analytics?.logEvent("reference_frame_uploaded", bundle)
    }

    // ==================== Subscription/Billing Events ====================

    /**
     * Track purchase started event.
     * @param productId Product ID being purchased
     * @param productType Product type (subscription, etc.)
     */
    fun trackPurchaseStarted(productId: String, productType: String = "subscription") {
        val bundle = Bundle().apply {
            putString("product_id", productId)
            putString("product_type", productType)
        }
        analytics?.logEvent("purchase_started", bundle)
    }

    /**
     * Track purchase completed event.
     * @param productId Product ID purchased
     * @param purchaseToken Purchase token
     * @param price Price in micros
     * @param currency Currency code
     */
    fun trackPurchaseCompleted(
        productId: String,
        purchaseToken: String,
        price: Long? = null,
        currency: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("product_id", productId)
            putString("purchase_token", purchaseToken)
            price?.let { putLong("price", it) }
            currency?.let { putString("currency", it) }
        }
        analytics?.logEvent("purchase_completed", bundle)
    }

    /**
     * Track purchase failed event.
     * @param productId Product ID attempted
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    fun trackPurchaseFailed(productId: String, errorCode: Int, errorMessage: String) {
        val bundle = Bundle().apply {
            putString("product_id", productId)
            putLong("error_code", errorCode.toLong())
            putString("error_message", errorMessage)
        }
        analytics?.logEvent("purchase_failed", bundle)
    }

    /**
     * Track purchase cancelled event.
     * @param productId Product ID that was cancelled
     */
    fun trackPurchaseCancelled(productId: String) {
        val bundle = Bundle().apply {
            putString("product_id", productId)
        }
        analytics?.logEvent("purchase_cancelled", bundle)
    }

    /**
     * Track subscription screen viewed event.
     */
    fun trackSubscriptionViewed() {
        analytics?.logEvent("subscription_viewed", null)
    }

    /**
     * Track subscription plan selected event.
     * @param planId Plan ID selected
     * @param planName Plan name
     */
    fun trackSubscriptionPlanSelected(planId: String, credit: Double) {
        val bundle = Bundle().apply {
            putString("plan_id", planId)
            putDouble("plan_credit", credit)
        }
        analytics?.logEvent("subscription_plan_selected", bundle)
    }

    // ==================== User Action Events ====================

    /**
     * Track video played event.
     * @param jobId Video job ID
     * @param modelId Model ID used for generation
     */
    fun trackVideoPlayed(jobId: String, modelId: String? = null) {
        val bundle = Bundle().apply {
            putString("job_id", jobId)
            modelId?.let { putString("model_id", it) }
        }
        analytics?.logEvent("video_played", bundle)
    }

    /**
     * Track video downloaded event.
     * @param jobId Video job ID
     */
    fun trackVideoDownloaded(jobId: String) {
        val bundle = Bundle().apply {
            putString("job_id", jobId)
        }
        analytics?.logEvent("video_downloaded", bundle)
    }

    /**
     * Track video shared event.
     * @param jobId Video job ID
     */
    fun trackVideoShared(jobId: String) {
        val bundle = Bundle().apply {
            putString("job_id", jobId)
        }
        analytics?.logEvent("video_shared", bundle)
    }

    /**
     * Track credits viewed event.
     */
    fun trackCreditsViewed() {
        analytics?.logEvent("credits_viewed", null)
    }

    /**
     * Track history viewed event.
     */
    fun trackHistoryViewed() {
        analytics?.logEvent("history_viewed", null)
    }

    /**
     * Track profile viewed event.
     */
    fun trackProfileViewed() {
        analytics?.logEvent("profile_viewed", null)
    }

    /**
     * Track model preview played event.
     * @param modelId Model ID
     */
    fun trackModelPreviewPlayed(modelId: String) {
        val bundle = Bundle().apply {
            putString("model_id", modelId)
        }
        analytics?.logEvent("model_preview_played", bundle)
    }

    /**
     * Track in-app review shown event.
     * @param appOpenCount Number of times app has been opened
     */
    fun trackInAppReviewShown(appOpenCount: Int) {
        val bundle = Bundle().apply {
            putLong("app_open_count", appOpenCount.toLong())
        }
        analytics?.logEvent("in_app_review_shown", bundle)
    }

    /**
     * Track in-app review failed event.
     * @param errorMessage Error message
     */
    fun trackInAppReviewFailed(errorMessage: String) {
        val bundle = Bundle().apply {
            putString("error_message", errorMessage)
        }
        analytics?.logEvent("in_app_review_failed", bundle)
    }

    // ==================== Onboarding Events ====================

    /**
     * Track onboarding screen viewed event.
     */
    fun trackOnboardingViewed() {
        analytics?.logEvent("onboarding_viewed", null)
    }

    /**
     * Track onboarding page viewed event.
     * @param pageNumber Page number (1-indexed)
     * @param totalPages Total number of pages
     */
    fun trackOnboardingPageViewed(pageNumber: Int, totalPages: Int) {
        val bundle = Bundle().apply {
            putLong("page_number", pageNumber.toLong())
            putLong("total_pages", totalPages.toLong())
        }
        analytics?.logEvent("onboarding_page_viewed", bundle)
    }

    /**
     * Track onboarding completed event.
     * @param totalPages Total number of pages
     * @param completedPageNumber Last page number viewed
     */
    fun trackOnboardingCompleted(totalPages: Int, completedPageNumber: Int) {
        val bundle = Bundle().apply {
            putLong("total_pages", totalPages.toLong())
            putLong("completed_page_number", completedPageNumber.toLong())
        }
        analytics?.logEvent("onboarding_completed", bundle)
    }

    /**
     * Track onboarding skipped event.
     * @param skippedAtPage Page number where user skipped (1-indexed)
     * @param totalPages Total number of pages
     */
    fun trackOnboardingSkipped(skippedAtPage: Int, totalPages: Int) {
        val bundle = Bundle().apply {
            putLong("skipped_at_page", skippedAtPage.toLong())
            putLong("total_pages", totalPages.toLong())
        }
        analytics?.logEvent("onboarding_skipped", bundle)
    }

    // ==================== App Lifecycle Events ====================

    /**
     * Track app launch event.
     * Should be called when app starts.
     */
    fun trackAppLaunch() {
        analytics?.logEvent("app_launch", null)
    }

    /**
     * Track app session start event.
     * @param isFirstLaunch Whether this is the first app launch
     */
    /*fun trackSessionStart(isFirstLaunch: Boolean = false) {
        val bundle = Bundle().apply {
            putLong("is_first_launch", if (isFirstLaunch) 1L else 0L)
        }
        analytics?.logEvent("session_start", bundle)
    }
*/
    // ==================== One-Time Purchase Events ====================

    /**
     * Track one-time purchase plan selected event.
     * @param productId Product ID selected
     * @param credits Credits amount
     * @param price Price in micros
     * @param currency Currency code
     */
    fun trackOneTimePurchasePlanSelected(
        productId: String,
        credits: Double,
        price: Long? = null,
        currency: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("product_id", productId)
            putDouble("credits", credits)
            price?.let { putLong("price", it) }
            currency?.let { putString("currency", it) }
        }
        analytics?.logEvent("one_time_purchase_plan_selected", bundle)
    }

    /**
     * Track one-time purchase viewed event.
     */
    fun trackOneTimePurchaseViewed() {
        analytics?.logEvent("one_time_purchase_viewed", null)
    }

    // ==================== User Properties ====================

    /**
     * Set user ID for Analytics and Crashlytics.
     * @param userId User ID to set
     */
    fun setUserId(userId: String) {
        analytics?.setUserId(userId)
        crashlytics.setUserId(userId)
    }

    /**
     * Set user property for Analytics.
     * @param name Property name
     * @param value Property value
     */
    fun setUserProperty(name: String, value: String) {
        analytics?.setUserProperty(name, value)
    }

    /**
     * Set anonymous user property.
     * @param isAnonymous Whether user is anonymous
     */
    fun setIsAnonymous(isAnonymous: Boolean) {
        setUserProperty("is_anonymous", if (isAnonymous) "true" else "false")
    }

    /**
     * Set subscription status property.
     * @param status Subscription status (e.g., "active", "inactive", "none")
     */
    fun setSubscriptionStatus(status: String) {
        setUserProperty("subscription_status", status)
    }

    /**
     * Set credit balance range property.
     * @param balance Current credit balance
     */
    fun setCreditBalance(balance: Int) {
        val range = when {
            balance == 0 -> "0"
            balance < 10 -> "1-9"
            balance < 50 -> "10-49"
            balance < 100 -> "50-99"
            balance < 500 -> "100-499"
            else -> "500+"
        }
        setUserProperty("credit_balance_range", range)
        crashlytics.setCustomKey("credit_balance", balance)
    }

    /**
     * Set app version property.
     * @param version App version string
     */
    fun setAppVersion(version: String) {
        setUserProperty("app_version", version)
    }

    // ==================== Crashlytics Logging ====================

    /**
     * Log a message to Crashlytics.
     * @param message Message to log
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * Record an exception to Crashlytics.
     * @param throwable Exception to record
     */
    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    /**
     * Set a custom key for Crashlytics.
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key for Crashlytics (Int).
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key for Crashlytics (Long).
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key for Crashlytics (Boolean).
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key for Crashlytics (Float).
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key for Crashlytics (Double).
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Record a non-fatal exception to Crashlytics.
     * @param throwable Exception to record
     */
    fun recordNonFatalException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}

