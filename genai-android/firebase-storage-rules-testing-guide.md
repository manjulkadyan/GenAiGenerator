# Firebase Storage Rules Testing Guide

## Current Rules (Correct ✅)

```javascript
rules_version = '2';
service firebase.storage {
    match /b/{bucket}/o {
        match /users/{userId}/{allPaths=**} {
            allow write: if request.auth != null && request.auth.uid == userId;
            allow read: if request.auth != null && request.auth.uid == userId;
        }
    }
}
```

## How to Test in Rules Playground

### Test 1: Successful Upload (Should PASS ✅)

**Simulation type:** `write` (or `upload`)

**Location:** `/b/genaivideogenerator.firebasestorage.app/o`

**Path:** `users/test-user-123/inputs/test-file.jpeg`

**Provider:** `custom` (or the provider your app uses - `google.com`, `password`, etc.)

**Firebase UID:** `test-user-123` (MUST match the userId in the path!)

**Authentication payload:**
```json
{
  "uid": "test-user-123",
  "token": {
    "sub": "test-user-123",
    "aud": "genaivideogenerator",
    "firebase": {
      "sign_in_provider": "custom"
    }
  }
}
```

**Expected Result:** ✅ Allow

---

### Test 2: Unauthorized Access (Should FAIL ❌)

**Simulation type:** `write`

**Location:** `/b/genaivideogenerator.firebasestorage.app/o`

**Path:** `users/test-user-123/inputs/test-file.jpeg`

**Provider:** `custom`

**Firebase UID:** `different-user-456` (Different from userId in path!)

**Authentication payload:**
```json
{
  "uid": "different-user-456",
  "token": {
    "sub": "different-user-456",
    "aud": "genaivideogenerator",
    "firebase": {
      "sign_in_provider": "custom"
    }
  }
}
```

**Expected Result:** ❌ Deny (because UID doesn't match userId in path)

---

### Test 3: Unauthenticated Access (Should FAIL ❌)

**Simulation type:** `write`

**Location:** `/b/genaivideogenerator.firebasestorage.app/o`

**Path:** `users/test-user-123/inputs/test-file.jpeg`

**Provider:** `anonymous` (or leave empty)

**Firebase UID:** (leave empty)

**Expected Result:** ❌ Deny (because `request.auth != null` is false)

---

## Common Mistakes in Rules Playground

1. ❌ **Empty UID** - Must provide a real UID
2. ❌ **UID doesn't match path** - If path is `users/abc123/...`, UID must be `abc123`
3. ❌ **Using anonymous provider** - Your app likely uses a different provider
4. ❌ **Wrong path format** - Must match: `users/{userId}/...`

## How to Get Your Real User UID

1. In your Android app, add this temporary code to log the UID:
   ```kotlin
   val uid = FirebaseAuth.getInstance().currentUser?.uid
   Log.d("UserUID", "Current user UID: $uid")
   ```

2. Or check Firebase Console → Authentication → Users

## Actual Paths Your App Uses

Based on the code, your app uploads to:
- `users/{userId}/inputs/{uuid}.jpeg`

Where:
- `{userId}` = Firebase Auth UID (e.g., `VcOvN3lz9tUGHnAArtt19GJI1KC3`)
- `{uuid}` = Random UUID generated for each file

## Testing with Real User

To test with your actual user:

1. Get your Firebase UID from the app or Firebase Console
2. Use that UID in the Rules Playground:
   - **Path:** `users/YOUR_ACTUAL_UID/inputs/test.jpeg`
   - **Firebase UID:** `YOUR_ACTUAL_UID`
   - **Provider:** (whatever your app uses - check Firebase Console → Authentication)

## Rules Are Correct - Next Steps

If the rules test correctly in the playground but uploads still fail:

1. ✅ **Publish the rules** - Make sure you clicked "Publish" after saving
2. ✅ **Wait a few seconds** - Rules can take 10-30 seconds to propagate
3. ✅ **Check user authentication** - Make sure user is logged in when uploading
4. ✅ **Verify UID matches** - The Firebase Auth UID must match the userId in the path

## Alternative: More Permissive Rules (For Testing Only)

If you want to test without matching UIDs (NOT for production):

```javascript
rules_version = '2';
service firebase.storage {
    match /b/{bucket}/o {
        match /users/{userId}/{allPaths=**} {
            allow read, write: if request.auth != null;
        }
    }
}
```

This allows any authenticated user to access any user's directory. Use only for testing!

