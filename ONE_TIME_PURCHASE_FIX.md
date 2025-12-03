# One-Time Purchase Consumption Fix

## Problem
When attempting to buy the same one-time credit package again, Google Play showed: **"You already own this item"**

## Root Cause
One-time purchases (INAPP products) must be **consumed** after purchase to allow repurchasing. The previous implementation was **acknowledging** the purchase (which works for subscriptions) instead of **consuming** it (required for one-time products).

## Solution

### Key Difference: Acknowledge vs. Consume

| Type | Method | Purpose |
|------|--------|---------|
| **Subscriptions (SUBS)** | `acknowledgePurchase()` | Confirms entitlement, prevents refund |
| **One-Time (INAPP)** | `consumePurchase()` | Marks as used, **allows repurchase** |

---

## Changes Made

### 1. **Android - BillingRepository.kt**

#### Added `consumePurchase()` Function
```kotlin
suspend fun consumePurchase(purchaseToken: String): Result<String> {
    // Consumes the purchase using ConsumeParams
    // Returns success when consumed
}
```

#### Updated `handlePurchase()`
- **Subscriptions**: Still acknowledged locally
- **One-time purchases**: Skipped (backend will consume them)

**Key Comment:**
```kotlin
// ⚠️ NOTE: For ONE-TIME purchases (INAPP), we CONSUME them (not acknowledge).
// The backend (Cloud Function) handles consumption AFTER verification and credit addition.
// Consumption allows the user to purchase the same product again.
```

---

### 2. **Backend - Firebase Cloud Function**

#### Updated `handleOneTimePurchase`

**BEFORE (❌ Wrong):**
```typescript
// Acknowledge purchase
await publisher.purchases.products.acknowledge({
  packageName: playPackageName,
  productId: productId,
  token: purchaseToken,
});
```

**AFTER (✅ Correct):**
```typescript
// Consume purchase (allows repurchase)
await publisher.purchases.products.consume({
  packageName: playPackageName,
  productId: productId,
  token: purchaseToken,
});
```

**Key Changes:**
- Changed from `acknowledge()` to `consume()`
- Check `consumptionState` instead of `acknowledgementState`
- Store `consumed: true` instead of `acknowledged: true` in Firestore

---

## How It Works Now

### Purchase Flow:

```
1. User buys "200 Credits" (credits_200)
   ↓
2. Google Play processes payment
   ↓
3. App sends purchase to backend (handleOneTimePurchase)
   ↓
4. Backend verifies with Google Play API
   ↓
5. Backend CONSUMES the purchase ✅
   ↓
6. Backend grants credits to user
   ↓
7. Purchase is stored in history
   ↓
8. ✅ User can now purchase "200 Credits" AGAIN!
```

---

## Google Play Billing States

### One-Time Purchase States:

| State | Value | Meaning |
|-------|-------|---------|
| **purchaseState** | 0 | Purchased (payment completed) |
| | 1 | Canceled |
| | 2 | Pending |
| **consumptionState** | 0 | Not consumed (❌ can't repurchase) |
| | 1 | Consumed (✅ can repurchase) |

---

## Testing

### Before Fix:
```
1. Buy "200 Credits" → Success ✅
2. Try to buy "200 Credits" again → ❌ "You already own this item"
```

### After Fix:
```
1. Buy "200 Credits" → Success ✅
2. Try to buy "200 Credits" again → Success ✅ (consumed)
3. Try to buy "200 Credits" again → Success ✅ (consumed)
...infinite repurchases possible!
```

---

## Deployment

### Deploy Firebase Function:
```bash
cd genai-android
firebase deploy --only functions:handleOneTimePurchase
```

### Android App:
- No deployment needed (change is backend-only)
- App already had `ConsumeParams` import added
- `consumePurchase()` function ready for future use

---

## Important Notes

⚠️ **CRITICAL DISTINCTION:**

| Product Type | Use | Reason |
|--------------|-----|--------|
| **Subscriptions** | `acknowledge()` | Recurring, one-time acknowledgment |
| **One-Time (Consumables)** | `consume()` | **Allows repurchase** of same item |

✅ **After consumption, the purchase is removed from user's inventory**
✅ **User can buy the same product unlimited times**
✅ **Credits accumulate in single pool**

---

## Status

- ✅ Android code updated with `consumePurchase()` function
- ✅ Backend updated to consume instead of acknowledge
- ✅ Firebase function deployed
- ✅ Tested and working

**Result:** Users can now purchase the same credit package multiple times! 🎉

