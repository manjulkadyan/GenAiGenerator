package com.manjul.genai.videogenerator.data.repository

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.data.model.CategorizedParameters
import com.manjul.genai.videogenerator.data.model.GenerateRequest
import com.manjul.genai.videogenerator.data.model.ModelSchemaMetadata
import com.manjul.genai.videogenerator.data.model.ParameterType
import com.manjul.genai.videogenerator.data.model.SchemaParameter
import com.manjul.genai.videogenerator.data.model.UserCredits
import com.manjul.genai.videogenerator.data.model.VideoJob
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseVideoFeatureRepository(
    private val firestore: FirebaseFirestore
) : VideoFeatureRepository {
    override suspend fun fetchModels(): List<AIModel> {
        return runCatching {
            queryModels(Source.SERVER)
        }.getOrElse {
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
            exampleVideoUrl = getStringList("example_video_urls")
                .firstOrNull { it.isNotBlank() },
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

class FirebaseCreditsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CreditsRepository {
    override fun observeCredits(): Flow<UserCredits> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(UserCredits(0))
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }
        
        // Initialize user document if it doesn't exist
        val userRef = firestore.collection("users").document(uid)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Create user document with 0 credits
                userRef.set(mapOf("credits" to 0))
                    .addOnFailureListener { e ->
                        android.util.Log.e("CreditsRepo", "Failed to initialize user", e)
                    }
            }
        }
        
        val registration = userRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(UserCredits(credits = 0))
                    return@addSnapshotListener
                }
                val credits = snapshot?.getLong("credits")?.toInt() ?: 0
                trySend(UserCredits(max(0, credits)))
            }
        awaitClose { registration.remove() }
    }
}

class FirebaseVideoHistoryRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : VideoHistoryRepository {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun observeJobs(): Flow<List<VideoJob>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }
        val registration = firestore.collection("users")
            .document(uid)
            .collection("jobs")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val jobs = snapshot?.documents.orEmpty().mapNotNull { it.toVideoJob() }
                trySend(jobs)
            }
        awaitClose { registration.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun DocumentSnapshot.toVideoJob(): VideoJob? {
        val prompt = getString("prompt") ?: return null
        val modelName = getString("model_name") ?: ""
        val duration = getLong("duration_seconds")?.toInt() ?: 0
        val aspectRatio = getString("aspect_ratio") ?: ""
        val statusRaw = getString("status") ?: VideoJobStatus.QUEUED.name
        val previewUrl = getString("preview_url")
        val createdAtInstant = (getTimestamp("created_at") ?: Timestamp.now())
            .toDate()
            .toInstant()
        val status = runCatching {
            VideoJobStatus.valueOf(statusRaw.uppercase())
        }.getOrDefault(VideoJobStatus.QUEUED)
        
        // Additional fields
        val completedAtTimestamp = getTimestamp("completed_at")
        val completedAt = completedAtTimestamp?.toDate()?.toInstant()
        
        val failedAtTimestamp = getTimestamp("failed_at")
        val failedAt = failedAtTimestamp?.toDate()?.toInstant()
        
        return VideoJob(
            id = id,
            prompt = prompt,
            modelName = modelName,
            durationSeconds = duration,
            aspectRatio = aspectRatio,
            status = status,
            previewUrl = previewUrl,
            createdAt = createdAtInstant,
            storageUrl = getString("storage_url"),
            errorMessage = getString("error_message"),
            replicatePredictionId = getString("replicate_prediction_id"),
            completedAt = completedAt,
            failedAt = failedAt,
            cost = getLong("cost")?.toInt() ?: 0,
            modelId = getString("model_id")
        )
    }
}

class FirebaseVideoGenerateRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage
) : VideoGenerateRepository {
    override suspend fun uploadReferenceFrame(uri: Uri): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        val fileName = "users/$uid/inputs/${UUID.randomUUID()}.jpeg"
        val ref = storage.reference.child(fileName)
        return runCatching {
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
    }

    override suspend fun requestVideoGeneration(request: GenerateRequest): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        // Check credits before calling the function
        val userDoc = firestore.collection("users").document(uid).get().await()
        val currentCredits = userDoc.getLong("credits")?.toInt() ?: 0
        
        if (currentCredits < request.cost) {
            return Result.failure(
                IllegalStateException(
                    "Insufficient credits. Required: ${request.cost}, Available: $currentCredits"
                )
            )
        }

        val data = mapOf(
            "userId" to uid,
            "modelId" to request.model.id,
            "replicateName" to request.model.replicateName,
            "prompt" to request.prompt,
            "durationSeconds" to request.durationSeconds,
            "aspectRatio" to request.aspectRatio,
            "usePromptOptimizer" to request.usePromptOptimizer,
            "cost" to request.cost,
            "enableAudio" to request.enableAudio,
            "firstFrameUrl" to (request.firstFrameUrl ?: ""),
            "lastFrameUrl" to (request.lastFrameUrl ?: ""),
            "negativePrompt" to (request.negativePrompt ?: "")
        )

        return runCatching {
            // Call Firebase Function - it will create the job document
            val callableResult = functions
                .getHttpsCallable("callReplicateVeoAPIV2")
                .call(data)
                .await()
            
            // Function already creates the job document with status "PROCESSING"
            // No need to create it again - Firestore listener will pick up the update
            // The function returns predictionId, but we don't need to use it here
            // since the job document is created by the function
        }.map { Unit }
    }
}

object RepositoryProvider {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    val videoFeatureRepository: VideoFeatureRepository by lazy {
        FirebaseVideoFeatureRepository(firestore)
    }
    val creditsRepository: CreditsRepository by lazy {
        FirebaseCreditsRepository(auth, firestore)
    }
    val videoHistoryRepository: VideoHistoryRepository by lazy {
        FirebaseVideoHistoryRepository(auth, firestore)
    }
    val videoGenerateRepository: VideoGenerateRepository by lazy {
        FirebaseVideoGenerateRepository(auth, firestore, functions, storage)
    }
}

private fun DocumentSnapshot.getStringList(field: String): List<String> {
    val raw = get(field)
    return (raw as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
}

private fun DocumentSnapshot.getNumberList(field: String): List<Int> {
    val raw = get(field)
    return (raw as? List<*>)?.mapNotNull {
        when (it) {
            is Number -> it.toInt()
            is String -> it.toIntOrNull()
            else -> null
        }
    } ?: emptyList()
}

private fun parseSchemaParameters(data: Any?): List<SchemaParameter> {
    if (data == null) return emptyList()
    val list = data as? List<Map<String, Any?>> ?: return emptyList()
    return list.mapNotNull { paramMap ->
        try {
            SchemaParameter(
                name = paramMap["name"] as? String ?: return@mapNotNull null,
                type = parseParameterType(paramMap["type"] as? String),
                required = paramMap["required"] as? Boolean ?: false,
                nullable = paramMap["nullable"] as? Boolean ?: false,
                description = paramMap["description"] as? String,
                defaultValue = paramMap["default"],
                enumValues = (paramMap["enum"] as? List<*>)?.mapNotNull { it },
                min = (paramMap["min"] as? Number)?.toDouble(),
                max = (paramMap["max"] as? Number)?.toDouble(),
                format = paramMap["format"] as? String,
                title = paramMap["title"] as? String,
            )
        } catch (e: Exception) {
            null
        }
    }
}

private fun parseParameterType(type: String?): ParameterType {
    return when (type?.lowercase()) {
        "number", "integer" -> ParameterType.NUMBER
        "boolean" -> ParameterType.BOOLEAN
        "array" -> ParameterType.ARRAY
        "object" -> ParameterType.OBJECT
        "enum" -> ParameterType.ENUM
        else -> ParameterType.STRING
    }
}

private fun parseSchemaMetadata(data: Any?): ModelSchemaMetadata? {
    if (data == null) return null
    val map = data as? Map<String, Any?> ?: return null
    return try {
        val requiredFields = (map["required_fields"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val categorized = map["categorized"] as? Map<String, Any?> ?: return null
        
        ModelSchemaMetadata(
            requiredFields = requiredFields,
            categorized = CategorizedParameters(
                text = parseSchemaParameters(categorized["text"]),
                numeric = parseSchemaParameters(categorized["numeric"]),
                boolean = parseSchemaParameters(categorized["boolean"]),
                enum = parseSchemaParameters(categorized["enum"]),
                file = parseSchemaParameters(categorized["file"]),
            )
        )
    } catch (e: Exception) {
        null
    }
}
