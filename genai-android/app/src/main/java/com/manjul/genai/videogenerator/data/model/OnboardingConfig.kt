package com.manjul.genai.videogenerator.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Configuration for a single onboarding page
 */
data class OnboardingPageConfig(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val order: Int = 0,
    val features: List<String> = emptyList()
)

/**
 * Complete onboarding configuration from Firebase
 */
data class OnboardingConfig(
    val pages: List<OnboardingPageConfig> = emptyList(),
    val showAppLogo: Boolean = true,
    val appLogoUrl: String? = null
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): OnboardingConfig? {
            return try {
                val pagesData = doc.get("pages") as? List<Map<String, Any>> ?: emptyList()
                val pages = pagesData.mapIndexed { index, pageData ->
                    OnboardingPageConfig(
                        id = pageData["id"] as? String ?: "page_$index",
                        title = pageData["title"] as? String ?: "",
                        subtitle = pageData["subtitle"] as? String ?: "",
                        imageUrl = pageData["imageUrl"] as? String,
                        videoUrl = pageData["videoUrl"] as? String,
                        thumbnailUrl = pageData["thumbnailUrl"] as? String,
                        order = (pageData["order"] as? Long)?.toInt() ?: index,
                        features = (pageData["features"] as? List<String>) ?: emptyList()
                    )
                }.sortedBy { it.order }
                
                OnboardingConfig(
                    pages = pages,
                    showAppLogo = doc.getBoolean("showAppLogo") ?: true,
                    appLogoUrl = doc.getString("appLogoUrl")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}





