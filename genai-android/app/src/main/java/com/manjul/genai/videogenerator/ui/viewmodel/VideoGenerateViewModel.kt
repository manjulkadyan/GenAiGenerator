package com.manjul.genai.videogenerator.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.model.GenerateRequest
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.data.repository.ModelRepository
import com.manjul.genai.videogenerator.data.repository.VideoGenerateRepository
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerateScreenState(
    val isLoading: Boolean = true,
    val models: List<AIModel> = emptyList(),
    val selectedModel: AIModel? = null,
    val prompt: String = "",
    val negativePrompt: String = "",
    val selectedDuration: Int? = null,
    val selectedAspectRatio: String? = null,
    val usePromptOptimizer: Boolean = true,
    val enableAudio: Boolean = false,
    val firstFrameUri: Uri? = null,
    val lastFrameUri: Uri? = null,
    val uploadMessage: String? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val estimatedCost: Int
        get() {
            val baseCost = (selectedModel?.pricePerSecond ?: 0) * (selectedDuration ?: 0)
            // Double the cost if audio is enabled
            return if (enableAudio) baseCost * 2 else baseCost
        }

    val canGenerate: Boolean
        get() {
            // Check if required fields are missing (only check if model supports the feature)
            val missingFirst = selectedModel?.supportsFirstFrame == true && 
                              selectedModel.requiresFirstFrame && 
                              firstFrameUri == null
            val missingLast = selectedModel?.supportsLastFrame == true && 
                             selectedModel.requiresLastFrame && 
                             lastFrameUri == null
            return !isLoading &&
                !isGenerating &&
                !missingFirst &&
                !missingLast &&
                prompt.isNotBlank() &&
                selectedModel != null &&
                selectedDuration != null &&
                selectedAspectRatio != null
        }
}

