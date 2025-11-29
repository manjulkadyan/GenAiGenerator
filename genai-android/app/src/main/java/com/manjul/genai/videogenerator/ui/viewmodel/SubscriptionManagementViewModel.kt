package com.manjul.genai.videogenerator.ui.viewmodel

import android.app.Application
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
import java.util.Date
import java.util.Locale

data class SubscriptionInfo(
    val productId: String = "",
    val productName: String = "",
    val creditsPerWeek: Int = 0,
    val status: String = "",
    val startDate: String = "",
    val nextRenewalDate: String = "",
    val isActive: Boolean = false
)

data class SubscriptionManagementUiState(
    val isLoading: Boolean = true,
    val hasActiveSubscription: Boolean = false,
    val subscriptionInfo: SubscriptionInfo? = null,
    val purchaseHistory: List<PurchaseHistoryItem> = emptyList(),
    val error: String? = null
)

class SubscriptionManagementViewModel(
    private val application: Application
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()
    
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    init {
        loadSubscriptionData()
        loadPurchaseHistory()
    }
    
    /**
     * Load active subscription information from Firestore
     */
    private fun loadSubscriptionData() {
        viewModelScope.launch {
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
                        subscriptionInfo = null
                    )
                    return@launch
                }
                
                // Get the first active subscription
                val subDoc = subscriptionsSnapshot.documents.firstOrNull()
                if (subDoc != null) {
                    val productId = subDoc.getString("productId") ?: ""
                    val creditsPerRenewal = subDoc.getLong("creditsPerRenewal")?.toInt() ?: 0
                    val status = subDoc.getString("status") ?: ""
                    val createdAt = subDoc.getTimestamp("createdAt")
                    val nextRenewalDate = subDoc.getTimestamp("nextRenewalDate")
                    
                    val productName = when {
                        productId.contains("60") -> "60 Credits Weekly"
                        productId.contains("100") -> "100 Credits Weekly"
                        productId.contains("150") -> "150 Credits Weekly"
                        else -> "Weekly Subscription"
                    }
                    
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val startDateStr = createdAt?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                    val nextRenewalStr = nextRenewalDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
                    
                    val subscriptionInfo = SubscriptionInfo(
                        productId = productId,
                        productName = productName,
                        creditsPerWeek = creditsPerRenewal,
                        status = status,
                        startDate = startDateStr,
                        nextRenewalDate = nextRenewalStr,
                        isActive = status == "active"
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasActiveSubscription = true,
                        subscriptionInfo = subscriptionInfo
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasActiveSubscription = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionManagementVM", "Error loading subscription data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load subscription data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load purchase history from Firestore
     */
    private fun loadPurchaseHistory() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    return@launch
                }
                
                // Query purchases ordered by purchase time
                val purchasesSnapshot = firestore
                    .collection("users")
                    .document(userId)
                    .collection("purchases")
                    .orderBy("purchaseTime", Query.Direction.DESCENDING)
                    .limit(50) // Limit to last 50 purchases
                    .get()
                    .await()
                
                val purchases = purchasesSnapshot.documents.mapNotNull { doc ->
                    try {
                        val productId = doc.getString("productId") ?: return@mapNotNull null
                        val type = doc.getString("type") ?: "one_time"
                        val credits = doc.getLong("credits")?.toInt() ?: 0
                        val priceMicros = doc.getLong("priceMicros") ?: 0L
                        val currency = doc.getString("currency") ?: "USD"
                        val purchaseTime = doc.getTimestamp("purchaseTime")
                        
                        val productName = if (type == "subscription") {
                            when {
                                productId.contains("60") -> "60 Credits Weekly"
                                productId.contains("100") -> "100 Credits Weekly"
                                productId.contains("150") -> "150 Credits Weekly"
                                else -> "Weekly Subscription"
                            }
                        } else {
                            "$credits Credits Top-Up"
                        }
                        
                        val price = if (priceMicros > 0) {
                            val priceValue = priceMicros / 1_000_000.0
                            "$currency ${String.format("%.2f", priceValue)}"
                        } else {
                            "N/A"
                        }
                        
                        PurchaseHistoryItem(
                            purchaseToken = doc.id,
                            productId = productId,
                            type = if (type == "subscription") PurchaseType.SUBSCRIPTION else PurchaseType.ONE_TIME,
                            credits = credits,
                            price = price,
                            currency = currency,
                            date = purchaseTime?.toDate()?.time ?: 0L,
                            productName = productName
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("SubscriptionManagementVM", "Error parsing purchase", e)
                        null
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    purchaseHistory = purchases
                )
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionManagementVM", "Error loading purchase history", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load purchase history: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get deep link to Google Play subscriptions page
     */
    fun getPlayStoreSubscriptionLink(productId: String): String {
        val packageName = application.packageName
        return "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
    }
    
    /**
     * Format date from timestamp
     */
    fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Format date and time from timestamp
     */
    fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionManagementViewModel(application) as T
                }
            }
        }
    }
}

