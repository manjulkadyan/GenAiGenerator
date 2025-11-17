package com.manjul.genai.videogenerator.data.model

data class AIModel(
    val id: String,
    val name: String,
    val description: String,
    val pricePerSecond: Int,
    val defaultDuration: Int,
    val durationOptions: List<Int>,
    val aspectRatios: List<String>,
    val supportsFirstFrame: Boolean = false,  // Model supports first frame feature
    val requiresFirstFrame: Boolean = false,   // First frame is required (only meaningful if supportsFirstFrame is true)
    val supportsLastFrame: Boolean = false,     // Model supports last frame feature
    val requiresLastFrame: Boolean = false,    // Last frame is required (only meaningful if supportsLastFrame is true)
    val previewUrl: String,
    val replicateName: String,
    val exampleVideoUrl: String? = null,
    // Additional fields from scraped data
    val supportsReferenceImages: Boolean = false,
    val maxReferenceImages: Int? = null,
    val supportsAudio: Boolean = false,
    val hardware: String? = null,
    val runCount: Long? = null,
    val tags: List<String> = emptyList(),
    val githubUrl: String? = null,
    val paperUrl: String? = null,
    val licenseUrl: String? = null,
    val coverImageUrl: String? = null,
    // Dynamic schema parameters - ALL input parameters from the model's schema
    val schemaParameters: List<SchemaParameter> = emptyList(),  // All parameters with full metadata
    val schemaMetadata: ModelSchemaMetadata? = null,            // Categorized parameters for UI
)
