package com.manjul.genai.videogenerator.ui.designsystem.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system card component constants.
 */
private object CardConstants {
    const val CARD_CORNER_RADIUS = 24
    const val DEFAULT_PADDING = 20
    const val DEFAULT_ELEVATION = 2
    const val ELEVATED_ELEVATION = 8
    const val BORDER_WIDTH = 1
    const val SELECTED_BORDER_WIDTH = 2
    const val SELECTED_SCALE = 1.02f
    const val ANIMATION_DURATION = 200
}

/**
 * Standard card component with rounded corners and dark background.
 *
 * Matches the card style from the reference design. Used for displaying
 * content in a contained, elevated surface.
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler. If provided, card becomes clickable
 * @param content The content to be displayed inside the card
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCardPreview
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(CardConstants.CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CardConstants.DEFAULT_ELEVATION.dp
        ),
        border = BorderStroke(CardConstants.BORDER_WIDTH.dp, AppColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(CardConstants.DEFAULT_PADDING.dp),
            content = content
        )
    }
}

/**
 * Elevated card variant with higher elevation and lighter background.
 *
 * Used when a card needs more visual emphasis or appears above other content.
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler. If provided, card becomes clickable
 * @param content The content to be displayed inside the card
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppElevatedCardPreview
 */
@Composable
fun AppElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(CardConstants.CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackgroundElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CardConstants.ELEVATED_ELEVATION.dp
        ),
        border = BorderStroke(CardConstants.BORDER_WIDTH.dp, AppColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(CardConstants.DEFAULT_PADDING.dp),
            content = content
        )
    }
}

/**
 * Selection card component with animated selection state.
 *
 * Used for selectable items like AI models. Shows a purple border and slight
 * scale animation when selected. Matches the model selection cards from the reference design.
 *
 * @param modifier Modifier to be applied to the card
 * @param isSelected Whether the card is in selected state
 * @param onClick Callback invoked when the card is clicked
 * @param content The content to be displayed inside the card
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppSelectionCardPreview
 */
@Composable
fun AppSelectionCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    padding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) CardConstants.SELECTED_SCALE else 1f,
        animationSpec = tween(CardConstants.ANIMATION_DURATION),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CardConstants.CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AppColors.SelectedBackground.copy(alpha = 0.1f)
            } else {
                AppColors.CardBackground
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) {
                CardConstants.ELEVATED_ELEVATION.dp
            } else {
                CardConstants.DEFAULT_ELEVATION.dp
            }
        ),
        border = if (isSelected) {
            BorderStroke(CardConstants.SELECTED_BORDER_WIDTH.dp, AppColors.BorderSelected)
        } else {
            BorderStroke(CardConstants.BORDER_WIDTH.dp, AppColors.CardBorder)
        }
    ) {
        Column(
            modifier = if (padding!=null) Modifier.padding(paddingValues = padding) else Modifier.padding(CardConstants.DEFAULT_PADDING.dp),
            content = content
        )
    }
}

/**
 * Card component with customizable padding.
 *
 * Similar to [AppCard] but allows custom padding values for different use cases.
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler. If provided, card becomes clickable
 * @param padding Custom padding values for the card content. Defaults to 20dp
 * @param content The content to be displayed inside the card
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCardWithPaddingPreview
 */
@Composable
fun AppCardWithPadding(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(CardConstants.DEFAULT_PADDING.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(CardConstants.CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CardConstants.DEFAULT_ELEVATION.dp
        ),
        border = BorderStroke(CardConstants.BORDER_WIDTH.dp, AppColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}

// ==================== Previews ====================

@Preview(
    name = "App Card",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppCardPreview() {
    GenAiVideoTheme {
        AppCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Standard Card",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Text(
                text = "This is a standard card with default padding and styling.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(
    name = "App Card - Clickable",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppCardClickablePreview() {
    GenAiVideoTheme {
        AppCard(
            modifier = Modifier.padding(16.dp),
            onClick = {}
        ) {
            Text(
                text = "Clickable Card",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Text(
                text = "This card can be clicked.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(
    name = "App Elevated Card",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppElevatedCardPreview() {
    GenAiVideoTheme {
        AppElevatedCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Elevated Card",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Text(
                text = "This card has higher elevation for emphasis.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(
    name = "App Selection Card - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppSelectionCardPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            AppSelectionCard(
                isSelected = false,
                onClick = {}
            ) {
                Text(
                    text = "Unselected Card",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
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
                Text(
                    text = "This card is selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(
    name = "App Card With Padding",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppCardWithPaddingPreview() {
    GenAiVideoTheme {
        AppCardWithPadding(
            modifier = Modifier.padding(16.dp),
            padding = PaddingValues(32.dp)
        ) {
            Text(
                text = "Card with Custom Padding",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Text(
                text = "This card has custom padding values.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

