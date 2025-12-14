package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FeedbackScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Form state
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    
    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("Feedback")
    }
    
    // Get device info
    val deviceModel = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
    val osVersion = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    // Get user info
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val userEmail = auth.currentUser?.email ?: ""
    
    fun submitFeedback() {
        if (feedbackText.isBlank()) {
            Toast.makeText(context, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSubmitting = true
        
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                
                val feedbackData = hashMapOf(
                    "userId" to userId,
                    "email" to userEmail,
                    "message" to feedbackText.trim(),
                    "deviceModel" to deviceModel,
                    "appVersion" to appVersion,
                    "osVersion" to osVersion,
                    "timestamp" to Timestamp.now()
                )
                
                firestore.collection("feedback")
                    .add(feedbackData)
                    .await()
                
                Log.d("FeedbackScreen", "Feedback submitted successfully")
                isSubmitting = false
                isSubmitted = true
                
                // Track analytics
                AnalyticsManager.log("Feedback submitted")
                
            } catch (e: Exception) {
                Log.e("FeedbackScreen", "Failed to submit feedback", e)
                isSubmitting = false
                Toast.makeText(context, "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars) // Handle 3-button navigation
    ) {
        // Header with back button
        AppToolbar(
            title = "Send Feedback",
            subtitle = "Help us improve",
            showBorder = true,
            showBackButton = true,
            onBackClick = onBackClick
        )
        
        if (isSubmitted) {
            // Success state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Thank You!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Your feedback has been submitted successfully. We really appreciate you taking the time to help us improve!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                AppPrimaryButton(
                    text = "Done",
                    onClick = onBackClick,
                    fullWidth = true
                )
            }
        } else {
            // Form state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Feedback text section
                SectionCard(
                    title = "Your Feedback",
                    description = "Tell us what you think about the app",
                    required = true
                ) {
                    AppTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = "Share your thoughts, suggestions, or report any issues...",
                        minLines = 5,
                        maxLines = 10
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit button
                AppPrimaryButton(
                    text = if (isSubmitting) "Submitting..." else "Submit Feedback",
                    onClick = { submitFeedback() },
                    enabled = !isSubmitting && feedbackText.isNotBlank(),
                    isLoading = isSubmitting,
                    fullWidth = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
