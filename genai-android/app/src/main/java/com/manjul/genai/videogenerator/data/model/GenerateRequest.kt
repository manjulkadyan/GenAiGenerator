package com.manjul.genai.videogenerator.data.model

data class GenerateRequest(
    val model: AIModel,
    val prompt: String,
    val negativePrompt: String? = null,
    val durationSeconds: Int,
    val aspectRatio: String,
    val cost: Int,
    val usePromptOptimizer: Boolean,
    val enableAudio: Boolean = false,
    val firstFrameUrl: String? = null,
    val lastFrameUrl: String? = null,
)
