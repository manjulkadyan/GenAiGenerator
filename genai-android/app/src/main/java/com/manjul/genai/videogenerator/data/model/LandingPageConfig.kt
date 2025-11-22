package com.manjul.genai.videogenerator.data.model

/**
 * Data models for landing page configuration from Firebase.
 */
data class LandingPageConfig(
    val backgroundVideoUrl: String = "",
    val features: List<LandingPageFeature> = emptyList(),
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    val testimonials: List<Testimonial> = emptyList()
)

data class LandingPageFeature(
    val title: String,
    val description: String,
    val icon: String // Icon identifier (e.g., "gear", "flame", "sound", etc.)
)

data class SubscriptionPlan(
    val credits: Int,
    val price: String,
    val isPopular: Boolean = false,
    val productId: String, // Google Play product ID
    val period: String = "Weekly" // Subscription period
)

data class Testimonial(
    val username: String,
    val rating: Int, // 1-5 stars
    val text: String
)

