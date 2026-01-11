package com.manjul.genai.videogenerator.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.manjul.genai.videogenerator.data.auth.AuthManager
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.tasks.await
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.StatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.designsystem.components.dialogs.AppDialog
import com.manjul.genai.videogenerator.ui.designsystem.components.sections.SectionCard
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import androidx.compose.material.icons.filled.Login
import com.manjul.genai.videogenerator.ui.viewmodel.CreditsViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import com.manjul.genai.videogenerator.utils.AnalyticsManager

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    creditsViewModel: CreditsViewModel = viewModel(factory = CreditsViewModel.Factory),
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    onBuyCreditsClick: () -> Unit = {},
    onVideosClick: () -> Unit = {},
    onSubscriptionManagementClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val credits by creditsViewModel.state.collectAsState()
    val jobs by historyViewModel.jobs.collectAsState()

    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("Profile")
        AnalyticsManager.trackProfileViewed()
    }

    // Get user info from Firebase Auth (fallback)
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: ""
    
    // Observe user profile from Firestore (real-time, no delay)
    var userName by remember { mutableStateOf(currentUser?.displayName ?: "User") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "user@example.com") }
    
    // Real-time listener for user profile data from Firestore (no delay)
    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(userId)
        
        // Get initial data immediately (no delay) - blocking call
        try {
            val snapshot = userRef.get().await()
            val firestoreName = snapshot.getString("name")
            val firestoreEmail = snapshot.getString("email")
            
            if (!firestoreName.isNullOrEmpty()) {
                userName = firestoreName
                Log.d("ProfileScreen", "Loaded name from Firestore: $firestoreName")
            }
            if (!firestoreEmail.isNullOrEmpty()) {
                userEmail = firestoreEmail
                Log.d("ProfileScreen", "Loaded email from Firestore: $firestoreEmail")
            }
        } catch (e: Exception) {
            android.util.Log.w("ProfileScreen", "Failed to load user profile from Firestore, using Auth fallback", e)
        }
        
        // Set up real-time listener for updates (non-blocking)
        // Note: Listener runs on main thread, state updates are safe
        val registration = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.w("ProfileScreen", "Error listening to user profile", error)
                return@addSnapshotListener
            }
            
            val firestoreName = snapshot?.getString("name")
            val firestoreEmail = snapshot?.getString("email")
            
            // Update name - prefer Firestore, fallback to Auth
            when {
                !firestoreName.isNullOrEmpty() -> {
                    userName = firestoreName
                    Log.d("ProfileScreen", "User name updated from Firestore: $firestoreName")
                }
                currentUser?.displayName != null -> {
                    userName = currentUser.displayName ?: "User"
                }
            }
            
            // Update email - prefer Firestore, fallback to Auth
            when {
                !firestoreEmail.isNullOrEmpty() -> {
                    userEmail = firestoreEmail
                    Log.d("ProfileScreen", "User email updated from Firestore: $firestoreEmail")
                }
                currentUser?.email != null -> {
                    userEmail = currentUser.email?: "user@example.com"
                }
            }
        }
        
        // Cleanup listener when composable leaves composition
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            registration.remove()
            Log.d("ProfileScreen", "Removed user profile listener")
        }
    }
    
    // Also update from Auth when user changes (immediate fallback)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d("ProfileScreen", "Current user changed: uid=${currentUser.uid}, displayName=${currentUser.displayName}, email=${currentUser.email}")
            
            // Try to reload user profile to get fresh data
            try {
                currentUser.reload().await()
                val reloadedUser = auth.currentUser
                Log.d("ProfileScreen", "After reload: displayName=${reloadedUser?.displayName}, email=${reloadedUser?.email}")
                
                // Use Auth data as fallback if Firestore doesn't have it yet
                val authDisplayName = reloadedUser?.displayName ?: currentUser.displayName
                val authEmail = reloadedUser?.email ?: currentUser.email
                
                // Try provider data if displayName is still null
                val providerDisplayName = currentUser.providerData.firstOrNull { 
                    it.providerId == "google.com" 
                }?.displayName
                
                val finalAuthName = authDisplayName ?: providerDisplayName
                
                if (userName == "User" && !finalAuthName.isNullOrEmpty()) {
                    userName = finalAuthName
                    Log.d("ProfileScreen", "Updated name from Auth: $finalAuthName")
                }
                if (userEmail == "user@example.com" && !authEmail.isNullOrEmpty()) {
                    userEmail = authEmail
                    Log.d("ProfileScreen", "Updated email from Auth: $authEmail")
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileScreen", "Failed to reload user profile", e)
                // Fallback to current user data without reload
                if (userName == "User" && !currentUser.displayName.isNullOrEmpty()) {
                    userName = currentUser.displayName?: ""
                }
                if (userEmail == "user@example.com" && !currentUser.email.isNullOrEmpty()) {
                    userEmail = currentUser.email?: ""
                }
            }
        }
    }

    // Calculate video count
    val videoCount = jobs.size

    // Check if user is anonymous
    val isAnonymous = AuthManager.isAnonymousUser()

    // Use the extracted content composable
    ProfileScreenContent(
        modifier = modifier,
        creditsCount = credits.credits,
        videoCount = videoCount,
        userName = userName,
        userEmail = userEmail,
        userId = userId,
        onBuyCreditsClick = onBuyCreditsClick,
        onVideosClick = onVideosClick,
        onLogout = {
            FirebaseAuth.getInstance().signOut()
        },
        isAnonymous = isAnonymous,
        onLoginClick = onLoginClick,
        onSubscriptionManagementClick = onSubscriptionManagementClick,
        onFeedbackClick = onFeedbackClick
    )
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconBackgroundColor: Color,
    badge: String? = null
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        padding = PaddingValues()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextPrimary
                    )
                    badge?.let {
                        StatusBadge(
                            text = it,
                            isRequired = false
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = AppColors.TextSecondary
            )
        }
    }
}


