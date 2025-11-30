package com.manjul.genai.videogenerator.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Sealed class representing purchase update events
 */
sealed class PurchaseUpdateEvent {
    data class Success(val purchase: Purchase) : PurchaseUpdateEvent()
    data class Error(val billingResult: BillingResult) : PurchaseUpdateEvent()
    data class AlreadyOwned(val purchase: Purchase) : PurchaseUpdateEvent()
    object UserCancelled : PurchaseUpdateEvent()
}

/**
 * Repository for handling Google Play Billing operations.
 */
class BillingRepository(private val context: Context) {
    private var billingClient: BillingClient? = null
    
    // Flow to emit purchase update events
    private val _purchaseUpdates = MutableSharedFlow<PurchaseUpdateEvent>(extraBufferCapacity = 1)
    val purchaseUpdates: SharedFlow<PurchaseUpdateEvent> = _purchaseUpdates.asSharedFlow()
    
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d("BillingRepository", "=== Purchase Update Received ===")
        Log.d("BillingRepository", "Response code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
        Log.d("BillingRepository", "Purchases count: ${purchases?.size ?: 0}")
        
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d("BillingRepository", "Purchase successful - processing ${purchases?.size ?: 0} purchase(s)")
                purchases?.forEach { purchase ->
                    Log.d("BillingRepository", "Processing purchase: ${purchase.products.firstOrNull()}, state: ${purchase.purchaseState}, acknowledged: ${purchase.isAcknowledged}")
                    handlePurchase(purchase)
                    
                    // Track purchase completion
                    val productId = purchase.products.firstOrNull() ?: "unknown"
                    AnalyticsManager.trackPurchaseCompleted(
                        productId = productId,
                        purchaseToken = purchase.purchaseToken
                    )
                    
                    // Update subscription status
                    AnalyticsManager.setSubscriptionStatus("active")
                    
                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Success(purchase))
                }
                if (purchases.isNullOrEmpty()) {
                    android.util.Log.w("BillingRepository", "Purchase OK but no purchases in list - this might be a restore or query")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d("BillingRepository", "User cancelled the purchase")
                // Track cancellation - we don't have product ID here, so use "unknown"
                AnalyticsManager.trackPurchaseCancelled("unknown")
                _purchaseUpdates.tryEmit(PurchaseUpdateEvent.UserCancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                android.util.Log.w("BillingRepository", "Item already owned - querying existing purchases to sync")
                // Query existing purchases and process them to ensure backend is synced
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = queryPurchases()
                        result.onSuccess { existingPurchases ->
                            Log.d("BillingRepository", "Found ${existingPurchases.size} existing purchases")
                            if (existingPurchases.isNotEmpty()) {
                                // Process the first unacknowledged purchase or the most recent one
                                val purchaseToProcess = existingPurchases.firstOrNull { !it.isAcknowledged }
                                    ?: existingPurchases.firstOrNull()
                                
                                purchaseToProcess?.let { purchase ->
                                    Log.d("BillingRepository", "Re-processing existing purchase: ${purchase.products.firstOrNull()}")
                                    handlePurchase(purchase)
                                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.AlreadyOwned(purchase))
                                }
                            } else {
                                android.util.Log.w("BillingRepository", "No existing purchases found despite ITEM_ALREADY_OWNED error")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BillingRepository", "Error querying existing purchases", e)
                    }
                }
            }
            else -> {
                android.util.Log.e("BillingRepository", "Purchase error: code=${billingResult.responseCode}, message=${billingResult.debugMessage}")
                // Track purchase failure
                AnalyticsManager.trackPurchaseFailed(
                    productId = "unknown",
                    errorCode = billingResult.responseCode,
                    errorMessage = billingResult.debugMessage ?: "Unknown error"
                )
                _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Error(billingResult))
            }
        }
    }
    
    /**
     * Initialize the billing client.
     */
    fun initialize(): Flow<BillingResult> = callbackFlow {
        // For Billing Library 8.0.0+, enablePendingPurchases() requires PendingPurchasesParams
        // Even though we only use subscriptions, we must enable one-time product support
        // This is required by the Billing Library API
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts() // Required even if we don't use one-time products
            .build()
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .enableAutoServiceReconnection()
            // Note: enableAutoServiceReconnection() may not be available in all versions
            // If compilation fails, remove this line - auto-reconnection is handled manually
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d("BillingRepository", "Billing setup finished: code=${billingResult.responseCode}, message=${billingResult.debugMessage}")
                trySend(billingResult)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingRepository", "Billing client is ready and connected")
                    // ⚠️ CRITICAL: Query purchases immediately after connection
                    // This ensures we catch purchases made while app was closed
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = queryPurchases()
                            result.onSuccess { purchases ->
                                Log.d("BillingRepository", "Queried ${purchases.size} purchases after connection")
                                purchases.forEach { purchase ->
                                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                        handlePurchase(purchase)
                                    }
                                }
                            }.onFailure { error ->
                                android.util.Log.e("BillingRepository", "Failed to query purchases after connection: ${error.message}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BillingRepository", "Error querying purchases after connection", e)
                        }
                    }
                } else {
                    android.util.Log.e("BillingRepository", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                android.util.Log.w("BillingRepository", "Billing service disconnected - connection lost")
                // Try to restart the connection on the next request
                // The billing client will need to be reinitialized
            }
        })
        
        awaitClose {
            billingClient?.endConnection()
        }
    }
    
    /**
     * Query product details for subscription plans.
     */
    suspend fun queryProductDetails(productIds: List<String>): Result<List<ProductDetails>> {
        return suspendCancellableCoroutine { continuation ->
            val billingClient = billingClient
            if (billingClient == null) {
                android.util.Log.e("BillingRepository", "Billing client is null - cannot query products")
                continuation.resume(Result.failure(IllegalStateException("Billing client not initialized")))
                return@suspendCancellableCoroutine
            }
            
            // Check if billing client is ready/connected
            if (!billingClient.isReady) {
                android.util.Log.e("BillingRepository", "Billing client is not ready - connection may be disconnected")
                continuation.resume(Result.failure(IllegalStateException("Billing client is not ready. Service connection is disconnected.")))
                return@suspendCancellableCoroutine
            }
            
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            
            billingClient.queryProductDetailsAsync(
                params
            ) { billingResult, productDetailsResult ->
                val productDetailsList = productDetailsResult?.productDetailsList ?: emptyList()
                Log.d("BillingRepository", "Query result: responseCode=${billingResult.responseCode}, products=${productDetailsList.size}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isEmpty()) {
                        android.util.Log.w("BillingRepository", "No products found for IDs: ${productIds.joinToString()}")
                        continuation.resume(Result.failure(
                            Exception("No products found. Make sure products exist in Play Console with IDs: ${productIds.joinToString()}")
                        ))
                    } else {
                        productDetailsList.forEach { product ->
                            Log.d("BillingRepository", "Found product: ${product.productId}, offers: ${product.subscriptionOfferDetails?.size ?: 0}")
                        }
                        continuation.resume(Result.success(productDetailsList))
                    }
                } else {
                    android.util.Log.e("BillingRepository", "Query failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})")
                    val errorMessage = when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> 
                            "Products not found in Play Console. Create subscriptions with IDs: ${productIds.joinToString()}"
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                            "Billing service unavailable. Make sure app is uploaded to Internal testing track."
                        else ->
                            "Failed to query products: ${billingResult.debugMessage}"
                    }
                    continuation.resume(Result.failure(Exception(errorMessage)))
                }
            }
        }
    }
    
    /**
     * Query product details for one-time INAPP products (not subscriptions).
     */
    suspend fun queryOneTimeProducts(productIds: List<String>): Result<List<ProductDetails>> {
        return suspendCancellableCoroutine { continuation ->
            val billingClient = billingClient
            if (billingClient == null) {
                android.util.Log.e("BillingRepository", "Billing client is null - cannot query one-time products")
                continuation.resume(Result.failure(IllegalStateException("Billing client not initialized")))
                return@suspendCancellableCoroutine
            }
            
            // Check if billing client is ready/connected
            if (!billingClient.isReady) {
                android.util.Log.e("BillingRepository", "Billing client is not ready - connection may be disconnected")
                continuation.resume(Result.failure(IllegalStateException("Billing client is not ready. Service connection is disconnected.")))
                return@suspendCancellableCoroutine
            }
            
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)  // INAPP type for one-time purchases
                    .build()
            }
            
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            
            billingClient.queryProductDetailsAsync(
                params
            ) { billingResult, productDetailsResult ->
                val productDetailsList = productDetailsResult?.productDetailsList ?: emptyList()
                Log.d("BillingRepository", "Query one-time products result: responseCode=${billingResult.responseCode}, products=${productDetailsList.size}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isEmpty()) {
                        android.util.Log.w("BillingRepository", "No one-time products found for IDs: ${productIds.joinToString()}")
                        continuation.resume(Result.failure(
                            Exception("No one-time products found. Make sure INAPP products exist in Play Console with IDs: ${productIds.joinToString()}")
                        ))
                    } else {
                        productDetailsList.forEach { product ->
                            Log.d("BillingRepository", "Found one-time product: ${product.productId}")
                        }
                        continuation.resume(Result.success(productDetailsList))
                    }
                } else {
                    android.util.Log.e("BillingRepository", "Query one-time products failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})")
                    val errorMessage = when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> 
                            "One-time products not found in Play Console. Create INAPP products with IDs: ${productIds.joinToString()}"
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                            "Billing service unavailable. Make sure app is uploaded to Internal testing track."
                        else ->
                            "Failed to query one-time products: ${billingResult.debugMessage}"
                    }
                    continuation.resume(Result.failure(Exception(errorMessage)))
                }
            }
        }
    }
    
    /**
     * Launch the billing flow for a subscription purchase.
     * Handles subscription offers (base plans and offers) for Google Play Billing Library 5.0+
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        obfuscatedAccountId: String? = null,
        obfuscatedProfileId: String? = null
    ): BillingResult {
        val billingClient = billingClient
        if (billingClient == null) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("Billing client not initialized")
                .build()
        }
        
        // For subscriptions, we need to get the subscription offer details
        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        if (subscriptionOfferDetails.isNullOrEmpty()) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("No subscription offers available for product: ${productDetails.productId}")
                .build()
        }
        
        // Get the first base plan and its first offer (you can customize this logic)
        // For most cases, we'll use the first available offer
        val offerDetails = subscriptionOfferDetails.firstOrNull()
        if (offerDetails == null) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .setDebugMessage("No offer details available for product: ${productDetails.productId}")
                .build()
        }
        
        // Get the base plan ID and offer token
        val basePlanId = offerDetails.basePlanId
        val offerToken = offerDetails.offerToken
        
        // Build product details params with subscription offer
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        
        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
        obfuscatedAccountId?.let { billingFlowParamsBuilder.setObfuscatedAccountId(it) }
        obfuscatedProfileId?.let { billingFlowParamsBuilder.setObfuscatedProfileId(it) }
        val billingFlowParams = billingFlowParamsBuilder.build()
        
        // Track purchase started
        AnalyticsManager.trackPurchaseStarted(productDetails.productId, "subscription")
        
        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }
    
    /**
     * Launch the billing flow for a one-time INAPP purchase.
     * Simpler than subscriptions - no subscription offers/base plans.
     */
    fun launchOneTimePurchase(
        activity: Activity,
        productDetails: ProductDetails,
        obfuscatedAccountId: String? = null,
        obfuscatedProfileId: String? = null
    ): BillingResult {
        val billingClient = billingClient
        if (billingClient == null) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("Billing client not initialized")
                .build()
        }
        
        // For INAPP (one-time) purchases, we don't need subscription offer details
        // Just create product details params directly
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        
        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
        obfuscatedAccountId?.let { billingFlowParamsBuilder.setObfuscatedAccountId(it) }
        obfuscatedProfileId?.let { billingFlowParamsBuilder.setObfuscatedProfileId(it) }
        val billingFlowParams = billingFlowParamsBuilder.build()
        
        // Track purchase started
        AnalyticsManager.trackPurchaseStarted(productDetails.productId, "one_time")
        
        Log.d("BillingRepository", "Launching one-time purchase flow for: ${productDetails.productId}")
        
        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }
    
    /**
     * Query existing purchases (subscriptions and one-time INAPP products).
     */
    suspend fun queryPurchases(): Result<List<Purchase>> {
        return suspendCancellableCoroutine { continuation ->
            val billingClient = billingClient
            if (billingClient == null) {
                continuation.resume(Result.failure(IllegalStateException("Billing client not initialized")))
                return@suspendCancellableCoroutine
            }
            
            // Query both subscription and INAPP purchases
            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            
            val inappParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            
            val allPurchases = mutableListOf<Purchase>()
            
            // Query subscriptions first
            billingClient.queryPurchasesAsync(subsParams) { subsBillingResult, subsPurchases ->
                if (subsBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingRepository", "Found ${subsPurchases.size} subscription purchases")
                    allPurchases.addAll(subsPurchases)
                    
                    // Then query INAPP purchases
                    billingClient.queryPurchasesAsync(inappParams) { inappBillingResult, inappPurchases ->
                        if (inappBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d("BillingRepository", "Found ${inappPurchases.size} INAPP purchases")
                            allPurchases.addAll(inappPurchases)
                            
                            // Update subscription status
                            val hasActiveSubscription = allPurchases.any { 
                                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                it.products.any { productId -> productId.startsWith("weekly_") }
                            }
                            AnalyticsManager.setSubscriptionStatus(
                                if (hasActiveSubscription) "active" else "inactive"
                            )
                            
                            continuation.resume(Result.success(allPurchases))
                        } else {
                            android.util.Log.e("BillingRepository", "Failed to query INAPP purchases: ${inappBillingResult.debugMessage}")
                            // Still return subscriptions even if INAPP query fails
                            continuation.resume(Result.success(allPurchases))
                        }
                    }
                } else {
                    android.util.Log.e("BillingRepository", "Failed to query subscription purchases: ${subsBillingResult.debugMessage}")
                    continuation.resume(Result.failure(
                        Exception("Failed to query purchases: ${subsBillingResult.debugMessage}")
                    ))
                }
            }
        }
    }

    /**
     * Re-query existing purchases and acknowledge any unacknowledged PURCHASED items.
     * Useful for catching pending -> purchased transitions when the app resumes.
     */
    suspend fun reprocessExistingPurchases(): Result<Int> {
        return try {
            val result = queryPurchases()
            result.fold(
                onSuccess = { purchases ->
                    var processed = 0
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            !purchase.isAcknowledged
                        ) {
                            handlePurchase(purchase)
                            processed++
                        }
                    }
                    Result.success(processed)
                },
                onFailure = { error -> Result.failure(error) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Acknowledge a purchase.
     * This is required for subscriptions - Google Play requires acknowledgment within 3 days.
     * 
     * ⚠️ CRITICAL: Only acknowledge purchases in PURCHASED state, not PENDING.
     * The three-day acknowledgement window begins only when purchase transitions from PENDING to PURCHASED.
     */
    private fun handlePurchase(purchase: Purchase) {
        // ⚠️ CRITICAL: Only process PURCHASED purchases, not PENDING
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            android.util.Log.w("BillingRepository", "Purchase is in ${purchase.purchaseState} state, not PURCHASED. Skipping acknowledgment.")
            return
        }
        
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.d("BillingRepository", "Purchase acknowledged successfully: ${purchase.products.firstOrNull()}")
                    }
                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        // Possibly stale cache - query purchases again
                        android.util.Log.w("BillingRepository", "Acknowledgment failed with ITEM_NOT_OWNED - possibly stale cache")
                        CoroutineScope(Dispatchers.IO).launch {
                            queryPurchases() // Refresh cache
                        }
                    }
                    else -> {
                        android.util.Log.e("BillingRepository", "Failed to acknowledge purchase: ${billingResult.debugMessage} (code: ${billingResult.responseCode})")
                        // ⚠️ TODO: Implement retry logic with exponential backoff for transient errors
                    }
                }
            }
        } else {
            Log.d("BillingRepository", "Purchase already acknowledged: ${purchase.products.firstOrNull()}")
        }
    }
    
    /**
     * Check if billing is available.
     */
    fun isBillingAvailable(): Boolean {
        return billingClient?.isReady == true
    }
}
