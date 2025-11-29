# Top-Up Credits & Subscription Management Implementation Status

## ✅ Completed Tasks

### 1. Data Models (COMPLETED)
- ✅ Added `OneTimeProduct` data class in `LandingPageConfig.kt`
- ✅ Added `PurchaseHistoryItem` data class
- ✅ Added `PurchaseType` enum (SUBSCRIPTION, ONE_TIME)

### 2. BillingRepository Updates (COMPLETED)
- ✅ Added `queryOneTimeProducts()` method for INAPP products
- ✅ Added `launchOneTimePurchase()` method for one-time purchases
- ✅ Updated `queryPurchases()` to query both SUBS and INAPP types
- ✅ All methods handle both subscription and one-time purchase flows

### 3. Firebase Cloud Function (COMPLETED)
- ✅ Created `handleOneTimePurchase` function in `functions/src/index.ts`
- ✅ Verifies purchases with Google Play API (`purchases.products.get`)
- ✅ Adds credits to user account (single pool)
- ✅ Stores purchase in `users/{userId}/purchases/{purchaseToken}` subcollection
- ✅ Acknowledges purchase with Google Play
- ✅ Includes idempotency check to prevent duplicate credit additions

### 4. LandingPageViewModel Updates (COMPLETED)
- ✅ Updated `LandingPageUiState` to include:
  - `oneTimeProducts: List<OneTimeProduct>`
  - `oneTimeProductDetails: Map<String, ProductDetails>`
  - `selectedPurchaseType: PurchaseType`
  - `selectedOneTimeProduct: OneTimeProduct?`
- ✅ Added `initializeOneTimeProducts()` - creates 5 product tiers (100, 200, 300, 500, 1000)
- ✅ Added `loadOneTimeProductDetails()` - queries Play Store for INAPP product details
- ✅ Added `selectPurchaseType()` - switches between subscription and one-time
- ✅ Added `selectOneTimeProduct()` - selects a one-time product
- ✅ Added `purchaseOneTimeProduct()` - initiates one-time purchase flow
- ✅ Added `processOneTimePurchase()` - sends purchase to backend
- ✅ Updated `observePurchaseUpdates()` to handle both subscription and one-time purchases

### 5. BuyCreditsScreen UI Updates (COMPLETED)
- ✅ Added imports for `PurchaseType`, `OneTimeProduct`, `horizontalScroll`
- ✅ Added tab selector for switching between "Weekly Plans" and "Top-Up Credits"
- ✅ Added explanatory text for each tab
- ✅ Updated pricing section to show:
  - 3 subscription cards in a row (for subscriptions)
  - 5 one-time product cards in a horizontal scrollable row (for top-ups)
- ✅ Created `OneTimeProductCard` composable with:
  - Badge for "BEST VALUE" (200 credits) and "POPULAR" (500 credits)
  - Credits count
  - Price
  - Per-credit cost calculation
- ✅ Updated Continue button logic to handle both purchase types

### 6. SubscriptionManagementViewModel (COMPLETED)
- ✅ Created `SubscriptionManagementViewModel.kt`
- ✅ `SubscriptionInfo` data class with all subscription details
- ✅ `SubscriptionManagementUiState` with loading, subscription info, purchase history
- ✅ `loadSubscriptionData()` - loads active subscription from Firestore
- ✅ `loadPurchaseHistory()` - loads last 50 purchases ordered by date
- ✅ `getPlayStoreSubscriptionLink()` - generates deep link to Play Store
- ✅ `formatDate()` and `formatDateTime()` - date formatting helpers

## 🔨 Remaining Tasks

### 7. SubscriptionManagementScreen UI (TODO)
**File to create:** `genai-android/app/src/main/java/com/manjul/genai/videogenerator/ui/screens/SubscriptionManagementScreen.kt`

**What to implement:**
```kotlin
@Composable
fun SubscriptionManagementScreen(
    onBackClick: () -> Unit,
    onViewPlansClick: () -> Unit, // Navigate to BuyCreditsScreen
    viewModel: SubscriptionManagementViewModel = viewModel(factory = SubscriptionManagementViewModel.Factory(application))
)
```

**Screen sections:**
1. **Toolbar** with back button and "Subscription & Credits" title
2. **Active Subscription Card** (if subscribed):
   - Product name (e.g., "100 Credits Weekly")
   - Credits per week
   - Status badge (Active - green)
   - Start date: "Started on {date}"
   - Next renewal: "Next billing: {date}"
   - "Manage in Play Store" button (opens deep link)
