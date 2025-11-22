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
import com.manjul.genai.videogenerator.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LandingPageUiState(
    val config: LandingPageConfig = LandingPageConfig(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPlan: SubscriptionPlan? = null,
    val productDetails: Map<String, ProductDetails> = emptyMap(),
    val billingInitialized: Boolean = false
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
            billingRepository.launchBillingFlow(activity, productDetails)
        } else {
            BillingResult.newBuilder()
                .setResponseCode(com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("Product details not available")
                .build()
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

