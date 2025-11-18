# Testing Credit Charging Functionality

This guide explains how to test credit deduction without calling the actual Replicate API (which costs money).

## Overview

We've created two test Firebase functions:

1. **`testCallReplicateVeoAPIV2`** - Mimics video generation and credit deduction without calling Replicate
2. **`addTestCredits`** - Adds test credits to your account

## Setup

### Step 1: Deploy Test Functions

Deploy the test functions to Firebase:

```bash
cd genai-android/functions
npm run build
firebase deploy --only functions:testCallReplicateVeoAPIV2,functions:addTestCredits
```

### Step 2: Switch to Test Function (Temporary)

In `FirebaseRepositories.kt`, change the function name:

```kotlin
// Change this line (around line 241):
val callableResult = functions
    .getHttpsCallable("callReplicateVeoAPIV2")  // ← Change this
    .call(data)
    .await()

// To:
val callableResult = functions
    .getHttpsCallable("testCallReplicateVeoAPIV2")  // ← Test function
    .call(data)
    .await()
```

**⚠️ IMPORTANT:** Remember to change it back to `callReplicateVeoAPIV2` before production!

### Step 3: Add Test Credits

You can add test credits in two ways:

#### Option A: From Android App (Recommended)

Create a simple test button in your app that calls:

```kotlin
// In your test/debug code
val functions = FirebaseFunctions.getInstance()
val data = hashMapOf(
    "credits" to 1000  // Add 1000 test credits
)
functions.getHttpsCallable("addTestCredits")
    .call(data)
    .addOnSuccessListener { result ->
        Log.d("Test", "Credits added: ${result.data}")
    }
```

#### Option B: From Firebase Console

1. Go to Firebase Console → Functions
2. Find `addTestCredits` function
3. Click "Test" tab
4. Enter test data:
```json
{
  "credits": 1000
}
```
5. Click "Test"

## How Test Function Works

The `testCallReplicateVeoAPIV2` function:

1. ✅ **Checks user credits** (same as production)
2. ✅ **Deducts credits immediately** (same as production)
3. ✅ **Creates a job document** with status "PROCESSING"
4. ✅ **Simulates completion** after 5 seconds (updates to "COMPLETE" with fake video URL)
5. ❌ **Does NOT call Replicate API** (saves money!)

## Testing Flow

1. **Add test credits:**
   - Call `addTestCredits` with `{ "credits": 1000 }`
   - Verify credits appear in Profile screen

2. **Generate a video:**
   - Go to Generate screen
   - Fill in prompt, select model, duration, etc.
   - Click "Generate Video"
   - Credits should be deducted immediately
   - Job should appear in History with "PROCESSING" status

3. **Wait 5 seconds:**
   - Job status should automatically update to "COMPLETE"
   - Video URL will be a fake/test URL

4. **Verify credit deduction:**
   - Check Profile screen - credits should be reduced by the cost
   - Check History screen - job should show the deducted cost

## Switching Back to Production

When ready to use real Replicate API:

1. Change function name back to `callReplicateVeoAPIV2` in `FirebaseRepositories.kt`
2. Redeploy if needed
3. Remove test credits or use real credits

## Test Function Features

- ✅ Full credit validation and deduction
- ✅ Job document creation (same structure as production)
- ✅ Automatic status update to COMPLETE after 5 seconds
- ✅ Test flag (`is_test: true`) to identify test jobs
- ✅ No Replicate API calls (no costs!)

## Troubleshooting

### Credits not deducting?
- Check Firebase Console → Functions logs
- Verify function is deployed: `firebase functions:list`
- Check user document in Firestore: `users/{userId}/credits`

### Job not appearing?
- Check Firestore: `users/{userId}/jobs/{jobId}`
- Verify function completed successfully
- Check Android logcat for errors

### Function not found?
- Make sure you deployed: `firebase deploy --only functions:testCallReplicateVeoAPIV2`
- Check function name matches exactly (case-sensitive)

## Production Checklist

Before going to production:

- [ ] Change function name back to `callReplicateVeoAPIV2`
- [ ] Remove or comment out test credit adding code
- [ ] Test with real Replicate API (small test first!)
- [ ] Verify credit deduction works correctly
- [ ] Check webhook handling for job completion