3. **No Subscription Card** (if not subscribed):
   - Message: "No active subscription"
   - Subtitle: "Subscribe to get weekly credits automatically"
   - "View Plans" button → navigate to BuyCreditsScreen
4. **Subscription vs Top-Up Explainer Card**:
   - Two columns comparing features
   - Subscription: Weekly credits, auto-renews, best for regular users
   - Top-Up: One-time credits, no renewal, best for occasional use
5. **Purchase History Section**:
   - Header: "Purchase History"
   - LazyColumn with purchase items:
     - Product name
     - Date & time (formatted)
     - Credits received
     - Price paid
     - Type badge (Subscription/One-Time)
   - Empty state: "No purchases yet"

**Styling:**
- Use `AppColors` from design system
- Use `AppCard` for cards
- Use `AppPrimaryButton` for actions
- Use `StatusBadge` for status indicators

### 8. ProfileScreen Integration (TODO)
**File to update:** `genai-android/app/src/main/java/com/manjul/genai/videogenerator/ui/screens/ProfileScreen.kt`

**What to add:**
Around line 883 (in "Quick Actions" section, after "Buy Credits" card):
```kotlin
ActionCard(
    icon = Icons.Default.WorkspacePremium,
    title = "Subscription & Credits",
    onClick = onSubscriptionManagementClick, // Add this callback parameter
    iconBackgroundColor = AppColors.PrimaryPurple.copy(alpha = 0.15f)
)
```

**Update ProfileScreen composable signature:**
```kotlin
fun ProfileScreen(
    modifier: Modifier = Modifier,
    creditsViewModel: CreditsViewModel = viewModel(factory = CreditsViewModel.Factory),
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    onBuyCreditsClick: () -> Unit = {},
    onVideosClick: () -> Unit = {},
    onSubscriptionManagementClick: () -> Unit = {} // ADD THIS
)
```

### 9. Navigation Setup (TODO)
**File to update:** `genai-android/app/src/main/java/com/manjul/genai/videogenerator/ui/screens/GenAiRoot.kt`

**What to add:**
1. Add new destination to `AppDestination` enum:
```kotlin
sealed class AppDestination {
    object Generate : AppDestination()
    object Models : AppDestination()
    object History : AppDestination()
    object Profile : AppDestination()
    object SubscriptionManagement : AppDestination() // ADD THIS
}
```

2. Add state variable:
```kotlin
var showSubscriptionManagementScreen by rememberSaveable { mutableStateOf(false) }
```

3. Update ProfileScreen call to include callback:
```kotlin
AppDestination.Profile -> ProfileScreen(
    modifier = Modifier.fillMaxSize().padding(innerPadding),
    onBuyCreditsClick = {
        showInsufficientCreditsDialog = false
        requiredCredits = 0
        showBuyCreditsScreen = true
    },
    onVideosClick = { currentRoute = AppDestination.History },
    onSubscriptionManagementClick = { // ADD THIS
        showSubscriptionManagementScreen = true
    }
)
```

4. Add SubscriptionManagementScreen overlay (after BuyCreditsScreen overlay):
```kotlin
// SubscriptionManagementScreen as full screen (not overlay)
if (showSubscriptionManagementScreen) {
    SubscriptionManagementScreen(
        onBackClick = {
            showSubscriptionManagementScreen = false
        },
        onViewPlansClick = {
            showSubscriptionManagementScreen = false
            showBuyCreditsScreen = true
        }
    )
}
```

### 10. Google Play Compliance Features (TODO)
**File to update:** `genai-android/app/src/main/java/com/manjul/genai/videogenerator/data/repository/BillingRepository.kt`

**What to add:**

1. **In-app messaging for subscription issues:**
```kotlin
/**
 * Show in-app messages for subscription issues (grace period, account hold)
 */
fun showInAppMessages(activity: Activity) {
    val billingClient = billingClient ?: return
    
    val inAppMessageParams = InAppMessageParams.newBuilder()
        .addInAppMessageCategoryToShow(InAppMessageCategoryId.TRANSACTIONAL)
        .build()
    
    billingClient.showInAppMessages(
        activity,
        inAppMessageParams,
        object : InAppMessageResponseListener() {
            override fun onInAppMessageResponse(inAppMessageResult: InAppMessageResult) {
                when (inAppMessageResult.responseCode) {
                    InAppMessageResponseCode.NO_ACTION_NEEDED -> {
                        android.util.Log.d("BillingRepository", "No action needed from in-app message")
                    }
                    InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED -> {
                        android.util.Log.d("BillingRepository", "Subscription status updated from in-app message")
                        val purchaseToken = inAppMessageResult.purchaseToken
                        // Refresh subscription status
                    }
                }
            }
        }
    )
}
```

