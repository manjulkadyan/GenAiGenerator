package com.manjul.genai.videogenerator.ui.designsystem.components.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors

/**
 * Pill-shaped selection button (like AI Model selector in reference)
 * Selected state: Purple background with white text
 * Unselected state: Dark gray background with light gray text
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
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "pillScale"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .scale(scale),
        shape = RoundedCornerShape(50),
        color = if (selected) {
            AppColors.SelectedBackground
        } else {
            AppColors.UnselectedBackground
        },
        border = if (selected) {
            BorderStroke(1.5.dp, AppColors.BorderSelected)
        } else {
            BorderStroke(1.dp, AppColors.UnselectedBorder)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) {
                        AppColors.SelectedText
                    } else {
                        AppColors.UnselectedText
                    }
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            AppColors.SelectedText.copy(alpha = 0.8f)
                        } else {
                            AppColors.UnselectedText.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

