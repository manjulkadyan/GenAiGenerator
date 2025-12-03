package com.manjul.genai.videogenerator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.data.model.OnboardingConfig
import com.manjul.genai.videogenerator.data.model.OnboardingPageConfig
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching onboarding configuration from Firebase Firestore
 */
interface OnboardingRepository {
    suspend fun getOnboardingConfig(): Result<OnboardingConfig>
}

class FirebaseOnboardingRepository(
    private val firestore: FirebaseFirestore
) : OnboardingRepository {
    
    override suspend fun getOnboardingConfig(): Result<OnboardingConfig> {
        return runCatching {
            val doc = firestore.collection("app")
                .document("onboarding")
                .get()
                .await()
            
            if (doc.exists()) {
                OnboardingConfig.fromFirestore(doc) ?: getDefaultConfig()
            } else {
                getDefaultConfig()
            }
        }.recoverCatching {
            // If Firebase fails, return default config
            getDefaultConfig()
        }
    }
    
    /**
     * Default onboarding configuration for new 3-screen design
     */
    private fun getDefaultConfig(): OnboardingConfig {
        return OnboardingConfig(
            pages = listOf(
                OnboardingPageConfig(
                    id = "create",
                    title = "Imagine Anything. Create Everything!",
                    subtitle = "Welcome to Gen AI Video, the app that turns your imagination into stunning videos. Simply enter your text and let our AI do the magic.",
                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/images%2Fonboarding%2FStart.png?alt=media",
                    videoUrl = null,
                    order = 0,
                    features = emptyList()
                ),
                OnboardingPageConfig(
                    id = "library",
                    title = "Manage and Organize Your Creations!",
                    subtitle = "Easily access and manage all your Gen AI videos in one place. Edit, delete, or share your masterpieces with ease.",
                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/images%2Fonboarding%2FHistory.png?alt=media",
                    videoUrl = null,
                    order = 1,
                    features = emptyList()
                ), OnboardingPageConfig(
                    id = "models",
                    title = "Choose from Multiple AI Models",
                    subtitle = "Access powerful AI models like Veo 3, Sora 2, Kling and 19 more. Each model brings unique capabilities to bring your vision to life.",
                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/images%2Fonboarding%2FModels.png?alt=media",
                    videoUrl = null,
                    order = 2,
                    features = emptyList()
                ),
//                OnboardingPageConfig(
//                    id = "premium",
//                    title = "Upgrade to Premium, Get More Possibilities",
//                    subtitle = "Enjoy more storage, advanced styles, faster processing, and priority support to enhance your video creation experience.",
//                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/onboarding%2Fpremium.jpg?alt=media",
//                    videoUrl = null,
//                    order = 3,
//                    features = emptyList()
//                )
            ),
            showAppLogo = false,
            appLogoUrl = null
        )
    }
}
