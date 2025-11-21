package com.manjul.genai.videogenerator.ui.designsystem.components.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors

/**
 * Bottom sheet style dialog (matching reference design)
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
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp, 32.dp, 16.dp, 16.dp),
                color = AppColors.CardBackground,
                tonalElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    AppColors.CardBorder.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    title?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.Top
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
 * Standard dialog variant
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
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBackground,
            tonalElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                AppColors.CardBorder.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                title?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Top
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

