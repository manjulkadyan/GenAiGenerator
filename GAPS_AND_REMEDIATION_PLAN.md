# Subscription System Gaps & Remediation Plan

**Priority Classification**:
- 🔴 **P0 (Critical)**: Must fix before production launch - impacts revenue or compliance
- 🟠 **P1 (High)**: Should fix soon - impacts user experience significantly
- 🟡 **P2 (Medium)**: Fix in next sprint - improves system reliability
- 🟢 **P3 (Low)**: Nice to have - enhances features

---

## 🔴 P0 (Critical) - Must Fix Before Launch

### 1. No Scheduled Renewal Job Backup

**Current State**: Renewal credits only granted when user launches app  
**Impact**: 
- Inactive users don't receive renewal credits
- Revenue recognition delays
- Potential subscriber dissatisfaction
- No backup if app-side check fails

**Risk**: **HIGH** - Could affect revenue and user trust

**Fix Required**: Implement scheduled Cloud Function

**Estimated Effort**: 4-6 hours

**Implementation**:

```typescript
// In functions/src/index.ts
import {onSchedule} from "firebase-functions/v2/scheduler";

/**
 * Scheduled job to check renewals for all active subscriptions.
 * Runs daily at 2 AM UTC to grant credits for renewals.
 * This is a backup to app-side checks.
 */
export const checkAllSubscriptionRenewals = onSchedule(
  {
    schedule: "0 2 * * *", // Daily at 2 AM UTC
    timeZone: "UTC",
    memory: "512MiB",
    timeoutSeconds: 540, // 9 minutes
  },
  async (event) => {
    const now = admin.firestore.Timestamp.now();
    let processedUsers = 0;
    let totalCreditsAdded = 0;
    let errors = 0;

    try {
      // Query all users with active subscriptions
      // Use cursor-based pagination for large datasets
      const usersSnapshot = await firestore.collection("users")
        .where("hasActiveSubscription", "==", true)
        .limit(100) // Process in batches
        .get();

      for (const userDoc of usersSnapshot.docs) {
        try {
          const userId = userDoc.id;
          
          // Check renewals for this user
          const result = await checkUserSubscriptionRenewalInternal(userId);
          
          if (result.success && result.processedCount > 0) {
            processedUsers++;
            totalCreditsAdded += result.totalCreditsAdded;
          }
        } catch (error) {
          console.error(`Error processing user ${userDoc.id}:`, error);
          errors++;
        }
      }

      console.log(
        `✅ Scheduled renewal check complete: ${processedUsers} users, ` +
        `${totalCreditsAdded} credits added, ${errors} errors`
      );
    } catch (error) {
      console.error("❌ Scheduled renewal job failed:", error);
      throw error;
    }
  }
);

// Add hasActiveSubscription field to users when subscription is created
// Update handleSubscriptionPurchase to set this flag
```

**Deployment**:
```bash
cd genai-android/functions
npm run build
firebase deploy --only functions:checkAllSubscriptionRenewals
```

**Testing**:
```bash
# Manually trigger the scheduled function
gcloud scheduler jobs run firebase-schedule-checkAllSubscriptionRenewals \
  --project=genaivideogenerator
```

---

### 2. No onResume Purchase Re-Query

**Current State**: Purchases only checked on app launch and initial billing connection  
**Impact**:
- Purchases requiring 3D Secure not detected until app restart
- Users with slow payment methods experience delays
- Pending→Purchased transitions missed

**Risk**: **HIGH** - Users don't receive entitlements after completing payment

**Fix Required**: Add purchase re-query in MainActivity.onResume()

**Estimated Effort**: 2-3 hours

**Implementation**:

```kotlin
// In MainActivity.kt
class MainActivity : ComponentActivity() {
    private lateinit var billingRepository: BillingRepository
    private val lifecycleScope = lifecycleScope

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize billing repository
        billingRepository = BillingRepository(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            GenAiVideoTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    AuthGate {
                        GenAiRoot()
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-query purchases when app resumes
        // This catches purchases completed while app was backgrounded
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Re-querying purchases on resume")
                billingRepository.queryPurchases().onSuccess { purchases ->
                    Log.d("MainActivity", "Found ${purchases.size} purchases on resume")
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED 
                            && !purchase.isAcknowledged) {
                            Log.d("MainActivity", "Found unacknowledged purchase: ${purchase.products.firstOrNull()}")
                            // Purchase will be handled by BillingRepository's internal logic
                        }
                    }
                }.onFailure { error ->
                    Log.e("MainActivity", "Failed to query purchases on resume", error)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error querying purchases on resume", e)
            }
        }
    }
}
```

