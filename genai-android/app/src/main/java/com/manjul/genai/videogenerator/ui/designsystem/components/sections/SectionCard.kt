package com.manjul.genai.videogenerator.ui.designsystem.components.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system section card component constants.
 */
private object SectionCardConstants {
    const val CARD_CORNER_RADIUS = 24
    const val PADDING = 20
    const val CONTENT_SPACING = 16
    const val HEADER_SPACING = 12
    const val TITLE_BADGE_SPACING = 10
    const val TITLE_DESCRIPTION_SPACING = 4
    const val BORDER_WIDTH = 1
    const val ELEVATION = 2
    const val BACKGROUND_ALPHA = 0.6f
    const val BORDER_ALPHA = 0.3f
    const val DESCRIPTION_ALPHA = 0.8f
    const val INFO_ICON_SIZE = 32
    const val INFO_ICON_INNER_SIZE = 18
    const val INFO_ICON_BACKGROUND_ALPHA = 0.5f
    const val INFO_ICON_ELEVATION = 1
    const val EXPAND_ANIMATION_DURATION = 300
    const val EXPANDED_ROTATION = 0f
    const val COLLAPSED_ROTATION = 180f
}

/**
 * Reusable section card component for organizing form sections.
 *
 * Matches the section card style from the reference design (e.g., "AI Model",
 * "Describe Your Video"). Features title, description, required/optional badges,
 * optional info icon, and expandable/collapsible functionality.
 *
 * @param title The section title text
 * @param description The section description text
 * @param required Whether the section is required. When true, shows "Required" badge
 * @param modifier Modifier to be applied to the section card
 * @param infoText Optional info text for the info icon tooltip
 * @param onInfoClick Optional callback invoked when the info icon is clicked
 * @param optionalLabel Optional label text to display instead of "Optional" badge
 * @param onHeaderClick Optional callback invoked when the header is clicked
 * @param expandable Whether the section can be expanded/collapsed
 * @param expanded Whether the section is currently expanded (only relevant if expandable is true)
 * @param content The content to be displayed inside the section card
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCardPreview
 */
@Composable
fun SectionCard(
    title: String,
    description: String,
    required: Boolean,
    modifier: Modifier = Modifier,
    infoText: String? = null,
    onInfoClick: (() -> Unit)? = null,
    optionalLabel: String? = null,
    onHeaderClick: (() -> Unit)? = null,
    expandable: Boolean = false,
    expanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(SectionCardConstants.CARD_CORNER_RADIUS.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = AppColors.CardBackground.copy(alpha = SectionCardConstants.BACKGROUND_ALPHA),
        tonalElevation = SectionCardConstants.ELEVATION.dp,
        border = BorderStroke(
            SectionCardConstants.BORDER_WIDTH.dp,
            AppColors.CardBorder.copy(alpha = SectionCardConstants.BORDER_ALPHA)
        )
    ) {
        Column(
            modifier = Modifier.padding(SectionCardConstants.PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(SectionCardConstants.CONTENT_SPACING.dp)
        ) {
            val headerModifier = if (onHeaderClick != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick)
            } else {
                Modifier.fillMaxWidth()
            }

            Row(
                modifier = headerModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SectionCardConstants.HEADER_SPACING.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SectionCardConstants.TITLE_BADGE_SPACING.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        if (required) {
                            StatusBadge(text = "Required")
                        } else if (optionalLabel != null) {
                            StatusBadge(text = optionalLabel, isRequired = false)
                        }
                    }
                    Spacer(modifier = Modifier.height(SectionCardConstants.TITLE_DESCRIPTION_SPACING.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary.copy(alpha = SectionCardConstants.DESCRIPTION_ALPHA)
                    )
                }
                infoText?.let {
                    InfoIcon(
                        onClick = onInfoClick,
                        contentDescription = it
                    )
                }
                if (expandable) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) {
                            SectionCardConstants.EXPANDED_ROTATION
                        } else {
                            SectionCardConstants.COLLAPSED_ROTATION
                        },
                        animationSpec = tween(SectionCardConstants.EXPAND_ANIMATION_DURATION),
                        label = "arrowRotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            if (!expandable || expanded) {
                content()
            }
        }
    }
}

@Composable
private fun InfoIcon(
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .size(SectionCardConstants.INFO_ICON_SIZE.dp)
            .clickable(onClick = onClick)
    } else {
        Modifier.size(SectionCardConstants.INFO_ICON_SIZE.dp)
    }
    Surface(
        modifier = clickableModifier,
        shape = CircleShape,
        color = AppColors.SurfaceElevated.copy(alpha = SectionCardConstants.INFO_ICON_BACKGROUND_ALPHA),
        tonalElevation = SectionCardConstants.INFO_ICON_ELEVATION.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = contentDescription,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(SectionCardConstants.INFO_ICON_INNER_SIZE.dp)
            )
        }
    }
}

// ==================== Previews ====================

@Preview(
    name = "Section Card - Required",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun SectionCardPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = "AI Model",
                description = "Choose the AI model for video generation",
                required = true,
                infoText = "Learn more about AI models",
                onInfoClick = {}
            ) {
                Text(
                    text = "Content goes here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
            SectionCard(
                title = "Reference Images",
                description = "Select 1-3 reference images",
                required = true
            ) {
                Text(
                    text = "Image selection content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Preview(
    name = "Section Card - Optional",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun SectionCardOptionalPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = "Advanced Settings",
                description = "Adjust Aspect Ratio and Duration settings",
                required = false,
                optionalLabel = "Optional"
            ) {
                Text(
                    text = "Advanced settings content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Preview(
    name = "Section Card - Expandable",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun SectionCardExpandablePreview() {
    GenAiVideoTheme {
        var expanded by remember { mutableStateOf(true) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = "Advanced Settings",
                description = "Adjust Aspect Ratio and Duration settings",
                required = false,
                optionalLabel = "Optional",
                expandable = true,
                expanded = expanded,
                onHeaderClick = { expanded = !expanded }
            ) {
                Text(
                    text = "This content can be expanded/collapsed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

