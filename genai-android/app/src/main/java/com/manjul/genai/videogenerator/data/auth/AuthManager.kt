package com.manjul.genai.videogenerator.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
}
