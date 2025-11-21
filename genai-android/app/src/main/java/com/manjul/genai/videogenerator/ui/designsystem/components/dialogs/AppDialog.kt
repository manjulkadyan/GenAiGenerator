package com.manjul.genai.videogenerator.ui.designsystem.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system dialog component constants.
 */
private object DialogConstants {
    const val BOTTOM_SHEET_TOP_CORNER_RADIUS = 32
    const val BOTTOM_SHEET_BOTTOM_CORNER_RADIUS = 16
    const val STANDARD_DIALOG_CORNER_RADIUS = 24
    const val SCRIMMER_ALPHA = 0.7f
    const val BORDER_ALPHA = 0.3f
    const val BORDER_WIDTH = 1
    const val ELEVATION = 12
    const val PADDING = 24
    const val HORIZONTAL_PADDING = 16
    const val VERTICAL_PADDING = 32
}

/**
 * Bottom sheet style dialog component.
 *
 * Matches the bottom sheet dialog style from the reference design. Slides up
 * from the bottom of the screen with rounded top corners. Used for pricing
 * dialogs and other modal content.
 *
 * @param onDismissRequest Callback invoked when the dialog should be dismissed
 * @param title Optional title text displayed at the top of the dialog
 * @param modifier Modifier to be applied to the dialog content
 * @param content The content to be displayed inside the dialog
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppBottomSheetDialogPreview
 */
@Composable
fun AppBottomSheetDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = DialogConstants.SCRIMMER_ALPHA))
                .padding(
                    horizontal = DialogConstants.HORIZONTAL_PADDING.dp,
                    vertical = DialogConstants.VERTICAL_PADDING.dp
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = DialogConstants.BOTTOM_SHEET_TOP_CORNER_RADIUS.dp,
                    topEnd = DialogConstants.BOTTOM_SHEET_TOP_CORNER_RADIUS.dp,
                    bottomStart = DialogConstants.BOTTOM_SHEET_BOTTOM_CORNER_RADIUS.dp,
                    bottomEnd = DialogConstants.BOTTOM_SHEET_BOTTOM_CORNER_RADIUS.dp
                ),
                color = AppColors.CardBackground,
                tonalElevation = DialogConstants.ELEVATION.dp,
                border = BorderStroke(
                    DialogConstants.BORDER_WIDTH.dp,
                    AppColors.CardBorder.copy(alpha = DialogConstants.BORDER_ALPHA)
                )
            ) {
                Column(
                    modifier = Modifier.padding(DialogConstants.PADDING.dp)
                ) {
                    title?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                    content()
                }
            }
        }
    }
}

/**
 * Standard centered dialog component.
 *
 * Used for displaying modal content in the center of the screen. Features
 * rounded corners and dark theme styling.
 *
 * @param onDismissRequest Callback invoked when the dialog should be dismissed
 * @param title Optional title text displayed at the top of the dialog
 * @param modifier Modifier to be applied to the dialog content
 * @param content The content to be displayed inside the dialog
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialogPreview
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DialogConstants.STANDARD_DIALOG_CORNER_RADIUS.dp),
            color = AppColors.CardBackground,
            tonalElevation = DialogConstants.ELEVATION.dp,
            border = BorderStroke(
                DialogConstants.BORDER_WIDTH.dp,
                AppColors.CardBorder.copy(alpha = DialogConstants.BORDER_ALPHA)
            )
        ) {
            Column(
                modifier = Modifier.padding(DialogConstants.PADDING.dp)
            ) {
                title?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AppColors.TextSecondary
                            )
                        }
                    }
                }
                content()
            }
        }
    }
}

// ==================== Previews ====================

@Preview(
    name = "App Bottom Sheet Dialog",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppBottomSheetDialogPreview() {
    GenAiVideoTheme {
        var showDialog by remember { mutableStateOf(true) }
        if (showDialog) {
            AppBottomSheetDialog(
                onDismissRequest = { showDialog = false },
                title = "Pricing"
            ) {
                Text(
                    text = "Dialog content goes here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview(
    name = "App Dialog",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppDialogPreview() {
    GenAiVideoTheme {
        var showDialog by remember { mutableStateOf(true) }
        if (showDialog) {
            AppDialog(
                onDismissRequest = { showDialog = false },
                title = "Dialog Title"
            ) {
                Text(
                    text = "Dialog content goes here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

