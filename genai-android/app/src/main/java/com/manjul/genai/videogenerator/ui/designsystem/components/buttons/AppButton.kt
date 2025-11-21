package com.manjul.genai.videogenerator.ui.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system button component constants.
 */
private object ButtonConstants {
    const val BUTTON_HEIGHT = 56
    const val PRIMARY_CORNER_RADIUS = 28
    const val SECONDARY_CORNER_RADIUS = 16
    const val TEXT_BUTTON_CORNER_RADIUS = 12
    const val HORIZONTAL_PADDING = 24
    const val VERTICAL_PADDING = 16
    const val ICON_SIZE_PRIMARY = 24
    const val ICON_SIZE_SECONDARY = 16
    const val ICON_SIZE_TEXT = 18
    const val LOADING_INDICATOR_SIZE_PRIMARY = 22
    const val LOADING_INDICATOR_SIZE_SECONDARY = 16
    const val LOADING_STROKE_WIDTH_PRIMARY = 2.5f
    const val LOADING_STROKE_WIDTH_SECONDARY = 2f
    const val ICON_SPACING_PRIMARY = 10
    const val ICON_SPACING_SECONDARY = 8
    const val LOADING_TEXT_SPACING = 12
}

/**
 * Primary button component with white background and black text.
 *
 * Matches the "Continue" button style from the reference design. This is the main
 * call-to-action button used throughout the app.
 *
 * @param text The button label text
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled. When false, button appears disabled
 * @param isLoading Whether the button is in loading state. Shows a progress indicator
 * @param icon Optional icon to display before the text
 * @param fullWidth Whether the button should fill the available width. Defaults to true
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButtonPreview
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) {
        modifier
            .fillMaxWidth()
            .height(ButtonConstants.BUTTON_HEIGHT.dp)
    } else {
        modifier.height(ButtonConstants.BUTTON_HEIGHT.dp)
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = buttonModifier,
        shape = RoundedCornerShape(ButtonConstants.PRIMARY_CORNER_RADIUS.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonPrimary,
            contentColor = AppColors.ButtonPrimaryText,
            disabledContainerColor = AppColors.ButtonDisabled,
            disabledContentColor = AppColors.ButtonDisabledText
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(
            horizontal = ButtonConstants.HORIZONTAL_PADDING.dp,
            vertical = ButtonConstants.VERTICAL_PADDING.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonConstants.LOADING_INDICATOR_SIZE_PRIMARY.dp),
                color = AppColors.ButtonPrimaryText,
                strokeWidth = ButtonConstants.LOADING_STROKE_WIDTH_PRIMARY.dp
            )
            Spacer(modifier = Modifier.width(ButtonConstants.LOADING_TEXT_SPACING.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.ButtonPrimaryText
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonConstants.ICON_SIZE_PRIMARY.dp),
                        tint = AppColors.ButtonPrimaryText
                    )
                    Spacer(modifier = Modifier.width(ButtonConstants.ICON_SPACING_PRIMARY.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.ButtonPrimaryText
                )
            }
        }
    }
}

/**
 * Secondary button component with outlined style.
 *
 * Used for secondary actions that don't require primary emphasis. Features
 * a transparent background with a border outline.
 *
 * @param text The button label text
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled. When false, button appears disabled
 * @param isLoading Whether the button is in loading state. Shows a progress indicator
 * @param icon Optional icon to display before the text
 * @param fullWidth Whether the button should fill the available width. Defaults to true
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButtonPreview
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) {
        modifier
            .fillMaxWidth()
            .height(ButtonConstants.BUTTON_HEIGHT.dp)
    } else {
        modifier.height(ButtonConstants.BUTTON_HEIGHT.dp)
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = buttonModifier,
        shape = RoundedCornerShape(ButtonConstants.SECONDARY_CORNER_RADIUS.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.ButtonSecondaryText,
            disabledContentColor = AppColors.ButtonDisabledText
        ),
        border = BorderStroke(2.dp, AppColors.BorderDefault),
        contentPadding = PaddingValues(
            horizontal = ButtonConstants.HORIZONTAL_PADDING.dp,
            vertical = ButtonConstants.VERTICAL_PADDING.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonConstants.LOADING_INDICATOR_SIZE_SECONDARY.dp),
                strokeWidth = ButtonConstants.LOADING_STROKE_WIDTH_SECONDARY.dp,
                color = AppColors.ButtonSecondaryText
            )
            Spacer(modifier = Modifier.width(ButtonConstants.ICON_SPACING_SECONDARY.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonConstants.ICON_SIZE_SECONDARY.dp),
                        tint = AppColors.ButtonSecondaryText
                    )
                    Spacer(modifier = Modifier.width(ButtonConstants.ICON_SPACING_SECONDARY.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Text button component with minimal style.
 *
 * Used for tertiary actions or inline links. Has no background, only text and optional icon.
 *
 * @param text The button label text
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled. When false, button appears disabled
 * @param icon Optional icon to display before the text
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButtonPreview
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(ButtonConstants.TEXT_BUTTON_CORNER_RADIUS.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = AppColors.TextPrimary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonConstants.ICON_SIZE_TEXT.dp)
                )
                Spacer(modifier = Modifier.width(ButtonConstants.ICON_SPACING_SECONDARY.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== Previews ====================

@Preview(
    name = "Primary Button - Enabled",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppPrimaryButtonPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppPrimaryButton(
                text = "Continue",
                onClick = {},
                icon = Icons.Default.PlayArrow
            )
            AppPrimaryButton(
                text = "Generate AI Video",
                onClick = {}
            )
            AppPrimaryButton(
                text = "Continue",
                onClick = {},
                fullWidth = false
            )
        }
    }
}

@Preview(
    name = "Primary Button - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppPrimaryButtonStatesPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppPrimaryButton(
                text = "Enabled",
                onClick = {},
                enabled = true
            )
            AppPrimaryButton(
                text = "Disabled",
                onClick = {},
                enabled = false
            )
            AppPrimaryButton(
                text = "Loading",
                onClick = {},
                isLoading = true
            )
        }
    }
}

@Preview(
    name = "Secondary Button - Enabled",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppSecondaryButtonPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSecondaryButton(
                text = "Share",
                onClick = {},
                icon = Icons.Default.PlayArrow
            )
            AppSecondaryButton(
                text = "Regenerate",
                onClick = {}
            )
            AppSecondaryButton(
                text = "Cancel",
                onClick = {},
                fullWidth = false
            )
        }
    }
}

@Preview(
    name = "Secondary Button - States",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppSecondaryButtonStatesPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSecondaryButton(
                text = "Enabled",
                onClick = {},
                enabled = true
            )
            AppSecondaryButton(
                text = "Disabled",
                onClick = {},
                enabled = false
            )
            AppSecondaryButton(
                text = "Loading",
                onClick = {},
                isLoading = true
            )
        }
    }
}

@Preview(
    name = "Text Button",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppTextButtonPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppTextButton(
                text = "Learn More",
                onClick = {}
            )
            AppTextButton(
                text = "Skip",
                onClick = {},
                icon = Icons.Default.PlayArrow
            )
            AppTextButton(
                text = "Disabled",
                onClick = {},
                enabled = false
            )
        }
    }
}

