package com.manjul.genai.videogenerator.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
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
            } ?: error("Anonymous user is null")
        }.onFailure {
            Log.e(TAG, "Anonymous sign-in failed", it)
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
     */
    suspend fun linkWithGoogle(idToken: String): Result<FirebaseUser> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("No user signed in"))

        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentUser.linkWithCredential(credential).await()
            result.user?.also { user ->
                Log.d(TAG, "Successfully linked anonymous account with Google: ${user.uid}")
            } ?: error("Linked user is null")
        }.onFailure {
            Log.e(TAG, "Failed to link with Google", it)
        }
    }

    /**
     * Sign in with Google using idToken (for new users or when not anonymous)
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.also { user ->
                Log.d(TAG, "Google sign-in success: ${user.uid}")
            } ?: error("Google user is null")
        }.onFailure {
            Log.e(TAG, "Google sign-in failed", it)
        }
    }
}
