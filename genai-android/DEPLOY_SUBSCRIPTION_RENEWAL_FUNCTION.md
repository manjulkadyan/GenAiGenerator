# Deploy Subscription Renewal Function

## 🔴 Problem

The `checkUserSubscriptionRenewal` function exists in code but hasn't been deployed to Firebase Functions, causing `NOT_FOUND` errors.

## ✅ Solution: Deploy Functions

### Step 1: Navigate to Functions Directory

```bash
cd genai-android/functions
```

### Step 2: Install Dependencies (if needed)

```bash
npm install
```

### Step 3: Build Functions

```bash
npm run build
```

This compiles TypeScript to JavaScript in the `lib/` directory.

### Step 4: Deploy Functions

**Option A: Deploy All Functions**
```bash
cd ..  # Go back to genai-android directory
firebase deploy --only functions
```

**Option B: Deploy Only the Subscription Renewal Function**
```bash
cd ..  # Go back to genai-android directory
firebase deploy --only functions:checkUserSubscriptionRenewal
```

### Step 5: Verify Deployment

After deployment, verify the function exists:

```bash
firebase functions:list
```

You should see `checkUserSubscriptionRenewal` in the list.

## 🧪 Test the Function

After deployment, test it from your app:

1. Open the app
2. Check logs for subscription renewal check
3. Should see: `✅ Renewal check complete` or `No active subscriptions`

## 📋 Functions That Need to Be Deployed

Make sure these functions are deployed:

- ✅ `callReplicateVeoAPIV2` - Video generation
- ✅ `replicateWebhook` - Webhook handler
- ✅ `handleSubscriptionPurchase` - Purchase handler
- ✅ `checkUserSubscriptionRenewal` - **NEW - Subscription renewal checker**
- ✅ `addSubscriptionCredits` - Manual credit addition
- ✅ `addTestCredits` - Test credits (optional)
- ✅ `processAccountDeletion` - Account deletion
- ✅ `requestAccountDeletion` - Account deletion request

## 🚨 Common Issues

### Issue: "Function not found" after deployment

**Solution:** Wait 1-2 minutes for Firebase to propagate the function, then try again.

### Issue: Build errors

**Solution:** 
```bash
cd functions
npm install
npm run build
# Check for TypeScript errors
```

### Issue: Deployment timeout

**Solution:** Deploy functions one at a time:
```bash
firebase deploy --only functions:checkUserSubscriptionRenewal
```

## ✅ Success Indicators

After successful deployment:

1. ✅ Firebase Console → Functions → Shows `checkUserSubscriptionRenewal`
2. ✅ App logs show: `Checking subscription renewals for user: {userId}`
3. ✅ No more `NOT_FOUND` errors
4. ✅ Credits are granted when renewals are due

## 📝 Quick Deploy Command

```bash
cd genai-android/functions && npm run build && cd .. && firebase deploy --only functions:checkUserSubscriptionRenewal
```