**Testing**:
1. Start purchase flow
2. Background app during payment
3. Complete payment (with 3D Secure if possible)
4. Return to app
5. Verify purchase is detected and acknowledged

---

### 3. Production Test Credits Fallback

**Current State**: `addTestCredits` used as fallback in production  
**Impact**:
- Security vulnerability - unverified credits can be granted
- Could be exploited if handleSubscriptionPurchase fails
- No audit trail for fallback credits

**Risk**: **CRITICAL** - Potential fraud/abuse

**Fix Required**: Remove fallback or restrict to debug builds

**Estimated Effort**: 1 hour

**Implementation**:

```kotlin
// In LandingPageViewModel.kt - REMOVE THE FALLBACK
private suspend fun addCreditsForPurchase(purchase: com.android.billingclient.api.Purchase) {
    // ... existing code ...
    
    try {
        val result = functions
            .getHttpsCallable("handleSubscriptionPurchase")
            .call(data)
            .await()
        
        android.util.Log.d("LandingPageViewModel", "✅ Subscription processed successfully")
        
        _uiState.value = _uiState.value.copy(
            purchaseMessage = "Subscription purchased! ${creditsToAdd} credits added to your account."
        )
    } catch (e: Exception) {
        android.util.Log.e("LandingPageViewModel", "❌ Failed to process subscription", e)
        
        // DO NOT grant unverified credits in production
        // Instead, show error and log for manual investigation
        _uiState.value = _uiState.value.copy(
            error = "Failed to process subscription. Please contact support if credits are not added within 24 hours."
        )
        
        // Log to analytics for monitoring
        AnalyticsManager.trackEvent(
            "subscription_processing_failed",
            mapOf(
                "userId" to userId,
                "productId" to productId,
                "purchaseToken" to purchase.purchaseToken,
                "error" to (e.message ?: "unknown")
            )
        )
    }
}
```

**Also restrict addTestCredits function**:

```typescript
// In functions/src/index.ts
export const addTestCredits = onCall<{userId?: string; credits: number}>(
  async ({data, auth}) => {
    // Restrict to specific admin users in production
    if (process.env.GCLOUD_PROJECT === 'genaivideogenerator') {
      // Production environment - restrict access
      const adminUids = process.env.ADMIN_UIDS?.split(',') || [];
      if (!adminUids.includes(auth?.uid || '')) {
        throw new Error('Unauthorized: Admin access required');
      }
    }
    
    // ... rest of implementation
  }
);
```

---

## 🟠 P1 (High Priority) - Fix Soon

### 4. No Upgrade/Downgrade/Proration Support

**Current State**: Users cannot change subscription tiers  
**Impact**:
- Poor UX - users must cancel and resubscribe
- Lost revenue during subscription gap
- Competitor advantage

**Fix Required**: Add subscription update parameters

**Estimated Effort**: 6-8 hours

**Implementation**:

```kotlin
// In BillingRepository.kt
fun launchUpgradeDowngradeFlow(
    activity: Activity,
    newProductDetails: ProductDetails,
    oldPurchaseToken: String,
    prorationMode: Int, // BillingFlowParams.ProrationMode.*
    obfuscatedAccountId: String? = null
): BillingResult {
    val billingClient = billingClient ?: return createErrorResult("Billing client not initialized")
    
    val subscriptionOfferDetails = newProductDetails.subscriptionOfferDetails
    if (subscriptionOfferDetails.isNullOrEmpty()) {
        return createErrorResult("No subscription offers available")
    }
    
    val offerDetails = subscriptionOfferDetails.firstOrNull()
        ?: return createErrorResult("No offer details available")
    
    // Build subscription update params
    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
        .setOldPurchaseToken(oldPurchaseToken)
        .setSubscriptionReplacementMode(prorationMode)
        .build()
    
    // Build product details params
    val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
        .setProductDetails(newProductDetails)
        .setOfferToken(offerDetails.offerToken)
        .build()
    
    val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(listOf(productDetailsParams))
        .setSubscriptionUpdateParams(subscriptionUpdateParams)
    
    obfuscatedAccountId?.let { billingFlowParamsBuilder.setObfuscatedAccountId(it) }
    
    val billingFlowParams = billingFlowParamsBuilder.build()
    
    AnalyticsManager.trackEvent("subscription_change_started", mapOf(
        "from_token" to oldPurchaseToken,
        "to_product" to newProductDetails.productId,
        "proration_mode" to prorationMode
    ))
    
    return billingClient.launchBillingFlow(activity, billingFlowParams)
}
```

