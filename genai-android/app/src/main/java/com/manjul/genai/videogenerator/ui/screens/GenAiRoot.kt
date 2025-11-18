package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.R
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
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
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GenAiRoot() {
    var currentRoute by remember { mutableStateOf<AppDestination>(AppDestination.Models) }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    var highlightModelId by remember { mutableStateOf<String?>(null) }
    var showGeneratingScreen by remember { mutableStateOf(false) }
    var resultJob by remember { mutableStateOf<com.manjul.genai.videogenerator.data.model.VideoJob?>(null) }
    var pendingJobId by remember { mutableStateOf<String?>(null) }
    var showBuyCreditsScreen by remember { mutableStateOf(false) }
    val destinations = remember { listOf(AppDestination.Models, AppDestination.Generate, AppDestination.History, AppDestination.Profile) }
    
    // Watch for job completion when generating
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
    val jobs by historyViewModel.jobs.collectAsState()
    
    // Get GenerateViewModel to access state for GeneratingScreen
    val generateViewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory)
    val generateState by generateViewModel.state.collectAsState()
    
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
                resultJob = jobToCheck
                pendingJobId = null
                // Clear all messages from GenerateScreen
                generateViewModel.dismissMessage()
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
    
    val navigationItems = remember(modelsLabel, generateLabel, historyLabel, profileLabel) {
        listOf(
            NavigationItem(icon = Icons.Outlined.ViewInAr, label = modelsLabel),
            NavigationItem(icon = Icons.Outlined.AutoAwesome, label = generateLabel),
            NavigationItem(icon = Icons.Outlined.History, label = historyLabel),
            NavigationItem(icon = Icons.Outlined.Face, label = profileLabel)
        )
    }
    
    val selectedNavigationItem = remember(currentRoute, navigationItems) {
        navigationItems[destinations.indexOf(currentRoute)]
    }
    
    Scaffold(
        bottomBar = {
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
                        if (resultJob != null) {
                            resultJob = null
                        }
                        // Reset buy credits screen when navigating away from Profile
                        if (destination != AppDestination.Profile) {
                            showBuyCreditsScreen = false
                        }
                        currentRoute = destination
                    }
                )
            }
        }
    ) { innerPadding ->
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
                            showGeneratingScreen = true
                            // The latest job will appear in the jobs list shortly after generation starts
                            // We'll detect it in LaunchedEffect below
                        }
                    )
                }
            }
            AppDestination.History -> HistoryScreen(
                modifier = Modifier.padding(innerPadding),
                onVideoClick = { job ->
                    if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE) {
                        resultJob = job
                    }
                }
            )
            AppDestination.Profile -> {
                if (showBuyCreditsScreen) {
                    BuyCreditsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = { showBuyCreditsScreen = false },
                        onPackageSelected = { packageInfo ->
                            // TODO: Handle package selection (payment integration)
                        }
                    )
                } else {
                    ProfileScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBuyCreditsClick = { showBuyCreditsScreen = true }
                    )
                }
            }
        }
        
        // Generating Screen Overlay - Non-blocking, allows navigation
        if (showGeneratingScreen) {
            GeneratingScreen(
                modifier = Modifier.fillMaxSize(),
                statusMessage = generateState.uploadMessage,
                errorMessage = generateState.errorMessage,
                onCancel = { 
                    showGeneratingScreen = false
                    pendingJobId = null
                    generateViewModel.dismissMessage() // Clear any messages
                },
                onRetry = if (generateState.errorMessage != null) {
                    {
                        generateViewModel.dismissMessage()
                        generateViewModel.generate()
                    }
                } else null
            )
        }
        
        // Results Screen - Show as Dialog to allow navigation and proper padding
        resultJob?.let { job ->
            ResultsScreenDialog(
                job = job,
                onClose = { resultJob = null },
                onRegenerate = {
                    resultJob = null
                    currentRoute = AppDestination.Generate
                },
                onDelete = {
                    resultJob = null
                    // TODO: Implement delete functionality
                }
            )
        }
    }
}
