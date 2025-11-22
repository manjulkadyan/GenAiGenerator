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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Repository for handling Google Play Billing operations.
 */
class BillingRepository(private val context: Context) {
    private var billingClient: BillingClient? = null
    
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
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
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        return billingClient?.launchBillingFlow(activity, billingFlowParams) ?: 
            BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("Billing client not initialized")
                .build()
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
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged successfully
                    }
                }
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

