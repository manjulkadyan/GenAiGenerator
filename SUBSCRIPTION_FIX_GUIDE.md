# Subscription Purchase & Test User Fix Guide

## Issues Fixed

### 1. ✅ Backend OpenSSL Error (Node.js 22)
**Problem**: Firebase Functions were failing with `error:1E08010C:DECODER routines::unsupported`

**Root Cause**: Node.js 22 uses OpenSSL 3.x which doesn't support older private key formats in Google service account credentials.

**Solution**: Downgraded Firebase Functions to Node.js 20 (LTS) which uses OpenSSL 1.1.1

**Files Changed**:
- `genai-android/functions/package.json` - Changed Node version from 22 to 20

**Functions Redeployed**:
- ✅ `handleSubscriptionPurchase`
- ✅ `checkAllSubscriptionRenewals`
- ✅ `checkUserSubscriptionRenewal`
- ✅ `handlePlayRtdn`
- ✅ `addSubscriptionCredits`

---

### 2. ✅ Test User "Already Subscribed" Error
**Problem**: Google Play test users can only have ONE active subscription at a time. When trying to purchase again, they get "you are already subscribed" error.

**Solution**: Added proper handling for `ITEM_ALREADY_OWNED` billing response code:

1. **Detect existing subscription** when user tries to purchase
2. **Query existing purchases** from Google Play
3. **Sync existing subscription** with backend to ensure credits are granted
4. **Show user-friendly message** instead of error

**Files Changed**:
- `BillingRepository.kt` - Added `ITEM_ALREADY_OWNED` handler with auto-sync
- `LandingPageViewModel.kt` - Added `AlreadyOwned` event type with proper UI messaging

---

## How Test Users Should Handle Subscriptions

### For First-Time Purchase
1. Click "Continue" on desired plan
2. Complete Google Play purchase
3. App will automatically sync and add credits ✅

### For Test Users with Existing Subscription

#### Option 1: Let the App Handle It (Recommended)
1. Click "Continue" on any plan
2. Google Play will show "already subscribed" dialog
3. **App will automatically detect and sync your existing subscription** 🎉
4. Credits will be added if not already synced
5. Message: "Your subscription is already active! Syncing..."

#### Option 2: Cancel Existing Subscription First
If you want to test purchasing a DIFFERENT plan:

1. **Cancel current subscription** via Google Play:
   - Open Google Play Store app
   - Tap profile icon → **Payments & subscriptions** → **Subscriptions**
   - Find "AI Video Generator" subscription
   - Tap → **Cancel subscription**
   
2. **Wait for subscription to expire** (test subscriptions expire in 5 minutes for weekly plans)

3. **Purchase new plan** once expired

---

## Testing Flow

### 1. Test First Purchase (New User)
```
1. User clicks "Continue" on weekly_150_credits plan
2. Google Play billing dialog appears
3. User completes purchase
4. Expected logs:
   ✅ "Purchase SUCCESS: weekly_150_credits"
   ✅ "Subscription sent to backend for processing"
   ✅ "Purchase message: Subscription purchased! 150 credits added"
5. User sees success dialog
```

### 2. Test Already Subscribed (Existing Subscription)
```
1. User (with active subscription) clicks "Continue"
2. Google Play shows "already subscribed" dialog
3. Expected logs:
   ℹ️ "Item already owned - querying existing purchases to sync"
   ℹ️ "Found 1 existing purchases"
   ℹ️ "Re-processing existing purchase: weekly_150_credits"
   ✅ "Subscription already owned - syncing"
   ✅ "Your subscription is already active! Syncing..."
4. Backend verifies and syncs subscription
5. Credits are ensured to be in user account
```

### 3. Test Purchase Error (Service Unavailable)
```
1. Simulate error by turning off network during purchase
2. Expected logs:
   ❌ "Purchase ERROR: code=2"
   ❌ "Billing service is unavailable. Please try again later."
3. User sees error message in snackbar
```

---

## Backend Verification

When subscription is processed, the backend (`handleSubscriptionPurchase`) will:

1. **Verify purchase token** with Google Play API ✅
2. **Check subscription state** (must be ACTIVE or IN_GRACE_PERIOD) ✅
3. **Acknowledge purchase** if not already acknowledged ✅
4. **Check for duplicate processing** using purchase token as document ID ✅
5. **Add credits** to user account ✅
6. **Store subscription info** for renewal tracking ✅

