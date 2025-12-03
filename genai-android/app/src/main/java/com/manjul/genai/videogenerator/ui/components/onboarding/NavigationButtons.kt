package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton

/**
 * Navigation buttons for onboarding screens
 * Layout matches the design: Skip (light purple) + Continue (full purple)
 */
@Composable
fun NavigationButtons(
    onNext: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onGetStarted: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onGetStarted != null) {
            // Last screen: full-width "Continue" button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF7C3AED))
                    .clickable(onClick = onGetStarted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 17.sp
                )
            }
        } else {
            // Other screens: Skip (light) + Continue (purple)
            if (onSkip != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFF3F4F6)) // Light gray
                        .clickable(onClick = onSkip),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280), // Medium gray
                        fontSize = 17.sp
                    )
                }
            }

            if (onNext != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF7C3AED)) // Purple
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

/**
 * Preview: Skip + Continue buttons (Screens 1 & 2)
 */
@Preview(name = "Navigation - Skip & Continue", showBackground = true)
@Composable
private fun NavigationButtonsSkipContinuePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationButtons(
            onNext = {},
            onSkip = {}
        )
    }
}

/**
 * Preview: Continue button only (Screen 3)
 */
@Preview(name = "Navigation - Continue Only", showBackground = true)
@Composable
private fun NavigationButtonsContinuePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationButtons(
            onGetStarted = {}
        )
    }
}

/**
 * Preview: All button states
 */
@Preview(name = "Navigation - All States", showBackground = true, heightDp = 400)
@Composable
private fun NavigationButtonsAllStatesPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // State 1: Skip + Continue
        NavigationButtons(
            onNext = {},
            onSkip = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // State 2: Continue only
        NavigationButtons(
            onGetStarted = {}
        )
    }
}

