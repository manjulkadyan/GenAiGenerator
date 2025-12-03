package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.R
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit = {}
) {
    // Animated scale for logo
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    // Pulsing glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Animate logo entrance
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
        )

        // Wait for minimum splash duration (2 seconds total)
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1a1f3a),
                        Color(0xFF0A0E27)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated logo with glow effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glow background
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale.value * 1.2f)
                        .alpha(glowAlpha * alpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF5B4FFF).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo/App icon - Using app name with gradient instead of icon
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale.value)
                        .alpha(alpha.value),
                    shape = CircleShape,
                    color = Color(0xFF5B4FFF)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "Gen AI Video",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Create stunning AI videos",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .alpha(alpha.value),
                color = Color(0xFF5B4FFF),
                strokeWidth = 3.dp
            )
        }
    }
}

@Preview(
    name = "Splash Screen",
    showBackground = true,
    backgroundColor = 0xFF0A0E27,
    showSystemUi = true
)
@Composable
private fun SplashScreenPreview() {
    GenAiVideoTheme {
        SplashScreen()
    }
}

