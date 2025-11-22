package com.manjul.genai.videogenerator.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppBottomSheetDialog
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialog
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppSelectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPill
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.ui.viewmodel.GenerateScreenState
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPill
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.InfoChip
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppElevatedCard
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

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

    BackHandler(enabled = true) {
        if (state.selectedModel != null) {
            onBackToModels()
        }
    }

    LaunchedEffect(preselectedModelId, state.models) {
        if (preselectedModelId != null && state.models.isNotEmpty()) {
            val model = state.models.find { it.id == preselectedModelId }
            if (model != null && state.selectedModel?.id != preselectedModelId) {
                viewModel.selectModel(model)
                onModelSelected()
            }
        }
    }

    val pickFirstFrame =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.setFirstFrameUri(uri)
        }
    val pickLastFrame =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.setLastFrameUri(uri)
        }

    val scrollState = rememberScrollState()
    var generationMode by rememberSaveable { mutableStateOf(GenerationMode.TextToVideo) }
    var showAdvanced by rememberSaveable { mutableStateOf(true) }
    var showPricingDialog by rememberSaveable { mutableStateOf(false) }

    // Premium dark background with subtle gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        when {
            state.isLoading -> GenerateLoadingState()
            state.models.isEmpty() -> EmptyGenerateState()
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 140.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GenerateHero(
                        generationMode = generationMode,
                        onModeSelected = { generationMode = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SectionCard(
                        title = "AI Model",
                        description = "Choose the AI model for video generation",
                        required = true,
                        infoText = "Different models excel at different subjects.",
                        onInfoClick = { showPricingDialog = true }
                    ) {
                        ModelSelector(
                            models = state.models,
                            selected = state.selectedModel,
                            onSelected = viewModel::selectModel
                        )
                    }

                    state.selectedModel?.let { model ->
                        if (model.supportsFirstFrame || model.supportsLastFrame) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionCard(
                                title = "Reference Images",
                                description = "Select reference frames to guide motion",
                                required = model.requiresFirstFrame || model.requiresLastFrame,
                                infoText = "Add keyframes to lock composition"
                            ) {
                                ReferenceFrameSection(
                                    model = model,
                                    firstFrame = state.firstFrameUri,
                                    lastFrame = state.lastFrameUri,
                                    onPickFirst = { pickFirstFrame.launch("image/*") },
                                    onPickLast = { pickLastFrame.launch("image/*") },
                                    onClearFirst = { viewModel.setFirstFrameUri(null) },
                                    onClearLast = { viewModel.setLastFrameUri(null) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionCard(
                        title = "Main Text Prompt",
                        description = "Describe what you want to see in detail",
                        required = true,
                        infoText = "Detailed prompts lead to richer scenes"
                    ) {
                        AppTextField(
                            value = state.prompt,
                            onValueChange = viewModel::updatePrompt,
                            placeholder = "Tap here to type your prompt",
                            maxLines = 5
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionCard(
                        title = "Advanced Settings",
                        description = "Adjust aspect ratio and duration",
                        required = false,
                        optionalLabel = "Optional",
                        infoText = "Fine tune generation controls",
                        onHeaderClick = { showAdvanced = !showAdvanced },
                        expandable = true,
                        expanded = showAdvanced
                    ) {
                        AnimatedVisibility(visible = showAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                DurationAspectRow(
                                    selectedModel = state.selectedModel,
                                    selectedDuration = state.selectedDuration,
                                    selectedAspectRatio = state.selectedAspectRatio,
                                    onDurationSelected = viewModel::updateDuration,
                                    onAspectRatioSelected = viewModel::updateAspectRatio
                                )

                                if (state.selectedModel?.supportsAudio == true) {
                                    AudioToggle(
                                        enabled = state.enableAudio,
                                        onToggle = viewModel::toggleAudio
                                    )
                                }

                                if (state.selectedModel?.schemaMetadata?.categorized?.text?.any { it.name == "negative_prompt" } == true) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Negative Prompt",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.TextPrimary
                                        )
                                        AppTextField(
                                            value = state.negativePrompt,
                                            onValueChange = viewModel::updateNegativePrompt,
                                            placeholder = "What should we avoid? e.g. blurry, low quality",
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.uploadMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusBanner(message = message)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    VideoExamplesSection(models = state.models)

                    Spacer(modifier = Modifier.height(20.dp))
                    ContentGuidelinesCard()

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        GenerateBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = state,
            onGenerate = {
                viewModel.dismissMessage()
                viewModel.generate()
                if (state.canGenerate) {
                    onGenerateStarted()
                }
            }
        )
    }

    if (!state.isGenerating && state.errorMessage != null) {
        AppDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = "Error"
        ) {
            Text(
                text = state.errorMessage ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AppTextButton(
                    text = "OK",
                    onClick = viewModel::dismissMessage
                )
            }
        }
    }

    if (showPricingDialog) {
        PricingDialog(
            models = state.models,
            onDismiss = { showPricingDialog = false }
        )
    }
}

@Composable
private fun GenerateLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Loading Gen-AI Studio",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fetching the latest models...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyGenerateState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No AI models available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Please try again in a moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GenerateHero(
    generationMode: GenerationMode,
    onModeSelected: (GenerationMode) -> Unit
) {
    // Premium gradient background
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(heroGradient)
                .clip(RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gen-AI Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Try Veo 3, Sora 2 and more",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                ModeToggle(
                    selected = generationMode,
                    onModeSelected = onModeSelected
                )
            }
        }
    }
}

@Composable
private fun ModeToggle(
    selected: GenerationMode,
    onModeSelected: (GenerationMode) -> Unit
) {
    val modes = GenerationMode.values()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            modes.forEach { mode ->
                val isSelected = mode == selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.95f,
                    animationSpec = tween(200),
                    label = "scale"
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(40))
                        .clickable { onModeSelected(mode) }
                        .scale(scale),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    tonalElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

// SectionCard, RequirementBadge, and InfoIcon now use design system components

@Composable
private fun ModelSelector(
    models: List<AIModel>,
    selected: AIModel?,
    onSelected: (AIModel) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected?.id) {
        if (selected != null) {
            val index = models.indexOfFirst { it.id == selected.id }
            if (index >= 0) {
                delay(50)
                listState.animateScrollToItem(index)
            }
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(models, key = { it.id }) { model ->
            val isSelected = selected?.id == model.id
            ModelCard(
                model = model,
                selected = isSelected,
                onClick = { onSelected(model) }
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: AIModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Get full subtitle with type and price (e.g., "Fast, ~4c/s" or "~4c/s")
    val fullSubtitle = getModelSubtitle(model)
    // Remove the type from model name if it appears in subtitle
    val displayName = getDisplayName(model, fullSubtitle)
    val logoUrl = getModelLogoUrl(model)

    AppSelectionCard(
        isSelected = selected,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model Logo - white icon that blends with theme (always show with fallback)
            ModelLogo(
                logoUrl = logoUrl,
                modelName = displayName.ifBlank { model.name },
                modifier = Modifier.size(48.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val nameText = displayName.ifBlank { model.name }
                val subtitleText = fullSubtitle.ifBlank { formatCredits(model.pricePerSecond) }
                
                Text(
                    text = nameText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Gets the logo URL for a model based on its name or company/provider.
 * Uses company logos from reliable sources. Returns null if no logo is available.
 */
private fun getModelLogoUrl(model: AIModel): String? {
    val modelId = model.id.lowercase()
    val modelName = model.name.lowercase()
    val replicateName = model.replicateName.lowercase()

    // Check by replicate name provider first (most reliable)
    if (replicateName.contains("/")) {
        val provider = replicateName.split("/").first().lowercase()
        // Map provider to company domain for logo API
        val companyDomain = when {
            provider.contains("google") -> "google.com"
            provider.contains("openai") -> "openai.com"
            provider.contains("runwayml") || provider.contains("runway") -> "runwayml.com"
            provider.contains("minimax") -> "minimax.chat"
            provider.contains("kwaivgi") || provider.contains("kuaishou") -> "kuaishou.com"
            provider.contains("lightricks") -> "lightricks.com"
            provider.contains("leonardoai") || provider.contains("leonardo") -> "leonardo.ai"
            provider.contains("character-ai") || provider.contains("characterai") -> "character.ai"
            provider.contains("pixverse") -> "pixverse.ai"
            provider.contains("luma") -> "lumalabs.ai"
            provider.contains("bytedance") -> "bytedance.com"
            else -> null
        }

        // Use Clearbit Logo API (free, no API key required for basic usage)
        return companyDomain?.let { "https://logo.clearbit.com/$it" }
    }

    // Map model IDs/names to company domains for logo API (fallback)
    val modelToDomainMap = mapOf(
        // Google/Veo models
        "veo" to "google.com",
        // OpenAI/Sora models
        "sora" to "openai.com",
        // Kling models (Kuaishou)
        "kling" to "kuaishou.com",
        // Hailuo models (Minimax)
        "hailuo" to "minimax.chat",
        // Gen4 models (Runway)
        "gen4" to "runwayml.com",
        // LTX models (Lightricks)
        "ltx" to "lightricks.com",
        // Motion models (Leonardo)
        "motion" to "leonardo.ai",
        // Ovi models (Character AI)
        "ovi" to "character.ai",
        // Pixverse models
        "pixverse" to "pixverse.ai",
        // Ray models (Luma)
        "ray" to "lumalabs.ai",
        // Seedance models (ByteDance)
        "seedance" to "bytedance.com"
    )

    // Check by model ID
    for ((key, domain) in modelToDomainMap) {
        if (modelId.contains(key)) {
            return "https://logo.clearbit.com/$domain"
        }
    }

    // Check by model name
    for ((key, domain) in modelToDomainMap) {
        if (modelName.contains(key)) {
            return "https://logo.clearbit.com/$domain"
        }
    }

    return null
}

/**
 * Composable that displays a model logo with fallback to initial letter.
 * Icons are displayed in white to blend with the theme.
 */
@Composable
private fun ModelLogo(
    logoUrl: String?,
    modelName: String,
    modifier: Modifier = Modifier
) {
    val initial = modelName.firstOrNull()?.uppercaseChar()?.toString() ?: "AI"

    // Color filter to convert logo to white - blends with theme
    // Uses a color matrix that desaturates and brightens the image
    val whiteColorFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply {
            setToSaturation(0f) // Remove color (grayscale)
            // Brighten to white
            val brightness = 1.5f
            setToScale(brightness, brightness, brightness, 1f)
        }
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$modelName logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit,
                colorFilter = whiteColorFilter, // Convert to white to blend with theme
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.TextPrimary.copy(alpha = 0.5f)
                        )
                    }
                },
                error = {
                    // Fallback to initial if image fails to load
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            )
        } else {
            // Fallback to initial letter
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * Gets the display name for the model card by removing the type from the model name
 * if it appears in the subtitle (e.g., "Veo 3.1 Fast" -> "Veo 3.1" if subtitle contains "Fast")
 */
private fun getDisplayName(model: AIModel, subtitle: String): String {
    // Extract type text from subtitle (everything before the comma and price)
    val typeText = subtitle.split(",").firstOrNull()?.trim() ?: return model.name

    // If subtitle is just price (no type), return original name
    if (typeText.startsWith("~") && typeText.endsWith("c/s")) {
        return model.name
    }

    // Remove the type from model name (case-insensitive)
    var displayName = model.name
    val typeWords = typeText.split(" ").filter { it.isNotBlank() }

    for (word in typeWords) {
        // Remove the word from display name (case-insensitive)
        val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
        displayName = regex.replace(displayName, "").trim()
    }

    // Clean up extra spaces
    displayName = displayName.replace(Regex("\\s+"), " ").trim()

    return displayName.ifBlank { model.name }
}

/**
 * Gets a subtitle for the model card based on available model data from Firestore.
 * Dynamically extracts type from model ID and combines with price (e.g., "Fast, ~4c/s" or "~4c/s")
 */
private fun getModelSubtitle(model: AIModel): String {
    val priceText = formatCredits(model.pricePerSecond)

    // Dynamically extract type from model ID by analyzing its structure
    val modelId = model.id.lowercase()
    val segments = modelId.split("-")

    // Filter out version-like segments (numbers, "v2", "v3", etc.) and base model names
    // Keep meaningful type segments (fast, pro, lite, master, turbo, i2v, t2v, resolutions, etc.)
    val versionPattern = Regex("^v?\\d+(\\.\\d+)?$") // Matches: "3", "v2", "2.5", "v2.1", etc.
    val baseModelNames = setOf(
        "veo", "sora", "kling", "hailuo", "gen4", "gen", "ltx", "motion",
        "ovi", "pixverse", "ray", "seedance", "wan", "hailuo"
    )

    val typeSegments = segments.filter { segment ->
        !versionPattern.matches(segment) &&
                !baseModelNames.contains(segment) &&
                segment.isNotBlank()
    }

    // If we have type segments, format and combine with price
    if (typeSegments.isNotEmpty()) {
        val typeText = typeSegments.joinToString(" ") { segment ->
            // Keep resolution formats like "720p", "1080p" as-is
            if (segment.endsWith("p") && segment.dropLast(1).all { it.isDigit() }) {
                segment
            } else {
                // Capitalize other segments (e.g., "fast" -> "Fast", "turbo-pro" -> "Turbo Pro")
                segment.split("-").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
                }
            }
        }
        return "$typeText, $priceText"
    }

    // If no type found, just show price
    return priceText
}

// InfoChip now uses design system component

@Composable
private fun ReferenceFrameSection(
    model: AIModel,
    firstFrame: Uri?,
    lastFrame: Uri?,
    onPickFirst: () -> Unit,
    onPickLast: () -> Unit,
    onClearFirst: () -> Unit,
    onClearLast: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (model.supportsFirstFrame) {
            ReferenceFramePicker(
                label = "First Frame",
                required = model.requiresFirstFrame,
                uri = firstFrame,
                onPick = onPickFirst,
                onClear = onClearFirst
            )
        }
        if (model.supportsLastFrame) {
            ReferenceFramePicker(
                label = "Last Frame",
                required = model.requiresLastFrame,
                uri = lastFrame,
                onPick = onPickLast,
                onClear = onClearLast
            )
        }
        if (!model.supportsFirstFrame && !model.supportsLastFrame) {
            Text(
                text = "Reference frames are not required for ${model.name}.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            if (required) {
                StatusBadge(text = "Required")
            }
        }
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(
                    2.dp,
                    if (uri == null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    20.dp
                )
                .clickable { onPick() },
            onClick = onPick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (uri == null) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (uri == null) Icons.Default.Image else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (uri == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = if (uri == null) "Add Reference Image" else "Image Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = if (uri == null) "Tap to begin" else "Tap to replace",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                if (uri != null) {
                    AppSecondaryButton(
                        text = "Remove",
                        onClick = onClear,
                        fullWidth = false
                    )
                }
            }
        }
    }
}

// PromptField and NegativePromptField now use AppTextField from design system

@Composable
private fun DurationAspectRow(
    selectedModel: AIModel?,
    selectedDuration: Int?,
    selectedAspectRatio: String?,
    onDurationSelected: (Int) -> Unit,
    onAspectRatioSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Duration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        DurationSelector(
            model = selectedModel,
            selected = selectedDuration,
            onSelected = onDurationSelected
        )
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        AspectRatioSelector(
            options = selectedModel?.aspectRatios.orEmpty(),
            selected = selectedAspectRatio,
            onSelected = onAspectRatioSelected
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationSelector(
    model: AIModel?,
    selected: Int?,
    onSelected: (Int) -> Unit
) {
    val options = model?.durationOptions.orEmpty()
    if (options.isEmpty()) {
        Text(
            text = "No duration options available",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { duration ->
                SelectionPill(
                    text = "${duration}s",
                    selected = duration == selected,
                    onClick = { onSelected(duration) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AspectRatioSelector(
    options: List<String>,
    selected: String?,
    onSelected: (String) -> Unit
) {
    if (options.isEmpty()) {
        Text(
            text = "Aspect ratios are unavailable",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { ratio ->
                SelectionPill(
                    text = ratio,
                    selected = ratio == selected,
                    onClick = { onSelected(ratio) }
                )
            }
        }
    }
}

// SelectionPill now uses design system component

@Composable
private fun AudioToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Enable Audio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (enabled) "Audio enabled • cost x2" else "Add AI-composed audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        AppColors.StatusError
                    } else {
                        AppColors.TextSecondary
                    }
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AppColors.PrimaryPurple,
                strokeWidth = 2.5.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun PricingDialog(
    models: List<AIModel>,
    onDismiss: () -> Unit
) {
    AppBottomSheetDialog(
        onDismissRequest = onDismiss,
        title = "Pricing"
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Credits per second",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                models
                    .sortedByDescending { it.pricePerSecond }
                    .forEach { model ->
                        PricingRow(model = model)
                    }
            }

            Text(
                text = "Final cost = credits/sec × video duration",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PricingRow(model: AIModel) {
    val logoUrl = getModelLogoUrl(model)

    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModelLogo(
                    logoUrl = logoUrl,
                    modelName = model.name,
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    model.description.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
            Text(
                text = formatCredits(model.pricePerSecond),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryPurple
            )
        }
    }
}

@Composable
private fun ModelAvatar(model: AIModel) {
    val initial = model.name.firstOrNull()?.uppercaseChar()?.toString() ?: "AI"
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun VideoExamplesSection(models: List<AIModel>) {
    if (models.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Video Examples",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(models.take(4), key = { it.id }) { model ->
                ExampleCard(model)
            }
        }
    }
}

@Composable
private fun ExampleCard(model: AIModel) {
    AppElevatedCard(
        modifier = Modifier
            .width(220.dp)
            .aspectRatio(3f / 2f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = AppColors.PrimaryPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentGuidelinesCard() {
    val guidelines = listOf(
        "No nudity, violence, or harmful content across all models.",
        "Veo: No minors or celebrity likenesses.",
        "Sora: Avoid minors, celebrities, or real person footage.",
        "Generation starts immediately and cannot be canceled."
    )
    AppCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppColors.StatusErrorBackground
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = AppColors.StatusError,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = "Content Guidelines",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
            guidelines.forEach { text ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.StatusError,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerateBottomBar(
    modifier: Modifier = Modifier,
    state: GenerateScreenState,
    onGenerate: () -> Unit
) {
    AppElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Cost",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${state.estimatedCost} credits",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryPurple
                    )
                }
                if (state.enableAudio) {
                    InfoChip("2x Audio")
                }
            }
            AppPrimaryButton(
                text = if (state.isGenerating) "Generating..." else "Generate AI Video",
                onClick = onGenerate,
                enabled = state.canGenerate,
                isLoading = state.isGenerating,
                icon = if (!state.isGenerating) Icons.Default.PlayArrow else null
            )
            Text(
                text = "Select reference images and enter a prompt to start.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

private enum class GenerationMode(val label: String) {
    TextToVideo("Text to Video"),
    ImageToVideo("Image to Video")
}

private fun formatCredits(rate: Int): String = "~$rate c/s"

private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp): Modifier =
    this.then(
        Modifier.drawBehind {
            val strokeWidth = width.toPx()
            val halfStroke = strokeWidth / 2
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                size = androidx.compose.ui.geometry.Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = pathEffect,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    )

// ==================== Preview ====================

@Preview(
    name = "Generate Screen",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
private fun GenerateScreenPreview() {
    GenAiVideoTheme {
        // Mock data for preview (no ViewModel)
        val mockModels = listOf(
            AIModel(
                id = "veo-3.1",
                name = "Veo 3.1",
                description = "High-quality video generation model",
                pricePerSecond = 4,
                defaultDuration = 4,
                durationOptions = listOf(4, 6, 8),
                aspectRatios = listOf("16:9", "9:16", "1:1"),
                supportsFirstFrame = true,
                requiresFirstFrame = false,
                supportsLastFrame = true,
                requiresLastFrame = false,
                previewUrl = "",
                replicateName = "veo-3.1",
                exampleVideoUrl = null,
                supportsAudio = true
            ),
            AIModel(
                id = "sora-2",
                name = "Sora 2",
                description = "Advanced video generation",
                pricePerSecond = 6,
                defaultDuration = 5,
                durationOptions = listOf(5, 10),
                aspectRatios = listOf("16:9", "9:16"),
                supportsFirstFrame = false,
                requiresFirstFrame = false,
                supportsLastFrame = false,
                requiresLastFrame = false,
                previewUrl = "",
                replicateName = "sora-2",
                exampleVideoUrl = null,
                supportsAudio = false
            )
        )

        val selectedModel = mockModels.first()
        var generationMode by remember { mutableStateOf(GenerationMode.TextToVideo) }
        var showAdvanced by remember { mutableStateOf(true) }
        var promptText by remember { mutableStateOf("A cinematic video of a sunset over mountains") }
        var negativePromptText by remember { mutableStateOf("") }
        var selectedDuration by remember { mutableStateOf<Int?>(4) }
        var selectedAspectRatio by remember { mutableStateOf<String?>("16:9") }
        var enableAudio by remember { mutableStateOf(false) }

        val scrollState = rememberScrollState()

        // Premium dark background with subtle gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 140.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                GenerateHero(
                    generationMode = generationMode,
                    onModeSelected = { generationMode = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SectionCard(
                    title = "AI Model",
                    description = "Choose the AI model for video generation",
                    required = true,
                    infoText = "Different models excel at different subjects.",
                    onInfoClick = {}
                ) {
                    ModelSelector(
                        models = mockModels,
                        selected = selectedModel,
                        onSelected = {}
                    )
                }

                if (selectedModel.supportsFirstFrame || selectedModel.supportsLastFrame) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionCard(
                        title = "Reference Images",
                        description = "Select reference frames to guide motion",
                        required = selectedModel.requiresFirstFrame || selectedModel.requiresLastFrame,
                        infoText = "Add keyframes to lock composition"
                    ) {
                        ReferenceFrameSection(
                            model = selectedModel,
                            firstFrame = null,
                            lastFrame = null,
                            onPickFirst = {},
                            onPickLast = {},
                            onClearFirst = {},
                            onClearLast = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionCard(
                    title = "Main Text Prompt",
                    description = "Describe what you want to see in detail",
                    required = true,
                    infoText = "Detailed prompts lead to richer scenes"
                ) {
                    AppTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = "Tap here to type your prompt",
                        maxLines = 5
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                SectionCard(
                    title = "Advanced Settings",
                    description = "Adjust aspect ratio and duration",
                    required = false,
                    optionalLabel = "Optional",
                    infoText = "Fine tune generation controls",
                    onHeaderClick = { showAdvanced = !showAdvanced },
                    expandable = true,
                    expanded = showAdvanced
                ) {
                    AnimatedVisibility(visible = showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            DurationAspectRow(
                                selectedModel = selectedModel,
                                selectedDuration = selectedDuration,
                                selectedAspectRatio = selectedAspectRatio,
                                onDurationSelected = { selectedDuration = it },
                                onAspectRatioSelected = { selectedAspectRatio = it }
                            )

                            if (selectedModel.supportsAudio) {
                                AudioToggle(
                                    enabled = enableAudio,
                                    onToggle = { enableAudio = it }
                                )
                            }

                            if (selectedModel.schemaMetadata?.categorized?.text?.any { it.name == "negative_prompt" } == true) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Negative Prompt",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary
                                    )
                                    AppTextField(
                                        value = negativePromptText,
                                        onValueChange = { negativePromptText = it },
                                        placeholder = "What should we avoid? e.g. blurry, low quality",
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                VideoExamplesSection(models = mockModels)

                Spacer(modifier = Modifier.height(20.dp))
                ContentGuidelinesCard()

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Mock bottom bar
            val estimatedCost =
                (selectedModel.pricePerSecond * (selectedDuration ?: 0)) * if (enableAudio) 2 else 1
            val canGenerate =
                promptText.isNotBlank() && selectedModel != null && selectedDuration != null && selectedAspectRatio != null

            GenerateBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                state = GenerateScreenState(
                    isLoading = false,
                    models = mockModels,
                    selectedModel = selectedModel,
                    prompt = promptText,
                    negativePrompt = negativePromptText,
                    selectedDuration = selectedDuration,
                    selectedAspectRatio = selectedAspectRatio,
                    enableAudio = enableAudio,
                    firstFrameUri = null,
                    lastFrameUri = null
                ),
                onGenerate = {}
            )
        }
    }
}
