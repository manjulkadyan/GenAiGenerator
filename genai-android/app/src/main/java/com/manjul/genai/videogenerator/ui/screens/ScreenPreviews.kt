package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.FilterChip
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppSelectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPill
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Preview composables for main screens.
 * These use mock data and don't require ViewModels or real data sources.
 */

// ==================== Generate Screen Preview ====================

@Preview(
    name = "Generate Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun GenerateScreenPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppToolbar(
                title = "AI Studio",
                subtitle = "Try Veo 3, Sora 2 and more"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                // Mode Selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionPill(
                        text = "Text to Video",
                        selected = false,
                        onClick = {}
                    )
                    SelectionPill(
                        text = "Image to Video",
                        selected = true,
                        onClick = {}
                    )
                }
                
                // AI Model Section
                SectionCard(
                    title = "AI Model",
                    description = "Choose the AI model for video generation",
                    required = true,
                    infoText = "Learn more about AI models",
                    onInfoClick = {}
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(listOf("Veo 3.1", "Sora 2", "Wan")) { index, modelName ->
                            AppSelectionCard(
                                isSelected = index == 0,
                                onClick = {}
                            ) {
                                Text(
                                    text = modelName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Fast, ~4c/s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                }
                
                // Reference Images Section
                SectionCard(
                    title = "Reference Images",
                    description = "Select 1-3 reference images",
                    required = true
                ) {
                    AppCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.TextSecondary
                            )
                            Text(
                                text = "Add Reference Images",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "Select 1-3 images • Tap to begin",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
                
                // Main Text Prompt Section
                var promptText by remember { mutableStateOf("") }
                SectionCard(
                    title = "Main Text Prompt",
                    description = "Describe what you want to see in detail",
                    required = true
                ) {
                    AppTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = "Tap here to type your prompt",
                        maxLines = 5
                    )
                }
                
                // Generate Button
                AppPrimaryButton(
                    text = "Generate AI Video",
                    onClick = {},
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ==================== Models Screen Preview ====================

@Preview(
    name = "Models Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun ModelsScreenPreview3() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(
                title = "AI Models",
                subtitle = "Choose your video generation model"
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(listOf("Veo 3.1", "Sora 2", "Wan")) { index, modelName ->
                    AppCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = modelName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = AppColors.TextPrimary
                                )
                                CustomStatusBadge(
                                    text = "Pro",
                                    backgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.2f),
                                    textColor = AppColors.PrimaryPurple
                                )
                            }
                            Text(
                                text = "Fast, ~4c/s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== History Screen Preview ====================

@Preview(
    name = "History Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun HistoryScreenPreview2() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(
                title = "History",
                subtitle = "Your video generations"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listOf("All", "Completed", "Processing", "Failed")) { filter ->
                        FilterChip(
                            text = filter,
                            isSelected = filter == "All",
                            onClick = {}
                        )
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed((0..2).toList()) { index, _ ->
                    AppCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Video Generation ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.TextPrimary
                                )
                                StatusBadge(
                                    text = "Completed",
                                    isRequired = false
                                )
                            }
                            Text(
                                text = "A cinematic video of a sunset over mountains",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Profile Screen Preview ====================

@Preview(
    name = "Profile Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun ProfileScreenPreview2() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppToolbar(
                title = "Profile",
                subtitle = "Account",
                showBorder = false
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Card
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "JD",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = AppColors.TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "John Doe",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "john.doe@example.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Videos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary
                                    )
                                    Text(
                                        text = "12",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.TextPrimary
                                    )
                                }
                            }
                            AppCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Credits",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.PrimaryPurple
                                    )
                                    Text(
                                        text = "1,250",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buy Credits",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ==================== Buy Credits Screen Preview ====================

@Preview(
    name = "Buy Credits Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun BuyCreditsScreenPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppToolbar(
                title = "Credits",
                showBackButton = true,
                onBackClick = {},
                showBorder = true
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Current Balance Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "1,250",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.PrimaryPurple
                        )
                        Text(
                            text = "credits available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                
                // Packages
                Text(
                    text = "Buy Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppCard(
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Starter",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "100",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryPurple
                            )
                            Text(
                                text = "$9.99",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    AppCard(
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomStatusBadge(
                                text = "Popular",
                                backgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.2f),
                                textColor = AppColors.PrimaryPurple
                            )
                            Text(
                                text = "Creator",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "500",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryPurple
                            )
                            Text(
                                text = "$39.99",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Results Screen Preview ====================

@Preview(
    name = "Results Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun ResultsScreenPreview2() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = AppColors.TextPrimary
                )
                CustomStatusBadge(
                    text = "completed",
                    backgroundColor = AppColors.StatusSuccessBackground,
                    textColor = AppColors.StatusSuccess
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Video Player Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = AppColors.TextSecondary
                        )
                    }
                }
                
                // Prompt Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "A cinematic video of a sunset over mountains with dramatic clouds",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                
                // Details Card
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Model",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "Veo 3.1",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "4s",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextPrimary
                            )
                        }
                    }
                }
                
                // Action Buttons
                AppPrimaryButton(
                    text = "Download Video",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== Models Screen Preview ====================

@Preview(
    name = "Models Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun ModelsScreenPreview2() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppToolbar(
                title = "AI Video Models",
                subtitle = "3 models available",
                showBorder = false
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(listOf("Veo 3.1", "Sora 2", "Wan")) { index, modelName ->
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modelName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    color = AppColors.TextPrimary
                                )
                                if (index == 0) {
                                    CustomStatusBadge(
                                        text = "Selected",
                                        backgroundColor = AppColors.SelectedBackground,
                                        textColor = AppColors.SelectedText
                                    )
                                }
                            }
                            Text(
                                text = "Fast video generation model with high quality output",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary,
                                maxLines = 2
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Price",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Text(
                                        text = "4",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.PrimaryPurple
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Duration",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Text(
                                        text = "4s, 8s",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.PrimaryPurple
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Generating Screen Preview ====================

@Preview(
    name = "Generating Screen Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun GeneratingScreenPreview() {
    GenAiVideoTheme {
        GeneratingScreen(
            statusMessage = "Uploading first frame...",
            onCancel = {},
            onRetry = null
        )
    }
}

@Preview(
    name = "Generating Screen Error Preview",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun GeneratingScreenErrorPreview() {
    GenAiVideoTheme {
        GeneratingScreen(
            errorMessage = "Failed to generate video. Please try again.",
            onCancel = {},
            onRetry = {}
        )
    }
}

