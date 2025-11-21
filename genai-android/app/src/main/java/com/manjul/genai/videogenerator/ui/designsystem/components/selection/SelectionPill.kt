package com.manjul.genai.videogenerator.ui.designsystem.components.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system selection pill component constants.
 */
private object SelectionPillConstants {
    const val PILL_CORNER_RADIUS = 50
    const val SELECTED_SCALE = 1.05f
    const val HORIZONTAL_PADDING = 18
    const val VERTICAL_PADDING = 12
    const val ICON_SIZE = 24
    const val ICON_SPACING = 12
    const val SUBTITLE_SPACING = 4
    const val SELECTED_BORDER_WIDTH = 1.5f
    const val UNSELECTED_BORDER_WIDTH = 1f
    const val SELECTED_TEXT_ALPHA = 0.8f
    const val UNSELECTED_TEXT_ALPHA = 0.7f
    const val ANIMATION_DURATION = 200
}

/**
 * Pill-shaped selection button component.
 *
 * Used for selecting options like AI models or filters. Features animated scale
 * on selection and purple accent color when selected. Matches the pill selector
 * style from the reference design.
 *
 * @param text The main text label for the pill
 * @param selected Whether the pill is in selected state
 * @param onClick Callback invoked when the pill is clicked
 * @param modifier Modifier to be applied to the pill
 * @param subtitle Optional subtitle text displayed below the main text
 * @param icon Optional icon displayed before the text
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.selection.SelectionPillPreview
 */
@Composable
fun SelectionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) SelectionPillConstants.SELECTED_SCALE else 1f,
        animationSpec = tween(SelectionPillConstants.ANIMATION_DURATION),
        label = "pillScale"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(SelectionPillConstants.PILL_CORNER_RADIUS))
            .clickable(onClick = onClick)
            .scale(scale),
        shape = RoundedCornerShape(SelectionPillConstants.PILL_CORNER_RADIUS),
        color = if (selected) {
            AppColors.SelectedBackground
        } else {
            AppColors.UnselectedBackground
        },
        border = if (selected) {
            BorderStroke(SelectionPillConstants.SELECTED_BORDER_WIDTH.dp, AppColors.BorderSelected)
        } else {
            BorderStroke(SelectionPillConstants.UNSELECTED_BORDER_WIDTH.dp, AppColors.UnselectedBorder)
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SelectionPillConstants.HORIZONTAL_PADDING.dp,
                vertical = SelectionPillConstants.VERTICAL_PADDING.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(SelectionPillConstants.ICON_SPACING.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(SelectionPillConstants.ICON_SIZE.dp),
                    tint = if (selected) {
                        AppColors.SelectedText
                    } else {
                        AppColors.UnselectedText
                    }
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(SelectionPillConstants.SUBTITLE_SPACING.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) {
                        AppColors.SelectedText
                    } else {
                        AppColors.UnselectedText
                    }
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            AppColors.SelectedText.copy(alpha = SelectionPillConstants.SELECTED_TEXT_ALPHA)
                        } else {
                            AppColors.UnselectedText.copy(alpha = SelectionPillConstants.UNSELECTED_TEXT_ALPHA)
                        }
                    )
                }
            }
        }
    }
}

// ==================== Previews ====================

@Preview(
    name = "Selection Pill - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun SelectionPillPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            SelectionPill(
                text = "Veo 3.1",
                subtitle = "Fast, ~4c/s",
                selected = true,
                onClick = {},
                icon = Icons.Default.PlayArrow
            )
            SelectionPill(
                text = "Sora 2",
                subtitle = "Fast, ~3c/s",
                selected = false,
                onClick = {},
                icon = Icons.Default.PlayArrow
            )
        }
    }
}

