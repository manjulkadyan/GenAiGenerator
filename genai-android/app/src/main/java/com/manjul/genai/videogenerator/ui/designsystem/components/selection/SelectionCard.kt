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
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors

/**
 * Card-based selection (like "What are you here to create?" options in reference)
 * Radio button style selection with icon, title, and description
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
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = tween(200),
        label = "selectionCardScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            AppColors.SelectedBackground.copy(alpha = 0.15f)
        } else {
            AppColors.CardBackground
        },
        border = if (isSelected) {
            BorderStroke(2.dp, AppColors.BorderSelected)
        } else {
            BorderStroke(1.dp, AppColors.CardBorder)
        },
        tonalElevation = if (isSelected) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon or emoji
            icon?.let {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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

            // Radio button indicator
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (isSelected) {
                    AppColors.SelectedBackground
                } else {
                    Color.Transparent
                },
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(2.dp, AppColors.UnselectedBorder)
                }
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = CircleShape,
                            color = AppColors.SelectedText
                        ) {}
                    }
                }
            }
        }
    }
}

