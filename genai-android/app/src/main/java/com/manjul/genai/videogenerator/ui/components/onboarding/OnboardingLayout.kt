package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Base layout for all onboarding screens
 * NEW LAYOUT STRUCTURE (matching Imagify design):
 *
 * ┌─────────────────────────┐
 * │  Purple Gradient Top    │
 * │  [Optional Logo]        │
 * │  [iPhone Mockup]        │
 * ├─────────────────────────┤
 * │  White Bottom Section   │
 * │  [Title]                │
 * │  [Description]          │
 * │  [Page Indicators]      │
 * │  [Skip + Continue]      │
 * └─────────────────────────┘
 */
@Composable
fun OnboardingLayout(
    currentPage: Int,
    totalPages: Int,
    mockupContent: @Composable () -> Unit,
    title: String,
    description: String,
    buttons: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP SECTION: Purple gradient with iPhone mockup (60% height, straight bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // Takes exactly 60% from top
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED), // Purple 600
                            Color(0xFF6D28D9)  // Purple 700
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    mockupContent()
                }
            }
        }

        // BOTTOM SECTION: White card (50% height, starts at 50%, overlaps purple by 10%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f) // Takes exactly 50% from bottom
                .align(Alignment.BottomStart)
                .clip(WavyTopShape()) // Custom wavy shape for curved top edge
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars) // Handle 3-button navigation
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title - Bold, dark text
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937), // Dark gray
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description - Gray text
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF6B7280), // Medium gray
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Page indicators
                PageIndicators(
                    currentPage = currentPage,
                    totalPages = totalPages
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation buttons
                buttons()
            }
        }
    }
}

/**
 * Preview: First page with logo and Skip/Continue buttons
 */
@Preview(name = "Page 1 - With Logo", showSystemUi = true)
@Composable
private fun OnboardingLayoutPage1Preview() {
    MaterialTheme {
        OnboardingLayout(
            currentPage = 0,
            totalPages = 3,
            mockupContent = {
                ScreenshotPlaceholder(title = "Premium Features")
            },
            title = "Upgrade to Premium, Get More Possibilities",
            description = "Enjoy more storage, advanced styles, faster processing, and priority support to enhance your video creation experience.",
            buttons = {
                NavigationButtons(
                    onNext = {},
                    onSkip = {}
                )
            }
        )
    }
}
