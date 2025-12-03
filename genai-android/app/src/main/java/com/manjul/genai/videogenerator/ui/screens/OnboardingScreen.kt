package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.data.model.OnboardingPageConfig
import com.manjul.genai.videogenerator.data.repository.FirebaseOnboardingRepository
import com.manjul.genai.videogenerator.ui.screens.onboarding.*
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.utils.AnalyticsManager

/**
 * Main onboarding screen with fade in/out animation
 * - Pure crossfade animation (no sliding)
 * - Swipe left/right to navigate
 * - Button navigation works
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {}
) {
    val repository = remember { FirebaseOnboardingRepository(FirebaseFirestore.getInstance()) }
    var onboardingPages by remember { mutableStateOf<List<OnboardingPageConfig>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(0) }
    
    // Load onboarding config from Firebase
    LaunchedEffect(Unit) {
        val result = repository.getOnboardingConfig()
        result.getOrNull()?.let { config ->
            onboardingPages = config.pages
        }
        isLoading = false
    }
    
    if (isLoading || onboardingPages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFF6D28D9)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Track onboarding views
    LaunchedEffect(currentPage) {
        AnalyticsManager.log("Onboarding page viewed: ${currentPage + 1}")
    }

    // Handlers
    val handleNext: () -> Unit = {
        if (currentPage < onboardingPages.size -1) {
            currentPage++
        }
    }

    val handleSkip: () -> Unit = {
        AnalyticsManager.log("Onboarding skipped at page ${currentPage + 1}")
        onComplete()
    }

    val handleGetStarted: () -> Unit = {
        AnalyticsManager.log("Onboarding completed")
        onComplete()
    }

    // Render screen with pure fade animation and swipe gesture detection
    var dragOffset by remember { mutableStateOf(0f) }
    
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            // Start fade in immediately while old screen is fading out
            fadeIn(
                animationSpec = tween(durationMillis = 400)
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 600)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Prevent black background
            .pointerInput(currentPage) {
                // Detect horizontal swipe gestures
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        // Only trigger page change when drag ends
                        when {
                            dragOffset < -100 && currentPage < 2 -> currentPage++ // Swipe left -> next
                            dragOffset > 100 && currentPage > 0 -> currentPage--  // Swipe right -> previous
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Accumulate drag offset
                        dragOffset += dragAmount
                    }
                )
            },
        label = "onboarding_fade_transition"
    ) { page ->
        val pageConfig = onboardingPages.getOrNull(page)
        
        // Use single screen component with data from config
        if (pageConfig != null) {
            OnboardingPageScreen(
                imageUrl = pageConfig.imageUrl,
                title = pageConfig.title,
                description = pageConfig.subtitle,
                isFirstPage = page == 0,
                isLastPage = page == onboardingPages.size,
                currentPage = page,
                totalPages = onboardingPages.size,
                onNext = if (page == (onboardingPages.size -1) ) handleGetStarted else handleNext,
                onSkip = handleSkip
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    GenAiVideoTheme {
        OnboardingScreen()
    }
}
