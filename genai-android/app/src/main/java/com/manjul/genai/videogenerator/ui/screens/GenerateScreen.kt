package com.manjul.genai.videogenerator.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel

@Composable
fun GenerateScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory),
    preselectedModelId: String? = null,
    onModelSelected: () -> Unit = {},
    onBackToModels: () -> Unit = {},
    onGenerateStarted: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    
    // Handle back button - navigate to Models screen with highlighted model
    BackHandler(enabled = true) {
        if (state.selectedModel != null) {
            onBackToModels()
        }
    }
    
    // Select the preselected model when it becomes available
    LaunchedEffect(preselectedModelId, state.models) {
        if (preselectedModelId != null && state.models.isNotEmpty()) {
            val model = state.models.find { it.id == preselectedModelId }
            if (model != null && state.selectedModel?.id != preselectedModelId) {
                viewModel.selectModel(model)
                onModelSelected()
            }
        }
    }
    val pickFirstFrame = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setFirstFrameUri(uri)
    }
    val pickLastFrame = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setLastFrameUri(uri)
    }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                title = "Create Your Video",
                subtitle = "Choose a model and describe your vision",
                showBorder = false
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

            // Model Selection - Simplified
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
        ModelSelector(
            models = state.models,
            selected = state.selectedModel,
            onSelected = viewModel::selectModel
        )
            }

            // Prompt Section - Simplified
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Video Prompt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.prompt,
            onValueChange = viewModel::updatePrompt,
                    placeholder = { Text("Describe what you want to generate...") },
            singleLine = false,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Duration and Aspect Ratio - Simplified
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Duration
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
        DurationSelector(
            model = state.selectedModel,
            selected = state.selectedDuration,
            onSelected = viewModel::updateDuration
        )
                }

                // Aspect Ratio
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aspect Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
        AspectRatioSelector(
            options = state.selectedModel?.aspectRatios.orEmpty(),
            selected = state.selectedAspectRatio,
            onSelected = viewModel::updateAspectRatio
        )
                }
            }

        // Audio toggle - only show if model supports audio
        if (state.selectedModel?.supportsAudio == true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                        if (state.enableAudio) {
                    Text(
                                text = "Cost will be doubled",
                        style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                    )
                        }
                }
                Switch(
                    checked = state.enableAudio,
                    onCheckedChange = viewModel::toggleAudio
                )
            }
        }

            // Reference Frames Section - Simplified
            if (state.selectedModel?.supportsFirstFrame == true || state.selectedModel?.supportsLastFrame == true) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reference Frames",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

        // Only show first frame picker if model supports it
        if (state.selectedModel?.supportsFirstFrame == true) {
            ReferenceFramePicker(
                label = "First frame",
                            required = state.selectedModel?.requiresFirstFrame ?: false,
                uri = state.firstFrameUri,
                onPick = { pickFirstFrame.launch("image/*") },
                onClear = { viewModel.setFirstFrameUri(null) }
            )
        }

        // Only show last frame picker if model supports it
        if (state.selectedModel?.supportsLastFrame == true) {
            ReferenceFramePicker(
                label = "Last frame",
                            required = state.selectedModel?.requiresLastFrame ?: false,
                uri = state.lastFrameUri,
                onPick = { pickLastFrame.launch("image/*") },
                onClear = { viewModel.setLastFrameUri(null) }
            )
                    }
                }
            }

            // Negative prompt - moved to end, only show if model supports it
            if (state.selectedModel?.schemaMetadata?.categorized?.text?.any { it.name == "negative_prompt" } == true) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Negative Prompt (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.negativePrompt,
                        onValueChange = viewModel::updateNegativePrompt,
                        placeholder = { Text("What to avoid (e.g., blurry, low quality)") },
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
        }

        state.uploadMessage?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
            Text(
                text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Add spacer to ensure content doesn't get hidden behind sticky button
            Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Sticky bottom section with cost and generate button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cost - Simplified
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Cost",
                style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.estimatedCost} credits",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
                    if (state.enableAudio) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "2x Audio",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Generate Button - Simplified
                Button(
                    onClick = {
                        // Dismiss any existing messages before starting
                        viewModel.dismissMessage()
                        viewModel.generate()
                        if (state.canGenerate) {
                            onGenerateStarted()
                        }
                    },
                    enabled = state.canGenerate && !state.isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.canGenerate && !state.isGenerating) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (state.canGenerate && !state.isGenerating) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (state.canGenerate && !state.isGenerating) 4.dp else 0.dp
                    )
                ) {
                    if (state.isGenerating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Generating...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Generate Video",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        }

    // Don't show any alert dialogs - GeneratingScreen will handle all status/errors
    // Only show error dialog if generation failed and we're not generating
    // (This handles edge cases where error occurs before GeneratingScreen is shown)
    if (!state.isGenerating && state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text("OK")
                }
            },
            title = { Text("Error") },
            text = { Text(state.errorMessage ?: "Unknown error") }
        )
    }
}

@Composable
private fun ReferenceFramePicker(
    label: String,
    required: Boolean,
    uri: Uri?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (required) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
        Text(
                        text = "Required",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
            }
            }
        }

            if (uri != null) {
            // Image Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box {
                    // Image preview would go here - for now showing placeholder
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Image Selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Remove button overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clickable(onClick = onClear),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        } else {
            // Select Image Button
            OutlinedButton(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Image")
                }
            }

        if (required && uri == null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
            Text(
                    text = "⚠ This image is required for this model",
                    modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            }
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<AIModel>,
    selected: AIModel?,
    onSelected: (AIModel) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Scroll to selected model when it changes
    LaunchedEffect(selected?.id) {
        if (selected != null) {
            val index = models.indexOfFirst { it.id == selected.id }
            if (index >= 0) {
                kotlinx.coroutines.delay(50)
                listState.animateScrollToItem(index)
            }
        }
    }
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(models, key = { it.id }) { model ->
            val isSelected = model.id == selected?.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(model) },
                label = { 
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    )
                } else null
            )
        }
    }
}

@Composable
private fun DurationSelector(
    model: AIModel?,
    selected: Int?,
    onSelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
            items(model?.durationOptions.orEmpty()) { duration ->
            val isSelected = duration == selected
                FilterChip(
                selected = isSelected,
                    onClick = { onSelected(duration) },
                label = { 
                    Text(
                        text = "${duration}s",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun AspectRatioSelector(
    options: List<String>,
    selected: String?,
    onSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
            items(options) { ratio ->
            val isSelected = ratio == selected
                FilterChip(
                selected = isSelected,
                    onClick = { onSelected(ratio) },
                label = { 
                    Text(
                        text = ratio,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
