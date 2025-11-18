package com.manjul.genai.videogenerator.ui.screens

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.ui.screens.ModelVideoPlayer
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

@RequiresApi(Build.VERSION_CODES.O)
class FullscreenVideoActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_ASPECT_RATIO = "aspect_ratio"
        
        // Helper function to parse aspect ratio
        fun parseAspectRatio(ratioString: String?): Float {
            return when (ratioString) {
                "16:9" -> 16f / 9f
                "9:16" -> 9f / 16f
                "1:1" -> 1f
                "3:2" -> 3f / 2f
                "2:3" -> 2f / 3f
                "4:3" -> 4f / 3f
                "3:4" -> 3f / 4f
                else -> 16f / 9f // Default
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: run {
            finish()
            return
        }
        
        val aspectRatioString = intent.getStringExtra(EXTRA_ASPECT_RATIO)
        val aspectRatio = parseAspectRatio(aspectRatioString)
        
        // Set orientation based on aspect ratio
        // 1:1 (aspectRatio = 1.0) -> Portrait
        // 16:9 (aspectRatio ≈ 1.78) -> Landscape
        // 9:16 (aspectRatio ≈ 0.56) -> Portrait
        val isPortraitOriented = aspectRatio <= 1.0f
        requestedOrientation = if (isPortraitOriented) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        enableEdgeToEdge()
        setContent {
            GenAiVideoTheme {
                FullscreenVideoContent(
                    videoUrl = videoUrl,
                    aspectRatio = aspectRatio,
                    onClose = { finish() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up video player when activity is destroyed
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        videoUrl?.let {
            VideoPlayerManager.unregisterPlayer(it)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoContent(
    videoUrl: String,
    aspectRatio: Float,
    onClose: () -> Unit
) {
    DisposableEffect(videoUrl) {
        onDispose {
            VideoPlayerManager.unregisterPlayer(videoUrl)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Close button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            color = Color.White.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
        
        // Video player
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            ModelVideoPlayer(
                videoUrl = videoUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                playbackEnabled = true,
                showControls = false,
                initialVolume = 1f,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onVideoClick = null
            )
        }
    }
}

