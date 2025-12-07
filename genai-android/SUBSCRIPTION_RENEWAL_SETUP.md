# Subscription Renewal Automation Setup Guide

## Overview

This guide explains how subscription renewals are automatically handled for weekly auto-renewing subscriptions. The system uses a combination of:

1. **Cloud Function Scheduled Job** - Runs daily to check for renewals and add credits
2. **Client-Side Checking** - App checks subscription status on launch
3. **Firestore Storage** - Subscription info stored for tracking

## How It Works

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Subscription Lifecycle                    │
└─────────────────────────────────────────────────────────────┘

1. Initial Purchase
   ↓
   Android App → handleSubscriptionPurchase() Cloud Function
   ↓
   - Add credits to user account
   - Store subscription info in Firestore:
     users/{userId}/subscriptions/{productId}
   - Set nextRenewalDate = 7 days from now

2. Weekly Renewal (Automatic)
   ↓
   Scheduled Function: checkSubscriptionRenewals()
   (Runs daily at 2 AM UTC)
   ↓
   - Checks all active subscriptions
   - If nextRenewalDate has passed:
     * Add credits to user account
     * Update nextRenewalDate (+7 days)
     * Update lastCreditsAdded timestamp

3. App Launch Check
   ↓
   BillingRepository.syncSubscriptionStatus()
   ↓
   - Queries Google Play for current subscription status
   - Updates local subscription status
   - Ensures subscription info is synced
