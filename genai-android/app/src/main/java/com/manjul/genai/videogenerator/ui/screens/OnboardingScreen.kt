package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.data.model.OnboardingPageConfig
import com.manjul.genai.videogenerator.data.repository.FirebaseOnboardingRepository
import com.manjul.genai.videogenerator.ui.screens.onboarding.*
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Main onboarding screen with fade animation
 * - Pages fade in/out when swiping or clicking buttons
 * - Swipe left/right gestures work
 * - Button navigation works
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {}
) {
    val repository = remember { FirebaseOnboardingRepository(FirebaseFirestore.getInstance()) }
    var onboardingPages by remember { mutableStateOf<List<OnboardingPageConfig>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load onboarding config from Firebase (use first 3 pages)
    LaunchedEffect(Unit) {
        val result = repository.getOnboardingConfig()
        result.getOrNull()?.let { config ->
            onboardingPages = config.pages.take(3) // Only use first 3 screens
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

    val pagerState = rememberPagerState(pageCount = { 3 }) // Fixed 3 screens
    val scope = rememberCoroutineScope()

    // Track onboarding views
    LaunchedEffect(pagerState.currentPage) {
        AnalyticsManager.log("Onboarding page viewed: ${pagerState.currentPage + 1}")
    }

    // Handlers
    val handleNext: () -> Unit = {
        scope.launch {
            if (pagerState.currentPage < 2) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val handleSkip: () -> Unit = {
        AnalyticsManager.log("Onboarding skipped at page ${pagerState.currentPage + 1}")
        onComplete()
    }

    val handleGetStarted: () -> Unit = {
        AnalyticsManager.log("Onboarding completed")
        onComplete()
    }

    // Render screens with fade effect during swipe
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val pageConfig = onboardingPages.getOrNull(page)
        
        // Calculate alpha based on page offset for fade effect
        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val alpha by animateFloatAsState(
            targetValue = if (pageOffset.absoluteValue < 1f) {
                1f - pageOffset.absoluteValue
            } else {
                0f
            },
            animationSpec = tween(durationMillis = 300),
            label = "page_alpha"
        )
        
        // Use single screen component with data from config and fade effect
        if (pageConfig != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            ) {
                OnboardingPageScreen(
                    imageUrl = pageConfig.imageUrl,
                    title = pageConfig.title,
                    description = pageConfig.subtitle,
                    isFirstPage = page == 0,
                    isLastPage = page == 2,
                    currentPage = page,
                    totalPages = 3,
                    onNext = if (page == 2) handleGetStarted else handleNext,
                    onSkip = handleSkip
                )
            }
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
