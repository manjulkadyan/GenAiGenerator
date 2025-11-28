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
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private val functions: FirebaseFunctions by lazy { Firebase.functions }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    init {
        loadConfig()
        initializeBilling()
        observePurchaseUpdates()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            landingPageRepository.observeConfig().collect { config ->
                val currentState = _uiState.value
                android.util.Log.d("LandingPageViewModel", "Config loaded: ${config.subscriptionPlans.size} plans, ${config.features.size} features")
                _uiState.value = currentState.copy(
                    config = config,
                    isLoading = false,
                    error = null
                )
                
                // Track subscription viewed
                AnalyticsManager.trackSubscriptionViewed()
                
                // Auto-select popular plan as default
                if (currentState.selectedPlan == null) {
                    val popularPlan = config.subscriptionPlans.firstOrNull { it.isPopular }
                    if (popularPlan != null) {
                        android.util.Log.d("LandingPageViewModel", "Auto-selecting popular plan: ${popularPlan.productId}")
                        _uiState.value = _uiState.value.copy(selectedPlan = popularPlan)
                        AnalyticsManager.trackSubscriptionPlanSelected(popularPlan.productId, popularPlan.credits.toDouble())
                    }
                }
                // NOTE: Do NOT load product details here - wait for billing to be initialized
                // Product details will be loaded in initializeBilling() after connection is established
            }
        }
    }
    
    private fun initializeBilling() {
        viewModelScope.launch {
            android.util.Log.d("LandingPageViewModel", "Starting billing initialization...")
            billingRepository.initialize().collect { billingResult ->
                android.util.Log.d("LandingPageViewModel", "Billing initialization result: code=${billingResult.responseCode}, message=${billingResult.debugMessage}")
                if (billingResult.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    android.util.Log.d("LandingPageViewModel", "Billing initialized successfully - loading product details")
                    _uiState.value = _uiState.value.copy(billingInitialized = true)
                    // Load product details after billing is initialized and ready
                    val currentConfig = _uiState.value.config
                    if (currentConfig.subscriptionPlans.isNotEmpty()) {
                        android.util.Log.d("LandingPageViewModel", "Loading product details for ${currentConfig.subscriptionPlans.size} plans")
                        loadProductDetails(currentConfig.subscriptionPlans.map { it.productId })
                    } else {
                        android.util.Log.w("LandingPageViewModel", "No subscription plans in config to load")
                    }
                } else {
                    android.util.Log.e("LandingPageViewModel", "Billing initialization failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})")
                    _uiState.value = _uiState.value.copy(
                        error = "Billing initialization failed: ${billingResult.debugMessage}",
                        billingInitialized = false
                    )
                }
            }
        }
    }
    
    private suspend fun loadProductDetails(productIds: List<String>) {
        if (productIds.isEmpty()) {
            android.util.Log.w("LandingPageViewModel", "No product IDs to load")
            return
        }
        
        android.util.Log.d("LandingPageViewModel", "Loading product details for: ${productIds.joinToString()}")
        val result = billingRepository.queryProductDetails(productIds)
        result.onSuccess { productDetailsList ->
            android.util.Log.d("LandingPageViewModel", "Loaded ${productDetailsList.size} product details")
            if (productDetailsList.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "No products found. Please create subscription products in Google Play Console with IDs: ${productIds.joinToString()}"
                )
            } else {
                val productDetailsMap = productDetailsList.associateBy { it.productId }
                _uiState.value = _uiState.value.copy(
                    productDetails = productDetailsMap,
                    error = null
                )
                // Log which products were found
                val foundIds = productDetailsList.map { it.productId }
                val missingIds = productIds.filter { it !in foundIds }
                if (missingIds.isNotEmpty()) {
                    android.util.Log.w("LandingPageViewModel", "Missing products: ${missingIds.joinToString()}")
                }
            }
        }.onFailure { exception ->
            android.util.Log.e("LandingPageViewModel", "Failed to load product details", exception)
            val errorMessage = when {
                exception.message?.contains("not found") == true -> 
                    "Products not found in Play Console. Create subscriptions with IDs: ${productIds.joinToString()}"
                exception.message?.contains("not available") == true ->
                    "Products not available. Make sure app is uploaded to Internal testing track in Play Console."
                else -> 
                    "Failed to load products: ${exception.message}. Check Play Console setup."
            }
            _uiState.value = _uiState.value.copy(
                error = errorMessage
            )
        }
    }
    
    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
        AnalyticsManager.trackSubscriptionPlanSelected(plan.productId, plan.credits.toDouble())
    }
    
    fun purchasePlan(activity: Activity, plan: SubscriptionPlan): BillingResult {
        val productDetails = _uiState.value.productDetails[plan.productId]
        return if (productDetails != null) {
            // Set purchase in progress
            _uiState.value = _uiState.value.copy(
                isPurchaseInProgress = true,
                purchaseMessage = null
            )
            val obfuscatedAccountId = auth.currentUser?.uid?.hashCode()?.toString()
            val obfuscatedProfileId = auth.currentUser?.uid?.reversed()?.hashCode()?.toString()
            billingRepository.launchBillingFlow(
                activity,
                productDetails,
                obfuscatedAccountId,
                obfuscatedProfileId
            )
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
        android.util.Log.d("LandingPageViewModel", "Starting to observe purchase updates")
        billingRepository.purchaseUpdates
            .onEach { event ->
                android.util.Log.d("LandingPageViewModel", "=== Purchase Event Received ===")
                when (event) {
                    is PurchaseUpdateEvent.Success -> {
                android.util.Log.d("LandingPageViewModel", "✅ Purchase SUCCESS: ${event.purchase.products.firstOrNull()}")
                android.util.Log.d("LandingPageViewModel", "Purchase state: ${event.purchase.purchaseState}, Acknowledged: ${event.purchase.isAcknowledged}")
                // Process subscription on the server (adds credits, stores renewal info)
                viewModelScope.launch {
                    processSubscriptionPurchase(event.purchase)
                }
                _uiState.value = _uiState.value.copy(
                    isPurchaseInProgress = false,
                    purchaseMessage = "Subscription purchased successfully!",
                    error = null
                )
                    }
                    is PurchaseUpdateEvent.Error -> {
                        android.util.Log.e("LandingPageViewModel", "❌ Purchase ERROR: code=${event.billingResult.responseCode}, message=${event.billingResult.debugMessage}")
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
                    }
                    is PurchaseUpdateEvent.UserCancelled -> {
                        android.util.Log.d("LandingPageViewModel", "⚠️ User CANCELLED purchase")
                        _uiState.value = _uiState.value.copy(
                            isPurchaseInProgress = false,
                            purchaseMessage = null,
                            error = null
                        )
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

    /**
     * Send subscription purchase to backend for verification and credit grant.
     */
    private suspend fun processSubscriptionPurchase(purchase: com.android.billingclient.api.Purchase) {
        val productId = purchase.products.firstOrNull()
        if (productId == null) {
            android.util.Log.e("LandingPageViewModel", "Cannot process subscription: product ID is null")
            return
        }

        if (purchase.purchaseState != com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
            android.util.Log.w(
                "LandingPageViewModel",
                "Purchase state is not PURCHASED (state=${purchase.purchaseState}), skipping server processing"
            )
            return
        }

        val plan = _uiState.value.config.subscriptionPlans.firstOrNull {
            it.productId == productId
        }
        if (plan == null) {
            android.util.Log.e("LandingPageViewModel", "Cannot process subscription: plan not found for product $productId")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            android.util.Log.e("LandingPageViewModel", "Cannot process subscription: user not authenticated")
            return
        }

        val data = hashMapOf(
            "userId" to userId,
            "productId" to productId,
            "purchaseToken" to purchase.purchaseToken,
            "credits" to plan.credits
        )

        try {
            functions
                .getHttpsCallable("handleSubscriptionPurchase")
                .call(data)
                .await()
            android.util.Log.d("LandingPageViewModel", "✅ Subscription sent to backend for processing")
            _uiState.value = _uiState.value.copy(
                purchaseMessage = "Subscription purchased! ${plan.credits} credits added to your account."
            )
        } catch (e: Exception) {
            android.util.Log.e("LandingPageViewModel", "❌ Failed to process subscription on backend", e)
            _uiState.value = _uiState.value.copy(
                error = "Subscription purchase succeeded but verification failed. Please contact support if credits are missing."
            )
        }
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
