package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import com.manjul.genai.videogenerator.ui.components.GlassmorphicNavigationBar
import com.manjul.genai.videogenerator.ui.components.NavigationItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.R
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.data.onboarding.OnboardingManager
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel
import kotlinx.coroutines.launch

sealed class AppDestination(
    val labelRes: Int,
    val icon: ImageVector,
) {
    data object Models : AppDestination(R.string.destination_models, Icons.Outlined.ViewInAr)
    data object Generate : AppDestination(R.string.destination_generate, Icons.Outlined.AutoAwesome)
    data object History : AppDestination(R.string.destination_history, Icons.Outlined.History)
    data object Profile : AppDestination(R.string.destination_profile, Icons.Outlined.Face)
    
    companion object {
        val Saver: Saver<AppDestination, String> = Saver(
            save = { destination ->
                when (destination) {
                    is AppDestination.Models -> "Models"
                    is AppDestination.Generate -> "Generate"
                    is AppDestination.History -> "History"
                    is AppDestination.Profile -> "Profile"
                }
            },
            restore = { value ->
                when (value) {
                    "Models" -> AppDestination.Models
                    "Generate" -> AppDestination.Generate
                    "History" -> AppDestination.History
                    "Profile" -> AppDestination.Profile
                    else -> AppDestination.Models // Default fallback
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GenAiRoot() {
    val context = LocalContext.current
    val activity = (context as? androidx.activity.ComponentActivity)
    
    // App flow states: Splash -> Onboarding (if first time) -> Main App
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showOnboarding by rememberSaveable { 
        mutableStateOf(false) // Will be set after splash based on OnboardingManager
    }
    
    when {
        showSplash -> {
            SplashScreen(
                onTimeout = {
                    showSplash = false
                    // Check if user has completed onboarding
                    showOnboarding = !OnboardingManager.hasCompletedOnboarding()
                }
            )
        }
        showOnboarding -> {
            OnboardingScreen(
                onComplete = {
                    OnboardingManager.setOnboardingCompleted()
                    showOnboarding = false
                }
            )
        }
        else -> {
            MainAppContent()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MainAppContent() {
    val context = LocalContext.current
    val activity = (context as? androidx.activity.ComponentActivity)
    
    // Check if we should navigate to History from notification
    var currentRoute by rememberSaveable(
        stateSaver = AppDestination.Saver
    ) { 
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        mutableStateOf<AppDestination>(
            if (navigateTo == "history") AppDestination.History else AppDestination.Generate
        )
    }
    
    // Handle navigation from notification intent
    LaunchedEffect(Unit) {
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        if (navigateTo == "history" && currentRoute != AppDestination.History) {
            currentRoute = AppDestination.History
            // Clear the intent extra after handling it
            activity?.intent?.removeExtra("navigate_to")
            Log.d("GenAiRoot", "Navigated to History from notification")
        }
    }
    var selectedModelId by rememberSaveable { mutableStateOf<String?>(null) }
    var highlightModelId by rememberSaveable { mutableStateOf<String?>(null) }
    var showGeneratingScreen by rememberSaveable { mutableStateOf(false) }
    // Store job ID instead of full VideoJob object for orientation change handling
    var resultJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBuyCreditsScreen by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionManagementScreen by rememberSaveable { mutableStateOf(false) }
    var showInsufficientCreditsDialog by rememberSaveable { mutableStateOf(false) }
    var requiredCredits by rememberSaveable { mutableStateOf(0) }
    val destinations = remember { listOf(AppDestination.Generate, AppDestination.Models, AppDestination.History, AppDestination.Profile) }
    
    // Watch for job completion when generating
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
    val jobs by historyViewModel.jobs.collectAsState()
    
    // Get the actual job from the ViewModel using the saved ID
    val resultJob = resultJobId?.let { jobId ->
        jobs.firstOrNull { it.id == jobId }
    }
    
    // Get GenerateViewModel to access state for GeneratingScreen
    val generateViewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory)
    val generateState by generateViewModel.state.collectAsState()
    
    // Show generating screen only when generation actually starts successfully
    // (isGenerating = true AND uploadMessage exists, meaning upload/request started)
    LaunchedEffect(generateState.isGenerating, generateState.uploadMessage, generateState.errorMessage) {
        Log.d("GenAiRoot", "=== GeneratingScreen LaunchedEffect Triggered ===")
        Log.d("GenAiRoot", "isGenerating: ${generateState.isGenerating}")
        Log.d("GenAiRoot", "uploadMessage: ${generateState.uploadMessage}")
        Log.d("GenAiRoot", "errorMessage: ${generateState.errorMessage}")
        Log.d("GenAiRoot", "showGeneratingScreen (current): $showGeneratingScreen")
        
        if (generateState.isGenerating && generateState.uploadMessage != null && generateState.errorMessage == null) {
            // Generation actually started (uploads began or request submitted)
            // Only show if not already showing
            if (!showGeneratingScreen) {
                Log.d("GenAiRoot", "Conditions met! Setting showGeneratingScreen = true")
                showGeneratingScreen = true
                Log.d("GenAiRoot", "showGeneratingScreen set to: $showGeneratingScreen")
            } else {
                Log.d("GenAiRoot", "GeneratingScreen already showing, skipping")
            }
        } else if (generateState.errorMessage != null && !generateState.isGenerating) {
            // Error occurred and generation stopped - hide generating screen
            Log.d("GenAiRoot", "Error occurred, hiding generating screen")
            showGeneratingScreen = false
        } else {
            Log.d("GenAiRoot", "Conditions not met for showing generating screen")
        }
        Log.d("GenAiRoot", "=== GeneratingScreen LaunchedEffect Completed ===")
    }
    
    // Auto-navigate to ResultsScreen when job completes
    LaunchedEffect(showGeneratingScreen, jobs) {
        if (showGeneratingScreen) {
            // If we don't have a pending job ID yet, get the latest job
            if (pendingJobId == null) {
                // Wait a bit for the job to appear in Firestore
                kotlinx.coroutines.delay(1500)
                pendingJobId = jobs.firstOrNull()?.id
            }
            
            // Check if the pending job (or latest job) has completed
            val jobToCheck = if (pendingJobId != null) {
                jobs.firstOrNull { it.id == pendingJobId }
            } else {
                jobs.firstOrNull()
            }
            
            if (jobToCheck != null && jobToCheck.status == VideoJobStatus.COMPLETE) {
                showGeneratingScreen = false
                // Launch ResultsActivity instead of showing dialog
                val intent = android.content.Intent(context, ResultsActivity::class.java).apply {
                    putExtra(ResultsActivity.EXTRA_JOB_ID, jobToCheck.id)
                }
                context.startActivity(intent)
                pendingJobId = null
                // Reset generation state when job completes
                generateViewModel.resetGenerationState()
            }
            
            // Also check for failed jobs
            if (jobToCheck != null && jobToCheck.status == VideoJobStatus.FAILED) {
                // Keep GeneratingScreen visible but show error
                // Error will be shown in GeneratingScreen via generateState.errorMessage
                // The job's error_message will be in Firestore, but we can also check generateState
            }
        }
    }

    // Get string resources outside of remember
    val modelsLabel = stringResource(R.string.destination_models)
    val generateLabel = stringResource(R.string.destination_generate)
    val historyLabel = stringResource(R.string.destination_history)
    val profileLabel = stringResource(R.string.destination_profile)
    
    // Get unread notification count for History badge - make it reactive
    var unreadCount by remember {
        androidx.compose.runtime.mutableStateOf(
            com.manjul.genai.videogenerator.data.notification.NotificationManager.getUnreadNotificationCount(context)
        )
    }
    
    // Periodically check for unread count changes (updates when FCM notification received)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500) // Check every 500ms for responsive updates
            val currentCount = com.manjul.genai.videogenerator.data.notification.NotificationManager.getUnreadNotificationCount(context)
            if (currentCount != unreadCount) {
                Log.d("GenAiRoot", "Unread count changed: $unreadCount -> $currentCount")
                unreadCount = currentCount
            }
        }
    }
    
    // Clear badge only when user actually views History screen
    LaunchedEffect(currentRoute) {
        if (currentRoute == AppDestination.History) {
            Log.d("GenAiRoot", "User viewed History screen - clearing badge")
            com.manjul.genai.videogenerator.data.notification.NotificationManager.clearUnreadCount(context)
            unreadCount = 0
        }
    }
    
    val navigationItems = remember(generateLabel, modelsLabel, historyLabel, profileLabel, unreadCount) {
        listOf(
            NavigationItem(icon = Icons.Outlined.AutoAwesome, label = generateLabel),
            NavigationItem(icon = Icons.Outlined.ViewInAr, label = modelsLabel),
            NavigationItem(icon = Icons.Outlined.History, label = historyLabel, badgeCount = unreadCount),
            NavigationItem(icon = Icons.Outlined.Face, label = profileLabel)
        )
    }
    
    val selectedNavigationItem = remember(currentRoute, navigationItems) {
        navigationItems[destinations.indexOf(currentRoute)]
    }
    
    Scaffold(
        bottomBar = {
            // Hide bottom navigation bar when BuyCreditsScreen or SubscriptionManagementScreen is shown (full screen)
            if (!showBuyCreditsScreen && !showSubscriptionManagementScreen) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlassmorphicNavigationBar(
                        items = navigationItems,
                        selectedItem = selectedNavigationItem,
                        onItemSelected = { item ->
                            val destination = destinations[navigationItems.indexOf(item)]
                            // When navigating back to Models from Generate, preserve the selected model for highlighting
                            if (destination == AppDestination.Models && currentRoute == AppDestination.Generate && selectedModelId != null) {
                                highlightModelId = selectedModelId
                            } else if (destination != AppDestination.Generate) {
                                selectedModelId = null
                                highlightModelId = null
                            }
                            // Hide generating screen when navigating (user can check History for status)
                            if (showGeneratingScreen && destination != AppDestination.Generate) {
                                showGeneratingScreen = false
                                pendingJobId = null
                            }
                            // Close results screen when navigating (user can reopen from History)
                            if (resultJobId != null) {
                                resultJobId = null
                            }
                            currentRoute = destination
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main navigation content
            when (currentRoute) {
                AppDestination.Models -> ModelsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onModelClick = { modelId ->
                        selectedModelId = modelId
                        highlightModelId = null
                        currentRoute = AppDestination.Generate
                    },
                    highlightModelId = highlightModelId,
                    onHighlightCleared = { highlightModelId = null }
                )
                AppDestination.Generate -> {
                    // Only show GenerateScreen if ResultsScreen is not showing
                    if (resultJob == null) {
                        GenerateScreen(
                            modifier = Modifier.padding(innerPadding),
                            preselectedModelId = selectedModelId,
                            onModelSelected = { selectedModelId = null },
                            onBackToModels = { 
                                highlightModelId = selectedModelId
                                currentRoute = AppDestination.Models
                            },
                            onGenerateStarted = { 
                                // This is now handled by observing generateState in LaunchedEffect above
                                // Only show generating screen when generation actually starts successfully
                            },
                            onSettingsClick = {
                                // Navigate to Profile screen
                                currentRoute = AppDestination.Profile
                                showBuyCreditsScreen = false
                            },
                            onCreditsClick = {
                                // Show BuyCreditsScreen overlay (no dialog, just show plans)
                                showInsufficientCreditsDialog = false
                                requiredCredits = 0
                                showBuyCreditsScreen = true
                            },
                            onBuyCreditsClick = { credits ->
                                // Show BuyCreditsScreen overlay with insufficient credits dialog
                                showInsufficientCreditsDialog = true
                                requiredCredits = credits
                                showBuyCreditsScreen = true
                            }
                        )
                    }
                }
                AppDestination.History -> HistoryScreen(
                    modifier = Modifier.padding(innerPadding),
                    onVideoClick = { job ->
                        if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE) {
                            // Launch ResultsActivity instead of showing dialog
                            val intent = android.content.Intent(context, ResultsActivity::class.java).apply {
                                putExtra(ResultsActivity.EXTRA_JOB_ID, job.id)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                AppDestination.Profile -> {
                    ProfileScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBuyCreditsClick = { 
                            showInsufficientCreditsDialog = false
                            requiredCredits = 0
                            showBuyCreditsScreen = true 
                        },
                        onVideosClick = {
                            currentRoute = AppDestination.History
                        },
                        onSubscriptionManagementClick = {
                            showSubscriptionManagementScreen = true
                        }
                    )
                }
            }
            
            // BuyCreditsScreen as overlay - shown on top of any screen
            if (showBuyCreditsScreen) {
                BuyCreditsScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { 
                        showBuyCreditsScreen = false
                        showInsufficientCreditsDialog = false
                        requiredCredits = 0
                    },
                    onPurchaseSuccess = {
                        // Close BuyCreditsScreen and navigate to GenerateScreen
                        showBuyCreditsScreen = false
                        showInsufficientCreditsDialog = false
                        requiredCredits = 0
                        currentRoute = AppDestination.Generate
                    },
                    showInsufficientCreditsDialog = showInsufficientCreditsDialog,
                    requiredCredits = requiredCredits
                )
            }
            
            // SubscriptionManagementScreen as overlay
            if (showSubscriptionManagementScreen) {
                SubscriptionManagementScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = {
                        showSubscriptionManagementScreen = false
                    },
                    onBuyCreditsClick = {
                        // Close subscription management and open buy credits
                        showSubscriptionManagementScreen = false
                        showBuyCreditsScreen = true
                    }
                )
            }
        }
        
        // Launch GeneratingActivity when showGeneratingScreen is true
        LaunchedEffect(showGeneratingScreen) {
            if (showGeneratingScreen) {
                val intent = android.content.Intent(context, GeneratingActivity::class.java)
                context.startActivity(intent)
                showGeneratingScreen = false // Reset flag
                generateViewModel.resetGenerationState()
            }
        }
    }
}