class VideoGenerateViewModel(
    private val featureRepository: ModelRepository,
    private val generateRepository: VideoGenerateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GenerateScreenState())
    val state: StateFlow<GenerateScreenState> = _state

    init {
        viewModelScope.launch {
            val models = featureRepository.fetchModels()
            val firstModel = models.firstOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    models = models,
                    selectedModel = firstModel,
                    selectedDuration = firstModel?.defaultDuration ?: firstModel?.durationOptions?.firstOrNull(),
                    selectedAspectRatio = firstModel?.aspectRatios?.firstOrNull()
                )
            }
        }
    }

    fun selectModel(model: AIModel) {
        _state.update {
            val supportsNegativePrompt = model.schemaMetadata?.categorized?.text?.any { it.name == "negative_prompt" } == true
            it.copy(
                selectedModel = model,
                selectedDuration = model.defaultDuration.takeIf { duration -> duration > 0 }
                    ?: model.durationOptions.firstOrNull(),
                selectedAspectRatio = model.aspectRatios.firstOrNull(),
                firstFrameUri = it.firstFrameUri,
                lastFrameUri = it.lastFrameUri,
                enableAudio = if (model.supportsAudio) it.enableAudio else false, // Reset audio if model doesn't support it
                negativePrompt = if (supportsNegativePrompt) it.negativePrompt else "", // Reset negative prompt if model doesn't support it
                errorMessage = null,
                successMessage = null
            )
        }
        AnalyticsManager.trackModelSelected(model.id, model.name)
    }

    fun updatePrompt(prompt: String) {
        _state.update { it.copy(prompt = prompt, errorMessage = null, successMessage = null) }
    }

    fun updateNegativePrompt(negativePrompt: String) {
        _state.update { it.copy(negativePrompt = negativePrompt, errorMessage = null, successMessage = null) }
    }

    fun updateDuration(duration: Int) {
        _state.update { it.copy(selectedDuration = duration, errorMessage = null, successMessage = null) }
        AnalyticsManager.trackDurationSelected(duration)
    }

    fun updateAspectRatio(ratio: String) {
        _state.update { it.copy(selectedAspectRatio = ratio, errorMessage = null, successMessage = null) }
        AnalyticsManager.trackAspectRatioSelected(ratio)
    }

    fun togglePromptOptimizer(enabled: Boolean) {
        _state.update { it.copy(usePromptOptimizer = enabled) }
        AnalyticsManager.trackPromptOptimizerToggled(enabled)
    }

    fun toggleAudio(enabled: Boolean) {
        _state.update { it.copy(enableAudio = enabled) }
        AnalyticsManager.trackAudioEnabled(enabled)
    }

    fun setFirstFrameUri(uri: Uri?) {
        _state.update { it.copy(firstFrameUri = uri, errorMessage = null, successMessage = null) }
    }

    fun setLastFrameUri(uri: Uri?) {
        _state.update { it.copy(lastFrameUri = uri, errorMessage = null, successMessage = null) }
    }

    fun generate() {
        val snapshot = _state.value
        val model = snapshot.selectedModel
        val duration = snapshot.selectedDuration
        val ratio = snapshot.selectedAspectRatio
        if (!snapshot.canGenerate || model == null || duration == null || ratio == null) {
            return
        }
        viewModelScope.launch {
            // Clear previous errors first
            _state.update { it.copy(errorMessage = null, successMessage = null) }
            
            // Upload frames first (before setting isGenerating)
            val firstUrl = snapshot.firstFrameUri?.let { uri ->
                _state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
                android.util.Log.d("VideoGenerateVM", "📤 Set uploadMessage: Uploading first frame...")
                val url = uploadReferenceFrame(uri, "first frame")
                android.util.Log.d("VideoGenerateVM", "✅ First frame upload complete: $url")
                AnalyticsManager.trackReferenceFrameUploaded("first", url != null)
                url ?: return@launch
            }
            val lastUrl = snapshot.lastFrameUri?.let { uri ->
                _state.update { it.copy(uploadMessage = "📤 Uploading last frame...") }
                android.util.Log.d("VideoGenerateVM", "📤 Set uploadMessage: Uploading last frame...")
                val url = uploadReferenceFrame(uri, "last frame")
                android.util.Log.d("VideoGenerateVM", "✅ Last frame upload complete: $url")
                AnalyticsManager.trackReferenceFrameUploaded("last", url != null)
                url ?: return@launch
            }

            // Now prepare the request and check credits BEFORE setting isGenerating
            val request = GenerateRequest(
                model = model,
                prompt = snapshot.prompt.trim(),
                negativePrompt = snapshot.negativePrompt.takeIf { it.isNotBlank() },
                durationSeconds = duration,
                aspectRatio = ratio,
                cost = snapshot.estimatedCost,
                usePromptOptimizer = snapshot.usePromptOptimizer,
                enableAudio = snapshot.enableAudio,
                firstFrameUrl = firstUrl,
                lastFrameUrl = lastUrl
            )

            // Update message before credit check
            _state.update { it.copy(uploadMessage = "✅ Frames uploaded • Submitting request...") }
            android.util.Log.d("VideoGenerateVM", "✅ Set uploadMessage: Frames uploaded • Submitting request...")
            
            
            // Track generation started
            AnalyticsManager.trackGenerateVideoStarted(
                modelId = model.id,
                modelName = model.name,
                durationSeconds = duration,
                aspectRatio = ratio,
                cost = snapshot.estimatedCost,
                hasAudio = snapshot.enableAudio,
                usePromptOptimizer = snapshot.usePromptOptimizer
            )
            
            // Call the repository - it will check credits
            generateRepository.requestVideoGeneration(request)
                .onSuccess {
                    // Only set isGenerating = true AFTER successful start (credits checked and deducted)
                    _state.update {
                        it.copy(
                            isGenerating = true,
                            uploadMessage = "✅ Request submitted • AI is generating...",
                            errorMessage = null,
                            successMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    // Don't show generating screen if credits are insufficient or other error
                    val errorMessage = throwable.message ?: "Unable to start generation"
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            uploadMessage = null,
                            errorMessage = errorMessage
                        )
                    }
                    
                    // Track failure
                    if (errorMessage.contains("Insufficient credits", ignoreCase = true)) {
                        val required = snapshot.estimatedCost
                        // Try to get available credits from error message or use 0
                        val availableMatch = errorMessage.find { it.isDigit() }?.digitToIntOrNull() ?: 0
                        AnalyticsManager.trackGenerateVideoInsufficientCredits(required, availableMatch)
                    } else {
                        AnalyticsManager.trackGenerateVideoFailed(model.id, errorMessage)
                    }
                    AnalyticsManager.recordException(throwable)
                }
        }
    }

    private suspend fun uploadReferenceFrame(uri: Uri, label: String): String? {
        _state.update { it.copy(uploadMessage = "Uploading $label...") }
        val result = generateRepository.uploadReferenceFrame(uri)
        return result.getOrElse {
            Log.e(TAG, "Failed to upload $label", it)
            AnalyticsManager.recordException(it)
            _state.update {
                it.copy(
                    isGenerating = false,
                    uploadMessage = null,
                    errorMessage = "Failed to upload $label. Please try again."
                )
            }
            null
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun resetGenerationState() {
        _state.update { 
            it.copy(
                isGenerating = false,
                uploadMessage = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    /**
     * Load generation parameters from a VideoJob for regeneration
     * This pre-fills the GenerateScreen with the same parameters used to generate the video
     */
    fun loadParametersForRegeneration(
        job: com.manjul.genai.videogenerator.data.model.VideoJob
    ) {
        viewModelScope.launch {
            // Find the model by ID
            val model = _state.value.models.find { it.id == job.modelId }
                ?: _state.value.models.firstOrNull()
            
            _state.update {
                it.copy(
                    selectedModel = model,
                    prompt = job.prompt,
                    negativePrompt = job.negativePrompt ?: "",
                    selectedDuration = job.durationSeconds,
                    selectedAspectRatio = job.aspectRatio,
                    enableAudio = job.enableAudio,
                    firstFrameUri = job.firstFrameUri?.let { android.net.Uri.parse(it) },
                    lastFrameUri = job.lastFrameUri?.let { android.net.Uri.parse(it) },
                    errorMessage = null,
                    successMessage = null,
                    isGenerating = false
                )
            }
        }
    }

    companion object {
        private const val TAG = "VideoGenerateVM"

        val Factory = viewModelFactory {
            initializer {
                VideoGenerateViewModel(
                    featureRepository = RepositoryProvider.modelRepository,
                    generateRepository = RepositoryProvider.videoGenerateRepository
                )
            }
        }
    }
}
