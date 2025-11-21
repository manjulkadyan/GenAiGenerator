package com.manjul.genai.videogenerator.ui.designsystem.components.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system selection card component constants.
 */
private object SelectionCardConstants {
    const val CARD_CORNER_RADIUS = 20
    const val SELECTED_SCALE = 1.01f
    const val PADDING = 20
    const val ICON_SIZE = 48
    const val RADIO_SIZE = 24
    const val RADIO_INNER_SIZE = 12
    const val ICON_SPACING = 16
    const val CONTENT_SPACING = 6
    const val SELECTED_BORDER_WIDTH = 2
    const val UNSELECTED_BORDER_WIDTH = 1
    const val SELECTED_ELEVATION = 4
    const val UNSELECTED_ELEVATION = 2
    const val SELECTED_ALPHA = 0.15f
    const val RADIO_BORDER_WIDTH = 2
    const val ANIMATION_DURATION = 200
}

/**
 * Card-based selection component with radio button indicator.
 *
 * Used for selecting options like "What are you here to create?" from the reference design.
 * Features an icon, title, description, and animated radio button indicator.
 *
 * @param title The main title text
 * @param description The description text below the title
 * @param isSelected Whether the card is in selected state
 * @param onClick Callback invoked when the card is clicked
 * @param modifier Modifier to be applied to the card
 * @param icon Optional composable icon displayed on the left side
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionCardPreview
 */
@Composable
fun SelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) SelectionCardConstants.SELECTED_SCALE else 1f,
        animationSpec = tween(SelectionCardConstants.ANIMATION_DURATION),
        label = "selectionCardScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(SelectionCardConstants.CARD_CORNER_RADIUS.dp),
        color = if (isSelected) {
            AppColors.SelectedBackground.copy(alpha = SelectionCardConstants.SELECTED_ALPHA)
        } else {
            AppColors.CardBackground
        },
        border = if (isSelected) {
            BorderStroke(SelectionCardConstants.SELECTED_BORDER_WIDTH.dp, AppColors.BorderSelected)
        } else {
            BorderStroke(SelectionCardConstants.UNSELECTED_BORDER_WIDTH.dp, AppColors.CardBorder)
        },
        tonalElevation = if (isSelected) {
            SelectionCardConstants.SELECTED_ELEVATION.dp
        } else {
            SelectionCardConstants.UNSELECTED_ELEVATION.dp
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SelectionCardConstants.PADDING.dp),
            horizontalArrangement = Arrangement.spacedBy(SelectionCardConstants.ICON_SPACING.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Box(
                    modifier = Modifier.size(SelectionCardConstants.ICON_SIZE.dp),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SelectionCardConstants.CONTENT_SPACING.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }

            Surface(
                modifier = Modifier.size(SelectionCardConstants.RADIO_SIZE.dp),
                shape = CircleShape,
                color = if (isSelected) {
                    AppColors.SelectedBackground
                } else {
                    Color.Transparent
                },
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(SelectionCardConstants.RADIO_BORDER_WIDTH.dp, AppColors.UnselectedBorder)
                }
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(SelectionCardConstants.RADIO_INNER_SIZE.dp),
                            shape = CircleShape,
                            color = AppColors.SelectedText
                        ) {}
                    }
                }
            }
        }
    }
}

// ==================== Previews ====================

@Preview(
    name = "Selection Card - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun SelectionCardPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                title = "Ultra-Realistic AI Videos",
                description = "Veo 3 and Sora 2, smooth consistency",
                isSelected = false,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AppColors.TextPrimary
                    )
                }
            )
            SelectionCard(
                title = "Text & Image Video",
                description = "Prompts or photos to cinematic results instantly",
                isSelected = true,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AppColors.PrimaryPurple
                    )
                }
            )
        }
    }
}

