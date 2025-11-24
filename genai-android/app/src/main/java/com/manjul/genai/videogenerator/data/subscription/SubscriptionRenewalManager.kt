package com.manjul.genai.videogenerator.data.subscription

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages subscription renewal credit checks.
 * 
 * This replaces the scheduled server function - credits are granted when
 * the user opens the app, saving server CPU costs.
 * 
 * The function checks if any subscription renewals are due and grants credits
 * accordingly. This is called on app launch after authentication.
 */
object SubscriptionRenewalManager {
    private const val TAG = "SubscriptionRenewal"
    private val functions: FirebaseFunctions by lazy { Firebase.functions }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Check and process subscription renewals for the current user.
     * 
     * This is a non-blocking operation that runs in the background.
     * Credits are granted if any subscription renewals are due.
     * 
     * @param userId Optional user ID (defaults to current authenticated user)
     * @return Result containing renewal processing summary
     */
    suspend fun checkRenewals(userId: String? = null): Result<RenewalResult> {
        return try {
            val currentUserId = userId ?: auth.currentUser?.uid
            if (currentUserId == null) {
                Log.w(TAG, "No authenticated user - skipping renewal check")
                return Result.failure(IllegalStateException("User not authenticated"))
            }

            Log.d(TAG, "Checking subscription renewals for user: $currentUserId")

            val data = mapOf("userId" to currentUserId)
            val result = functions
                .getHttpsCallable("checkUserSubscriptionRenewal")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultData = result.data as? Map<String, Any> ?: emptyMap()

            val success = resultData["success"] as? Boolean ?: false
            val processedCount = (resultData["processedCount"] as? Number)?.toInt() ?: 0
            val totalCreditsAdded = (resultData["totalCreditsAdded"] as? Number)?.toInt() ?: 0
            val message = resultData["message"] as? String ?: "Unknown"

            @Suppress("UNCHECKED_CAST")
            val subscriptions = (resultData["subscriptions"] as? List<Map<String, Any>>)?.map {
                RenewalSubscription(
                    productId = it["productId"] as? String ?: "",
                    creditsAdded = (it["creditsAdded"] as? Number)?.toInt() ?: 0,
                    periodsPassed = (it["periodsPassed"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            if (success) {
                Log.d(
                    TAG,
                    "✅ Renewal check complete: $processedCount subscription(s) processed, " +
                        "$totalCreditsAdded credits added"
                )
                if (processedCount > 0) {
                    subscriptions.forEach { sub ->
                        Log.d(
                            TAG,
                            "  - ${sub.productId}: ${sub.creditsAdded} credits " +
                                "(${sub.periodsPassed} period(s))"
                        )
                    }
                }
            } else {
                val error = resultData["error"] as? String ?: "Unknown error"
                Log.w(TAG, "⚠️ Renewal check failed: $error")
            }

            Result.success(
                RenewalResult(
                    success = success,
                    processedCount = processedCount,
                    totalCreditsAdded = totalCreditsAdded,
                    subscriptions = subscriptions,
                    message = message
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription renewals", e)
            Result.failure(e)
        }
    }

    /**
     * Check renewals in a fire-and-forget manner (non-blocking).
     * Use this when you don't need to wait for the result.
     */
    fun checkRenewalsAsync(userId: String? = null) {
        scope.launch {
            checkRenewals(userId).onFailure { error ->
                // Log error but don't crash - this is a background operation
                Log.w(TAG, "Background renewal check failed (non-critical)", error)
            }
        }
    }
}

/**
 * Result of a subscription renewal check.
 */
data class RenewalResult(
    val success: Boolean,
    val processedCount: Int,
    val totalCreditsAdded: Int,
    val subscriptions: List<RenewalSubscription>,
    val message: String
)

/**
 * Information about a processed subscription renewal.
 */
data class RenewalSubscription(
    val productId: String,
    val creditsAdded: Int,
    val periodsPassed: Int
)

