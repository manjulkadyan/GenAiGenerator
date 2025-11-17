package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.data.repository.VideoFeatureRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AIModelsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val models: List<AIModel> = emptyList()
)

class AIModelsViewModel(
    private val repository: VideoFeatureRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AIModelsState())
    val state: StateFlow<AIModelsState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.fetchModels() }
                .onSuccess { models ->
                    _state.value = AIModelsState(isLoading = false, models = models)
                }
                .onFailure { throwable ->
                    _state.value = AIModelsState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load models"
                    )
                }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AIModelsViewModel(RepositoryProvider.videoFeatureRepository)
            }
        }
    }
}
