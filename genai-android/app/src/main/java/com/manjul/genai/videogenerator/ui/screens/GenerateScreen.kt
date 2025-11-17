package com.manjul.genai.videogenerator.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel

@Composable
fun GenerateScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory),
    preselectedModelId: String? = null,
    onModelSelected: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Select model", style = MaterialTheme.typography.titleMedium)
        ModelSelector(
            models = state.models,
            selected = state.selectedModel,
            onSelected = viewModel::selectModel
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.prompt,
            onValueChange = viewModel::updatePrompt,
            label = { Text("Prompt") },
            singleLine = false,
            maxLines = 4
        )

        // Negative prompt - only show if model supports it
        if (state.selectedModel?.schemaMetadata?.categorized?.text?.any { it.name == "negative_prompt" } == true) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.negativePrompt,
                onValueChange = viewModel::updateNegativePrompt,
                label = { Text("Negative Prompt (Optional)") },
                placeholder = { Text("Things you don't want to see in the video") },
                singleLine = false,
                maxLines = 3
            )
        }

        DurationSelector(
            model = state.selectedModel,
            selected = state.selectedDuration,
            onSelected = viewModel::updateDuration
        )

        AspectRatioSelector(
            options = state.selectedModel?.aspectRatios.orEmpty(),
            selected = state.selectedAspectRatio,
            onSelected = viewModel::updateAspectRatio
        )

        Text(
            text = "Estimated cost: ${state.estimatedCost} credits",
            fontWeight = FontWeight.Bold
        )

        // Audio toggle - only show if model supports audio
        if (state.selectedModel?.supportsAudio == true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Audio",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (state.enableAudio) "Cost will be doubled" else "Generate video with audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.enableAudio,
                    onCheckedChange = viewModel::toggleAudio
                )
            }
        }

        // Only show first frame picker if model supports it
        if (state.selectedModel?.supportsFirstFrame == true) {
            ReferenceFramePicker(
                label = "First frame",
                required = state.selectedModel?.requiresFirstFrame?: false,
                uri = state.firstFrameUri,
                onPick = { pickFirstFrame.launch("image/*") },
                onClear = { viewModel.setFirstFrameUri(null) }
            )
        }

        // Only show last frame picker if model supports it
        if (state.selectedModel?.supportsLastFrame == true) {
            ReferenceFramePicker(
                label = "Last frame",
                required = state.selectedModel?.requiresLastFrame?: false,
                uri = state.lastFrameUri,
                onPick = { pickLastFrame.launch("image/*") },
                onClear = { viewModel.setLastFrameUri(null) }
            )
        }

        state.uploadMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = viewModel::generate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isGenerating) "Generating..." else "Generate Video")
        }
    }

    val message = state.errorMessage ?: state.successMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text("OK")
                }
            },
            title = { Text(if (state.errorMessage != null) "Alert" else "Success") },
            text = { Text(message) }
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = buildString {
                append(label)
                if (required) append(" (required)")
            },
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPick) {
                Text(if (uri == null) "Select Image" else "Replace Image")
            }
            if (uri != null) {
                TextButton(onClick = onClear) {
                    Text("Remove")
                }
            }
        }
        if (required && uri == null) {
            Text(
                text = "Required for this model",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (uri != null) {
            Text(
                text = "Selected: ${uri.lastPathSegment ?: uri}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<AIModel>,
    selected: AIModel?,
    onSelected: (AIModel) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(models, key = { it.id }) { model ->
            FilterChip(
                selected = model.id == selected?.id,
                onClick = { onSelected(model) },
                label = { Text(model.name) }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Duration", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(model?.durationOptions.orEmpty()) { duration ->
                FilterChip(
                    selected = duration == selected,
                    onClick = { onSelected(duration) },
                    label = { Text("${duration}s") }
                )
            }
        }
    }
}

@Composable
private fun AspectRatioSelector(
    options: List<String>,
    selected: String?,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Aspect Ratio", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { ratio ->
                FilterChip(
                    selected = ratio == selected,
                    onClick = { onSelected(ratio) },
                    label = { Text(ratio) }
                )
            }
        }
    }
}
