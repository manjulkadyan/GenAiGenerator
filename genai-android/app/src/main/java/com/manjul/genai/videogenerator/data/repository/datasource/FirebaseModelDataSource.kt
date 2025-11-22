package com.manjul.genai.videogenerator.data.repository.datasource

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.repository.getStringList
import com.manjul.genai.videogenerator.data.repository.getNumberList
import com.manjul.genai.videogenerator.data.repository.parseSchemaMetadata
import com.manjul.genai.videogenerator.data.repository.parseSchemaParameters
import kotlinx.coroutines.tasks.await

class FirebaseModelDataSource(
    private val firestore: FirebaseFirestore
) : RemoteModelDataSource {
    
    override suspend fun fetchModels(): List<AIModel> {
        return runCatching {
            // Try server first
            queryModels(Source.SERVER)
        }.getOrElse {
            // If server fails, try cache as fallback
            queryModels(Source.CACHE)
        }
    }
    
    private suspend fun queryModels(source: Source): List<AIModel> {
        val snapshot = firestore.collection("video_features")
            .orderBy("index", Query.Direction.ASCENDING)
            .get(source)
            .await()
        return snapshot.documents.mapNotNull { it.toAIModel() }
    }
    
    private fun DocumentSnapshot.toAIModel(): AIModel? {
        val name = getString("name") ?: return null
        val replicateName = getString("replicate_name") ?: return null
        return AIModel(
            id = id,
            name = name,
            description = getString("description") ?: "",
            pricePerSecond = getLong("price_per_sec")?.toInt() ?: 0,
            defaultDuration = getLong("default_duration")?.toInt() ?: 0,
            durationOptions = getNumberList("duration_options"),
            aspectRatios = getStringList("aspect_ratios"),
            supportsFirstFrame = getBoolean("supports_first_frame") ?: false,
            requiresFirstFrame = getBoolean("requires_first_frame") ?: false,
            supportsLastFrame = getBoolean("supports_last_frame") ?: false,
            requiresLastFrame = getBoolean("requires_last_frame") ?: false,
            previewUrl = getString("preview_url") ?: "",
            replicateName = replicateName,
            exampleVideoUrls = getStringList("example_video_urls"),
            // Additional fields
            supportsReferenceImages = getBoolean("supports_reference_images") ?: false,
            maxReferenceImages = getLong("max_reference_images")?.toInt(),
            supportsAudio = getBoolean("supports_audio") ?: false,
            hardware = getString("hardware"),
            runCount = getLong("run_count"),
            tags = getStringList("tags"),
            githubUrl = getString("github_url"),
            paperUrl = getString("paper_url"),
            licenseUrl = getString("license_url"),
            coverImageUrl = getString("cover_image_url"),
            // Parse dynamic schema parameters
            schemaParameters = parseSchemaParameters(get("schema_parameters")),
            schemaMetadata = parseSchemaMetadata(get("schema_metadata"))
        )
    }
}