**UI Flow**:
```kotlin
// In subscription UI
if (userHasActiveSubscription) {
    // Show "Change Plan" button instead of "Subscribe"
    Button(onClick = {
        val currentToken = getCurrentSubscriptionToken()
        val prorationMode = when {
            isUpgrade -> BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
            isDowngrade -> BillingFlowParams.ProrationMode.DEFERRED
            else -> BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        }
        
        billingRepository.launchUpgradeDowngradeFlow(
            activity = activity,
            newProductDetails = newPlanDetails,
            oldPurchaseToken = currentToken,
            prorationMode = prorationMode,
            obfuscatedAccountId = userId
        )
    }) {
        Text(if (isUpgrade) "Upgrade Plan" else "Change Plan")
    }
}
```

---

### 5. No Pending Purchase State Handling

**Current State**: PENDING purchases are skipped  
**Impact**:
- Users with pending purchases don't see status
- No notification when pending→purchased transition occurs
- Poor UX for payment methods requiring approval

**Fix Required**: Track and monitor pending purchases

**Estimated Effort**: 4-5 hours

**Implementation**:

```kotlin
// Add pending purchase tracking to BillingRepository
private val _pendingPurchases = MutableStateFlow<Set<String>>(emptySet())
val pendingPurchases: StateFlow<Set<String>> = _pendingPurchases.asStateFlow()

private fun handlePurchase(purchase: Purchase) {
    when (purchase.purchaseState) {
        Purchase.PurchaseState.PENDING -> {
            // Track pending purchase
            _pendingPurchases.value = _pendingPurchases.value + purchase.purchaseToken
            
            android.util.Log.w(
                "BillingRepository",
                "Purchase is PENDING: ${purchase.products.firstOrNull()}. " +
                "Will be processed when state changes to PURCHASED."
            )
            
            // Store in local database for tracking
            storePendingPurchase(purchase)
            
            // Emit event to show UI message
            _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Pending(purchase))
        }
        
        Purchase.PurchaseState.PURCHASED -> {
            // Remove from pending if it was there
            _pendingPurchases.value = _pendingPurchases.value - purchase.purchaseToken
            
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    // ... handle result
                }
            }
        }
        
        else -> {
            android.util.Log.w(
                "BillingRepository",
                "Purchase in unexpected state: ${purchase.purchaseState}"
            )
        }
    }
}

// Add to PurchaseUpdateEvent sealed class
sealed class PurchaseUpdateEvent {
    data class Success(val purchase: Purchase) : PurchaseUpdateEvent()
    data class Pending(val purchase: Purchase) : PurchaseUpdateEvent()
    data class Error(val billingResult: BillingResult) : PurchaseUpdateEvent()
    object UserCancelled : PurchaseUpdateEvent()
}
```

**UI Update**:
```kotlin
// In LandingPageViewModel
when (event) {
    is PurchaseUpdateEvent.Pending -> {
        _uiState.value = _uiState.value.copy(
            isPurchaseInProgress = true,
            purchaseMessage = "Payment is being processed. You'll receive credits when payment is confirmed."
        )
    }
    // ... other cases
}
```

---

## 🟡 P2 (Medium Priority) - Next Sprint

### 6. No obfuscatedProfileId

**Current State**: Only account ID set, no profile ID  
**Impact**: Cannot distinguish between multiple profiles/devices per account

**Fix Required**: Add profile ID parameter

**Estimated Effort**: 1-2 hours

```kotlin
// In BillingRepository.kt
fun launchBillingFlow(
    activity: Activity,
    productDetails: ProductDetails,
    obfuscatedAccountId: String? = null,
    obfuscatedProfileId: String? = null  // NEW
): BillingResult {
    // ... existing code ...
    
    val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(listOf(productDetailsParams))
    
    obfuscatedAccountId?.let { billingFlowParamsBuilder.setObfuscatedAccountId(it) }
    obfuscatedProfileId?.let { billingFlowParamsBuilder.setObfuscatedProfileId(it) }  // NEW
    
    val billingFlowParams = billingFlowParamsBuilder.build()
    
    return billingClient.launchBillingFlow(activity, billingFlowParams)
}
```

---

### 7. Limited Offer Selection (First Offer Only)

**Current State**: Always uses first offer  
**Impact**: Cannot leverage trials, promotional pricing, or multiple base plans

**Fix Required**: Add offer selection logic

**Estimated Effort**: 3-4 hours

```kotlin
// In BillingRepository.kt
fun launchBillingFlow(
    activity: Activity,
    productDetails: ProductDetails,
    offerToken: String? = null,  // NEW: Allow specific offer selection
    obfuscatedAccountId: String? = null
): BillingResult {
    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
    if (subscriptionOfferDetails.isNullOrEmpty()) {
        return createErrorResult("No subscription offers available")
    }
    
    // Select offer by token if provided, otherwise use first
    val offerDetails = if (offerToken != null) {
        subscriptionOfferDetails.find { it.offerToken == offerToken }
            ?: subscriptionOfferDetails.firstOrNull()
    } else {
        subscriptionOfferDetails.firstOrNull()
    }
    
    // ... rest of implementation
}
```

