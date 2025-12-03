package com.manjul.genai.videogenerator.ui.components.onboarding

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Screenshot image loader with loading and error states
 * 
 * HOW TO USE YOUR OWN SCREENSHOTS:
 * 1. Upload your screenshots to Firebase Storage in the "onboarding" folder
 * 2. Update the imageUrl in onboardingConfig.json or Firebase Firestore
 * 3. Recommended dimensions: 390px x 844px (iPhone 14/15 Pro size)
 * 4. Format: PNG or JPG
 * 
 * Current setup loads images from Firebase Storage URLs defined in:
 * - Local: genai-android/onboardingConfig.json
 * - Remote: Firebase Firestore collection "config/onboarding"
 */
@Composable
fun ScreenshotImage(
    imageUrl: String?,
    contentDescription: String,
    fallbackContent: @Composable () -> Unit = { ScreenshotPlaceholder(contentDescription) }
) {
    if (!imageUrl.isNullOrEmpty()) {
        Log.d("ScreenshotImage", "Loading: $imageUrl")
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            error = { 
                Log.e("ScreenshotImage", "Failed: $imageUrl")
                fallbackContent() 
            },
            onSuccess = {
                Log.d("ScreenshotImage", "Success: $imageUrl")
            }
        )
    } else {
        Log.d("ScreenshotImage", "Empty URL, showing fallback")
        fallbackContent()
    }
}

/**
 * Placeholder shown when screenshot is not available
 * Shows a simple gradient background with the title
 */
@Composable
fun ScreenshotPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1f3a),
                        Color(0xFF0F1420)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Simple text placeholder - no random icons
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your screenshot",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview: Screenshot loading state
 */
@androidx.compose.ui.tooling.preview.Preview(
    name = "Screenshot - Loading",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ScreenshotImageLoadingPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Simulates loading state
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }
}

/**
 * Preview: Screenshot placeholder/error state
 */
@androidx.compose.ui.tooling.preview.Preview(
    name = "Screenshot - Placeholder",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ScreenshotPlaceholderPreview() {
    ScreenshotPlaceholder(title = "Premium Features")
}

/**
 * Preview: ScreenshotImage with null URL (shows placeholder)
 */
@androidx.compose.ui.tooling.preview.Preview(
    name = "Screenshot - Null URL",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ScreenshotImageNullPreview() {
    ScreenshotImage(
        imageUrl = "null",
        contentDescription = "Premium Features"
    )
}

