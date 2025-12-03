package com.manjul.genai.videogenerator.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.manjul.genai.videogenerator.data.model.AIModel
import com.manjul.genai.videogenerator.player.VideoPlayerManager
import com.manjul.genai.videogenerator.ui.components.VideoThumbnail
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.InfoChip
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppElevatedCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppSelectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppBottomSheetDialog
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialog
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPill
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.ui.viewmodel.CreditsViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.GenerateScreenState
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.delay

@Composable
fun GenerateScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory),
    creditsViewModel: CreditsViewModel = viewModel(factory = CreditsViewModel.Factory),
    preselectedModelId: String? = null,
    onModelSelected: () -> Unit = {},
    onBackToModels: () -> Unit = {},
    onGenerateStarted: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onBuyCreditsClick: (requiredCredits: Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val creditsState by creditsViewModel.state.collectAsState()

    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("Generate")
    }

    BackHandler(enabled = false) {
        // Disable back handler - let system handle back button to exit app
        // This ensures pressing back minimizes the app instead of navigating to ModelsScreen
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

    var generationMode by rememberSaveable { mutableStateOf(GenerationMode.TextToVideo) }
    var showAdvanced by rememberSaveable { mutableStateOf(true) }
    var showPricingDialog by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Handle mode change and model compatibility
    val onModeSelected: (GenerationMode) -> Unit = { newMode ->
        generationMode = newMode
        // Auto-select first compatible model if current model doesn't support the new mode
        state.selectedModel?.let { currentModel ->
            val isCompatible = when (newMode) {
                GenerationMode.TextToVideo -> isTextToVideoModel(currentModel)
                GenerationMode.ImageToVideo -> isImageToVideoModel(currentModel)
            }
            if (!isCompatible) {
                // Find first compatible model for the new mode
                val compatibleModel = when (newMode) {
                    GenerationMode.TextToVideo -> state.models.firstOrNull { isTextToVideoModel(it) }
                    GenerationMode.ImageToVideo -> state.models.firstOrNull { isImageToVideoModel(it) }
                }
                compatibleModel?.let { viewModel.selectModel(it) }
            }
        } ?: run {
            // If no model is selected, auto-select first compatible model
            val compatibleModel = when (newMode) {
                GenerationMode.TextToVideo -> state.models.firstOrNull { isTextToVideoModel(it) }
                GenerationMode.ImageToVideo -> state.models.firstOrNull { isImageToVideoModel(it) }
            }
            compatibleModel?.let { viewModel.selectModel(it) }
        }
    }

    // Reset isSubmitting when generation state changes
    LaunchedEffect(state.isGenerating, state.errorMessage) {
        if (!state.isGenerating || state.errorMessage != null) {
            isSubmitting = false
        }
    }

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
                GenerateScreenContent(
                    state = state,
                    creditsCount = creditsState.credits,
                    generationMode = generationMode,
                    showAdvanced = showAdvanced,
                    isSubmitting = isSubmitting,
                    onModeSelected = onModeSelected,
                    onModelSelected = viewModel::selectModel,
                    onPromptChanged = viewModel::updatePrompt,
                    onNegativePromptChanged = viewModel::updateNegativePrompt,
                    onDurationSelected = viewModel::updateDuration,
                    onAspectRatioSelected = viewModel::updateAspectRatio,
                    onAudioToggled = viewModel::toggleAudio,
                    onPickFirstFrame = { pickFirstFrame.launch("image/*") },
                    onPickLastFrame = { pickLastFrame.launch("image/*") },
                    onClearFirstFrame = { viewModel.setFirstFrameUri(null) },
                    onClearLastFrame = { viewModel.setLastFrameUri(null) },
                    onAdvancedToggle = { showAdvanced = !showAdvanced },
                    onGenerateClick = {
                        if (!isSubmitting && state.canGenerate && !state.isGenerating && state.uploadMessage == null) {
                            // Check if user has enough credits
                            val estimatedCost = state.estimatedCost
                            if (creditsState.credits < estimatedCost) {
                                // Navigate to BuyCreditsScreen if insufficient credits
                                onBuyCreditsClick(estimatedCost)
                            } else {
                                isSubmitting = true
                                viewModel.dismissMessage()
                                viewModel.generate()
                            }
                        }
                    },
                    onSettingsClick = onSettingsClick,
                    onCreditsClick = onCreditsClick,
                    onPricingInfoClick = { showPricingDialog = true }
                )
            }
        }
    }

    if (!state.isGenerating && state.errorMessage != null) {
        // Only show error dialog for non-credit related errors
        // Credit-related errors are handled by navigating to BuyCreditsScreen
        val errorMessage = state.errorMessage ?: ""
        if (!errorMessage.contains("Insufficient credits", ignoreCase = true) && 
            !errorMessage.contains("not enough credits", ignoreCase = true)) {
            AppDialog(
                onDismissRequest = viewModel::dismissMessage,
                title = "Error"
            ) {
                Text(
                    text = errorMessage.ifBlank { "Unknown error" },
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
        } else {
            // For credit errors, dismiss the message and navigate to BuyCreditsScreen
            LaunchedEffect(errorMessage) {
                viewModel.dismissMessage()
                // Try to extract the required credits from error message, default to estimated cost
                onBuyCreditsClick(state.estimatedCost)
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

/**
 * Extracted content composable that accepts state directly.
 * This allows both the actual screen (with ViewModels) and preview (with mock state) to use the same UI.
 */
@Composable
private fun GenerateScreenContent(
    state: GenerateScreenState,
    creditsCount: Int,
    generationMode: GenerationMode,
    showAdvanced: Boolean,
    isSubmitting: Boolean,
    onModeSelected: (GenerationMode) -> Unit,
    onModelSelected: (AIModel) -> Unit,
    onPromptChanged: (String) -> Unit,
    onNegativePromptChanged: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onAspectRatioSelected: (String) -> Unit,
    onAudioToggled: (Boolean) -> Unit,
    onPickFirstFrame: () -> Unit,
    onPickLastFrame: () -> Unit,
    onClearFirstFrame: () -> Unit,
    onClearLastFrame: () -> Unit,
    onAdvancedToggle: () -> Unit,
    onGenerateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onPricingInfoClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content with bottom padding for pinned button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 140.dp) // Space for pinned button
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with title, credits, settings, and description
            GenerateHeader(
                creditsCount = creditsCount,
                onSettingsClick = onSettingsClick,
                onCreditsClick = onCreditsClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Container with border wrapping all generation controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = AppColors.BackgroundDarkGray,
                border = BorderStroke(
                    width = 1.dp,
                    color = AppColors.TextSecondary.copy(alpha = 0.2f)
                ),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Mode Toggle
                    ModeToggle(
                        selected = generationMode,
                        onModeSelected = onModeSelected
                    )

                    SectionCard(
                        title = "AI Model",
                        description = "Choose the AI model for video generation",
                        required = true,
                        infoText = "Different models excel at different subjects.",
                        onInfoClick = onPricingInfoClick
                    ) {
                        ModelSelector(
                            models = state.models,
                            selected = state.selectedModel,
                            generationMode = generationMode,
                            onSelected = onModelSelected
                        )
                    }

                    // Only show Reference Images section in Image-to-Video mode and for models that support it
                    state.selectedModel?.let { model ->
                        if (generationMode == GenerationMode.ImageToVideo &&
                            (model.supportsFirstFrame || model.supportsLastFrame)
                        ) {
                            SectionCard(
                                title = "Reference Images",
                                description = "Select reference frames to guide motion",
                                required = model.requiresFirstFrame || model.requiresLastFrame,
                                useGradientBorder = true
                            ) {
                                ReferenceFrameSection(
                                    model = model,
                                    firstFrame = state.firstFrameUri,
                                    lastFrame = state.lastFrameUri,
                                    onPickFirst = onPickFirstFrame,
                                    onPickLast = onPickLastFrame,
                                    onClearFirst = onClearFirstFrame,
                                    onClearLast = onClearLastFrame
                                )
                            }
                        }
                    }

                    SectionCard(
                        title = "Main Text Prompt",
                        description = "Describe what you want to see in detail",
                        required = true,
                        useGradientBorder = true
                    ) {
                        AppTextField(
                            value = state.prompt,
                            onValueChange = onPromptChanged,
                            placeholder = "\uD83D\uDC46 Tap here to type your prompt \\nEg: A medium shot, historical adventure setting: Warm lamplight illuminates a cartographer in a clustered study, poring over an ancient, sprawling map spread across a large table. Cartographer: \"According to this old sea chart,the lost island isn't myth!, We must prepare an exp..........",
                            maxLines = 5
                        )
                    }

                    SectionCard(
                        title = "Advanced Settings",
                        description = "Adjust aspect ratio and duration",
                        required = false,
                        optionalLabel = "Optional",
                        onHeaderClick = onAdvancedToggle,
                        expandable = true,
                        expanded = showAdvanced
                    ) {
                        AnimatedVisibility(visible = showAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DurationAspectRow(
                                    selectedModel = state.selectedModel,
                                    selectedDuration = state.selectedDuration,
                                    selectedAspectRatio = state.selectedAspectRatio,
                                    onDurationSelected = onDurationSelected,
                                    onAspectRatioSelected = onAspectRatioSelected
                                )

                                if (state.selectedModel?.supportsAudio == true) {
                                    AudioToggle(
                                        enabled = state.enableAudio,
                                        onToggle = onAudioToggled
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
                                            onValueChange = onNegativePromptChanged,
                                            placeholder = "What should we avoid? e.g. blurry, low quality",
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            state.selectedModel?.let { model -> VideoExamplesSection(model = model) }
            ContentGuidelinesCard()

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Pinned CTA at the bottom of the screen
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .drawBehind {
                    val borderWidth = 2.dp.toPx()
                    val cornerRadius = 32.dp.toPx()

                    // Draw top border with rounded corners
                    val topPath = Path().apply {
                        // Start from top-left, after the rounded corner starts
                        moveTo(0f, cornerRadius)
                        // Draw arc for top-left rounded corner
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                offset = Offset(0f, 0f),
                                size = Size(cornerRadius * 2, cornerRadius * 2)
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        // Draw straight top border line
                        lineTo(size.width - cornerRadius, 0f)
                        // Draw arc for top-right rounded corner
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                offset = Offset(size.width - cornerRadius * 2, 0f),
                                size = Size(cornerRadius * 2, cornerRadius * 2)
                            ),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                    }
                    drawPath(
                        path = topPath,
                        color = AppColors.BorderSelected,
                        style = Stroke(width = borderWidth, cap = StrokeCap.Round)
                    )

                    // Draw left border (straight line from below top corner to bottom)
                    drawLine(
                        color = AppColors.BorderSelected,
                        start = Offset(0f, cornerRadius),
                        end = Offset(0f, size.height),
                        strokeWidth = borderWidth,
                        cap = StrokeCap.Round
                    )

                    // Draw right border (straight line from below top corner to bottom)
                    drawLine(
                        color = AppColors.BorderSelected,
                        start = Offset(size.width, cornerRadius),
                        end = Offset(size.width, size.height),
                        strokeWidth = borderWidth,
                        cap = StrokeCap.Round
                    )
                },
            color = AppColors.BackgroundDarkGray,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 32.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Determine button state and text based on all conditions
                val isUploading = state.uploadMessage != null
                val buttonText = when {
                    isUploading -> state.uploadMessage ?: "Uploading..."
                    state.isGenerating || isSubmitting -> "Submitting..."
                    else -> "Generate AI Video"
                }

                // Debug logging
                LaunchedEffect(state.uploadMessage, buttonText) {
                    Log.d(
                        "GenerateScreen",
                        "📱 Button Update - uploadMessage: '${state.uploadMessage}', buttonText: '$buttonText', isUploading: $isUploading"
                    )
                }

                val isButtonLoading = state.isGenerating || isSubmitting || isUploading
                val isButtonEnabled =
                    state.canGenerate && !isSubmitting && !state.isGenerating && !isUploading

                // Gradient Generate Button with dynamic state
                GradientGenerateButton(
                    text = buttonText,
                    onClick = onGenerateClick,
                    enabled = isButtonEnabled,
                    isLoading = isButtonLoading
                )
                // Cost and audio indicator on the left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Absolute.Center
                ) {
                    Text(
                        text = "Estimated Cost: ${state.estimatedCost} credits",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.PrimaryPurple
                    )
                    if (state.enableAudio) {
                        InfoChip("2x Audio")
                    }
                }
            }
        }
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
private fun GradientGenerateButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    // Animated scale for active state
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = tween(200),
        label = "buttonScale"
    )

    // Gradient from purple to orange - catchy and blends with design system
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            AppColors.PrimaryPurple,
            Color(0xFFFF6B35) // Orange color that blends with purple
        )
    )

    // Active/enabled state with gradient, disabled state with gray
    val buttonBrush = if (enabled && !isLoading) {
        gradientBrush
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                AppColors.TextSecondary.copy(alpha = 0.3f),
                AppColors.TextSecondary.copy(alpha = 0.3f)
            )
        )
    }

    Surface(
        modifier = Modifier
            .scale(scale)
            .height(56.dp)
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = if (enabled && !isLoading) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(buttonBrush),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.White else AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedCreditBadge(
    creditsCount: Int,
    onClick: () -> Unit
) {
    // Track previous credits for change detection
    var previousCredits by remember { mutableStateOf(creditsCount) }
    var creditChange by remember { mutableStateOf<Int?>(null) }
    
    // Animated counter that smoothly counts from old value to new value
    val animatedCredits by animateIntAsState(
        targetValue = creditsCount,
        animationSpec = tween(
            durationMillis = when {
                kotlin.math.abs(creditsCount - previousCredits) > 100 -> 1500 // Larger changes take longer
                kotlin.math.abs(creditsCount - previousCredits) > 50 -> 1000
                else -> 800
            },
            easing = LinearEasing
        ),
        label = "creditCount"
    )
    
    // Detect credit change and show floating indicator
    LaunchedEffect(creditsCount) {
        if (creditsCount != previousCredits) {
            creditChange = creditsCount - previousCredits
            previousCredits = creditsCount
            
            // Clear change indicator after animation
            delay(2500)
            creditChange = null
        }
    }
    
    // Scale animation when credits change
    val scale by animateFloatAsState(
        targetValue = if (creditChange != null) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f
        ),
        label = "creditScale"
    )
    
    Box(
        modifier = Modifier.scale(scale)
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = if (creditChange != null && creditChange!! > 0) {
                // Green tint for credit increase
                Color(0xFF4CAF50).copy(alpha = 0.2f)
            } else if (creditChange != null && creditChange!! < 0) {
                // Red tint for credit decrease
                Color(0xFFFF5252).copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            },
            tonalElevation = 2.dp,
            border = BorderStroke(
                width = 1.dp,
                color = if (creditChange != null && creditChange!! > 0) {
                    Color(0xFF4CAF50).copy(alpha = 0.6f)
                } else if (creditChange != null && creditChange!! < 0) {
                    Color(0xFFFF5252).copy(alpha = 0.6f)
                } else {
                    AppColors.TextSecondary.copy(alpha = 0.3f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Credits",
                    tint = Color(0xFFFFD700), // Yellow star color
                    modifier = Modifier.size(18.dp)
                )
                
                // Counting animation - shows intermediate values
                Text(
                    text = animatedCredits.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        }
        
        // Floating change indicator (+X or -X)
        creditChange?.let { change ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 0 },
                    animationSpec = tween(300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it * 2 },
                    animationSpec = tween(1500, delayMillis = 500)
                ) + fadeOut(animationSpec = tween(1000, delayMillis = 1000)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (change > 0) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFFF5252)
                    },
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = if (change > 0) "+$change" else "$change",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerateHeader(
    creditsCount: Int,
    onSettingsClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "AI Studio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = "Try Veo 3, Sora 2 and more",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated credit counter badge
            AnimatedCreditBadge(
                creditsCount = creditsCount,
                onClick = onCreditsClick
            )
            // Settings icon
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, AppColors.BorderLight)
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
                        .clip(RoundedCornerShape(24.dp))
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

/**
 * Determines if a model supports text-to-video generation.
 * A model supports T2V if:
 * - It doesn't support first frame (text-only), OR
 * - It supports first frame but doesn't require it (can work with or without image)
 */
private fun isTextToVideoModel(model: AIModel): Boolean {
    return !model.supportsFirstFrame || !model.requiresFirstFrame
}

/**
 * Determines if a model supports image-to-video generation.
 * A model supports I2V if it supports first frame input.
 */
private fun isImageToVideoModel(model: AIModel): Boolean {
    return model.supportsFirstFrame
}

@Composable
private fun ModelSelector(
    models: List<AIModel>,
    selected: AIModel?,
    generationMode: GenerationMode,
    onSelected: (AIModel) -> Unit
) {
    // Filter models based on generation mode using supports_first_frame and requires_first_frame
    val filteredModels = remember(models, generationMode) {
        when (generationMode) {
            GenerationMode.TextToVideo -> {
                // Show models that support text-to-video (don't require first frame)
                models.filter { isTextToVideoModel(it) }
            }

            GenerationMode.ImageToVideo -> {
                // Show only models that support image-to-video (support first frame)
                models.filter { isImageToVideoModel(it) }
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(selected?.id) {
        if (selected != null) {
            val index = filteredModels.indexOfFirst { it.id == selected.id }
            if (index >= 0) {
                delay(50)
                listState.animateScrollToItem(index)
            }
        }
    }

    if (filteredModels.isEmpty()) {
        Text(
            text = "No models available for ${generationMode.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(filteredModels, key = { it.id }) { model ->
                val isSelected = selected?.id == model.id
                ModelCard(
                    model = model,
                    selected = isSelected,
                    onClick = { onSelected(model) }
                )
            }
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
        onClick = onClick,
        padding = PaddingValues(12.dp)
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
                    // Enable caching for logos - cache in both memory and disk
                    // Coil automatically generates cache keys from the data URL, so no need to set it explicitly
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
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
    var fullscreenImageUri by remember { mutableStateOf<Uri?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (model.supportsFirstFrame) {
            ReferenceFramePicker(
                label = "First Frame",
                required = model.requiresFirstFrame,
                uri = firstFrame,
                onPick = onPickFirst,
                onClear = onClearFirst,
                onViewFullscreen = { fullscreenImageUri = firstFrame }
            )
        }
        if (model.supportsLastFrame) {
            ReferenceFramePicker(
                label = "Last Frame",
                required = model.requiresLastFrame,
                uri = lastFrame,
                onPick = onPickLast,
                onClear = onClearLast,
                onViewFullscreen = { fullscreenImageUri = lastFrame }
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

    // Show fullscreen image dialog
    fullscreenImageUri?.let { uri ->
        FullscreenImageDialog(
            imageUri = uri,
            onDismiss = { fullscreenImageUri = null }
        )
    }
}

@Composable
private fun ReferenceFramePicker(
    label: String,
    required: Boolean,
    uri: Uri?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onViewFullscreen: () -> Unit
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

        if (uri == null) {
            // Empty state - show picker card with dashed/dotted border
            // Use Surface instead of AppCard to avoid border conflicts
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBorder(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        26.dp
                    )
                    .clickable { onPick() },
                shape = RoundedCornerShape(26.dp),
                color = AppColors.CardBackground,
                tonalElevation = 2.dp
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
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = "Add Reference Image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Tap to begin",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        } else {
            // Image selected - show preview with buttons
            // Use Surface with solid border (not dashed) for selected state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = AppColors.CardBackground,
                tonalElevation = 2.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image preview - clickable to view fullscreen
                    // Match corner radius with card to prevent flicker
                    val imageCornerRadius = remember { RoundedCornerShape(12.dp) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(imageCornerRadius)
                            .clickable { onViewFullscreen() }
                    ) {
                        key(uri.toString()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .crossfade(false) // Disable crossfade to prevent flicker
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Reference image preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp,
                                            color = AppColors.PrimaryPurple
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AppColors.CardBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = AppColors.TextSecondary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Text(
                                                text = "Failed to load image",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppColors.TextSecondary
                                            )
                                        }
                                    }
                                })
                        }

                        // Overlay hint to tap to view fullscreen (subtle, appears on top)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                tonalElevation = 2.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Tap to view fullscreen",
                                    modifier = Modifier.padding(10.dp),
                                    tint = AppColors.PrimaryPurple.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppSecondaryButton(
                            text = "Change",
                            onClick = onPick,
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )
                        AppSecondaryButton(
                            text = "Remove",
                            onClick = onClear,
                            fullWidth = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                .fillMaxWidth(),
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
private fun VideoExamplesSection(model: AIModel) {
    if (model.exampleVideoUrls.isEmpty()) return

    // Reset selected video when model changes
    var selectedVideoUrl by remember(model.id) { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Video Examples",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        // Use key() to force complete recreation of LazyRow when model changes
        // This ensures all items are properly disposed and recreated
        key(model.id) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = model.exampleVideoUrls,
                    key = { it }
                ) { videoUrl ->
                    ExampleCard(
                        videoUrl = videoUrl,
                        onClick = { selectedVideoUrl = videoUrl }
                    )
                }
            }
        }
    }

    // Show fullscreen video dialog when a video is selected
    selectedVideoUrl?.let { url ->
        FullscreenVideoDialog(
            videoUrl = url,
            onDismiss = { selectedVideoUrl = null }
        )
    }
}

@Composable
private fun ExampleCard(
    videoUrl: String,
    onClick: () -> Unit
) {
    AppElevatedCard(
        modifier = Modifier
            .width(220.dp)
            .aspectRatio(3f / 2f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)) // Clip to match card's corner radius
        ) {
            // Video thumbnail - clipped to stay inside container
            VideoThumbnail(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                jobId = null // No job ID for example videos
            )

            // Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        modifier = Modifier.padding(12.dp),
                        tint = AppColors.PrimaryPurple
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    // Stop video player when dialog is dismissed
    DisposableEffect(videoUrl) {
        onDispose {
            // Release video player when fullscreen dialog is dismissed
            VideoPlayerManager.unregisterPlayer(videoUrl)
        }
    }

    Dialog(
        onDismissRequest = {
            // Stop video player before closing
            VideoPlayerManager.unregisterPlayer(videoUrl)
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ModelVideoPlayer(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                showControls = false, // No controls anywhere in the app
                initialVolume = 1f,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                playbackEnabled = true,
                onVideoClick = null
            )
        }
    }
}

@Composable
private fun FullscreenImageDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Fullscreen image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Fullscreen reference image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = Color.White
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Failed to load image",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )
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
            // Draw border inset by half stroke width to ensure it's fully visible
            drawRoundRect(
                color = color,
                topLeft = Offset(halfStroke, halfStroke),
                size = Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx() - halfStroke),
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
        // Mock data for preview - using same structure as actual screen
        val mockModels = remember {
            listOf(
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
                    exampleVideoUrls = emptyList(),
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
                    exampleVideoUrls = emptyList(),
                    supportsAudio = false
                )
            )
        }

        var selectedModel by remember { mutableStateOf(mockModels.first()) }
        var generationMode by remember { mutableStateOf(GenerationMode.TextToVideo) }
        var showAdvanced by remember { mutableStateOf(true) }
        var promptText by remember { mutableStateOf("A cinematic video of a sunset over mountains") }
        var negativePromptText by remember { mutableStateOf("") }
        var selectedDuration by remember { mutableStateOf<Int?>(4) }
        var selectedAspectRatio by remember { mutableStateOf<String?>("16:9") }
        var enableAudio by remember { mutableStateOf(false) }
        var firstFrameUri by remember { mutableStateOf<Uri?>(null) }
        var lastFrameUri by remember { mutableStateOf<Uri?>(null) }

        // Create mock state matching GenerateScreenState structure - recomputes on state changes
        val mockState = GenerateScreenState(
            isLoading = false,
            models = mockModels,
            selectedModel = selectedModel,
            prompt = promptText,
            negativePrompt = negativePromptText,
            selectedDuration = selectedDuration,
            selectedAspectRatio = selectedAspectRatio,
            enableAudio = enableAudio,
            firstFrameUri = firstFrameUri,
            lastFrameUri = lastFrameUri,
            uploadMessage = null,
            isGenerating = false,
            errorMessage = null
        )

        // Premium dark background with subtle gradient - same as actual screen
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
            // Use the same GenerateScreenContent composable with mock state
            GenerateScreenContent(
                state = mockState,
                creditsCount = 1250,
                generationMode = generationMode,
                showAdvanced = showAdvanced,
                isSubmitting = false,
                onModeSelected = { generationMode = it },
                onModelSelected = { selectedModel = it },
                onPromptChanged = { promptText = it },
                onNegativePromptChanged = { negativePromptText = it },
                onDurationSelected = { selectedDuration = it },
                onAspectRatioSelected = { selectedAspectRatio = it },
                onAudioToggled = { enableAudio = it },
                onPickFirstFrame = { firstFrameUri = android.net.Uri.parse("content://preview") },
                onPickLastFrame = { lastFrameUri = android.net.Uri.parse("content://preview") },
                onClearFirstFrame = { firstFrameUri = null },
                onClearLastFrame = { lastFrameUri = null },
                onAdvancedToggle = { showAdvanced = !showAdvanced },
                onGenerateClick = {},
                onSettingsClick = {},
                onCreditsClick = {},
                onPricingInfoClick = {}
            )
        }
    }
}
