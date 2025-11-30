package com.manjul.genai.videogenerator.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialog
import com.manjul.genai.videogenerator.ui.viewmodel.LandingPageViewModel
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import com.manjul.genai.videogenerator.data.model.PurchaseType
import com.manjul.genai.videogenerator.data.model.OneTimeProduct
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BuyCreditsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPurchaseSuccess: () -> Unit = {}, // Callback when purchase is successful
    showInsufficientCreditsDialog: Boolean = false, // Show dialog when coming from insufficient credits
    requiredCredits: Int = 0, // Credits needed for the generation
    viewModel: LandingPageViewModel = viewModel(
        factory = LandingPageViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    // Video takes 50% of screen, leaving room for bottom sheet
    val videoHeightDp = (screenHeightDp * 0.45f).toInt() // 50% of screen for video
    
    // Snackbar for showing purchase messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("BuyCredits")
        AnalyticsManager.trackCreditsViewed()
        android.util.Log.d("BuyCreditsScreen", "=== Screen Initialized ===")
        android.util.Log.d("BuyCreditsScreen", "Screen height: ${screenHeightDp}dp, Video height: ${videoHeightDp}dp")
    }
    
    // Log complete state summary when key values change
    LaunchedEffect(
        uiState.isLoading,
        uiState.billingInitialized,
        uiState.selectedPlan?.productId,
        uiState.config.subscriptionPlans.size,
        uiState.productDetails.keys.size
    ) {
        android.util.Log.d("BuyCreditsScreen", "=== State Summary ===")
        android.util.Log.d("BuyCreditsScreen", "Loading: ${uiState.isLoading}")
        android.util.Log.d("BuyCreditsScreen", "Billing initialized: ${uiState.billingInitialized}")
        android.util.Log.d("BuyCreditsScreen", "Purchase in progress: ${uiState.isPurchaseInProgress}")
        android.util.Log.d("BuyCreditsScreen", "Selected plan: ${uiState.selectedPlan?.productId ?: "none"}")
        android.util.Log.d("BuyCreditsScreen", "Plans available: ${uiState.config.subscriptionPlans.size}")
        android.util.Log.d("BuyCreditsScreen", "Product details loaded: ${uiState.productDetails.keys.size}")
        android.util.Log.d("BuyCreditsScreen", "Features count: ${uiState.config.features.size}")
        android.util.Log.d("BuyCreditsScreen", "Error: ${uiState.error ?: "none"}")
        android.util.Log.d("BuyCreditsScreen", "Purchase message: ${uiState.purchaseMessage ?: "none"}")
        android.util.Log.d("BuyCreditsScreen", "Continue button enabled: ${!uiState.isPurchaseInProgress && uiState.selectedPlan != null && uiState.billingInitialized}")
    }
    
    // Log UI state changes
    LaunchedEffect(uiState.billingInitialized) {
        android.util.Log.d("BuyCreditsScreen", "Billing initialized: ${uiState.billingInitialized}")
    }
    
    LaunchedEffect(uiState.selectedPlan) {
        android.util.Log.d("BuyCreditsScreen", "Selected plan changed: ${uiState.selectedPlan?.productId ?: "null"}")
        if (uiState.selectedPlan != null) {
            android.util.Log.d("BuyCreditsScreen", "Plan details - ProductId: ${uiState.selectedPlan?.productId}, Credits: ${uiState.selectedPlan?.credits}, Price: ${uiState.selectedPlan?.price}")
        }
    }
    
    LaunchedEffect(uiState.isPurchaseInProgress) {
        android.util.Log.d("BuyCreditsScreen", "Purchase in progress: ${uiState.isPurchaseInProgress}")
    }
    
    LaunchedEffect(uiState.isLoading) {
        android.util.Log.d("BuyCreditsScreen", "Loading state: ${uiState.isLoading}")
    }
    
    LaunchedEffect(uiState.config.subscriptionPlans.size) {
        android.util.Log.d("BuyCreditsScreen", "Subscription plans count: ${uiState.config.subscriptionPlans.size}")
        uiState.config.subscriptionPlans.forEachIndexed { index, plan ->
            android.util.Log.d("BuyCreditsScreen", "Plan $index: productId=${plan.productId}, price=${plan.price}, credits=${plan.credits}, isPopular=${plan.isPopular}")
        }
    }
    
    LaunchedEffect(uiState.productDetails.keys.size) {
        android.util.Log.d("BuyCreditsScreen", "Product details loaded: ${uiState.productDetails.keys.size} products")
        uiState.productDetails.forEach { (productId, _) ->
            android.util.Log.d("BuyCreditsScreen", "Product detail available: $productId")
        }
    }
    
    // Handle system back button - navigate back instead of closing app
    BackHandler(enabled = true) {
        android.util.Log.d("BuyCreditsScreen", "Back button pressed - navigating back")
        onBackClick()
    }
    
    // Auto-select popular plan when config loads
    LaunchedEffect(uiState.config.subscriptionPlans) {
        if (uiState.selectedPlan == null && uiState.config.subscriptionPlans.isNotEmpty()) {
            val popularPlan = uiState.config.subscriptionPlans.firstOrNull { it.isPopular }
            if (popularPlan != null) {
                android.util.Log.d("BuyCreditsScreen", "Auto-selecting popular plan: ${popularPlan.productId}")
                viewModel.selectPlan(popularPlan)
            } else {
                android.util.Log.w("BuyCreditsScreen", "No popular plan found in ${uiState.config.subscriptionPlans.size} plans")
            }
        }
    }
    
    // Track if we should show success dialog
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Track insufficient credits dialog
    var showInsufficientDialog by remember { mutableStateOf(showInsufficientCreditsDialog) }
    
    // Show purchase success/error messages
    LaunchedEffect(uiState.purchaseMessage) {
        uiState.purchaseMessage?.let { message ->
            android.util.Log.d("BuyCreditsScreen", "Purchase message received: $message")
            // Check if it's a success message
            if (message.contains("successfully", ignoreCase = true)) {
                // Show success dialog instead of snackbar
                showSuccessDialog = true
            } else {
                // Show snackbar for other messages
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearPurchaseMessage()
                }
            }
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.util.Log.e("BuyCreditsScreen", "ERROR: $error")
            android.util.Log.e("BuyCreditsScreen", "Error stack trace:", Exception("Error context"))
            scope.launch {
                //snackbarHostState.showSnackbar(error)
                viewModel.clearPurchaseMessage()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top section: Video (50% of screen, fixed, not scrollable)
        // Positioned at top with status bar padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(videoHeightDp.dp)
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Background video layer
            if (uiState.config.backgroundVideoUrl.isNotEmpty()) {
                // Debug: Log the video URL we're receiving
                LaunchedEffect(uiState.config.backgroundVideoUrl) {
                    android.util.Log.d("BuyCreditsScreen", "=== Video Configuration ===")
                    android.util.Log.d("BuyCreditsScreen", "Video URL from config: ${uiState.config.backgroundVideoUrl}")
                    android.util.Log.d("BuyCreditsScreen", "Video URL length: ${uiState.config.backgroundVideoUrl.length}")
                    android.util.Log.d("BuyCreditsScreen", "Is m3u8: ${uiState.config.backgroundVideoUrl.contains("m3u8", ignoreCase = true)}")
                }
                
                // HLS-only video player for adaptive streaming
                BackgroundVideoPlayer(
                    videoUrl = uiState.config.backgroundVideoUrl,
                    modifier = Modifier.fillMaxSize(),
                    overlayAlpha = 0f // Remove overlay - make video fully clear
                )
            } else {
                // Fallback: black background if no video URL
                LaunchedEffect(Unit) {
                    android.util.Log.w("BuyCreditsScreen", "=== Video Configuration Missing ===")
                    android.util.Log.w("BuyCreditsScreen", "No video URL in config - showing black background")
                    android.util.Log.w("BuyCreditsScreen", "Config loaded: ${!uiState.isLoading}, Features count: ${uiState.config.features.size}, Plans count: ${uiState.config.subscriptionPlans.size}")
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            
            // Close button (top-right, over video) - appears after 3-5 seconds
            var showCloseButton by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                // Show close button after 3-5 seconds (randomized between 3-5)
                kotlinx.coroutines.delay((3000..5000).random().toLong())
                showCloseButton = true
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                AnimatedVisibility(
                    visible = showCloseButton,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    // Close button with subtle background (not too prominent)
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.3f), // Subtle dark background with alpha
                                shape = CircleShape
                            )
                            .size(32.dp)
                            .clickable(onClick = {
                                android.util.Log.d("BuyCreditsScreen", "Close button clicked")
                                onBackClick()
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White.copy(alpha = 0.9f) // Slightly transparent white
                        )
                    }
                }
            }
        }
        
        // Bottom section: Draggable bottom sheet for features + Sticky pricing/button
        // Sheet positioned from bottom, can expand to cover video
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Persistent draggable bottom sheet for features - always visible, never dismissible
            // Can be dragged up to full screen (covering video) but always remains visible
            // Starts at 50% of screen, positioned from bottom
            DraggableBottomSheet(
                initialHeightPercent = 0.6f // Start at 60% of screen (will overlap video slightly)
            ) {
                // Features section - NOT scrollable, sheet itself is draggable
                LaunchedEffect(uiState.config.features.size) {
                    if (uiState.config.features.isNotEmpty()) {
                        android.util.Log.d("BuyCreditsScreen", "Displaying ${uiState.config.features.size} features")
                    } else {
                        android.util.Log.w("BuyCreditsScreen", "No features in config to display")
                    }
                }
                
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
            
            // Sticky bottom section: Tab selector + Pricing cards + Continue button + Footer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab selector for subscription vs one-time purchase
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Subscription tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION)
                                    Color.White
                                else
                                    Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.selectPurchaseType(PurchaseType.SUBSCRIPTION)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Weekly Plans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION)
                                Color.Black
                            else
                                Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // One-time purchase tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (uiState.selectedPurchaseType == PurchaseType.ONE_TIME)
                                    Color.White
                                else
                                    Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.selectPurchaseType(PurchaseType.ONE_TIME)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Top-Up Credits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedPurchaseType == PurchaseType.ONE_TIME)
                                Color.Black
                            else
                                Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Explanatory text
                Text(
                    text = if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION) {
                        "Get credits every week with auto-renewing subscriptions"
                    } else {
                        "Buy credits once - no recurring charges"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // Pricing cards - different based on selected tab
                if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION) {
                    // Subscription plans section - 3 cards in a row
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
                                        android.util.Log.d("BuyCreditsScreen", "Plan clicked: ${plan.productId}")
                                        viewModel.selectPlan(plan)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // One-time products section - 5 cards in a horizontal scrollable LazyRow
                    // Using same SubscriptionPlanCard component for consistency
                    if (uiState.oneTimeProducts.isNotEmpty()) {
                        val listState = rememberLazyListState()
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        
                        // Auto-scroll to center the selected product when it changes
                        LaunchedEffect(uiState.selectedOneTimeProduct?.productId) {
                            uiState.selectedOneTimeProduct?.let { selectedProduct ->
                                val selectedIndex = uiState.oneTimeProducts.indexOfFirst { 
                                    it.productId == selectedProduct.productId 
                                }
                                if (selectedIndex >= 0) {
                                    android.util.Log.d("BuyCreditsScreen", "Auto-scrolling to center product at index $selectedIndex")
                                    
                                    // Calculate offset to center the item
                                    // Card width is 120.dp + 8.dp spacing, screen width / 2 to center
                                    val cardWidth = 128 // 120dp card + 8dp spacing
                                    val offset = (screenWidth.value  - cardWidth ).toInt().coerceAtLeast(0)
                                    
                                    listState.animateScrollToItem(
                                        index = selectedIndex,
                                        scrollOffset = -offset
                                    )
                                }
                            }
                        }
                        
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.oneTimeProducts) { product ->
                                // Convert OneTimeProduct to SubscriptionPlan for card display
                                val planForDisplay = SubscriptionPlan(
                                    productId = product.productId,
                                    credits = product.credits,
                                    price = product.price,
                                    period = "One Time", // Show "One Time" instead of "Weekly"
                                    isPopular = product.isPopular,
                                    isBestValue = product.isBestValue
                                )
                                
                                SubscriptionPlanCard(
                                    plan = planForDisplay,
                                    isSelected = uiState.selectedOneTimeProduct?.productId == product.productId,
                                    onClick = {
                                        android.util.Log.d("BuyCreditsScreen", "One-time product clicked: ${product.productId}")
                                        viewModel.selectOneTimeProduct(product)
                                    },
                                    modifier = Modifier.width(120.dp),
                                    periodText = "One Time"
                                    // showPerCreditCost defaults to true
                                )
                            }
                        }
                    }
                }
                
                // Continue button - sticky at bottom
                val canPurchase = if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION) {
                    !uiState.isPurchaseInProgress && uiState.selectedPlan != null && uiState.billingInitialized
                } else {
                    !uiState.isPurchaseInProgress && uiState.selectedOneTimeProduct != null && uiState.billingInitialized
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (canPurchase) {
                                Color.White
                            } else {
                                Color.Gray.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canPurchase) {
                            android.util.Log.d("BuyCreditsScreen", "=== Continue Button Clicked ===")
                            android.util.Log.d("BuyCreditsScreen", "Purchase type: ${uiState.selectedPurchaseType}")
                            
                            if (uiState.selectedPurchaseType == PurchaseType.SUBSCRIPTION) {
                                uiState.selectedPlan?.let { plan ->
                                    android.util.Log.d("BuyCreditsScreen", "Attempting to purchase plan: ${plan.productId}")
                                    if (context is Activity && uiState.billingInitialized) {
                                        val billingResult = viewModel.purchasePlan(context, plan)
                                        android.util.Log.d("BuyCreditsScreen", "Purchase initiated - Response code: ${billingResult.responseCode}")
                                    }
                                }
                            } else {
                                uiState.selectedOneTimeProduct?.let { product ->
                                    android.util.Log.d("BuyCreditsScreen", "Attempting to purchase one-time product: ${product.productId}")
                                    if (context is Activity && uiState.billingInitialized) {
                                        val billingResult = viewModel.purchaseOneTimeProduct(context, product)
                                        android.util.Log.d("BuyCreditsScreen", "Purchase initiated - Response code: ${billingResult.responseCode}")
                                    }
                                }
                            }
                        }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isPurchaseInProgress) {
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (canPurchase) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Continue",
                                tint = if (canPurchase) Color.Black else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Footer links - sticky at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/genai-videogenerator-terms-of-use/5812e7a9-a6e1-4e96-9255-e5c39fba5b5e/terms"))
                        context.startActivity(intent)
                    }) {
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
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/genai-videogenerator-privacy-policy/cd6d3993-1e48-4a61-831a-c9154da6d101/privacy"))
                        context.startActivity(intent)
                    }) {
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
                    TextButton(onClick = { 
                        android.util.Log.d("BuyCreditsScreen", "Restore button clicked")
                        // TODO: Implement restore purchases
                    }) {
                        Text(
                            text = "Restore",
                            color = Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Snackbar host for showing purchase messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Position above the sticky bottom section
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = Color(0xFF1F1F1F),
                contentColor = Color.White
            )
        }
        
        // Insufficient credits dialog - shown when user tries to generate without enough credits
        if (showInsufficientDialog) {
            AppDialog(
                onDismissRequest = { showInsufficientDialog = false },
                title = "Insufficient Credits"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Credits",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Text(
                        text = if (requiredCredits > 0) {
                            "You need $requiredCredits credits to generate this video. Please purchase a plan or top-up your credit below to continue."
                        } else {
                            "You don't have enough credits to generate this video. Please purchase a plan or top-up your credit below to continue."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    AppPrimaryButton(
                        text = "View Plans",
                        onClick = { showInsufficientDialog = false },
                        fullWidth = true
                    )
                }
            }
        }
        
        // Success dialog for successful purchases
        if (showSuccessDialog) {
            AppDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    viewModel.clearPurchaseMessage()
                    // Navigate to GenerateScreen and close BuyCreditsScreen
                    onPurchaseSuccess()
                },
                title = "Purchase Successful!"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Success icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Text(
                        text = "Your subscription has been activated successfully. You can now start generating amazing AI videos!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    AppPrimaryButton(
                        text = "Start Creating",
                        onClick = {
                            showSuccessDialog = false
                            viewModel.clearPurchaseMessage()
                            // Navigate to GenerateScreen and close BuyCreditsScreen
                            onPurchaseSuccess()
                        },
                        fullWidth = true
                    )
                }
            }
        }
    }
}
