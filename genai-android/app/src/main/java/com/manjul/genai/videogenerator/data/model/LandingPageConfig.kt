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

/**
 * One-time credit purchase product (INAPP type, not subscription)
 */
data class OneTimeProduct(
    val productId: String,
    val name: String,
    val credits: Int,
    val price: String,
    val isPopular: Boolean = false,
    val isBestValue: Boolean = false
)

/**
 * Purchase history item (for both subscriptions and one-time purchases)
 */
data class PurchaseHistoryItem(
    val purchaseToken: String = "",
    val productId: String = "",
    val type: PurchaseType = PurchaseType.ONE_TIME,
    val credits: Int = 0,
    val price: String = "",
    val currency: String = "USD",
    val date: Long = 0L,
    val productName: String = ""
)

/**
 * Type of purchase
 */
enum class PurchaseType {
    SUBSCRIPTION,
    ONE_TIME
}

