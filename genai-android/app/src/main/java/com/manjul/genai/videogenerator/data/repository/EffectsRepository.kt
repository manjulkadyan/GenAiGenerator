package com.manjul.genai.videogenerator.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.manjul.genai.videogenerator.data.model.VideoEffect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching and managing video effects from Firestore
 */
interface EffectsRepository {
    fun getEffects(): Flow<List<VideoEffect>>
    fun getEffectsByCategory(category: String): Flow<List<VideoEffect>>
    fun getTrendingEffects(): Flow<List<VideoEffect>>
    fun getNewEffects(): Flow<List<VideoEffect>>
    suspend fun getEffectById(templateId: Long): VideoEffect?
    suspend fun refreshEffectsFromApi(): Result<Int>
}

class FirebaseEffectsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EffectsRepository {
    
    private val effectsCollection = firestore.collection("video_effects")
    
    override fun getEffects(): Flow<List<VideoEffect>> = callbackFlow {
        val listener = effectsCollection
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val effects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toVideoEffect()
                } ?: emptyList()
                
                trySend(effects)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getEffectsByCategory(category: String): Flow<List<VideoEffect>> = callbackFlow {
        val query = if (category == "all") {
            effectsCollection.whereEqualTo("isActive", true)
        } else {
            effectsCollection
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
        }
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val effects = snapshot?.documents?.mapNotNull { doc ->
                doc.toVideoEffect()
            } ?: emptyList()
            
            trySend(effects)
        }
        
        awaitClose { listener.remove() }
    }
    
    override fun getTrendingEffects(): Flow<List<VideoEffect>> = callbackFlow {
        val listener = effectsCollection
            .whereEqualTo("isActive", true)
            .whereEqualTo("marker", "hot")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val effects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toVideoEffect()
                } ?: emptyList()
                
                trySend(effects)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getNewEffects(): Flow<List<VideoEffect>> = callbackFlow {
        val listener = effectsCollection
            .whereEqualTo("isActive", true)
            .whereEqualTo("marker", "new")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val effects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toVideoEffect()
                } ?: emptyList()
                
                trySend(effects)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun getEffectById(templateId: Long): VideoEffect? {
        return try {
            val doc = effectsCollection.document(templateId.toString()).get().await()
            doc.toVideoEffect()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun refreshEffectsFromApi(): Result<Int> {
        // This would call a Firebase Function to refresh effects from Pixverse API
        // For now, effects are seeded via the seedEffects script
        return Result.success(0)
    }
    
    private fun com.google.firebase.firestore.DocumentSnapshot.toVideoEffect(): VideoEffect? {
        return try {
            VideoEffect(
                templateId = getLong("templateId") ?: return null,
                name = getString("name") ?: return null,
                prompt = getString("prompt") ?: "",
                duration = getLong("duration")?.toInt() ?: 5,
                previewGif = getString("previewGif") ?: "",
                previewVideo = getString("previewVideo") ?: "",
                previewImage = getString("previewImage") ?: "",
                marker = getString("marker") ?: "default",
                effectType = getString("effectType") ?: "1",
                credits = getLong("credits")?.toInt() ?: 45,
                isActive = getBoolean("isActive") ?: true,
                category = getString("category") ?: "all",
                requiredImages = getLong("requiredImages")?.toInt() ?: 1,
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Fake repository for previews and testing
 */
class FakeEffectsRepository : EffectsRepository {
    
    private val sampleEffects = listOf(
        VideoEffect(
            templateId = 308621408717184,
            name = "Muscle Surge",
            prompt = "Show off your strong muscles and have everyone hooked.",
            duration = 5,
            previewGif = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_muscle_0610.gif",
            previewVideo = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_muscle_0610.mp4",
            previewImage = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_muscle_0610.png",
            marker = "hot",
        ),
        VideoEffect(
            templateId = 315446315336768,
            name = "Kiss Kiss",
            prompt = "One click to send your kiss",
            duration = 5,
            previewGif = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_kisskiss_0610.gif",
            previewVideo = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_kisskiss_0610.mp4",
            previewImage = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_kisskiss_250610.png",
            marker = "hot",
            requiredImages = 2,
        ),
        VideoEffect(
            templateId = 340541567573824,
            name = "Fin-tastic Mermaid",
            prompt = "Dive in! Get your fins now",
            duration = 5,
            previewGif = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_mermaid_250528.gif",
            previewVideo = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_mermaid_250528.mp4",
            previewImage = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_mermaid_250528.png",
            marker = "new",
        ),
        VideoEffect(
            templateId = 349110259052160,
            name = "Earth Zoom Challenge",
            prompt = "From space to you — the world is watching",
            duration = 5,
            previewGif = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_earthzoom_250716.gif",
            previewVideo = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_earthzoom_250716.mp4",
            previewImage = "https://media.pixverse.ai/asset%2Ftemplate%2Fweb_earthzoom_250716.png",
        ),
    )
    
    override fun getEffects(): Flow<List<VideoEffect>> = callbackFlow {
        trySend(sampleEffects)
        awaitClose { }
    }
    
    override fun getEffectsByCategory(category: String): Flow<List<VideoEffect>> = getEffects()
    
    override fun getTrendingEffects(): Flow<List<VideoEffect>> = callbackFlow {
        trySend(sampleEffects.filter { it.marker == "hot" })
        awaitClose { }
    }
    
    override fun getNewEffects(): Flow<List<VideoEffect>> = callbackFlow {
        trySend(sampleEffects.filter { it.marker == "new" })
        awaitClose { }
    }
    
    override suspend fun getEffectById(templateId: Long): VideoEffect? {
        return sampleEffects.find { it.templateId == templateId }
    }
    
    override suspend fun refreshEffectsFromApi(): Result<Int> = Result.success(sampleEffects.size)
}


