package com.manjul.genai.videogenerator.ui.designsystem.components.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

/**
 * Design system text field component constants.
 */
private object TextFieldConstants {
    const val CORNER_RADIUS = 20
    const val PLACEHOLDER_ALPHA = 0.5f
    const val FOCUSED_CONTAINER_ALPHA = 0.3f
    const val UNFOCUSED_CONTAINER_ALPHA = 0.2f
}

/**
 * Outlined text field component matching the reference design.
 *
 * Features purple border when focused, supports multi-line input, and displays
 * error states. Matches the text input style from the reference design.
 *
 * @param value The current text value
 * @param onValueChange Callback invoked when the text value changes
 * @param modifier Modifier to be applied to the text field
 * @param placeholder Placeholder text displayed when the field is empty
 * @param label Optional label text displayed above the field
 * @param maxLines Maximum number of lines. Set to 1 for single-line, or higher for multi-line
 * @param isError Whether the field is in error state
 * @param errorMessage Optional error message displayed below the field
 * @param enabled Whether the field is enabled. When false, field appears disabled
 *
 * @sample com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextFieldPreview
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    maxLines: Int = 1,
    minLines: Int = 1,
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
                color = AppColors.TextSecondary.copy(alpha = TextFieldConstants.PLACEHOLDER_ALPHA)
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
            focusedContainerColor = AppColors.SurfaceDark.copy(alpha = TextFieldConstants.FOCUSED_CONTAINER_ALPHA),
            unfocusedContainerColor = AppColors.SurfaceDark.copy(alpha = TextFieldConstants.UNFOCUSED_CONTAINER_ALPHA),
            cursorColor = AppColors.BorderSelected,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            errorBorderColor = AppColors.StatusError,
            errorLabelColor = AppColors.StatusError
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = AppColors.TextPrimary
        ),
        shape = RoundedCornerShape(TextFieldConstants.CORNER_RADIUS.dp),
        minLines = minLines,
        maxLines = maxLines
    )
}

// ==================== Previews ====================

@Preview(
    name = "App Text Field - Single Line",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppTextFieldPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            var text1 by remember { mutableStateOf("") }
            AppTextField(
                value = text1,
                onValueChange = { text1 = it },
                placeholder = "Tap here to type your prompt",
                label = "Main Text Prompt"
            )
            var text2 by remember { mutableStateOf("Some text") }
            AppTextField(
                value = text2,
                onValueChange = { text2 = it },
                placeholder = "Enter text"
            )
        }
    }
}

@Preview(
    name = "App Text Field - Multi Line",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppTextFieldMultiLinePreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            var text1 by remember { mutableStateOf("") }
            AppTextField(
                value = text1,
                onValueChange = { text1 = it },
                placeholder = "Describe what you want to see in detail",
                label = "Prompt",
                maxLines = 5
            )
        }
    }
}

@Preview(
    name = "App Text Field - Error State",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppTextFieldErrorPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            var text1 by remember { mutableStateOf("") }
            AppTextField(
                value = text1,
                onValueChange = { text1 = it },
                placeholder = "Enter text",
                label = "Required Field",
                isError = true,
                errorMessage = "This field is required"
            )
            var text2 by remember { mutableStateOf("invalid") }
            AppTextField(
                value = text2,
                onValueChange = { text2 = it },
                placeholder = "Enter text",
                isError = true,
                errorMessage = "Invalid input format"
            )
        }
    }
}

@Preview(
    name = "App Text Field - Disabled",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun AppTextFieldDisabledPreview() {
    GenAiVideoTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            AppTextField(
                value = "Disabled text",
                onValueChange = {},
                placeholder = "Enter text",
                enabled = false
            )
        }
    }
}

