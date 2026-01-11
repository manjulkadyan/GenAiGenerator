package com.manjul.genai.videogenerator.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Manages test accounts for Google Play Store review.
 * 
 * Test accounts are granted free credits to allow reviewers to test all app features
 * without hitting the paywall. This is required by Google Play Store policy.
 * 
 * IMPORTANT: Create a Google account with one of these emails and provide
 * the credentials to Google Play Console for app review.
 */
object TestAccountManager {
    private const val TAG = "TestAccountManager"
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    /**
     * Test email addresses that get free credits.
     * 
     * HOW TO SET UP TEST ACCOUNTS:
     * 1. Go to Firebase Console → Authentication → Users
     * 2. Click "Add User" and create accounts with emails listed below
     * 3. Set any password you want (e.g., "TestPassword123!")
     * 4. Add these credentials in Google Play Console → App Access:
     *    - Go to: Play Console → Your App → App Content → App Access
     *    - Select "All or some app functionality is restricted"
     *    - Add login instructions with the test email and password
     * 5. Reviewers will sign in with this account to test the app
     */
    private val TEST_EMAILS = setOf(
        // Primary test account for Google Play reviewers
        "playstore.reviewer@genai-test.com",
        "genai.test.reviewer@gmail.com",
        // Add more test emails as needed
    )
    
    /**
     * Number of credits to grant to test accounts.
     * This should be enough to test all app features.
     */
    private const val TEST_ACCOUNT_CREDITS = 100
    
    /**
     * Check if an email is a test account
     */
    fun isTestEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val normalizedEmail = email.trim().lowercase()
        return TEST_EMAILS.any { it.lowercase() == normalizedEmail }
    }
    
    /**
     * Check if the current signed-in user is a test account
     */
    fun isCurrentUserTestAccount(): Boolean {
        val email = FirebaseAuth.getInstance().currentUser?.email
        return isTestEmail(email)
    }
    
    /**
     * Grant free credits to a test account.
     * This is called after a test account signs in.
     * 
     * @param userId The Firebase user ID
     * @param email The user's email
     * @return true if credits were granted, false otherwise
     */
    suspend fun grantTestCreditsIfNeeded(userId: String, email: String): Boolean {
        if (!isTestEmail(email)) {
            Log.d(TAG, "Not a test account: $email")
            return false
        }
        
        return try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            
            // Check if test credits were already granted
            val testCreditsGranted = userDoc.getBoolean("test_credits_granted") ?: false
            if (testCreditsGranted) {
                Log.d(TAG, "Test credits already granted for: $email")
                return true
            }
            
            // Get current credits
            val currentCredits = userDoc.getLong("credits")?.toInt() ?: 0
            
            // Grant test credits
            val updateData = mapOf(
                "credits" to (currentCredits + TEST_ACCOUNT_CREDITS),
                "test_credits_granted" to true,
                "is_test_account" to true,
                "test_credits_amount" to TEST_ACCOUNT_CREDITS,
                "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            if (userDoc.exists()) {
                userRef.update(updateData).await()
            } else {
                userRef.set(
                    updateData + mapOf(
                        "email" to email,
                        "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }
            
            Log.d(TAG, "✅ Granted $TEST_ACCOUNT_CREDITS test credits to: $email")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to grant test credits", e)
            false
        }
    }
    
    /**
     * Check if current user should skip the paywall.
     * Test accounts with credits should skip the initial BuyCreditsScreen.
     */
    suspend fun shouldSkipPaywall(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val email = user.email ?: return false
        
        if (!isTestEmail(email)) {
            return false
        }
        
        // Check if user has credits (test credits were granted)
        return try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val credits = userDoc.getLong("credits")?.toInt() ?: 0
            credits > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check test account credits", e)
            false
        }
    }
}
