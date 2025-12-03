package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated page indicators for onboarding flow
 */
@Composable
fun PageIndicators(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalPages) { index ->
            PageIndicator(
                isActive = index == currentPage,
                index = index,
                currentPage = currentPage
            )
        }
    }
}

@Composable
private fun PageIndicator(
    isActive: Boolean,
    index: Int,
    currentPage: Int
) {
    val width by animateDpAsState(
        targetValue = if (isActive) 32.dp else 8.dp,
        animationSpec = tween(300),
        label = "indicatorWidth"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            kotlin.math.abs(index - currentPage) == 1 -> 0.5f
            else -> 0.3f
        },
        animationSpec = tween(300),
        label = "indicatorAlpha"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(
                if (isActive) {
                    Color(0xFF7C3AED) // Purple for active (on white background)
                } else {
                    Color(0xFFD1D5DB) // Light gray for inactive
                }
            )
    )
}
