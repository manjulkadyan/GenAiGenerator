package com.manjul.genai.videogenerator.ui.designsystem.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors

/**
 * Outlined text field matching reference design
 * Supports multi-line input and error states
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    maxLines: Int = 1,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = label?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary.copy(alpha = 0.5f)
            )
        },
        isError = isError,
        supportingText = errorMessage?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.StatusError
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.BorderSelected,
            unfocusedBorderColor = AppColors.BorderLight,
            focusedContainerColor = AppColors.SurfaceDark.copy(alpha = 0.3f),
            unfocusedContainerColor = AppColors.SurfaceDark.copy(alpha = 0.2f),
            cursorColor = AppColors.BorderSelected,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            errorBorderColor = AppColors.StatusError,
            errorLabelColor = AppColors.StatusError
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = AppColors.TextPrimary
        ),
        shape = RoundedCornerShape(20.dp),
        maxLines = maxLines
    )
}

