package com.manjul.genai.videogenerator.ui.designsystem.components.badges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system badge component constants.
 */
private object BadgeConstants {
    const val BADGE_CORNER_RADIUS = 8
    const val CHIP_CORNER_RADIUS = 50
    const val FILTER_CHIP_CORNER_RADIUS = 1000
    const val STATUS_HORIZONTAL_PADDING = 10
    const val STATUS_VERTICAL_PADDING = 4
    const val CHIP_HORIZONTAL_PADDING = 12
    const val CHIP_VERTICAL_PADDING = 6
    const val FILTER_CHIP_HORIZONTAL_PADDING = 20
    const val FILTER_CHIP_VERTICAL_PADDING = 10
    const val CUSTOM_BADGE_HORIZONTAL_PADDING = 8
    const val CUSTOM_BADGE_VERTICAL_PADDING = 2
    const val ICON_SIZE = 16
    const val ICON_SPACING = 8
    const val BORDER_WIDTH = 0.5f
    const val FILTER_CHIP_BORDER_WIDTH = 0.5f
    const val CUSTOM_BADGE_BORDER_ALPHA = 0.2f
    const val CHIP_ELEVATION = 2
    const val FILTER_CHIP_SELECTED_ELEVATION = 2
}

/**
 * Status badge component for displaying required/optional labels.
 *
 * Used in section headers to indicate whether a field is required or optional.
 * Matches the "Required" badge style from the reference design.
 *
 * @param text The badge text (e.g., "Required", "Optional")
 * @param isRequired Whether the badge represents a required field. When true,
 *                   uses red styling. When false, uses blue styling.
 * @param modifier Modifier to be applied to the badge
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadgePreview
 */
@Composable
fun StatusBadge(
    text: String,
    isRequired: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(BadgeConstants.BADGE_CORNER_RADIUS.dp),
        color = if (isRequired) {
            AppColors.BadgeRequired
        } else {
            AppColors.BadgeOptional
        },
        contentColor = if (isRequired) {
            AppColors.BadgeRequiredText
        } else {
            AppColors.BadgeOptionalText
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = BadgeConstants.STATUS_HORIZONTAL_PADDING.dp,
                vertical = BadgeConstants.STATUS_VERTICAL_PADDING.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Info chip component for displaying informational content.
 *
 * Used to show additional information or tips. Can be clickable to show more details.
 *
 * @param text The chip text content
 * @param modifier Modifier to be applied to the chip
 * @param onClick Optional click handler. If provided, chip becomes clickable
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.badges.InfoChipPreview
 */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val chipModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = chipModifier,
        shape = RoundedCornerShape(BadgeConstants.CHIP_CORNER_RADIUS),
        color = AppColors.StatusInfoBackground,
        tonalElevation = BadgeConstants.CHIP_ELEVATION.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = BadgeConstants.CHIP_HORIZONTAL_PADDING.dp,
                vertical = BadgeConstants.CHIP_VERTICAL_PADDING.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.StatusInfo
        )
    }
}

/**
 * Filter chip component for filtering options.
 *
 * Used in filter bars to allow users to select different filter options.
 * Shows selected state with purple background and bold text.
 *
 * @param text The chip text label
 * @param isSelected Whether the chip is in selected state
 * @param onClick Callback invoked when the chip is clicked
 * @param modifier Modifier to be applied to the chip
 * @param icon Optional icon displayed before the text
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.badges.FilterChipPreview
 */
@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(BadgeConstants.FILTER_CHIP_CORNER_RADIUS.dp),
        color = if (isSelected) {
            AppColors.SelectedBackground
        } else {
            AppColors.CardBackground
        },
        border = if (!isSelected) {
            BorderStroke(BadgeConstants.FILTER_CHIP_BORDER_WIDTH.dp, AppColors.BorderDefault)
        } else null,
        tonalElevation = if (isSelected) {
            BadgeConstants.FILTER_CHIP_SELECTED_ELEVATION.dp
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = BadgeConstants.FILTER_CHIP_HORIZONTAL_PADDING.dp,
                vertical = BadgeConstants.FILTER_CHIP_VERTICAL_PADDING.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(BadgeConstants.ICON_SPACING.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(BadgeConstants.ICON_SIZE.dp),
                    tint = if (isSelected) {
                        AppColors.SelectedText
                    } else {
                        AppColors.UnselectedText
                    }
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    AppColors.SelectedText
                } else {
                    AppColors.UnselectedText
                },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Status badge component with customizable colors.
 *
 * Used when standard badge colors don't fit the use case. Allows full
 * control over background and text colors.
 *
 * @param text The badge text content
 * @param backgroundColor The background color of the badge
 * @param textColor The text color of the badge
 * @param modifier Modifier to be applied to the badge
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadgePreview
 */
@Composable
fun CustomStatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(BadgeConstants.BADGE_CORNER_RADIUS.dp),
        color = backgroundColor,
        border = BorderStroke(
            BadgeConstants.BORDER_WIDTH.dp,
            textColor.copy(alpha = BadgeConstants.CUSTOM_BADGE_BORDER_ALPHA)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = BadgeConstants.CUSTOM_BADGE_HORIZONTAL_PADDING.dp,
                vertical = BadgeConstants.CUSTOM_BADGE_VERTICAL_PADDING.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ==================== Previews ====================

@Preview(
    name = "Status Badge",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun StatusBadgePreview() {
    GenAiVideoTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusBadge(text = "Required", isRequired = true)
            StatusBadge(text = "Optional", isRequired = false)
        }
    }
}

@Preview(
    name = "Info Chip",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun InfoChipPreview() {
    GenAiVideoTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoChip(text = "Info")
            InfoChip(text = "Clickable", onClick = {})
        }
    }
}

@Preview(
    name = "Filter Chip - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun FilterChipPreview() {
    GenAiVideoTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                text = "All",
                isSelected = true,
                onClick = {},
                icon = Icons.Default.FilterList
            )
            FilterChip(
                text = "Ads",
                isSelected = false,
                onClick = {},
                icon = Icons.Default.FilterList
            )
            FilterChip(
                text = "Family",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Custom Status Badge",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun CustomStatusBadgePreview() {
    GenAiVideoTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomStatusBadge(
                text = "Popular",
                backgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.2f),
                textColor = AppColors.PrimaryPurple
            )
            CustomStatusBadge(
                text = "New",
                backgroundColor = AppColors.StatusSuccess.copy(alpha = 0.2f),
                textColor = AppColors.StatusSuccess
            )
        }
    }
}

