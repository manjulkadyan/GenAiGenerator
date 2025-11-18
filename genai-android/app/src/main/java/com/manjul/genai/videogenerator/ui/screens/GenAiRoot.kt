package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.manjul.genai.videogenerator.R

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
    val destinations = remember { listOf(AppDestination.Models, AppDestination.Generate, AppDestination.History, AppDestination.Profile) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentRoute,
                        onClick = { 
                            // When navigating back to Models from Generate, preserve the selected model for highlighting
                            if (destination == AppDestination.Models && currentRoute == AppDestination.Generate && selectedModelId != null) {
                                highlightModelId = selectedModelId
                            } else if (destination != AppDestination.Generate) {
                                selectedModelId = null
                                highlightModelId = null
                            }
                            currentRoute = destination
                        },
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                        label = { Text(text = stringResource(destination.labelRes)) }
                    )
                }
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
            AppDestination.Generate -> GenerateScreen(
                modifier = Modifier.padding(innerPadding),
                preselectedModelId = selectedModelId,
                onModelSelected = { selectedModelId = null },
                onBackToModels = { 
                    highlightModelId = selectedModelId
                    currentRoute = AppDestination.Models
                },
                onGenerateStarted = { showGeneratingScreen = true }
            )
            AppDestination.History -> HistoryScreen(
                modifier = Modifier.padding(innerPadding),
                onVideoClick = { job ->
                    if (job.status == com.manjul.genai.videogenerator.data.model.VideoJobStatus.COMPLETE) {
                        resultJob = job
                    }
                }
            )
            AppDestination.Profile -> ProfileScreen(modifier = Modifier.padding(innerPadding))
        }
        
        // Generating Screen Overlay
        if (showGeneratingScreen) {
            GeneratingScreen(
                modifier = Modifier.fillMaxSize(),
                progress = 0, // TODO: Get actual progress from ViewModel
                onCancel = { showGeneratingScreen = false }
            )
        }
        
        // Results Screen
        resultJob?.let { job ->
            ResultsScreen(
                modifier = Modifier.fillMaxSize(),
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
