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
import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manjul.genai.videogenerator.data.repository.datasource.FirebaseModelDataSource
import com.manjul.genai.videogenerator.data.repository.datasource.RoomModelDataSource
import com.manjul.genai.videogenerator.data.repository.ModelRepository
import com.manjul.genai.videogenerator.data.repository.ModelRepositoryImpl
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

// Old FirebaseVideoFeatureRepository removed - now using ModelRepository with separate data sources

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
            modelId = getString("model_id"),
            negativePrompt = getString("negative_prompt"),
            enableAudio = getBoolean("enable_audio") ?: false,
            firstFrameUri = getString("first_frame_url"),
            lastFrameUri = getString("last_frame_url"),
            seed = getLong("seed")?.toInt()
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
        
        // Ensure the inputs directory exists by creating a placeholder if needed
        val inputsRef = storage.reference.child("users/$uid/inputs")
        val fileName = "${UUID.randomUUID()}.jpeg"
        val ref = inputsRef.child(fileName)
        
        return runCatching {
            // Upload with metadata to ensure proper content type
            val uploadTask = ref.putFile(uri)
            // await() will throw an exception if upload fails
            val snapshot = uploadTask.await()
            
            // If we get here, upload was successful
            // Get download URL
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        }.getOrElse { exception ->
            // Enhanced error handling
            android.util.Log.e("StorageUpload", "Upload failed", exception)
            when {
                exception.message?.contains("404") == true || 
                exception.message?.contains("Not Found") == true -> {
                    Result.failure(
                        Exception(
                            "Storage bucket not configured. Please check Firebase Storage settings.",
                            exception
                        )
                    )
                }
                exception.message?.contains("permission") == true ||
                exception.message?.contains("403") == true ||
                (exception is com.google.firebase.storage.StorageException && 
                 exception.errorCode == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED) -> {
                    Result.failure(
                        Exception(
                            "Permission denied. Please configure Firebase Storage rules to allow authenticated users to upload files. See firebase-storage-rules.txt for the correct rules.",
                            exception
                        )
                    )
                }
                else -> {
                    Result.failure(
                        Exception(
                            "Upload failed: ${exception.message ?: "Unknown error"}",
                            exception
                        )
                    )
                }
            }
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
            // TODO: Switch to "testCallReplicateVeoAPIV2" for testing (see TESTING_CREDITS.md)
            // IMPORTANT: Function name must match exactly (case-sensitive)
            val callableResult = functions
                .getHttpsCallable("testCallReplicateVeoAPIV2") // Fixed: lowercase 'c' to match deployed function
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
    
    // Context will be set when app initializes
    private var appContext: Context? = null
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val modelRepository: ModelRepository by lazy {
        if (appContext == null) {
            throw IllegalStateException("RepositoryProvider must be initialized with context before accessing modelRepository")
        }
        val localDataSource = RoomModelDataSource(appContext!!)
        val remoteDataSource = FirebaseModelDataSource(firestore)
        ModelRepositoryImpl(localDataSource, remoteDataSource)
    }
    
    // Keep for backward compatibility - delegates to modelRepository
    @Deprecated("Use modelRepository instead", ReplaceWith("modelRepository"))
    val videoFeatureRepository: VideoFeatureRepository by lazy {
        object : VideoFeatureRepository {
            override suspend fun fetchModels(): List<AIModel> {
                return modelRepository.fetchModels()
            }
        }
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

// Helper functions moved to FirestoreHelpers.kt
