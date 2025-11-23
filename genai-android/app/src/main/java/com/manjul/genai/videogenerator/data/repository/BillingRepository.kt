package com.manjul.genai.videogenerator.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
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
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Success(purchase))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseUpdates.tryEmit(PurchaseUpdateEvent.UserCancelled)
            }
            else -> {
                _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Error(billingResult))
            }
        }
    }
    
    /**
     * Initialize the billing client.
     */
    fun initialize(): Flow<BillingResult> = callbackFlow {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                trySend(billingResult)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Billing client is ready
                }
            }
            
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request
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
                continuation.resume(Result.failure(IllegalStateException("Billing client not initialized")))
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
            ) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(productDetailsList))
                } else {
                    continuation.resume(Result.failure(
                        Exception("Failed to query product details: ${billingResult.debugMessage}")
                    ))
                }
            }
        }
    }
    
    /**
     * Launch the billing flow for a subscription purchase.
     * Handles subscription offers (base plans and offers) for Google Play Billing Library 5.0+
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
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
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        
        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }
    
    /**
     * Query existing purchases.
     */
    suspend fun queryPurchases(): Result<List<Purchase>> {
        return suspendCancellableCoroutine { continuation ->
            val billingClient = billingClient
            if (billingClient == null) {
                continuation.resume(Result.failure(IllegalStateException("Billing client not initialized")))
                return@suspendCancellableCoroutine
            }
            
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            
            billingClient.queryPurchasesAsync(
                params
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(purchases))
                } else {
                    continuation.resume(Result.failure(
                        Exception("Failed to query purchases: ${billingResult.debugMessage}")
                    ))
                }
            }
        }
    }
    
    /**
     * Acknowledge a purchase.
     * This is required for subscriptions - Google Play requires acknowledgment within 3 days.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        android.util.Log.d("BillingRepository", "Purchase acknowledged successfully: ${purchase.products.firstOrNull()}")
                    } else {
                        android.util.Log.e("BillingRepository", "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                android.util.Log.d("BillingRepository", "Purchase already acknowledged: ${purchase.products.firstOrNull()}")
            }
        }
    }
    
    /**
     * Check if billing is available.
     */
    fun isBillingAvailable(): Boolean {
        return billingClient?.isReady == true
    }
}

