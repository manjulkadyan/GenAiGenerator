package com.manjul.genai.videogenerator.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private const val TAG = "AuthManager"

    suspend fun ensureAnonymousUser(): Result<FirebaseUser> {
        auth.currentUser?.let {
            Log.d(TAG, "Using existing anonymous user: ${it.uid}")
            return Result.success(it)
        }
        Log.d(TAG, "No cached user, signing in anonymously")
        return runCatching {
            val result = auth.signInAnonymously().await()
            result.user?.also { user ->
                Log.d(TAG, "Anonymous sign-in success: ${user.uid}")
                AnalyticsManager.trackSignInAnonymous()
                AnalyticsManager.setUserId(user.uid)
                AnalyticsManager.setIsAnonymous(true)
            } ?: error("Anonymous user is null")
        }.onFailure {
            Log.e(TAG, "Anonymous sign-in failed", it)
            AnalyticsManager.trackSignInFailed("anonymous", null, it.message)
            AnalyticsManager.recordException(it)
        }
    }

    /**
     * Check if current user is anonymous
     */
    fun isAnonymousUser(): Boolean {
        return auth.currentUser?.isAnonymous == true
    }

    /**
     * Link anonymous account with Google account using idToken
     * If the credential is already in use, signs out anonymous user and signs in with Google directly
     * @param idToken Google ID token
     * @param displayName Display name from GoogleSignIn account (available immediately)
     * @param email Email from GoogleSignIn account (available immediately)
     */
    suspend fun linkWithGoogle(
        idToken: String,
        displayName: String = "",
        email: String = ""
    ): Result<FirebaseUser> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("No user signed in"))

        if (!currentUser.isAnonymous) {
            Log.w(TAG, "Current user is not anonymous, cannot link")
            return Result.failure(IllegalStateException("Current user is not anonymous"))
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        return runCatching {
            val result = currentUser.linkWithCredential(credential).await()
            result.user?.also { user ->
                Log.d(TAG, "Successfully linked anonymous account with Google: ${user.uid}")
                
                // Use provided name/email from GoogleSignIn account (most reliable)
                // Fallback to reloaded user data if not provided
                var finalDisplayName = displayName
                var finalEmail = email
                
                if (finalDisplayName.isEmpty() || finalEmail.isEmpty()) {
                    // Reload user profile to get fresh data
                    user.reload().await()
                    val updatedUser = auth.currentUser ?: user
                    
                    if (finalDisplayName.isEmpty()) {
                        finalDisplayName = updatedUser.displayName ?: ""
                        // Try provider data as fallback
                        if (finalDisplayName.isEmpty()) {
                            finalDisplayName = updatedUser.providerData.firstOrNull { 
                                it.providerId == GoogleAuthProvider.PROVIDER_ID 
                            }?.displayName ?: ""
                        }
                    }
                    
                    if (finalEmail.isEmpty()) {
                        finalEmail = updatedUser.email ?: ""
                    }
                }
                
                Log.d(TAG, "Linked user info: name=$finalDisplayName, email=$finalEmail")
                updateUserInfoOnLink(user.uid, finalDisplayName, finalEmail)
                AnalyticsManager.trackLinkAccount("google")
                AnalyticsManager.setUserId(user.uid)
                AnalyticsManager.setIsAnonymous(false)
            } ?: error("Linked user is null")
        }.recoverCatching { error ->
            val errorCode = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            val errorMessage = error.message ?: ""
            
            Log.e(TAG, "Failed to link with Google: errorCode=$errorCode, message=$errorMessage", error)
            AnalyticsManager.trackSignInFailed("google_link", errorCode, errorMessage)
            AnalyticsManager.recordException(error)
            
            // Check if credential is already in use (error code 17025 or ERROR_CREDENTIAL_ALREADY_IN_USE)
            val isCredentialInUse = errorCode == "ERROR_CREDENTIAL_ALREADY_IN_USE" ||
                    errorCode == "17025" ||
                    errorMessage.contains("credential-already-in-use", ignoreCase = true) ||
                    errorMessage.contains("already-in-use", ignoreCase = true) ||
                    errorMessage.contains("ERROR_CREDENTIAL_ALREADY_IN_USE", ignoreCase = true)
            
            if (isCredentialInUse) {
                Log.d(TAG, "Credential already in use - merging data and signing in with Google")
                
                // Get anonymous user's data before signing out
                val anonymousUid = currentUser.uid
                val anonymousData = getAnonymousUserData(anonymousUid)
                Log.d(TAG, "Anonymous user data: credits=${anonymousData.credits}, jobs=${anonymousData.jobs.size}")
                
                // Sign out anonymous user
                auth.signOut()
                Log.d(TAG, "Signed out anonymous user")
                
                // Sign in directly with Google instead (this will use the existing account)
                val signInResult = auth.signInWithCredential(credential).await()
                var googleUser = signInResult.user ?: error("Google user is null after sign-in")
                
                Log.d(TAG, "Successfully signed in with Google (account was already linked): ${googleUser.uid}")
                
                // Use provided name/email from GoogleSignIn account (most reliable)
                // Fallback to reloaded user data if not provided
                var finalDisplayName = displayName
                var finalEmail = email
                
                if (finalDisplayName.isEmpty() || finalEmail.isEmpty()) {
                    // Reload user profile to get fresh data
                    googleUser.reload().await()
                    googleUser = auth.currentUser ?: googleUser
                    
                    if (finalDisplayName.isEmpty()) {
                        finalDisplayName = googleUser.displayName ?: ""
                        // Try provider data as fallback
                        if (finalDisplayName.isEmpty()) {
                            finalDisplayName = googleUser.providerData.firstOrNull { 
                                it.providerId == GoogleAuthProvider.PROVIDER_ID 
                            }?.displayName ?: ""
                        }
                    }
                    
                    if (finalEmail.isEmpty()) {
                        finalEmail = googleUser.email ?: ""
                    }
                }
                
                Log.d(TAG, "Google user info: name=$finalDisplayName, email=$finalEmail")
                
                // Merge anonymous user's data into Google account
                if (anonymousData.credits > 0 || anonymousData.jobs.isNotEmpty()) {
                    Log.d(TAG, "Merging data: ${anonymousData.credits} credits and ${anonymousData.jobs.size} jobs")
                    mergeUserData(
                        anonymousUid, 
                        googleUser.uid, 
                        anonymousData.credits, 
                        anonymousData.jobs,
                        finalDisplayName,
                        email
                    )
                } else {
                    Log.d(TAG, "No data to merge from anonymous user")
                    // Still update user info and store previous user ID
                    updateUserInfo(googleUser.uid, finalDisplayName, email, anonymousUid)
                }
                
                // Mark anonymous user as merged (preserve data for reference)
                markAnonymousUserAsMerged(anonymousUid, googleUser.uid)
                
                // Track account merge
                AnalyticsManager.trackAccountMerge(anonymousUid, googleUser.uid)
                AnalyticsManager.setUserId(googleUser.uid)
                AnalyticsManager.setIsAnonymous(false)
                
                return Result.success(googleUser)
            } else {
                // Re-throw other errors
                throw error
            }
        }
    }

    /**
     * Sign in with Google using idToken (for new users or when not anonymous)
     * @param idToken Google ID token
     * @param displayName Display name from GoogleSignIn account (available immediately)
     * @param email Email from GoogleSignIn account (available immediately)
     */
    suspend fun signInWithGoogle(
        idToken: String,
        displayName: String = "",
        email: String = ""
    ): Result<FirebaseUser> {
        val currentUser = auth.currentUser
        
        // If user is already signed in with Google, check if it's the same account
        if (currentUser != null && !currentUser.isAnonymous) {
            val hasGoogleProvider = currentUser.providerData.any { 
                it.providerId == GoogleAuthProvider.PROVIDER_ID 
            }
            
            if (hasGoogleProvider) {
                Log.d(TAG, "User already signed in with Google account: ${currentUser.uid}")
                // User is already signed in with Google - return success
                return Result.success(currentUser)
            }
        }
        
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            var user = result.user ?: error("Google user is null")
            
            Log.d(TAG, "Google sign-in success: ${user.uid}")
            
            // Use provided name/email from GoogleSignIn account (most reliable)
            // Fallback to reloaded user data if not provided
            var finalDisplayName = displayName
            var finalEmail = email
            
            if (finalDisplayName.isEmpty() || finalEmail.isEmpty()) {
                // Reload user profile to get fresh data
                user.reload().await()
                user = auth.currentUser ?: user
                
                if (finalDisplayName.isEmpty()) {
                    finalDisplayName = user.displayName ?: ""
                    // Try provider data as fallback
                    if (finalDisplayName.isEmpty()) {
                        finalDisplayName = user.providerData.firstOrNull { 
                            it.providerId == GoogleAuthProvider.PROVIDER_ID 
                        }?.displayName ?: ""
                    }
                }
                
                if (finalEmail.isEmpty()) {
                    finalEmail = user.email ?: ""
                }
            }
            
            // Update user info in Firestore
            if (finalDisplayName.isNotEmpty() || finalEmail.isNotEmpty()) {
                updateUserInfoOnLink(user.uid, finalDisplayName, finalEmail)
            }
            
            Log.d(TAG, "Google sign-in complete: name=$finalDisplayName, email=$finalEmail")
            AnalyticsManager.trackSignInGoogle()
            AnalyticsManager.setUserId(user.uid)
            AnalyticsManager.setIsAnonymous(false)
            user
        }.onFailure { error ->
            // Check for specific Firebase Auth errors
            val errorMessage = error.message ?: ""
            val errorCode = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            when {
                errorMessage.contains("already-in-use", ignoreCase = true) -> {
                    Log.w(TAG, "Google account already linked to another user")
                    AnalyticsManager.trackSignInFailed("google", errorCode, errorMessage)
                }
                errorMessage.contains("credential-already-in-use", ignoreCase = true) -> {
                    Log.w(TAG, "Credential already in use")
                    AnalyticsManager.trackSignInFailed("google", errorCode, errorMessage)
                }
                else -> {
                    Log.e(TAG, "Google sign-in failed", error)
                    AnalyticsManager.trackSignInFailed("google", errorCode, errorMessage)
                }
            }
            AnalyticsManager.recordException(error)
        }
    }
    
    /**
     * Get anonymous user's data (credits and jobs) before merging
     */
    private suspend fun getAnonymousUserData(anonymousUid: String): AnonymousUserData {
        return try {
            // Get credits
            val userDoc = firestore.collection("users").document(anonymousUid).get().await()
            val credits = userDoc.getLong("credits")?.toInt() ?: 0
            
            // Get jobs
            val jobsSnapshot = firestore.collection("users")
                .document(anonymousUid)
                .collection("jobs")
                .get()
                .await()
            
            val jobs = jobsSnapshot.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    // Ensure job ID is included
                    put("id", doc.id)
                }
            }
            
            Log.d(TAG, "Retrieved anonymous user data: credits=$credits, jobs=${jobs.size}")
            AnonymousUserData(credits, jobs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get anonymous user data", e)
            AnalyticsManager.recordException(e)
            AnonymousUserData(0, emptyList())
        }
    }
    
    /**
     * Merge credits and jobs from anonymous user to Google account
     * Also stores previous user ID and user info (name, email)
     */
    private suspend fun mergeUserData(
        fromUserId: String,
        toUserId: String,
        credits: Int,
        jobs: List<Map<String, Any?>>,
        displayName: String,
        email: String
    ) {
        if (fromUserId == toUserId) {
            Log.d(TAG, "Same user, no merge needed")
            return
        }
        
        try {
            val toUserRef = firestore.collection("users").document(toUserId)
            val userDoc = toUserRef.get().await()
            
            // Get existing previous user IDs or create empty list
            val existingPreviousUserIds = userDoc.get("previous_user_ids") as? List<*> ?: emptyList<Any>()
            val previousUserIds = (existingPreviousUserIds.mapNotNull { it as? String } + fromUserId).distinct()
            
            val updateData = mutableMapOf<String, Any>(
                "name" to displayName,
                "email" to email,
                "previous_user_ids" to previousUserIds,
                "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            // Merge credits
            if (credits > 0) {
                if (userDoc.exists()) {
                    // User exists, increment credits
                    toUserRef.update(
                        mapOf(
                            "credits" to com.google.firebase.firestore.FieldValue.increment(credits.toLong())
                        ) + updateData
                    ).await()
                    Log.d(TAG, "Merged $credits credits to Google account")
                } else {
                    // User doesn't exist, create with credits
                    toUserRef.set(
                        mapOf("credits" to credits) + updateData
                    ).await()
                    Log.d(TAG, "Created Google account with $credits credits")
                }
            } else {
                // No credits to merge, just update user info
                if (userDoc.exists()) {
                    toUserRef.update(updateData).await()
                } else {
                    toUserRef.set(
                        mapOf("credits" to 0) + updateData
                    ).await()
                }
            }
            
            // Merge jobs (copy to new user, skip duplicates)
            if (jobs.isNotEmpty()) {
                val batch = firestore.batch()
                var jobsMerged = 0
                
                for (jobData in jobs) {
                    val jobId = jobData["id"] as? String ?: continue
                    val jobRef = toUserRef.collection("jobs").document(jobId)
                    
                    // Check if job already exists
                    val existingJob = jobRef.get().await()
                    if (!existingJob.exists()) {
                        batch.set(jobRef, jobData)
                        jobsMerged++
                    }
                }
                
                if (jobsMerged > 0) {
                    batch.commit().await()
                    Log.d(TAG, "Merged $jobsMerged jobs to Google account")
                } else {
                    Log.d(TAG, "All jobs already exist in Google account")
                }
            }
            
            Log.d(TAG, "Data merge completed successfully. Previous user IDs: $previousUserIds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge user data", e)
            AnalyticsManager.recordException(e)
            // Don't throw - merging is best effort, user is already signed in
        }
    }
    
    /**
     * Update user info when successfully linking (first time link)
     */
    private suspend fun updateUserInfoOnLink(
        userId: String,
        displayName: String,
        email: String
    ) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            
            val updateData = mapOf(
                "name" to displayName,
                "email" to email,
                "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            if (userDoc.exists()) {
                userRef.update(updateData).await()
            } else {
                userRef.set(
                    mapOf("credits" to 0) + updateData
                ).await()
            }
            
            Log.d(TAG, "Updated user info on link: name=$displayName, email=$email")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user info on link", e)
            AnalyticsManager.recordException(e)
        }
    }
    
    /**
     * Update user info (name, email) and store previous user ID
     */
    private suspend fun updateUserInfo(
        userId: String,
        displayName: String,
        email: String,
        previousUserId: String
    ) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            
            // Get existing previous user IDs or create empty list
            val existingPreviousUserIds = userDoc.get("previous_user_ids") as? List<*> ?: emptyList<Any>()
            val previousUserIds = (existingPreviousUserIds.mapNotNull { it as? String } + previousUserId).distinct()
            
            val updateData = mapOf(
                "name" to displayName,
                "email" to email,
                "previous_user_ids" to previousUserIds,
                "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            if (userDoc.exists()) {
                userRef.update(updateData).await()
            } else {
                userRef.set(
                    mapOf("credits" to 0) + updateData
                ).await()
            }
            
            Log.d(TAG, "Updated user info: name=$displayName, email=$email, previous_user_ids=$previousUserIds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user info", e)
            AnalyticsManager.recordException(e)
        }
    }
    
    /**
     * Mark anonymous user as merged (don't delete, preserve for reference)
     * Add a flag to indicate it's been merged
     */
    private suspend fun markAnonymousUserAsMerged(anonymousUid: String, mergedToUserId: String) {
        try {
            Log.d(TAG, "Marking anonymous user as merged: $anonymousUid -> $mergedToUserId")
            
            val userRef = firestore.collection("users").document(anonymousUid)
            
            // Mark as merged instead of deleting
            userRef.update(
                mapOf(
                    "merged_to_user_id" to mergedToUserId,
                    "merged_at" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "is_merged" to true
                )
            ).await()
            
            Log.d(TAG, "Marked anonymous user as merged: $anonymousUid")
        } catch (e: Exception) {
            // Don't throw - marking is best effort
            Log.w(TAG, "Failed to mark anonymous user as merged (non-critical): $anonymousUid", e)
        }
    }
    
    /**
     * Data class to hold anonymous user's data before merge
     */
    private data class AnonymousUserData(
        val credits: Int,
        val jobs: List<Map<String, Any?>>
    )
}
