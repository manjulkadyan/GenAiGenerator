package com.manjul.genai.videogenerator.data.repository

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.model.GenerateRequest
import com.manjul.genai.videogenerator.data.model.UserCredits
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private object FakeDataStore {
    val models: List<AIModel> = listOf(
        AIModel(
            id = "veo-1",
            name = "Veo Cinematic",
            description = "Rich storytelling scenes with cinematic color grading.",
            pricePerSecond = 10,
            defaultDuration = 6,
            durationOptions = listOf(4, 6, 8, 10),
            aspectRatios = listOf("16:9", "9:16", "1:1"),
            requiresFirstFrame = false,
            requiresLastFrame = false,
            previewUrl = "",
            replicateName = "replicate/veo-cinematic",
            exampleVideoUrls = listOf("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
            supportsReferenceImages = true,
            maxReferenceImages = 5,
            supportsAudio = true,
            hardware = "A100",
            runCount = 10000L,
            tags = listOf("cinematic", "high-quality"),
            githubUrl = "https://github.com/example/veo",
            paperUrl = "https://arxiv.org/example",
            licenseUrl = "https://example.com/license",
            coverImageUrl = "https://example.com/cover.jpg"
        ),
        AIModel(
            id = "veo-portrait",
            name = "Veo Portrait",
            description = "Optimized for social portrait content with vivid palettes.",
            pricePerSecond = 8,
            defaultDuration = 5,
            durationOptions = listOf(3, 5, 7),
            aspectRatios = listOf("9:16", "3:4"),
            requiresFirstFrame = true,
            requiresLastFrame = false,
            previewUrl = "",
            replicateName = "replicate/veo-portrait",
            exampleVideoUrls = listOf("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            supportsReferenceImages = false,
            supportsAudio = false,
            hardware = "T4",
            runCount = 5000L,
            tags = listOf("portrait", "social"),
            coverImageUrl = "https://example.com/portrait-cover.jpg"
        )
    )

    val credits: MutableStateFlow<UserCredits> = MutableStateFlow(UserCredits(credits = 120))
    @RequiresApi(Build.VERSION_CODES.O)
    val jobs: MutableStateFlow<List<VideoJob>> = MutableStateFlow(
        listOf(
            VideoJob(
                id = UUID.randomUUID().toString(),
                prompt = "A serene forest with glowing spirits",
                modelName = "Veo Cinematic",
                durationSeconds = 6,
                aspectRatio = "16:9",
                status = VideoJobStatus.COMPLETE,
                previewUrl = "https://example.com/video/preview.mp4",
                createdAt = Instant.now().minusSeconds(3600),
                storageUrl = "https://example.com/video/full.mp4",
                completedAt = Instant.now().minusSeconds(3500),
                cost = 60,
                modelId = "veo-1",
                replicatePredictionId = UUID.randomUUID().toString()
            )
        )
    )
}

class FakeVideoFeatureRepository : VideoFeatureRepository {
    override suspend fun fetchModels(): List<AIModel> = FakeDataStore.models
}

class FakeCreditsRepository : CreditsRepository {
    override fun observeCredits(): Flow<UserCredits> = FakeDataStore.credits
}

class FakeVideoHistoryRepository : VideoHistoryRepository {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun observeJobs(): Flow<List<VideoJob>> = FakeDataStore.jobs
}

class FakeVideoGenerateRepository : VideoGenerateRepository {
    override suspend fun uploadReferenceFrame(uri: Uri): Result<String> {
        delay(200)
        return Result.success("https://example.com/reference/${UUID.randomUUID()}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun requestVideoGeneration(request: GenerateRequest): Result<Unit> {
        delay(500) // simulate upload
        val currentCredits = FakeDataStore.credits.value.credits
        if (currentCredits < request.cost) {
            return Result.failure(IllegalStateException("Not enough credits"))
        }
        FakeDataStore.credits.value = UserCredits(max(0, currentCredits - request.cost))

        val job = VideoJob(
            id = UUID.randomUUID().toString(),
            prompt = request.prompt,
            modelName = request.model.name,
            durationSeconds = request.durationSeconds,
            aspectRatio = request.aspectRatio,
            status = VideoJobStatus.PROCESSING,
            previewUrl = null,
            createdAt = Instant.now(),
            cost = request.cost,
            modelId = request.model.id,
            replicatePredictionId = UUID.randomUUID().toString()
        )
        FakeDataStore.jobs.update { listOf(job) + it }

        delay(800) // simulate processing
        FakeDataStore.jobs.update { jobs ->
            jobs.map {
                if (it.id == job.id) {
                    it.copy(
                        status = VideoJobStatus.COMPLETE,
                        previewUrl = "https://example.com/video/${it.id}.mp4",
                        storageUrl = "https://example.com/video/${it.id}.mp4",
                        completedAt = Instant.now()
                    )
                } else {
                    it
                }
            }
        }
        return Result.success(Unit)
    }
}
