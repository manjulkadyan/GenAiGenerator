package com.manjul.genai.videogenerator.ui.screens

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

@Composable
fun GenAiRoot() {
    var currentRoute by remember { mutableStateOf<AppDestination>(AppDestination.Models) }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    val destinations = remember { listOf(AppDestination.Models, AppDestination.Generate, AppDestination.History, AppDestination.Profile) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentRoute,
                        onClick = { 
                            currentRoute = destination
                            // Clear selected model when navigating away from Generate
                            if (destination != AppDestination.Generate) {
                                selectedModelId = null
                            }
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
                    currentRoute = AppDestination.Generate
                }
            )
            AppDestination.Generate -> GenerateScreen(
                modifier = Modifier.padding(innerPadding),
                preselectedModelId = selectedModelId,
                onModelSelected = { selectedModelId = null }
            )
            AppDestination.History -> HistoryScreen(modifier = Modifier.padding(innerPadding))
            AppDestination.Profile -> ProfileScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
