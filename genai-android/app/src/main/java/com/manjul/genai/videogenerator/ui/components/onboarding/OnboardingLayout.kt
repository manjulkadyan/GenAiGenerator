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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
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
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    
    // Calculate responsive sizes based on screen height
    // For smaller screens (< 700dp), adjust proportions to give more space to content
    val isVerySmallScreen = screenHeightDp < 650  // 640dp and below
    val isSmallScreen = screenHeightDp < 700
    val isExactly700 = screenHeightDp in 700..720  // Specifically handle 700dp
    val isMediumScreen = screenHeightDp in 720..800
    val isLargeScreen = screenHeightDp > 800
    
    // Dynamic top section height: smaller screens need less top, more bottom
    val topSectionHeight = when {
        isVerySmallScreen -> 0.5f  // 45% for very small screens (640dp) - give more to bottom
        isSmallScreen -> 0.50f      // 50% for small screens
        isExactly700 -> 0.55f       // 55% for 700dp screens
        isMediumScreen -> 0.62f     // 62% for medium screens
        else -> 0.7f                // 70% for large screens/tablets
    }
    
    // Dynamic bottom section height: smaller screens need more space for content and text
    val bottomSectionHeight = when {
        isVerySmallScreen -> 0.70f  // 60% for very small screens (640dp) - more space for text
        isSmallScreen -> 0.6f      // 58% for small screens - more space for text
        isExactly700 -> 0.60f       // 50% for 700dp screens - balanced
        isMediumScreen -> 0.45f     // 45% for medium screens
        else -> 0.45f               // 45% for large screens
    }
    
    // Responsive font sizes based on screen height
    val titleFontSize = when {
        isVerySmallScreen -> 20.sp
        isSmallScreen -> 22.sp
        isExactly700 -> 23.sp       // Slightly larger for 700dp
        isMediumScreen -> 24.sp
        else -> 28.sp
    }
    
    val titleLineHeight = when {
        isVerySmallScreen -> 26.sp
        isSmallScreen -> 28.sp
        isExactly700 -> 29.sp       // Slightly larger for 700dp
        isMediumScreen -> 30.sp
        else -> 34.sp
    }
    
    val descriptionFontSize = when {
        isVerySmallScreen -> 13.sp
        isSmallScreen -> 14.sp
        isExactly700 -> 14.sp       // Same as small for 700dp
        isMediumScreen -> 15.sp
        else -> 16.sp
    }
    
    // Responsive padding and spacing - much more compact for small screens
    val horizontalPadding = when {
        isVerySmallScreen -> 16.dp
        isSmallScreen -> 20.dp
        isMediumScreen -> 22.dp
        else -> 24.dp
    }
    
    val topPadding = when {
        isVerySmallScreen -> 26.dp  // Even less top padding for very small screens (640dp)
        isSmallScreen -> 32.dp      // Reduced for small screens
        isExactly700 -> 40.dp       // Moderate padding for 700dp
        isMediumScreen -> 55.dp
        else -> 80.dp
    }
    
    val bottomPadding = when {
        isVerySmallScreen -> 10.dp  // Less bottom padding to save space
        isSmallScreen -> 18.dp      // Slightly less for small screens
        isExactly700 -> 16.dp       // Moderate for 700dp
        isMediumScreen -> 24.dp
        else -> 32.dp
    }
    
    val titleSpacing = when {
        isVerySmallScreen -> 1.dp
        isSmallScreen -> 2.dp
        isExactly700 -> 2.dp        // Same as small for 700dp
        isMediumScreen -> 3.dp
        else -> 4.dp
    }
    
    val descriptionSpacing = when {
        isVerySmallScreen -> 1.dp
        isSmallScreen -> 2.dp
        isExactly700 -> 2.dp        // Same as small for 700dp
        isMediumScreen -> 3.dp
        else -> 4.dp
    }
    
    val indicatorsSpacing = when {
        isVerySmallScreen -> 4.dp
        isSmallScreen -> 6.dp
        isExactly700 -> 6.dp        // Same as small for 700dp
        isMediumScreen -> 7.dp
        else -> 8.dp
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP SECTION: Purple gradient with iPhone mockup (dynamic height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(topSectionHeight)
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
                    .padding(horizontal = horizontalPadding),
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

        // BOTTOM SECTION: White card (dynamic height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(bottomSectionHeight)
                .align(Alignment.BottomStart)
                .clip(WavyTopShape()) // Custom wavy shape for curved top edge
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars) // Handle 3-button navigation
                    .padding(horizontal = horizontalPadding)
                    .padding(top = topPadding, bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Content area with weight to push buttons down
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title - Bold, dark text (responsive size)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937), // Dark gray
                        textAlign = TextAlign.Center,
                        fontSize = titleFontSize,
                        lineHeight = titleLineHeight
                    )

                    Spacer(modifier = Modifier.height(titleSpacing))

                    // Description - Gray text (responsive size)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = TextUnit(1.1f, TextUnitType.Em)),
                        color = Color(0xFF6B7280), // Medium gray
                        textAlign = TextAlign.Center,
                        fontSize = descriptionFontSize,
                        modifier = Modifier.padding(horizontal = when {
                            isVerySmallScreen -> 2.dp
                            isSmallScreen -> 4.dp
                            isExactly700 -> 4.dp
                            isMediumScreen -> 6.dp
                            else -> 8.dp
                        })
                    )

                    Spacer(modifier = Modifier.height(descriptionSpacing))

                    // Page indicators
                    PageIndicators(
                        currentPage = currentPage,
                        totalPages = totalPages
                    )
                }

                Spacer(modifier = Modifier.height(indicatorsSpacing))

                // Navigation buttons - always at bottom
                buttons()
            }
        }
    }
}

/**
 * Preview: First page with logo and Skip/Continue buttons
 */
@Preview(name = "Height 700dp", heightDp = 699, showSystemUi = true)
@Composable
private fun OnboardingLayoutPage1Preview() {
    MaterialTheme {
        OnboardingLayout(
            currentPage = 0,
            totalPages = 3,
            mockupContent = {
                ScreenshotPlaceholder(title = "Premium Features")
            },
            title = "Imagine Anything. Create Everything!",
            description = "Welcome to Gen AI VIdeo, The app that turns your imagination into stunning videos. Simply enter your text and let our AI do the magic.",
            buttons = {
                NavigationButtons(
                    onNext = {},
                    onSkip = {}
                )
            }
        )
    }
}
