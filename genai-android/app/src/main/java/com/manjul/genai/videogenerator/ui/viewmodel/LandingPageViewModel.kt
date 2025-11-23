package com.manjul.genai.videogenerator.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.manjul.genai.videogenerator.data.model.LandingPageConfig
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan
import com.manjul.genai.videogenerator.data.repository.BillingRepository
import com.manjul.genai.videogenerator.data.repository.LandingPageRepository
import com.manjul.genai.videogenerator.data.repository.PurchaseUpdateEvent
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class LandingPageUiState(
    val config: LandingPageConfig = LandingPageConfig(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPlan: SubscriptionPlan? = null,
    val productDetails: Map<String, ProductDetails> = emptyMap(),
    val billingInitialized: Boolean = false,
    val purchaseMessage: String? = null, // Success or error message for purchase
    val isPurchaseInProgress: Boolean = false
)

class LandingPageViewModel(
    private val landingPageRepository: LandingPageRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LandingPageUiState())
    val uiState: StateFlow<LandingPageUiState> = _uiState.asStateFlow()
    
    init {
        loadConfig()
        initializeBilling()
        observePurchaseUpdates()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            landingPageRepository.observeConfig().collect { config ->
                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    config = config,
                    isLoading = false,
                    error = null
                )
                // Load product details when config is available
                if (config.subscriptionPlans.isNotEmpty()) {
                    loadProductDetails(config.subscriptionPlans.map { it.productId })
                    // Auto-select popular plan as default
                    if (currentState.selectedPlan == null) {
                        val popularPlan = config.subscriptionPlans.firstOrNull { it.isPopular }
                        if (popularPlan != null) {
                            _uiState.value = _uiState.value.copy(selectedPlan = popularPlan)
                        }
                    }
                }
            }
        }
    }
    
    private fun initializeBilling() {
        viewModelScope.launch {
            billingRepository.initialize().collect { billingResult ->
                if (billingResult.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    _uiState.value = _uiState.value.copy(billingInitialized = true)
                }
            }
        }
    }
    
    private suspend fun loadProductDetails(productIds: List<String>) {
        val result = billingRepository.queryProductDetails(productIds)
        result.onSuccess { productDetailsList ->
            val productDetailsMap = productDetailsList.associateBy { it.productId }
            _uiState.value = _uiState.value.copy(productDetails = productDetailsMap)
        }.onFailure { exception ->
            _uiState.value = _uiState.value.copy(
                error = "Failed to load product details: ${exception.message}"
            )
        }
    }
    
    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }
    
    fun purchasePlan(activity: Activity, plan: SubscriptionPlan): BillingResult {
        val productDetails = _uiState.value.productDetails[plan.productId]
        return if (productDetails != null) {
            // Set purchase in progress
            _uiState.value = _uiState.value.copy(
                isPurchaseInProgress = true,
                purchaseMessage = null
            )
            billingRepository.launchBillingFlow(activity, productDetails)
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Product details not available. Please try again.",
                isPurchaseInProgress = false
            )
            BillingResult.newBuilder()
                .setResponseCode(com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Product details not available")
                .build()
        }
    }
    
    /**
     * Observe purchase updates from BillingRepository
     */
    private fun observePurchaseUpdates() {
        billingRepository.purchaseUpdates
            .onEach { event ->
                when (event) {
                    is PurchaseUpdateEvent.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isPurchaseInProgress = false,
                            purchaseMessage = "Subscription purchased successfully!",
                            error = null
                        )
                        android.util.Log.d("LandingPageViewModel", "Purchase successful: ${event.purchase.products.firstOrNull()}")
                    }
                    is PurchaseUpdateEvent.Error -> {
                        val errorMessage = when (event.billingResult.responseCode) {
                            com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                                "You already have an active subscription"
                            }
                            com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                                "Billing service is unavailable. Please try again later."
                            }
                            com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                                "Billing is not available on this device"
                            }
                            else -> {
                                "Purchase failed: ${event.billingResult.debugMessage}"
                            }
                        }
                        _uiState.value = _uiState.value.copy(
                            isPurchaseInProgress = false,
                            purchaseMessage = null,
                            error = errorMessage
                        )
                        android.util.Log.e("LandingPageViewModel", "Purchase error: ${event.billingResult.debugMessage}")
                    }
                    is PurchaseUpdateEvent.UserCancelled -> {
                        _uiState.value = _uiState.value.copy(
                            isPurchaseInProgress = false,
                            purchaseMessage = null,
                            error = null
                        )
                        android.util.Log.d("LandingPageViewModel", "User cancelled purchase")
                    }
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Clear purchase message
     */
    fun clearPurchaseMessage() {
        _uiState.value = _uiState.value.copy(purchaseMessage = null, error = null)
    }
    
    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LandingPageViewModel(
                        RepositoryProvider.landingPageRepository,
                        BillingRepository(application)
                    ) as T
                }
            }
        }
    }
}

