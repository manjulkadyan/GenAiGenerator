package com.manjul.genai.videogenerator.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages ExoPlayer instances to prevent memory leaks and limit concurrent players.
 * This ensures only a limited number of players are active at once.
 */
object VideoPlayerManager {
    private val activePlayers = ConcurrentHashMap<String, ExoPlayer>()
    private const val MAX_ACTIVE_PLAYERS = 10 // Allow more players to stay in memory (prevents rebuffering)
    
    /**
     * Register a player for a video URL
     */
    fun registerPlayer(videoUrl: String, player: ExoPlayer) {
        synchronized(this) {
            // Release existing player for this URL if any
            val hadExistingPlayer = activePlayers.containsKey(videoUrl)
            activePlayers[videoUrl]?.let { oldPlayer ->
                try {
                    oldPlayer.pause()
                    oldPlayer.stop()
                    oldPlayer.release()
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerManager", "Error releasing old player", e)
                }
            }
            
            // If we're at max capacity and this is a new player, release the oldest player
            // (excluding current URL which we just removed)
            if (!hadExistingPlayer && activePlayers.size >= MAX_ACTIVE_PLAYERS) {
                releaseOldestPlayer(videoUrl)
            }
            
            activePlayers[videoUrl] = player
        }
    }
    
    /**
     * Unregister and release a player for a video URL
     */
    fun unregisterPlayer(videoUrl: String) {
        synchronized(this) {
            activePlayers.remove(videoUrl)?.let { player ->
                try {
                    player.pause()
                    player.stop()
                    player.release()
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerManager", "Error releasing player", e)
                }
            }
        }
    }
    
    /**
     * Release the oldest player (FIFO), excluding the specified URL
     */
    private fun releaseOldestPlayer(excludeUrl: String) {
        val oldestEntry = activePlayers.entries.firstOrNull { it.key != excludeUrl }
        oldestEntry?.let { (url, player) ->
            try {
                player.pause()
                player.stop()
                player.release()
                activePlayers.remove(url)
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerManager", "Error releasing oldest player", e)
            }
        }
    }
    
    /**
     * Release all players (call on app termination or low memory)
     */
    fun releaseAllPlayers() {
        synchronized(this) {
            activePlayers.values.forEach { player ->
                try {
                    player.pause()
                    player.stop()
                    player.release()
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerManager", "Error releasing player", e)
                }
            }
            activePlayers.clear()
        }
    }
    
    /**
     * Get current active player count
     */
    fun getActivePlayerCount(): Int = activePlayers.size
}

