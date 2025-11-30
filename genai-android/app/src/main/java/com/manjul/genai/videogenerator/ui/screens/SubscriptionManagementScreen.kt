package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.data.model.PurchaseHistoryItem
import com.manjul.genai.videogenerator.data.model.PurchaseType
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.viewmodel.SubscriptionManagementViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.SubscriptionManagementViewModelFactory

/**
 * Subscription Management Screen
 *
 * Displays:
 * - Active subscription status with deep link to Google Play
 * - Subscription vs Top-Up explainer
 * - Complete purchase history (subscriptions + one-time purchases)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManagementScreen(
    onBackClick: () -> Unit,
    onBuyCreditsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val billingRepository = remember { com.manjul.genai.videogenerator.data.repository.BillingRepository(context) }
    val viewModel: SubscriptionManagementViewModel = viewModel(
        factory = SubscriptionManagementViewModelFactory(
            context.applicationContext as android.app.Application,
            billingRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    // Show error snackbar if any
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subscription & Credits",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.PrimaryPurple
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F0720)
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Active Subscription Card or No Subscription Card
            item {
                if (uiState.isLoading) {
                    LoadingSubscriptionCard()
                } else if (uiState.hasActiveSubscription && uiState.activeSubscription != null) {
                    ActiveSubscriptionCard(
                        subscription = uiState.activeSubscription!!,
                        onManageClick = { viewModel.openSubscriptionManagement(context) },
                        formatDate = { viewModel.formatDate(it) }
                    )
                } else {
                    NoSubscriptionCard(onViewPlansClick = onBuyCreditsClick)
                }
            }

            // Subscription vs Top-Up Explainer
            item {
                SubscriptionVsTopUpCard()
            }

            // Purchase History Section
            item {
                Text(
                    text = "Purchase History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Purchase History Items
            if (uiState.isLoadingHistory) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.PrimaryPurple)
                    }
                }
            } else if (uiState.purchaseHistory.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            } else {
                items(uiState.purchaseHistory) { purchase ->
                    PurchaseHistoryCard(
                        purchase = purchase,
                        formatDateTime = { viewModel.formatDateTime(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LoadingSubscriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.PrimaryPurple)
        }
    }
}

@Composable
private fun ActiveSubscriptionCard(
    subscription: com.manjul.genai.videogenerator.ui.viewmodel.ActiveSubscriptionInfo,
    onManageClick: () -> Unit,
    formatDate: (java.util.Date?) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Subscription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Plan details
            InfoRow(
                icon = Icons.Default.CardGiftcard,
                label = "Plan",
                value = "${subscription.creditsPerWeek} Credits Weekly"
            )

            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Started",
                value = formatDate(subscription.startDate)
            )

            InfoRow(
                icon = Icons.Default.Refresh,
                label = "Next Billing",
                value = formatDate(subscription.nextRenewalDate)
            )

            Divider(color = Color(0xFF2D2442), thickness = 1.dp)

            // Manage button with Play Store icon
            Button(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage in Play Store",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NoSubscriptionCard(onViewPlansClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "No Active Subscription",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Subscribe to get weekly credits automatically delivered to your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onViewPlansClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "View Subscription Plans",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SubscriptionVsTopUpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Subscription vs. Top-Up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subscription column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = AppColors.PrimaryPurple.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Subscription",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.PrimaryPurple
                        )
                    }

                    FeatureItem("✅ Weekly credits")
                    FeatureItem("✅ Auto-renews")
                    FeatureItem("✅ Best for regulars")
                    FeatureItem("✅ Cancel anytime")
                }

                // Top-Up column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Top-Up",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    FeatureItem("✅ One-time credits")
                    FeatureItem("✅ No renewal")
                    FeatureItem("✅ Best for occasional")
                    FeatureItem("✅ Bulk discounts")
                }
            }
        }
    }
}

@Composable
private fun PurchaseHistoryCard(
    purchase: PurchaseHistoryItem,
    formatDateTime: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and Details
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon based on type
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (purchase.type == PurchaseType.SUBSCRIPTION) {
                                AppColors.PrimaryPurple.copy(alpha = 0.2f)
                            } else {
                                Color(0xFF10B981).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (purchase.type == PurchaseType.SUBSCRIPTION) {
                            Icons.Default.Refresh
                        } else {
                            Icons.Default.ShoppingCart
                        },
                        contentDescription = null,
                        tint = if (purchase.type == PurchaseType.SUBSCRIPTION) {
                            AppColors.PrimaryPurple
                        } else {
                            Color(0xFF10B981)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = purchase.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = formatDateTime(purchase.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )

                    // Type badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (purchase.type == PurchaseType.SUBSCRIPTION) {
                                    AppColors.PrimaryPurple.copy(alpha = 0.3f)
                                } else {
                                    Color(0xFF10B981).copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (purchase.type == PurchaseType.SUBSCRIPTION) "Subscription" else "One-Time",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (purchase.type == PurchaseType.SUBSCRIPTION) {
                                AppColors.PrimaryPurple
                            } else {
                                Color(0xFF10B981)
                            },
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Right: Price
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = purchase.price,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "+${purchase.credits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1134)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No purchases yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your purchase history will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.PrimaryPurple,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFE5E7EB),
        fontSize = 12.sp
    )
}