---

### 8. No Grace/Hold/Pause State in App UI

**Current State**: App doesn't reflect subscription lifecycle states  
**Impact**: Users don't see warnings about payment issues

**Fix Required**: Add subscription state UI

**Estimated Effort**: 4-5 hours

```kotlin
// Create subscription status data class
data class SubscriptionStatus(
    val state: String,  // active, grace_period, on_hold, paused, canceled
    val expiryDate: Date?,
    val needsPaymentUpdate: Boolean
)

// Add to ViewModel
private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus?>(null)
val subscriptionStatus: StateFlow<SubscriptionStatus?> = _subscriptionStatus.asStateFlow()

// Fetch from server
suspend fun loadSubscriptionStatus() {
    try {
        val result = functions
            .getHttpsCallable("getUserSubscriptionStatus")
            .call(mapOf("userId" to userId))
            .await()
        
        // Parse and update UI
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load subscription status", e)
    }
}
```

---

## 🟢 P3 (Low Priority) - Nice to Have

### 9. No Background Sync Job in App

**Current State**: All sync requires app launch  
**Impact**: Relies entirely on app launches for renewal checks

**Fix Required**: Add WorkManager periodic sync

**Estimated Effort**: 3-4 hours

```kotlin
// Create WorkManager worker
class SubscriptionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            SubscriptionRenewalManager.checkRenewals().getOrThrow()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule periodic work
val syncRequest = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(
    repeatInterval = 24,
    repeatIntervalTimeUnit = TimeUnit.HOURS
).setConstraints(
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
).build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "subscription_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
```

---

### 10. No Price-Change Consent Handling

**Current State**: No specific handling for price change notifications  
**Impact**: May not handle price changes gracefully

**Fix Required**: Add RTDN notification type handling

**Estimated Effort**: 2 hours

```typescript
// In handlePlayRtdn function
const notificationType = subNotification.notificationType;

switch (notificationType) {
  case 6: // SUBSCRIPTION_PRICE_CHANGE_CONFIRMED
    updateData.priceChangeAccepted = true;
    updateData.priceChangeDate = now;
    console.log(`✅ Price change confirmed for ${productId}`);
    break;
  
  // ... other notification types
}
```

---

### 11. No Top-Up Product Support

**Current State**: Only subscriptions supported  
**Impact**: Cannot offer one-time credit purchases

**Fix Required**: Add one-time product purchase flow

**Estimated Effort**: 6-8 hours

*(Implementation similar to subscription flow but using `ProductType.INAPP`)*

---

## Implementation Priority Timeline

### Week 1 (Critical)
- Day 1-2: Implement scheduled renewal job (Gap #1)
- Day 3: Add onResume purchase re-query (Gap #2)
- Day 4: Remove test credits fallback (Gap #3)
- Day 5: Testing and bug fixes

### Week 2 (High Priority)
- Day 1-2: Implement upgrade/downgrade support (Gap #4)
- Day 3-4: Add pending purchase handling (Gap #5)
- Day 5: Testing

### Week 3 (Medium Priority)
- Day 1: Add obfuscatedProfileId (Gap #6)
- Day 2: Improve offer selection (Gap #7)
- Day 3-4: Add subscription state UI (Gap #8)
- Day 5: Testing

### Week 4 (Low Priority + Testing)
- Day 1-2: Optional enhancements (Gaps #9-11)
- Day 3-5: End-to-end testing, load testing, production verification

---

## Success Metrics

### Pre-Launch Requirements (Must Complete)
- ✅ All P0 gaps fixed
- ✅ End-to-end purchase flow tested
- ✅ RTDN webhook verified
- ✅ Scheduled job deployed and tested
- ✅ Monitoring and alerts configured

### Post-Launch Monitoring
- Purchase success rate > 95%
- Acknowledgement within 3 days: 100%
- Renewal credit grant within 24 hours: 100%
- RTDN webhook success rate > 99%
- Average purchase-to-credit time < 30 seconds

---

## Conclusion

**Total Gaps**: 11 identified  
**Critical (P0)**: 3 gaps - **Must fix before launch**  
**High (P1)**: 2 gaps - **Fix in first month**  
**Medium (P2)**: 3 gaps - **Fix in quarter**  
**Low (P3)**: 3 gaps - **Nice to have**

**Estimated Total Effort**: 4-5 weeks for complete implementation and testing

**Recommended Launch Criteria**: Fix all P0 gaps + deploy RTDN + complete end-to-end testing

