package com.manjul.genai.videogenerator.ui.designsystem.components.sections

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge

/**
 * Reusable section card component (like "AI Model", "Describe Your Video" in reference)
 * Supports title, description, required/optional badges, expandable/collapsible, and info icon
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
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = AppColors.CardBackground.copy(alpha = 0.6f),
        tonalElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            AppColors.CardBorder.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        if (required) {
                            StatusBadge(text = "Required")
                        } else if (optionalLabel != null) {
                            StatusBadge(text = optionalLabel, isRequired = false)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary.copy(alpha = 0.8f)
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
                        targetValue = if (expanded) 0f else 180f,
                        animationSpec = tween(300),
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
            .size(32.dp)
            .clickable(onClick = onClick)
    } else {
        Modifier.size(32.dp)
    }
    Surface(
        modifier = clickableModifier,
        shape = CircleShape,
        color = AppColors.SurfaceElevated.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = contentDescription,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