```

## Firestore Structure

### Subscription Document
**Path:** `users/{userId}/subscriptions/{productId}`

```json
{
  "productId": "weekly_100_credits",
  "purchaseToken": "google_purchase_token_here",
  "creditsPerRenewal": 100,
  "status": "active",
  "lastCreditsAdded": Timestamp,
  "nextRenewalDate": Timestamp,  // 7 days from last renewal
  "createdAt": Timestamp,
  "updatedAt": Timestamp
}
```

### User Document
**Path:** `users/{userId}`

```json
{
  "credits": 1000,
  "created_at": Timestamp
}
```

## Cloud Functions

### 1. `handleSubscriptionPurchase`

**Purpose:** Called when user purchases a subscription

**Parameters:**
```typescript
{
  userId: string,
  productId: string,
  purchaseToken: string,
  credits: number
}
```

**What it does:**
- Adds initial credits to user account
- Stores subscription info in Firestore
- Sets `nextRenewalDate` to 7 days from now
- Sets `lastCreditsAdded` timestamp

**Called from:** Android app after successful purchase

### 2. `checkSubscriptionRenewals` (Scheduled)

**Purpose:** Automatically checks for renewals and adds credits

**Schedule:** Daily at 2 AM UTC (`0 2 * * *`)

**What it does:**
1. Queries all active subscriptions from Firestore
2. For each subscription where `nextRenewalDate` has passed:
   - Calculates how many renewal periods have passed
   - Adds credits for each period (handles missed renewals)
   - Updates `nextRenewalDate` to next week
   - Updates `lastCreditsAdded` timestamp

**Example:**
- Subscription renewed 10 days ago
- Calculates: 10 days / 7 days = 1.4 → 2 periods
- Adds: `creditsPerRenewal * 2` credits
- Sets new `nextRenewalDate` to 7 days from now

### 3. `addSubscriptionCredits`

**Purpose:** Helper function to manually add credits for a subscription

**Parameters:**
```typescript
{
  userId: string,
  productId: string,
  credits: number
}
```

**Use cases:**
- Manual credit addition
- Testing
- Recovery from errors

## Setup Instructions

### Step 1: Deploy Cloud Functions

```bash
cd genai-android/functions
npm run build
firebase deploy --only functions:handleSubscriptionPurchase,functions:checkSubscriptionRenewals,functions:addSubscriptionCredits
```

### Step 2: Verify Scheduled Function

After deployment, verify the scheduled function is active:

```bash
firebase functions:list
```

You should see:
- `checkSubscriptionRenewals` (scheduled function)

### Step 3: Test Initial Purchase Flow

1. Make a test subscription purchase in the app
2. Check Firestore:
   - `users/{userId}/subscriptions/{productId}` should exist
   - `users/{userId}/credits` should be incremented
3. Verify logs:
   ```bash
   firebase functions:log --only handleSubscriptionPurchase
   ```

### Step 4: Test Renewal Flow

**Option A: Wait for scheduled run**
- Wait until 2 AM UTC
- Check Firestore for credit updates
- Check logs: `firebase functions:log --only checkSubscriptionRenewals`

**Option B: Manual trigger (for testing)**
```bash
# Manually trigger the scheduled function
firebase functions:shell
# Then in the shell:
checkSubscriptionRenewals()
```

**Option C: Temporarily change schedule**
Edit `functions/src/index.ts`:
```typescript
export const checkSubscriptionRenewals = onSchedule(
  {
    schedule: "*/5 * * * *", // Every 5 minutes (for testing only!)
    timeZone: "UTC",
  },
  // ... rest of function
);
```
Deploy, test, then change back to daily schedule.

## Client-Side Integration

### App Launch Check

The app automatically checks subscription status on launch. This is handled in `BillingRepository.syncSubscriptionStatus()`.

**To add to your app:**

```kotlin
// In your MainActivity or Application class
viewModelScope.launch {
    billingRepository.syncSubscriptionStatus()
}
```

This ensures:
- Subscription status is synced with Google Play
- Active subscriptions are acknowledged
- Subscription info is up to date

## Monitoring & Debugging

### Check Active Subscriptions

```bash
# Query Firestore
firebase firestore:get users/{userId}/subscriptions
```

### Check Renewal Logs

```bash
firebase functions:log --only checkSubscriptionRenewals
```

Look for:
- `🔄 Checking subscription renewals...`
- `✅ Renewal processed: ...`
- `✅ Renewal check complete: X subscriptions processed`

### Manual Credit Addition (If Needed)

If a renewal was missed, you can manually add credits:

```bash
firebase functions:shell
addSubscriptionCredits({
  userId: "user_id_here",
  productId: "weekly_100_credits",
  credits: 100
})
```

Or call from Android app:
```kotlin
val data = hashMapOf(
    "userId" to userId,
    "productId" to productId,
    "credits" to creditsToAdd
)
functions.getHttpsCallable("addSubscriptionCredits").call(data).await()
```

## Handling Edge Cases

### 1. Missed Renewals

The scheduled function handles this automatically:
- If a renewal was missed (e.g., function was down), it calculates how many periods passed
- Adds credits for all missed periods
- Updates `nextRenewalDate` correctly

### 2. Subscription Cancelled

When a subscription is cancelled:
- Google Play will stop renewing
- The scheduled function will still run, but won't find the subscription in Google Play
- You should add logic to mark subscription as "cancelled" in Firestore when detected

**To add cancellation detection:**
```typescript
// In checkSubscriptionRenewals, after checking Google Play API:
if (subscriptionNotFoundInGooglePlay) {
  await subDoc.ref.update({
    status: "cancelled",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}
```

### 3. Payment Failure

If payment fails:
- Google Play will retry automatically
- Subscription remains active during grace period
- If payment fails permanently, subscription is cancelled
- The scheduled function will stop adding credits once subscription is cancelled

### 4. User Uninstalls App

- Subscription continues to renew (Google Play handles this)
- Credits are still added by scheduled function
- When user reinstalls, they'll have accumulated credits

## Advanced: Google Play Real-time Developer Notifications (RTDN)

For more real-time updates, you can set up Google Play RTDN:

1. **Set up webhook endpoint** in Google Play Console
2. **Create Cloud Function** to handle RTDN webhooks
3. **Verify purchase tokens** using Google Play Developer API

This provides instant notifications for:
- Subscription renewals
- Payment failures
- Cancellations
- Grace period changes

**Note:** RTDN requires OAuth setup with Google Play Developer API. The scheduled function approach is simpler and sufficient for most use cases.

## Troubleshooting

### Credits Not Being Added

1. **Check scheduled function is running:**
   ```bash
   firebase functions:log --only checkSubscriptionRenewals
   ```

2. **Verify subscription exists in Firestore:**
   ```bash
   firebase firestore:get users/{userId}/subscriptions/{productId}
   ```

3. **Check nextRenewalDate:**
   - Should be in the past for renewal to trigger
   - Format: `nextRenewalDate: Timestamp`

4. **Check function logs for errors:**
   ```bash
   firebase functions:log
   ```

### Scheduled Function Not Running

1. **Verify function is deployed:**
   ```bash
   firebase functions:list
   ```

2. **Check Cloud Scheduler (if using):**
   - Go to Google Cloud Console → Cloud Scheduler
   - Verify job exists and is enabled

3. **Check function permissions:**
   - Function needs Firestore read/write permissions
   - Check IAM roles in Google Cloud Console

### Subscription Status Out of Sync

1. **Call sync from app:**
   ```kotlin
   billingRepository.syncSubscriptionStatus()
   ```

2. **Manually update Firestore:**
   - Check Google Play Console for actual subscription status
   - Update Firestore document accordingly

## Best Practices

1. **Monitor scheduled function logs** regularly
2. **Set up alerts** for function failures
3. **Test renewal flow** before production
4. **Keep subscription info in sync** with Google Play
5. **Handle edge cases** (cancellations, payment failures)
6. **Document any manual interventions** needed

## Summary

✅ **Automatic renewals** - Scheduled function runs daily
✅ **Handles missed renewals** - Calculates and adds credits for all missed periods
✅ **Client-side sync** - App checks status on launch
✅ **Firestore tracking** - All subscription info stored for audit
✅ **Error handling** - Graceful fallbacks and logging

The system is designed to be **reliable**, **automatic**, and **resilient** to failures.










