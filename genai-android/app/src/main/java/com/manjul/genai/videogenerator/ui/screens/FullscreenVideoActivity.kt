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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        
        // Restore saved orientation or use default (unspecified)
        val savedOrientation = savedInstanceState?.getInt("saved_orientation", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        if (savedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            if (savedOrientation != null) {
                requestedOrientation = savedOrientation
            }
        }
        
        enableEdgeToEdge()
        setContent {
            GenAiVideoTheme {
                FullscreenVideoContent(
                    videoUrl = videoUrl,
                    aspectRatio = aspectRatio,
                    onClose = { finish() },
                    onRotateClick = { targetOrientation ->
                        requestedOrientation = targetOrientation
                    }
                )
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current orientation
        outState.putInt("saved_orientation", requestedOrientation)
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
    onClose: () -> Unit,
    onRotateClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    // Track current orientation state
    var currentOrientation by remember {
        mutableStateOf<Int?>(
            activity?.requestedOrientation?.takeIf { 
                it != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED 
            }
        )
    }
    
    // Determine optimal orientation based on aspect ratio for initial suggestion
    val isPortraitOriented = aspectRatio <= 1.0f
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
        // Video player (behind buttons)
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
        
        // Buttons container with status bar padding (on top of video)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
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
            
            // Rotation button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        // Toggle between portrait and landscape
                        val targetOrientation = when (currentOrientation) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                            else -> {
                                // Default to optimal orientation based on aspect ratio
                                if (isPortraitOriented) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                        }
                        currentOrientation = targetOrientation
                        onRotateClick(targetOrientation)
                    },
                color = Color.White.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Rotate",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

