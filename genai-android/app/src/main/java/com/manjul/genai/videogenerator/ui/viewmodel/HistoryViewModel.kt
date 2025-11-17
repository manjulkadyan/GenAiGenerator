package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.data.repository.VideoHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: VideoHistoryRepository
) : ViewModel() {
    private val _jobs = MutableStateFlow<List<VideoJob>>(emptyList())
    val jobs: StateFlow<List<VideoJob>> = _jobs

    init {
        viewModelScope.launch {
            repository.observeJobs().collect { list ->
                _jobs.value = list
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HistoryViewModel(RepositoryProvider.videoHistoryRepository)
            }
        }
    }
}
