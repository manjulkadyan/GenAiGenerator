# Subscription Purchase Flow Documentation

## Current Flow (After Fix)

### 1. User Initiates Purchase
```
User clicks "Subscribe" button
  ↓
LandingPageViewModel.purchasePlan()
  ↓
BillingRepository.launchBillingFlow()
  ↓
Google Play Billing UI shown
```

### 2. Purchase Processing
```
User completes purchase in Google Play
  ↓
Google Play processes payment
  ↓
BillingRepository.purchasesUpdatedListener receives callback
  ↓
BillingRepository.handlePurchase() acknowledges purchase
  ↓
PurchaseUpdateEvent.Success emitted
```

### 3. Credits Addition (NEW - Fixed)
```
LandingPageViewModel.observePurchaseUpdates() receives Success event
  ↓
addCreditsForPurchase() called
  ↓
1. Extract product ID from purchase
2. Find matching SubscriptionPlan from config
3. Get credits amount from plan
4. Get current user ID from Firebase Auth
5. Call Firebase Function: addTestCredits
  ↓
Firebase Function adds credits to user document in Firestore
  ↓
UI updates to show: "Subscription purchased! X credits added to your account."
```

## Bug That Was Fixed

### Problem
**Before the fix:**
- Purchase was successful ✅
- Purchase was acknowledged ✅
- Success message shown: "Subscription purchased successfully!" ✅
- **BUT: Credits were NOT added to user account** ❌

### Root Cause
The `observePurchaseUpdates()` function in `LandingPageViewModel` only updated the UI state to show a success message, but never called any code to actually add credits to the user's Firestore document.

### Solution
Added `addCreditsForPurchase()` function that:
1. Extracts the product ID from the purchase
2. Finds the matching subscription plan from the config
3. Gets the credits amount from the plan
4. Calls the Firebase Function `addTestCredits` to add credits to the user's account

## Code Changes

### File: `LandingPageViewModel.kt`

**Added imports:**
```kotlin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
```

**Added properties:**
```kotlin
private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
private val functions: FirebaseFunctions by lazy { Firebase.functions }
```

**Modified `observePurchaseUpdates()`:**
- Added call to `addCreditsForPurchase()` when purchase succeeds

**Added new function:**
```kotlin
private suspend fun addCreditsForPurchase(productId: String?)
```
- Finds subscription plan by product ID
- Gets credits amount from plan
- Calls Firebase Function to add credits
- Updates UI message to show credits were added

## Firebase Function Used

### `addTestCredits`
**Location:** `genai-android/functions/src/index.ts`

**Parameters:**
```typescript
{
  userId?: string,  // Optional - defaults to auth.uid
  credits: number   // Required - number of credits to add
}
```

**What it does:**
1. Gets user ID from auth or parameter
2. Gets or creates user document in Firestore
3. Increments credits field by the specified amount
4. Returns success with new balance

**Example call:**
```kotlin
val data = hashMapOf(
    "userId" to userId,
    "credits" to creditsToAdd
)
functions.getHttpsCallable("addTestCredits").call(data).await()
```

## Testing the Fix

### Steps to Test:
1. Deploy Firebase Functions:
   ```bash
   cd genai-android/functions
   npm run build
   firebase deploy --only functions:addTestCredits
   ```

2. Make a test purchase in the app

3. Verify:
   - ✅ Purchase completes successfully
   - ✅ Success message shows: "Subscription purchased! X credits added to your account."
   - ✅ Check Firestore: `users/{userId}/credits` should be incremented
   - ✅ Check app UI: Credits balance should update in real-time

### Expected Behavior:
- Before purchase: User has X credits
- After purchase: User has X + plan.credits credits
- UI message: Shows both purchase success and credits added

## Error Handling

The fix includes error handling:
- If product ID is null → Logs error, no credits added
- If subscription plan not found → Logs error, no credits added
- If user not authenticated → Logs error, no credits added
- If Firebase Function call fails → Logs error, but doesn't show error to user (purchase was successful)

**Note:** If credits fail to add, they can be added manually via Firebase Console or by calling the function again.

## Subscription Renewals

**✅ IMPLEMENTED:** Automatic subscription renewal handling is now set up!

See `SUBSCRIPTION_RENEWAL_SETUP.md` for complete details on:
- How renewals are automatically processed
- Scheduled Cloud Function that runs daily
- Client-side subscription status checking
- Handling missed renewals

### Quick Summary:
1. **Initial Purchase:** `handleSubscriptionPurchase()` stores subscription info and adds credits
2. **Weekly Renewals:** `checkSubscriptionRenewals()` scheduled function runs daily and adds credits automatically
3. **App Launch:** App checks subscription status and syncs with server

## Future Improvements

1. **Retry Logic:** Add retry mechanism if Firebase Function call fails
2. **Transaction Safety:** Use Firestore transactions to ensure credits are added atomically
3. **Google Play RTDN:** Use Google Play Real-time Developer Notifications for instant renewal notifications (requires OAuth setup)
4. **Credit History:** Track credit additions in a separate collection for audit purposes
5. **Notification:** Send push notification when credits are added

