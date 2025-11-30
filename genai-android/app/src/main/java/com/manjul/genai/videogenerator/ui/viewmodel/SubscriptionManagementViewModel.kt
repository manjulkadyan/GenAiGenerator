package com.manjul.genai.videogenerator.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.manjul.genai.videogenerator.data.model.PurchaseHistoryItem
import com.manjul.genai.videogenerator.data.model.PurchaseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Subscription Management Screen
 * Handles:
 * - Active subscription status
 * - Purchase history (subscriptions + one-time purchases)
 * - Deep links to Google Play subscription management
 */
class SubscriptionManagementViewModel(
    private val application: Application
) : ViewModel() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptionStatus()
        loadPurchaseHistory()
    }

    /**
     * Load active subscription status from Firestore
     */
    private fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                // Query active subscriptions
                val subscriptionsSnapshot = firestore
                    .collection("users")
                    .document(userId)
                    .collection("subscriptions")
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                if (subscriptionsSnapshot.isEmpty) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasActiveSubscription = false,
                        activeSubscription = null
                    )
                    return@launch
                }

                // Get the first active subscription
                val subscriptionDoc = subscriptionsSnapshot.documents.firstOrNull()
                if (subscriptionDoc != null) {
                    val productId = subscriptionDoc.getString("productId") ?: ""
                    val creditsPerRenewal = subscriptionDoc.getLong("creditsPerRenewal")?.toInt() ?: 0
                    val nextRenewalDate = subscriptionDoc.getTimestamp("nextRenewalDate")
                    val createdAt = subscriptionDoc.getTimestamp("createdAt")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasActiveSubscription = true,
                        activeSubscription = ActiveSubscriptionInfo(
                            productId = productId,
                            creditsPerWeek = creditsPerRenewal,
                            startDate = createdAt?.toDate(),
                            nextRenewalDate = nextRenewalDate?.toDate()
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasActiveSubscription = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionMgmtVM", "Error loading subscription status", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load subscription status: ${e.message}"
                )
            }
        }
    }

    /**
     * Load purchase history from Firestore
     * Includes both subscription and one-time purchases
     */
    private fun loadPurchaseHistory() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    return@launch
                }

                // Query all purchases, sorted by date (newest first)
                val purchasesSnapshot = firestore
                    .collection("users")
                    .document(userId)
                    .collection("purchases")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50) // Limit to last 50 purchases
                    .get()
                    .await()

                val purchaseHistory = purchasesSnapshot.documents.mapNotNull { doc ->
                    try {
                        val purchaseToken = doc.id
                        val productId = doc.getString("productId") ?: return@mapNotNull null
                        val typeString = doc.getString("type") ?: "one_time"
                        val type = if (typeString == "subscription") PurchaseType.SUBSCRIPTION else PurchaseType.ONE_TIME
                        val credits = doc.getLong("credits")?.toInt() ?: 0
                        val priceMicros = doc.getLong("priceMicros") ?: 0L
                        val currency = doc.getString("currency") ?: "USD"
                        val date = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                        // Format price from micros (divide by 1,000,000)
                        val price = if (priceMicros > 0) {
                            val priceValue = priceMicros / 1_000_000.0
                            "$${String.format("%.2f", priceValue)}"
                        } else {
                            "$0.00"
                        }

                        // Create product name
                        val productName = when {
                            type == PurchaseType.SUBSCRIPTION -> "$credits Credits Weekly"
                            type == PurchaseType.ONE_TIME -> "$credits Credits Top-Up"
                            else -> "$credits Credits"
                        }

                        PurchaseHistoryItem(
                            purchaseToken = purchaseToken,
                            productId = productId,
                            type = type,
                            credits = credits,
                            price = price,
                            currency = currency,
                            date = date,
                            productName = productName
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("SubscriptionMgmtVM", "Error parsing purchase: ${doc.id}", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    purchaseHistory = purchaseHistory,
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionMgmtVM", "Error loading purchase history", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = "Failed to load purchase history: ${e.message}"
                )
            }
        }
    }

    /**
     * Generate deep link to Google Play subscription management
     * Format: https://play.google.com/store/account/subscriptions?sku={productId}&package={packageName}
     */
    fun getSubscriptionManagementUrl(): String {
        val packageName = application.packageName
        val productId = _uiState.value.activeSubscription?.productId ?: ""
        return "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
    }

    /**
     * Open subscription management in Google Play
     */
    fun openSubscriptionManagement(context: Context) {
        try {
            val url = getSubscriptionManagementUrl()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionMgmtVM", "Error opening subscription management", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to open subscription management"
            )
        }
    }

    /**
     * Format date for display
     */
    fun formatDate(date: Date?): String {
        if (date == null) return "Unknown"
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Format date and time for display
     */
    fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown"
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadSubscriptionStatus()
        loadPurchaseHistory()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Subscription Management Screen
 */
data class SubscriptionManagementUiState(
    val isLoading: Boolean = true,
    val isLoadingHistory: Boolean = true,
    val hasActiveSubscription: Boolean = false,
    val activeSubscription: ActiveSubscriptionInfo? = null,
    val purchaseHistory: List<PurchaseHistoryItem> = emptyList(),
    val error: String? = null
)

/**
 * Active subscription information
 */
data class ActiveSubscriptionInfo(
    val productId: String,
    val creditsPerWeek: Int,
    val startDate: Date?,
    val nextRenewalDate: Date?
)

/**
 * Factory for creating SubscriptionManagementViewModel
 */
class SubscriptionManagementViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionManagementViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
