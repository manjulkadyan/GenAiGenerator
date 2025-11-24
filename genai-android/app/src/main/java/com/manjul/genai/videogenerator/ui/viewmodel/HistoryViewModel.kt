package com.manjul.genai.videogenerator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import com.manjul.genai.videogenerator.data.repository.VideoHistoryRepository
import com.manjul.genai.videogenerator.utils.AnalyticsManager
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
            var previousJobs = emptyList<VideoJob>()
            repository.observeJobs().collect { list ->
                // Track status changes
                list.forEach { job ->
                    val previousJob = previousJobs.find { it.id == job.id }
                    if (previousJob != null && previousJob.status != job.status) {
                        when (job.status) {
                            VideoJobStatus.COMPLETE -> {
                                AnalyticsManager.trackGenerateVideoCompleted(
                                    modelId = job.modelId ?: "unknown",
                                    jobId = job.id,
                                    cost = job.cost
                                )
                            }
                            VideoJobStatus.FAILED -> {
                                AnalyticsManager.trackGenerateVideoFailed(
                                    modelId = job.modelId ?: "unknown",
                                    errorMessage = job.errorMessage ?: "Unknown error",
                                    errorCode = null
                                )
                            }
                            else -> {}
                        }
                    }
                }
                previousJobs = list
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
