package com.manjul.genai.videogenerator.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.manjul.genai.videogenerator.data.model.PurchaseHistoryItem
import com.manjul.genai.videogenerator.data.model.PurchaseType
import com.manjul.genai.videogenerator.data.repository.BillingRepository
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
 * - Active subscription status FROM GOOGLE PLAY BILLING LIBRARY (real-time)
 * - Purchase history (subscriptions + one-time purchases) from Firestore
 * - Deep links to Google Play subscription management
 */
class SubscriptionManagementViewModel(
    private val application: Application,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()

    init {
        Log.d("SubscriptionMgmtVM", "Initializing - will query Google Play Billing")
        loadSubscriptionStatusFromGooglePlay()
        loadPurchaseHistory()
    }

    /**
     * Load active subscription status FROM GOOGLE PLAY BILLING LIBRARY
     * This queries Google Play directly for real-time subscription status
     */
    private fun loadSubscriptionStatusFromGooglePlay() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                Log.d("SubscriptionMgmtVM", "Querying purchases from Google Play Billing...")
                
                // Query purchases from Google Play Billing Library
                val result = billingRepository.queryPurchases()
                
                result.fold(
                    onSuccess = { purchases ->
                        Log.d("SubscriptionMgmtVM", "Found ${purchases.size} total purchases from Google Play")
                        
                        // Filter for active subscriptions (PURCHASED state and product ID starts with "weekly_")
                        val activeSubscriptions = purchases.filter { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any { it.startsWith("weekly_") }
                        }
                        
                        Log.d("SubscriptionMgmtVM", "Found ${activeSubscriptions.size} active subscriptions")
                        
                        if (activeSubscriptions.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasActiveSubscription = false,
                                activeSubscription = null
                            )
                            return@fold
                        }

                        // Get the first active subscription
                        val subscription = activeSubscriptions.firstOrNull()
                        if (subscription != null) {
                            val productId = subscription.products.firstOrNull() ?: ""
                            
                            // Now fetch additional details from Firestore (like credits per renewal)
                            loadSubscriptionDetailsFromFirestore(productId, subscription.purchaseToken)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasActiveSubscription = false
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("SubscriptionMgmtVM", "Error querying purchases from Google Play", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load subscription status: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("SubscriptionMgmtVM", "Error loading subscription status", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load subscription status: ${e.message}"
                )
            }
        }
    }

    /**
     * Load subscription details from Firestore (credits, renewal date, etc.)
     * This supplements the Google Play data with our backend info
     */
    private suspend fun loadSubscriptionDetailsFromFirestore(productId: String, purchaseToken: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w("SubscriptionMgmtVM", "User not authenticated")
                _uiState.value = _uiState.value.copy(isLoading = false)
                return
            }

            Log.d("SubscriptionMgmtVM", "Loading Firestore details for product: $productId")

            // Query subscription details from Firestore
            val subscriptionDoc = firestore
                .collection("users")
                .document(userId)
                .collection("subscriptions")
                .document(productId)
                .get()
                .await()

            if (subscriptionDoc.exists()) {
                val creditsPerRenewal = subscriptionDoc.getLong("creditsPerRenewal")?.toInt() ?: 0
                val nextRenewalDate = subscriptionDoc.getTimestamp("nextRenewalDate")
                val createdAt = subscriptionDoc.getTimestamp("createdAt")

                Log.d("SubscriptionMgmtVM", "Found subscription: $creditsPerRenewal credits/week, next renewal: $nextRenewalDate")

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
                Log.w("SubscriptionMgmtVM", "Subscription found in Google Play but not in Firestore. This might be normal for new subscriptions.")
                // Still show that there's an active subscription, even if we don't have all details
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasActiveSubscription = true,
                    activeSubscription = ActiveSubscriptionInfo(
                        productId = productId,
                        creditsPerWeek = 0, // Unknown
                        startDate = null,
                        nextRenewalDate = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("SubscriptionMgmtVM", "Error loading Firestore subscription details", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load subscription details: ${e.message}"
            )
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

                Log.d("SubscriptionMgmtVM", "Loading purchase history from Firestore...")

                // Query all purchases, sorted by date (newest first)
                val purchasesSnapshot = firestore
                    .collection("users")
                    .document(userId)
                    .collection("purchases")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50) // Limit to last 50 purchases
                    .get()
                    .await()

                Log.d("SubscriptionMgmtVM", "Found ${purchasesSnapshot.size()} purchase history items")

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
                        Log.e("SubscriptionMgmtVM", "Error parsing purchase: ${doc.id}", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    purchaseHistory = purchaseHistory,
                    isLoadingHistory = false
                )
            } catch (e: Exception) {
                Log.e("SubscriptionMgmtVM", "Error loading purchase history", e)
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
            Log.d("SubscriptionMgmtVM", "Opening subscription management: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SubscriptionMgmtVM", "Error opening subscription management", e)
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
        Log.d("SubscriptionMgmtVM", "Refreshing subscription status and purchase history")
        loadSubscriptionStatusFromGooglePlay()
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
 * Active subscription information (from Google Play + Firestore)
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
    private val application: Application,
    private val billingRepository: BillingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionManagementViewModel(application, billingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
