package com.manjul.genai.videogenerator.ui.designsystem.components.badges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors

/**
 * Status badge for required/optional labels
 */
@Composable
fun StatusBadge(
    text: String,
    isRequired: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Info chip for displaying information
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
        shape = RoundedCornerShape(50),
        color = AppColors.StatusInfoBackground,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.StatusInfo
        )
    }
}

/**
 * Filter chip for filtering options
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
        shape = RoundedCornerShape(1000.dp), // Fully rounded
        color = if (isSelected) {
            AppColors.SelectedBackground
        } else {
            AppColors.CardBackground
        },
        border = if (!isSelected) {
            BorderStroke(0.5.dp, AppColors.BorderDefault)
        } else null,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
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
 * Status badge with custom colors
 */
@Composable
fun CustomStatusBadge(
    text: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(
            0.5.dp,
            textColor.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

