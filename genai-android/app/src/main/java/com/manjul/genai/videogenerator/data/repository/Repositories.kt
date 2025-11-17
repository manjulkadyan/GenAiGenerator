package com.manjul.genai.videogenerator.data.repository

import android.net.Uri
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.model.GenerateRequest
import com.manjul.genai.videogenerator.data.model.UserCredits
import com.manjul.genai.videogenerator.data.model.VideoJob
import kotlinx.coroutines.flow.Flow

interface VideoFeatureRepository {
    suspend fun fetchModels(): List<AIModel>
}

interface CreditsRepository {
    fun observeCredits(): Flow<UserCredits>
}

interface VideoHistoryRepository {
    fun observeJobs(): Flow<List<VideoJob>>
}

interface VideoGenerateRepository {
    suspend fun uploadReferenceFrame(uri: Uri): Result<String>
    suspend fun requestVideoGeneration(request: GenerateRequest): Result<Unit>
}
