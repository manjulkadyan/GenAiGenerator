package com.manjul.genai.videogenerator.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.FilterChip
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.InfoChip
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppElevatedCard
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppSelectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionCard
import com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPill
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Comprehensive design system showcase preview.
 *
 * Displays all design system components organized by category for easy
 * visual review and testing. Use this to verify component styling and
 * ensure consistency across the design system.
 */
@Preview(
    name = "Design System Showcase",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
fun DesignSystemShowcase() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Buttons Section
            ShowcaseSection("Buttons") {
                var text1 by remember { mutableStateOf("") }
                AppPrimaryButton(
                    text = "Primary Button",
                    onClick = {},
                    icon = Icons.Default.PlayArrow
                )
                AppPrimaryButton(
                    text = "Primary Button (No Icon)",
                    onClick = {}
                )
                AppSecondaryButton(
                    text = "Secondary Button",
                    onClick = {}
                )
                AppTextButton(
                    text = "Text Button",
                    onClick = {}
                )
            }

            // Cards Section
            ShowcaseSection("Cards") {
                AppCard {
                    Text(
                        text = "Standard Card",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "This is a standard card component.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                AppElevatedCard {
                    Text(
                        text = "Elevated Card",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "This card has higher elevation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                AppSelectionCard(
                    isSelected = true,
                    onClick = {}
                ) {
                    Text(
                        text = "Selected Card",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                }
            }

            // Selection Components Section
            ShowcaseSection("Selection Components") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionPill(
                        text = "Selected",
                        selected = true,
                        onClick = {}
                    )
                    SelectionPill(
                        text = "Unselected",
                        selected = false,
                        onClick = {}
                    )
                }
                SelectionCard(
                    title = "Selection Card",
                    description = "This is a selection card",
                    isSelected = true,
                    onClick = {}
                )
            }

            // Inputs Section
            ShowcaseSection("Inputs") {
                var text1 by remember { mutableStateOf("") }
                AppTextField(
                    value = text1,
                    onValueChange = { text1 = it },
                    placeholder = "Enter text",
                    label = "Text Field"
                )
                var text2 by remember { mutableStateOf("") }
                AppTextField(
                    value = text2,
                    onValueChange = { text2 = it },
                    placeholder = "Multi-line text",
                    maxLines = 3
                )
            }

            // Badges Section
            ShowcaseSection("Badges & Chips") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusBadge(text = "Required", isRequired = true)
                    StatusBadge(text = "Optional", isRequired = false)
                    InfoChip(text = "Info")
                    FilterChip(
                        text = "Filter",
                        isSelected = true,
                        onClick = {}
                    )
                    CustomStatusBadge(
                        text = "Custom",
                        backgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.2f),
                        textColor = AppColors.PrimaryPurple
                    )
                }
            }

            // Section Cards Section
            ShowcaseSection("Section Cards") {
                SectionCard(
                    title = "Section Title",
                    description = "Section description text",
                    required = true,
                    infoText = "Info",
                    onInfoClick = {}
                ) {
                    Text(
                        text = "Section content",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