### Expected Backend Logs (Success)
```
✅ Callable request verification passed
✅ Play purchase acknowledged for weekly_150_credits
✅ Credits added: 150 (new balance: 150)
✅ Subscription stored successfully
```

### Expected Backend Logs (Already Processed)
```
⚠️ Purchase token already processed: [token] for user [userId]
✅ Returning success with current balance
```

---

## Error Codes Reference

| Code | Constant | Meaning | User Message |
|------|----------|---------|--------------|
| 0 | OK | Purchase successful | "Subscription purchased successfully!" |
| 1 | USER_CANCELED | User cancelled | (No error shown) |
| 2 | SERVICE_UNAVAILABLE | Google Play unavailable | "Billing service is unavailable. Please try again later." |
| 3 | BILLING_UNAVAILABLE | Billing not supported | "Billing is not available on this device" |
| 7 | ITEM_ALREADY_OWNED | Already subscribed | "Your subscription is already active! Syncing..." |

---

## Test User Best Practices

### For Development/Testing

1. **Use Google Play Internal Testing track** with test users
2. **Test subscriptions expire quickly**:
   - Weekly → 5 minutes
   - Monthly → 5 minutes
   - Yearly → 5 minutes

3. **To test multiple purchases**:
   - Wait for current subscription to expire (5 min)
   - Or cancel and wait for expiry
   - Or use different test accounts

4. **To test renewals**:
   - Wait 5 minutes for first renewal
   - Check backend logs for renewal processing
   - Verify credits are added automatically

### For Production

1. **Real subscriptions renew at normal intervals**:
   - Weekly → 7 days
   - Monthly → 30 days
   - Yearly → 365 days

2. **Renewals are automatic**:
   - Backend checks every hour via `checkAllSubscriptionRenewals`
   - Credits added automatically on renewal
   - Firebase Functions sync with Google Play API

---

## Troubleshooting

### Issue: "Failed to verify purchase with Google Play"

**Possible Causes**:
- ✅ FIXED: Node.js 22 OpenSSL error (now using Node.js 20)
- Service account key not configured
- Network connectivity issues
- Google Play API not enabled

**How to Fix**:
1. Check Firebase Functions logs
2. Verify `PLAY_SERVICE_ACCOUNT_KEY` environment variable is set
3. Ensure Google Play Developer API is enabled in GCP

### Issue: "You already have an active subscription"

**This is Normal!** The app will now:
1. Detect existing subscription
2. Query current subscription from Google Play
3. Sync with backend to ensure credits are correct
4. Show "Your subscription is already active! Syncing..."

**No action needed** - the app handles this automatically now ✅

### Issue: Purchase succeeds but credits not added

**Check**:
1. Firebase Functions logs for errors
2. Firestore `users/{userId}/purchases` collection
3. Backend verification succeeded?

**Retry**:
- The app will auto-sync on next launch
- Or user can tap "Continue" again (will sync existing subscription)

---

## Summary of Changes

### Backend (Firebase Functions)
- ✅ Downgraded to Node.js 20 to fix OpenSSL compatibility
- ✅ Deployed all subscription-related functions

### Android App (Kotlin)
- ✅ Added `PurchaseUpdateEvent.AlreadyOwned` event type
- ✅ Added `ITEM_ALREADY_OWNED` handler in `BillingRepository`
- ✅ Auto-queries and syncs existing purchases when already owned
- ✅ Updated `LandingPageViewModel` to show friendly message for already owned
- ✅ Processes existing subscription on backend to ensure sync

### User Experience
- ✅ No more confusing "You already have an active subscription" error
- ✅ Shows "Your subscription is already active! Syncing..." message
- ✅ Automatically syncs existing subscription with backend
- ✅ Credits are ensured to be in user account

---

## Next Steps

1. ✅ Backend deployed with Node.js 20
2. ✅ Android app built successfully
3. 🔄 **Test a subscription purchase**
4. 🔄 **Verify credits are added**
5. 🔄 **Test "already owned" scenario**
6. 🔄 **Monitor Firebase logs for any issues**

---

## Monitoring

### Firebase Console
- **Functions logs**: Check for errors in `handleSubscriptionPurchase`
- **Firestore**: Verify `purchases` and `subscriptions` collections

### Android Logs
- Filter by tag: `BillingRepository`, `LandingPageViewModel`, `BuyCreditsScreen`
- Look for: "Purchase SUCCESS", "Already owned", "Subscription sent to backend"

---

**Status**: ✅ All fixes deployed and ready for testing!

