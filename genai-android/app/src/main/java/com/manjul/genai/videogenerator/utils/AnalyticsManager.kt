package com.manjul.genai.videogenerator.utils

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase

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
        analytics = Firebase.analytics
    }

    // ==================== Screen View Tracking ====================

    /**
     * Track a screen view event.
     * @param screenName Name of the screen being viewed
     */
    fun trackScreenView(screenName: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    // ==================== Authentication Events ====================

    /**
     * Track anonymous sign-in event.
     */
    fun trackSignInAnonymous() {
        analytics?.logEvent("sign_in_anonymous") {
            param("method", "anonymous")
        }
    }

    /**
     * Track Google sign-in event.
     */
    fun trackSignInGoogle() {
        analytics?.logEvent("sign_in_google") {
            param("method", "google")
        }
    }

    /**
     * Track account linking event.
     * @param method The linking method (e.g., "google")
     */
    fun trackLinkAccount(method: String) {
        analytics?.logEvent("link_account_google") {
            param("method", method)
        }
    }

    /**
     * Track account merge event.
     * @param fromUserId The user ID being merged from
     * @param toUserId The user ID being merged to
     */
    fun trackAccountMerge(fromUserId: String, toUserId: String) {
        analytics?.logEvent("account_merge") {
            param("from_user_id", fromUserId)
            param("to_user_id", toUserId)
        }
    }

    /**
     * Track failed sign-in attempt.
     * @param method The sign-in method attempted
     * @param errorCode Error code if available
     * @param errorMessage Error message
     */
    fun trackSignInFailed(method: String, errorCode: String? = null, errorMessage: String? = null) {
        analytics?.logEvent("sign_in_failed") {
            param("method", method)
            errorCode?.let { param("error_code", it) }
            errorMessage?.let { param("error_message", it) }
        }
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
        analytics?.logEvent("generate_video_started") {
            param("model_id", modelId)
            param("model_name", modelName)
            param("duration_seconds", durationSeconds.toLong())
            param("aspect_ratio", aspectRatio)
            param("cost", cost.toLong())
            param("has_audio", if (hasAudio) 1L else 0L)
            param("use_prompt_optimizer", if (usePromptOptimizer) 1L else 0L)
        }
    }

    /**
     * Track video generation completed event.
     * @param modelId The AI model ID used
     * @param jobId The job ID
     * @param cost Credits cost
     */
    fun trackGenerateVideoCompleted(modelId: String, jobId: String, cost: Int) {
        analytics?.logEvent("generate_video_completed") {
            param("model_id", modelId)
            param("job_id", jobId)
            param("cost", cost.toLong())
        }
    }

    /**
     * Track video generation failed event.
     * @param modelId The AI model ID used
     * @param errorMessage Error message
     * @param errorCode Error code if available
     */
    fun trackGenerateVideoFailed(modelId: String, errorMessage: String, errorCode: String? = null) {
        analytics?.logEvent("generate_video_failed") {
            param("model_id", modelId)
            param("error_message", errorMessage)
            errorCode?.let { param("error_code", it) }
        }
    }

    /**
     * Track insufficient credits event.
     * @param requiredCredits Credits required
     * @param availableCredits Credits available
     */
    fun trackGenerateVideoInsufficientCredits(requiredCredits: Int, availableCredits: Int) {
        analytics?.logEvent("generate_video_insufficient_credits") {
            param("required_credits", requiredCredits.toLong())
            param("available_credits", availableCredits.toLong())
        }
    }

    /**
     * Track model selection event.
     * @param modelId Selected model ID
     * @param modelName Selected model name
     */
    fun trackModelSelected(modelId: String, modelName: String) {
        analytics?.logEvent("model_selected") {
            param("model_id", modelId)
            param("model_name", modelName)
        }
    }

    /**
     * Track aspect ratio selection event.
     * @param aspectRatio Selected aspect ratio
     */
    fun trackAspectRatioSelected(aspectRatio: String) {
        analytics?.logEvent("aspect_ratio_selected") {
            param("aspect_ratio", aspectRatio)
        }
    }

    /**
     * Track duration selection event.
     * @param durationSeconds Selected duration in seconds
     */
    fun trackDurationSelected(durationSeconds: Int) {
        analytics?.logEvent("duration_selected") {
            param("duration_seconds", durationSeconds.toLong())
        }
    }

    /**
     * Track prompt optimizer toggle event.
     * @param enabled Whether prompt optimizer is enabled
     */
    fun trackPromptOptimizerToggled(enabled: Boolean) {
        analytics?.logEvent("prompt_optimizer_toggled") {
            param("enabled", if (enabled) 1L else 0L)
        }
    }

    /**
     * Track audio enabled event.
     * @param enabled Whether audio is enabled
     */
    fun trackAudioEnabled(enabled: Boolean) {
        analytics?.logEvent("audio_enabled") {
            param("enabled", if (enabled) 1L else 0L)
        }
    }

    /**
     * Track reference frame upload event.
     * @param frameType Type of frame ("first" or "last")
     * @param success Whether upload was successful
     */
    fun trackReferenceFrameUploaded(frameType: String, success: Boolean) {
        analytics?.logEvent("reference_frame_uploaded") {
            param("frame_type", frameType)
            param("success", if (success) 1L else 0L)
        }
    }

    // ==================== Subscription/Billing Events ====================

    /**
     * Track purchase started event.
     * @param productId Product ID being purchased
     * @param productType Product type (subscription, etc.)
     */
    fun trackPurchaseStarted(productId: String, productType: String = "subscription") {
        analytics?.logEvent("purchase_started") {
            param("product_id", productId)
            param("product_type", productType)
        }
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
        analytics?.logEvent("purchase_completed") {
            param("product_id", productId)
            param("purchase_token", purchaseToken)
            price?.let { param("price", it) }
            currency?.let { param("currency", it) }
        }
    }

    /**
     * Track purchase failed event.
     * @param productId Product ID attempted
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    fun trackPurchaseFailed(productId: String, errorCode: Int, errorMessage: String) {
        analytics?.logEvent("purchase_failed") {
            param("product_id", productId)
            param("error_code", errorCode.toLong())
            param("error_message", errorMessage)
        }
    }

    /**
     * Track purchase cancelled event.
     * @param productId Product ID that was cancelled
     */
    fun trackPurchaseCancelled(productId: String) {
        analytics?.logEvent("purchase_cancelled") {
            param("product_id", productId)
        }
    }

    /**
     * Track subscription screen viewed event.
     */
    fun trackSubscriptionViewed() {
        analytics?.logEvent("subscription_viewed") {}
    }

    /**
     * Track subscription plan selected event.
     * @param planId Plan ID selected
     * @param planName Plan name
     */
    fun trackSubscriptionPlanSelected(planId: String, credit: Double) {
        analytics?.logEvent("subscription_plan_selected") {
            param("plan_id", planId)
            param("plan_credit", credit)
        }
    }

    // ==================== User Action Events ====================

    /**
     * Track video played event.
     * @param jobId Video job ID
     * @param modelId Model ID used for generation
     */
    fun trackVideoPlayed(jobId: String, modelId: String? = null) {
        analytics?.logEvent("video_played") {
            param("job_id", jobId)
            modelId?.let { param("model_id", it) }
        }
    }

    /**
     * Track video downloaded event.
     * @param jobId Video job ID
     */
    fun trackVideoDownloaded(jobId: String) {
        analytics?.logEvent("video_downloaded") {
            param("job_id", jobId)
        }
    }

    /**
     * Track video shared event.
     * @param jobId Video job ID
     */
    fun trackVideoShared(jobId: String) {
        analytics?.logEvent("video_shared") {
            param("job_id", jobId)
        }
    }

    /**
     * Track credits viewed event.
     */
    fun trackCreditsViewed() {
        analytics?.logEvent("credits_viewed") {}
    }

    /**
     * Track history viewed event.
     */
    fun trackHistoryViewed() {
        analytics?.logEvent("history_viewed") {}
    }

    /**
     * Track profile viewed event.
     */
    fun trackProfileViewed() {
        analytics?.logEvent("profile_viewed") {}
    }

    /**
     * Track model preview played event.
     * @param modelId Model ID
     */
    fun trackModelPreviewPlayed(modelId: String) {
        analytics?.logEvent("model_preview_played") {
            param("model_id", modelId)
        }
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

