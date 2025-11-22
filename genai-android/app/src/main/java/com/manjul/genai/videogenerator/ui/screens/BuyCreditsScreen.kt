package com.manjul.genai.videogenerator.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.ui.components.BackgroundVideoPlayer
import com.manjul.genai.videogenerator.ui.components.DraggableBottomSheet
import com.manjul.genai.videogenerator.ui.components.LandingPageFeatureItem
import com.manjul.genai.videogenerator.ui.components.SubscriptionPlanCard
import com.manjul.genai.videogenerator.ui.components.getFeatureIcon
import com.manjul.genai.videogenerator.ui.viewmodel.LandingPageViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BuyCreditsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: LandingPageViewModel = viewModel(
        factory = LandingPageViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val videoHeightDp = (screenHeightDp * 0.33f).toInt() // 33% of screen for video
    
    // Handle system back button - navigate back instead of closing app
    BackHandler(enabled = true) {
        onBackClick()
    }
    
    // Auto-select popular plan when config loads
    LaunchedEffect(uiState.config.subscriptionPlans) {
        if (uiState.selectedPlan == null && uiState.config.subscriptionPlans.isNotEmpty()) {
            val popularPlan = uiState.config.subscriptionPlans.firstOrNull { it.isPopular }
            if (popularPlan != null) {
                viewModel.selectPlan(popularPlan)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top section: Video (30-35% of screen, fixed, not scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(videoHeightDp.dp)
        ) {
            // Background video layer
            if (uiState.config.backgroundVideoUrl.isNotEmpty()) {
                BackgroundVideoPlayer(
                    videoUrl = uiState.config.backgroundVideoUrl,
                    modifier = Modifier.fillMaxSize(),
                    overlayAlpha = 0.7f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            
            // Close button (top-left, over video)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBackClick),
                    tint = Color.White
                )
            }
        }
        
        // Bottom section: Draggable bottom sheet for features + Sticky pricing/button
        // Sheet can expand to full screen (covering video area)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Persistent draggable bottom sheet for features - always visible, never dismissible
            // Can be dragged up to full screen (covering video) but always remains visible
            DraggableBottomSheet(
                initialHeightPercent = 0.6f // Start at 60% of screen to show 3-4 features initially
            ) {
                // Features section - NOT scrollable, sheet itself is draggable
                if (uiState.config.features.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.config.features.forEach { feature ->
                            LandingPageFeatureItem(
                                title = feature.title,
                                description = feature.description,
                                icon = getFeatureIcon(feature.icon),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            // Sticky bottom section: Pricing cards + Continue button + Footer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subscription plans section - sticky at bottom
                if (uiState.config.subscriptionPlans.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.config.subscriptionPlans.forEach { plan ->
                            SubscriptionPlanCard(
                                plan = plan,
                                isSelected = uiState.selectedPlan?.productId == plan.productId,
                                onClick = {
                                    viewModel.selectPlan(plan)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Continue button - sticky at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            uiState.selectedPlan?.let { plan ->
                                if (context is Activity && uiState.billingInitialized) {
                                    viewModel.purchasePlan(context, plan)
                                }
                            }
                        }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Continue",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Footer links - sticky at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { /* Terms */ }) {
                        Text(
                            text = "Terms",
                            color = Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "|",
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TextButton(onClick = { /* Privacy */ }) {
                        Text(
                            text = "Privacy",
                            color = Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "|",
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TextButton(onClick = { /* Restore */ }) {
                        Text(
                            text = "Restore",
                            color = Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