@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        padding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = if (isHighlighted) AppColors.PrimaryPurple else AppColors.TextSecondary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isHighlighted) AppColors.PrimaryPurple else AppColors.TextSecondary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) AppColors.PrimaryPurple else AppColors.TextPrimary
            )
        }
    }
}


@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color,
    trailingContent: @Composable () -> Unit
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.TextPrimary
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.TextPrimary
                )
            }

            trailingContent()
        }
    }
}

// ==================== Preview ====================

@Preview(
    name = "Profile Screen",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
private fun ProfileScreenPreview() {
    GenAiVideoTheme {
        // Mock data for preview (no ViewModels)
        val mockCredits = remember { 1250 }
        val mockVideoCount = remember { 12 }
        val mockUserName = "John Doe"
        val mockUserEmail = "john.doe@example.com"
        val mockUserId = "user123456789"

        // Create a preview version that doesn't use ViewModels
        ProfileScreenContent(
            creditsCount = mockCredits,
            videoCount = mockVideoCount,
            userName = mockUserName,
            userEmail = mockUserEmail,
            userId = mockUserId,
            onBuyCreditsClick = {},
            onVideosClick = {},
            isAnonymous = true,
            onLoginClick = {},
            onSubscriptionManagementClick = {},
            onFeedbackClick = {}
        )
    }
}

// Extracted content composable for preview support
@Composable
private fun ProfileScreenContent(
    creditsCount: Int,
    videoCount: Int,
    userName: String,
    userEmail: String,
    userId: String,
    modifier: Modifier = Modifier,
    onBuyCreditsClick: () -> Unit = {},
    onVideosClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    isAnonymous: Boolean = false,
    onLoginClick: () -> Unit = {},
    onSubscriptionManagementClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        AppToolbar(
            title = "Profile",
            subtitle = "Account",
            showBorder = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Card
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.PrimaryPurple.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            AppColors.PrimaryPurple.copy(alpha = 0.3f),
                                            AppColors.PrimaryPurple.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .border(
                                    3.dp,
                                    AppColors.PrimaryPurple.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(36.dp),
                                tint = AppColors.PrimaryPurple
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Name, Email, and User ID
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnPrimaryPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.OnPrimaryPurple
                            )
                            if (userId.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            val clipboard =
                                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("User ID", userId)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(
                                                context,
                                                "User ID copied to clipboard",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "ID: $userId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy User ID",
                                        modifier = Modifier.size(14.dp),
                                        tint = AppColors.TextSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.PlayArrow,
                            label = "Videos",
                            value = videoCount.toString(),
                            modifier = Modifier.weight(1f),
                            onClick = onVideosClick
                        )

                        StatCard(
                            icon = Icons.Default.Star,
                            label = "Credits",
                            value = creditsCount.toString(),
                            isHighlighted = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onBuyCreditsClick() },
                            onClick = onBuyCreditsClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Go Premium Card
            if (false)
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Navigate to premium */ },
                    onClick = { /* TODO: Navigate to premium */ },
                    padding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFF59E0B).copy(alpha = 0.1f),
                                        Color(0xFFF59E0B).copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Premium",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color(0xFFF59E0B)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Go Premium",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextPrimary
                                        )
                                        CustomStatusBadge(
                                            text = "New",
                                            backgroundColor = Color(0xFFF59E0B),
                                            textColor = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Unlimited generations & priority access",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Go Premium",
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.TextSecondary
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign-In Section (only show if anonymous)
            if (isAnonymous) {
                SectionCard(
                    title = "Account",
                    description = "Sign in to save your progress",
                    required = false
                ) {
                    ActionCard(
                        icon = Icons.Default.Login,
                        title = "Sign In / Sign Up",
                        onClick = onLoginClick,
                        iconBackgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Actions Section
            SectionCard(
                title = "Quick Actions",
                description = "Common actions and shortcuts",
                required = false
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        icon = Icons.Default.CreditCard,
                        title = "Buy Credits",
                        onClick = onBuyCreditsClick,
                        iconBackgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.1f)
                    )
                    ActionCard(
                        icon = Icons.Default.WorkspacePremium,
                        title = "Subscription & Credits",
                        onClick = onSubscriptionManagementClick,
                        iconBackgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Section
            SectionCard(
                title = "Settings",
                description = "App preferences and configuration",
                required = false
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    /*// Dark Mode - Always ON (display only)
                    ActionCard(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        onClick = { },
                        iconBackgroundColor = AppColors.SurfaceElevated.copy(alpha = 0.5f),
                        badge = "Always On"
                    )*/

                    // Notifications - Opens app settings
                    ActionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        iconBackgroundColor = AppColors.SurfaceElevated.copy(alpha = 0.5f)
                    )
                    
                    // Send Feedback
                    ActionCard(
                        icon = Icons.Default.Feedback,
                        title = "Send Feedback",
                        onClick = onFeedbackClick,
                        iconBackgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.1f)
                    )

                    // Privacy Policy
                    ActionCard(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/genai-videogenerator-privacy-policy/cd6d3993-1e48-4a61-831a-c9154da6d101/privacy"))
                            context.startActivity(intent)
                        },
                        iconBackgroundColor = AppColors.SurfaceElevated.copy(alpha = 0.5f)
                    )

                    // Terms of Use
                    ActionCard(
                        icon = Icons.Default.Article,
                        title = "Terms of Use",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doc-hosting.flycricket.io/genai-videogenerator-terms-of-use/5812e7a9-a6e1-4e96-9255-e5c39fba5b5e/terms"))
                            context.startActivity(intent)
                        },
                        iconBackgroundColor = AppColors.SurfaceElevated.copy(alpha = 0.5f)
                    )

                    // Logout - Only show for non-anonymous users (Google signed in)
                    if (!isAnonymous) {
                        ActionCard(
                            icon = Icons.Default.ExitToApp,
                            title = "Logout",
                            onClick = { showLogoutDialog = true },
                            iconBackgroundColor = AppColors.StatusError.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AppDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = "Logout"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Are you sure you want to logout?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTextButton(
                        text = "Cancel",
                        onClick = { showLogoutDialog = false }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AppSecondaryButton(
                        text = "Logout",
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        fullWidth = false
                    )
                }
            }
        }
    }
}
