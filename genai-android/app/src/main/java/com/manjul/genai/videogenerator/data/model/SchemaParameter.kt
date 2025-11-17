package com.manjul.genai.videogenerator.data.model

/**
 * Represents a single input parameter from a model's schema
 */
data class SchemaParameter(
    val name: String,                    // Parameter name (e.g., "prompt", "seed", "guidance_scale")
    val type: ParameterType,             // Parameter type
    val required: Boolean,                // Is this parameter required?
    val nullable: Boolean = false,        // Can this parameter be null?
    val description: String? = null,     // Parameter description
    val defaultValue: Any? = null,        // Default value
    val enumValues: List<Any>? = null,   // Enum values (if type is enum)
    val min: Double? = null,              // Minimum value (for numbers)
    val max: Double? = null,              // Maximum value (for numbers)
    val format: String? = null,           // Format hint (e.g., "uri" for URLs)
    val title: String? = null,           // Human-readable title
)

enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY,
    OBJECT,
    ENUM
}

/**
 * Categorized schema parameters for easier UI rendering
 */
data class CategorizedParameters(
    val text: List<SchemaParameter> = emptyList(),      // Text inputs (prompt, negative_prompt, etc.)
    val numeric: List<SchemaParameter> = emptyList(),   // Numeric inputs (seed, guidance_scale, duration, etc.)
    val boolean: List<SchemaParameter> = emptyList(),    // Boolean flags (enable_audio, camera_fixed, etc.)
    val enum: List<SchemaParameter> = emptyList(),       // Enum selections (aspect_ratio, resolution, etc.)
    val file: List<SchemaParameter> = emptyList(),       // File/URL inputs (image, video, audio, frames, etc.)
)

/**
 * Complete schema metadata for a model
 */
data class ModelSchemaMetadata(
    val requiredFields: List<String> = emptyList(),
    val categorized: CategorizedParameters = CategorizedParameters(),
)

