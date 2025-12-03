package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App logo component for onboarding splash screen
 * Shows the app's ic_launcher icon in a circular white background
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Circular white background with app logo
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // App launcher icon
            // Replace R.mipmap.ic_launcher with your actual launcher icon resource
            Image(
                painter = painterResource(id = android.R.mipmap.sym_def_app_icon), // Placeholder - will be replaced
                contentDescription = "Gen AI Video Logo",
                modifier = Modifier
                    .size(50.dp)
                    .padding(4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // App name
        Text(
            text = "Gen AI Video",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 22.sp
        )
    }
}

/**
 * Preview: AppLogo on purple gradient background
 */
@Preview(name = "App Logo - On Gradient", showBackground = true)
@Composable
private fun AppLogoPreview() {
    MaterialTheme {
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
            AppLogo()
        }
    }
}

