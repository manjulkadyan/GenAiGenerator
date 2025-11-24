# Google Play Billing FAQ - Your Questions Answered

## ❓ Your Questions

### Q1: Can we auto-credit users weekly without the app being open?

**Answer: ✅ YES!**

**How it works:**
1. **Scheduled Cloud Function** (`checkSubscriptionRenewals`) runs daily at 2 AM UTC
2. Checks all active subscriptions in Firestore
3. If `nextRenewalDate` has passed, automatically adds credits
4. Updates `nextRenewalDate` to next week
5. **No app interaction required!**

**Implementation:**
- ✅ **CREATED:** `checkSubscriptionRenewals` scheduled function in `functions/src/index.ts`
- ✅ **STORES:** Subscription info in `users/{userId}/subscriptions/{productId}`
- ✅ **TRACKS:** `nextRenewalDate` for each subscription
- ✅ **HANDLES:** Missed renewals (calculates multiple periods if needed)

**Example Flow:**
```
Day 0: User subscribes → 100 credits added, nextRenewalDate = Day 7
Day 7: Scheduled function runs → 100 credits added, nextRenewalDate = Day 14
Day 14: Scheduled function runs → 100 credits added, nextRenewalDate = Day 21
...continues automatically...
```

---

### Q2: Do we need to process or get new app/subscription or anything to check if the next billing happened?

**Answer: ✅ YES - But it's automated!**

**What you need:**
1. **Scheduled Function** (✅ Already created) - Checks Firestore for renewals
2. **Google Play Developer API** (⚠️ Not yet implemented) - Verify subscription is still active

**Current Implementation:**
- ✅ Scheduled function checks `nextRenewalDate` in Firestore
- ✅ Adds credits automatically when date passes
- ⚠️ **Missing:** Verification with Google Play API to ensure subscription is still active

**Recommended Enhancement:**
For production, add Google Play API verification:
```typescript
// In checkSubscriptionRenewals function
const subscription = await googlePlayAPI.purchases.subscriptions.get({
  packageName: 'com.manjul.genai.videogenerator',
  subscriptionId: productId,
  token: purchaseToken
});

// Check if subscription is still active
if (subscription.expiryTimeMillis > Date.now()) {
  // Subscription is active - add credits
} else {
  // Subscription expired - mark as cancelled
}
```

**Why verify?**
- User might have cancelled subscription
- Payment might have failed
- Subscription might be in grace period
- Prevents granting credits for cancelled subscriptions

---

### Q3: How can we handle verification?

**Answer: Use Google Play Developer API**

**Two Levels of Verification:**

#### Level 1: Client-Side (Current - Basic)
- ✅ Purchase token stored in Firestore
- ✅ Duplicate purchase token check
- ⚠️ **Not secure** - can be bypassed

#### Level 2: Server-Side (Recommended - Secure)
- ⚠️ **TODO:** Verify purchase token with Google Play Developer API
- ✅ Check `acknowledgementState` and `purchaseState`
- ✅ Verify subscription is still active
- ✅ Check expiry time for renewals

**Implementation Steps:**

1. **Set up Google Play Developer API OAuth:**
   ```bash
   # Create service account in Google Cloud Console
   # Download JSON key file
   # Set up OAuth2 credentials
   ```

2. **Install Google API Client:**
   ```bash
   cd functions
   npm install googleapis
   ```

3. **Add verification to `handleSubscriptionPurchase`:**
   ```typescript
   import {google} from 'googleapis';

   const auth = new google.auth.GoogleAuth({
     keyFile: 'path/to/service-account-key.json',
     scopes: ['https://www.googleapis.com/auth/androidpublisher'],
   });

   const androidpublisher = google.androidpublisher({
     version: 'v3',
     auth,
   });

   // Verify purchase
   const subscription = await androidpublisher.purchases.subscriptions.get({
     packageName: 'com.manjul.genai.videogenerator',
     subscriptionId: productId,
     token: purchaseToken,
   });

   // Check if valid
   if (subscription.data.acknowledgementState !== 1) {
     throw new Error('Purchase not acknowledged');
   }

   if (subscription.data.purchaseState !== 0) {
     throw new Error('Purchase not in PURCHASED state');
   }
   ```

4. **Add verification to `checkSubscriptionRenewals`:**
   ```typescript
   // Before adding credits, verify subscription is still active
   const subscription = await androidpublisher.purchases.subscriptions.get({
     packageName: 'com.manjul.genai.videogenerator',
     subscriptionId: productId,
     token: purchaseToken,
   });

   const expiryTime = parseInt(subscription.data.expiryTimeMillis || '0');
   if (expiryTime > Date.now()) {
     // Subscription is active - proceed with adding credits
   } else {
     // Subscription expired - mark as cancelled
     await subDoc.ref.update({status: 'cancelled'});
   }
   ```