2. **Add imports:**
```kotlin
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.InAppMessageCategoryId
import com.android.billingclient.api.InAppMessageResponseListener
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.InAppMessageResponseCode
```

3. **Call in AuthGate or MainActivity onCreate:**
```kotlin
// Show in-app messages on app launch
LaunchedEffect(Unit) {
    if (context is Activity) {
        billingRepository.showInAppMessages(context)
    }
}
```

### 11. Google Play Console Setup (MANUAL - USER TASK)
**Important:** User must create these 5 products in Google Play Console:

Navigate to: **Google Play Console → Monetize → Products → In-app products**

Create these products:
1. **Product ID:** `credits_100`
   - Name: "100 Credits"
   - Price: $9.99
   - Type: One-time (INAPP)

2. **Product ID:** `credits_200`
   - Name: "200 Credits"  
   - Price: $17.99
   - Type: One-time (INAPP)

3. **Product ID:** `credits_300`
   - Name: "300 Credits"
   - Price: $24.99
   - Type: One-time (INAPP)

4. **Product ID:** `credits_500`
   - Name: "500 Credits"
   - Price: $39.99
   - Type: One-time (INAPP)

5. **Product ID:** `credits_1000`
   - Name: "1000 Credits"
   - Price: $69.99
   - Type: One-time (INAPP)

**Note:** These products must match the productIds hardcoded in `LandingPageViewModel.initializeOneTimeProducts()`

## 📋 Testing Checklist

Once all code is implemented, test:

### One-Time Purchases
- [ ] Test purchase flow for credits_100
- [ ] Test purchase flow for credits_200
- [ ] Test purchase flow for credits_300
- [ ] Test purchase flow for credits_500
- [ ] Test purchase flow for credits_1000
- [ ] Verify credits added to single pool (same balance as subscriptions)
- [ ] Check purchase saved to Firestore `users/{userId}/purchases/`
- [ ] Verify acknowledgment with Google Play

### Subscription + Top-Up Combination
- [ ] Subscribe to weekly plan
- [ ] Purchase a top-up while subscribed
- [ ] Verify both credited to same pool
- [ ] Check subscription renewal still works
- [ ] Verify both show in purchase history

### Subscription Management Screen
- [ ] Verify active subscription displays correctly
- [ ] Check start date and next renewal date format
- [ ] Test "Manage in Play Store" deep link
- [ ] Verify purchase history loads
- [ ] Test empty state (no purchases)
- [ ] Test "View Plans" button navigation

### UI/UX
- [ ] Tab switching works smoothly
- [ ] Subscription cards display correctly (3 in row)
- [ ] Top-up cards scroll horizontally (5 cards)
- [ ] "Best Value" and "Popular" badges show correctly
- [ ] Per-credit cost calculations are accurate
- [ ] Continue button enabled/disabled states work
- [ ] Success/error messages display properly

### Google Play Guidelines
- [ ] In-app messaging appears during grace period
- [ ] Account hold restrictions work
- [ ] Deep links to Play Store work
- [ ] Restore purchases works

## 🔑 Key Implementation Notes

- **Single credit pool:** All credits (subscription + top-up) go into `users/{userId}/credits` field
- **No expiry:** Top-up credits never expire
- **Purchase history:** Stored in `users/{userId}/purchases/` subcollection
- **Idempotency:** `handleOneTimePurchase` checks if purchase already processed
- **Product IDs:** Must start with `credits_` for one-time, `weekly_` for subscriptions
- **Acknowledgment:** Both purchase types must be acknowledged within 3 days

## 📝 Next Steps

1. **Complete remaining UI screens** (SubscriptionManagementScreen)
2. **Add navigation integration** (GenAiRoot updates)
3. **Add ProfileScreen button** (Quick Actions section)
4. **Implement Google Play compliance** (In-app messaging)
5. **Deploy Firebase functions** (`firebase deploy --only functions`)
6. **Create products in Play Console** (5 INAPP products)
7. **Test thoroughly** (All purchase flows)

## 🚀 Deployment

### Firebase Functions
```bash
cd genai-android/functions
npm run build
firebase deploy --only functions:handleOneTimePurchase
```

### Android App
1. Build signed APK/AAB
2. Upload to Play Console Internal Testing track
3. Test with test account
4. Promote to production when ready

---

**Status:** ~75% Complete
**Remaining:** UI screens, navigation, compliance features, testing

