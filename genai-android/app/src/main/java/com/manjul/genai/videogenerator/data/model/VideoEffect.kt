package com.manjul.genai.videogenerator.data.model

/**
 * Represents a Pixverse video effect template
 * These are pre-trained AI workflows that transform images into specific effects
 * like "Muscle Surge", "Mermaid", "Kiss Kiss", etc.
 */
data class VideoEffect(
    val templateId: Long,              // Pixverse template_id (e.g., 308621408717184)
    val name: String,                  // Display name (e.g., "Muscle Surge")
    val prompt: String,                // Effect description/prompt
    val duration: Int,                 // Default duration in seconds (5 or 8)
    val previewGif: String,            // GIF preview URL
    val previewVideo: String,          // Video preview URL
    val previewImage: String,          // Static image preview URL
    val marker: String = "default",    // "new", "hot", or "default"
    val effectType: String = "1",      // Effect type identifier
    val credits: Int = 45,             // Credits required (default for 5s 540p)
    val isActive: Boolean = true,      // Whether this effect is enabled
    val category: String = "all",      // Category for filtering
    val requiredImages: Int = 1,       // Number of images required (some effects need 2)
)

/**
 * Categories for grouping effects in the UI
 */
enum class EffectCategory(val displayName: String) {
    ALL("All"),
    TRENDING("🔥 Trending"),
    NEW("✨ New"),
    TRANSFORMATION("Transformation"),
    SOCIAL("Social Viral"),
    DANCE("Dance & Motion"),
    HOLIDAY("Holiday"),
    PET("Pet & Animals"),
    FUNNY("Funny"),
}

/**
 * Quality options for effect generation
 */
enum class EffectQuality(val value: String, val credits: Int) {
    TURBO_360P("360p", 45),
    STANDARD_540P("540p", 45),
    HD_720P("720p", 60),
    FULL_HD_1080P("1080p", 120),
}

/**
 * Request model for generating an effect video
 */
data class GenerateEffectRequest(
    val templateId: Long,
    val imageUrl: String,
    val quality: EffectQuality = EffectQuality.STANDARD_540P,
    val duration: Int = 5,
)

/**
 * Response from effect generation
 */
data class EffectGenerationResult(
    val jobId: String,
    val status: EffectStatus,
    val videoUrl: String? = null,
    val errorMessage: String? = null,
)

enum class EffectStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    MODERATION_FAILED,
}