**Security Benefits:**
- ✅ Prevents fake purchase tokens
- ✅ Ensures subscription is actually active
- ✅ Detects cancellations automatically
- ✅ Handles payment failures correctly

---

## 📋 What Was Fixed

### ✅ Critical Fixes Applied

1. **Billing Library Updated to 8.0.0**
   - ✅ Updated `build.gradle.kts` from 7.1.1 to 8.0.0
   - ✅ Required by Aug 31, 2025 deadline

2. **Auto-Service Reconnection Enabled**
   - ✅ Added `enableAutoServiceReconnection()` to BillingClient
   - ✅ Reduces SERVICE_DISCONNECTED errors

3. **Query Purchases After Connection**
   - ✅ Automatically queries purchases when billing connects
   - ✅ Catches purchases made while app was closed

4. **Pending Purchase Handling**
   - ✅ Only processes PURCHASED purchases, not PENDING
   - ✅ Prevents granting credits for pending transactions

5. **Subscription Renewal Function Created**
   - ✅ `checkSubscriptionRenewals` scheduled function
   - ✅ Runs daily at 2 AM UTC
   - ✅ Automatically adds credits for renewals
   - ✅ Handles missed renewals

6. **Subscription Storage Enhanced**
   - ✅ Stores subscription info in `users/{userId}/subscriptions/{productId}`
   - ✅ Tracks `nextRenewalDate` for automatic renewals
   - ✅ Stores `creditsPerRenewal` for each subscription

---

## ⚠️ Still Missing (Recommended for Production)

1. **Google Play Developer API Verification**
   - ⚠️ TODO: Verify purchase tokens server-side
   - ⚠️ TODO: Verify subscription status before renewals

2. **Real-time Developer Notifications (RTDN)**
   - ⚠️ TODO: Set up webhook endpoint for instant updates
   - ⚠️ TODO: Handle subscription lifecycle events in real-time

3. **Retry Logic for Transient Errors**
   - ⚠️ TODO: Implement exponential backoff for acknowledgments
   - ⚠️ TODO: Retry failed purchase queries

4. **User Identifiers in Purchase Flow**
   - ⚠️ TODO: Add `obfuscatedAccountId` to purchase flow
   - ⚠️ TODO: Help attribute purchases to correct user

5. **Query Purchases on App Resume**
   - ⚠️ TODO: Add to Activity/Fragment `onResume()`
   - ⚠️ TODO: Catch purchases from other devices

---

## 🚀 Next Steps

### Immediate (Critical)
1. ✅ Deploy updated functions:
   ```bash
   cd genai-android/functions
   firebase deploy --only functions:handleSubscriptionPurchase,functions:checkSubscriptionRenewals,functions:addSubscriptionCredits
   ```

2. ✅ Test subscription purchase flow
3. ✅ Verify subscription info is stored in Firestore

### Short-term (Recommended)
1. ⚠️ Set up Google Play Developer API OAuth
2. ⚠️ Add purchase verification to `handleSubscriptionPurchase`
3. ⚠️ Add subscription verification to `checkSubscriptionRenewals`

### Long-term (Nice to Have)
1. ⚠️ Set up RTDN webhook endpoint
2. ⚠️ Add retry logic for transient errors
3. ⚠️ Add user identifiers to purchase flow
4. ⚠️ Query purchases on app resume

---

## 📚 Reference Documents

- **Gaps Analysis:** `BILLING_IMPLEMENTATION_GAPS.md`
- **Renewal Setup:** `SUBSCRIPTION_RENEWAL_SETUP.md`
- **Testing Guide:** `TESTING_GUIDE.md`

---

## ✅ Summary

**Your Questions Answered:**

1. **Can credits be added without app open?** ✅ YES - Scheduled function handles it automatically
2. **Do we need to check if billing happened?** ✅ YES - But it's automated via scheduled function
3. **How to handle verification?** ⚠️ Use Google Play Developer API (TODO for production)

**Current Status:**
- ✅ Basic auto-renewal working (Firestore-based)
- ✅ Credits added automatically without app
- ⚠️ Production verification still needed (Google Play API)

**Recommendation:**
- Use current implementation for testing
- Add Google Play API verification before production launch
- Set up RTDN for real-time updates (optional but recommended)

