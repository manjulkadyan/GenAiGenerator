# Account Linking Explained - Google + Anonymous Users

## ❌ Firebase Limitation

**One Google account = One Firebase user**

Firebase Authentication enforces a **one-to-one relationship** between a credential (like Google) and a Firebase user account. Once a Google account is linked to one Firebase user, it **cannot** be linked to another.

## 🔄 Current Behavior

### Scenario 1: First Time Linking (Success)
```
1. User creates anonymous account → Firebase User A (anonymous)
2. User links Google account → Firebase User A (now has Google + anonymous)
3. ✅ Success - User A now has both providers
```

### Scenario 2: Google Already Linked (Auto-Recovery)
```
1. User creates anonymous account → Firebase User B (anonymous)
2. User tries to link Google account
3. Firebase says: "Credential already in use" (linked to User A)
4. ✅ Our code automatically:
   - Signs out anonymous User B
   - Signs in with Google (gets User A)
   - User now has their original account with all data
```

## 📊 Data Storage Structure

### Firestore Collections

**User Credits:**
```
users/{userId}/credits
```

**Video Jobs:**
```
users/{userId}/jobs/{jobId}
```

**Important:** Each Firebase user has their own data. Data is **not shared** between users.

## 🎯 What Happens When Linking Fails

When you try to link a Google account that's already linked to another user:

### Before (Old Behavior)
- ❌ Error: "Account linking failed"
- ❌ User stays as anonymous
- ❌ Data remains separate

### After (Current Behavior - Fixed with Data Merging)
- ✅ Automatically retrieves anonymous user's data (credits & jobs)
- ✅ Signs out anonymous user
- ✅ Signs in with Google (gets existing account)
- ✅ **Merges credits and jobs from anonymous session to Google account**
- ✅ User gets their Google account with merged data
- ✅ No data loss - credits and videos are preserved

## 💡 Options for Handling Multiple Anonymous Users

### Option 1: Current Implementation (✅ IMPLEMENTED)
**Behavior:** When Google account is already linked, merge anonymous data and sign in with existing account.

**Pros:**
- ✅ Simple and automatic
- ✅ User gets their Google account data
- ✅ **Credits from anonymous session are merged**
- ✅ **Video jobs from anonymous session are merged**
- ✅ No data loss - everything is preserved

**How it works:**
1. Detects credential already in use
2. Retrieves anonymous user's credits and jobs
3. Signs out anonymous user
4. Signs in with Google account
5. Merges credits (adds to existing)
6. Merges jobs (copies, skips duplicates)

**Use Case:** Best for all scenarios - preserves all user data.

---

### Option 2: Merge Data Before Signing In
**Behavior:** Before signing in with Google, merge credits and jobs from anonymous user to Google account.

**Implementation:**
```kotlin
suspend fun linkWithGoogle(idToken: String): Result<FirebaseUser> {
    // ... existing code ...
    
    if (isCredentialInUse) {
        // Get anonymous user's data
        val anonymousUid = currentUser.uid
        val anonymousCredits = getCredits(anonymousUid)
        val anonymousJobs = getJobs(anonymousUid)
        
        // Sign out anonymous user
        auth.signOut()
        
        // Sign in with Google
        val googleUser = auth.signInWithCredential(credential).await().user
        
        // Merge data
        mergeUserData(anonymousUid, googleUser.uid, anonymousCredits, anonymousJobs)
        
        return Result.success(googleUser)
    }
}
```

**Pros:**
- ✅ No data loss
- ✅ Credits and videos transfer
- ✅ Better user experience

**Cons:**
- ❌ More complex implementation
- ❌ Requires Firebase Functions or client-side merge logic
- ❌ Potential for duplicate jobs if not handled carefully

**Use Case:** When you want to preserve all user data across sessions.

---

### Option 3: Warn User Before Signing In
**Behavior:** Show a dialog warning that data will be lost, let user choose.

**Implementation:**
```kotlin
// In ProfileScreen.kt
if (isCredentialInUse) {
    showMergeDataDialog(
        message = "This Google account is already linked. Signing in will use your existing account. Data from this anonymous session will not be transferred.",
        onConfirm = {
            // Sign out and sign in with Google
        },
        onCancel = {
            // Stay anonymous
        }
    )
}
```

**Pros:**
- ✅ User is informed
- ✅ User can choose
- ✅ Transparent behavior

**Cons:**
- ❌ More UI complexity
- ❌ User might be confused

**Use Case:** When you want to give users control.

---

## 🔧 Recommended Approach

### For Most Apps: **Option 1 (Current Implementation)**

**Why:**
- Simple and automatic
- Users typically expect one account per Google email
- Data loss from anonymous session is usually acceptable
- Matches user expectations

### For Apps with Valuable Data: **Option 2 (Merge Data)**

**When to use:**
- Users generate valuable content as anonymous
- Credits are purchased/spent as anonymous
- You want to preserve all user activity

**Implementation Required:**
1. Create a Firebase Function to merge user data
2. Call it before signing in with Google
3. Handle edge cases (duplicates, conflicts)

---

## 📝 Example: Merge Data Implementation

### Firebase Function (TypeScript)

```typescript
export const mergeUserData = onCall(async (data, context) => {
  if (!context.auth) {
    throw new Error("Unauthorized");
  }
  
  const { fromUserId, toUserId } = data;
  
  if (fromUserId === toUserId) {
    return { success: true, message: "Same user, no merge needed" };
  }
  
  const db = admin.firestore();
  const fromUserRef = db.collection("users").doc(fromUserId);
  const toUserRef = db.collection("users").doc(toUserId);
  
  // Get source data
  const fromUserData = await fromUserRef.get();
  const fromCredits = fromUserData.data()?.credits || 0;
  const fromJobs = await fromUserRef.collection("jobs").get();
  
  // Merge credits
  await toUserRef.set({
    credits: admin.firestore.FieldValue.increment(fromCredits)
  }, { merge: true });
  
  // Merge jobs (copy to new user)
  const batch = db.batch();
  fromJobs.docs.forEach(job => {
    const newJobRef = toUserRef.collection("jobs").doc(job.id);
    batch.set(newJobRef, job.data());
  });
  await batch.commit();
  
  // Delete source user data (optional)
  // await fromUserRef.delete();
  
  return { success: true, creditsMerged: fromCredits, jobsMerged: fromJobs.size };
});
```

### Android Code (Kotlin)

```kotlin
suspend fun linkWithGoogle(idToken: String): Result<FirebaseUser> {
    // ... existing linking code ...
    
    if (isCredentialInUse) {
        val anonymousUid = currentUser.uid
        
        // Call merge function
        val mergeFunction = functions.getHttpsCallable("mergeUserData")
        try {
            mergeFunction.call(hashMapOf(
                "fromUserId" to anonymousUid,
                "toUserId" to "will-be-set-after-signin" // We'll need to handle this
            )).await()
        } catch (e: Exception) {
            Log.w(TAG, "Merge failed, continuing with sign-in", e)
        }
        
        // Sign out and sign in
        auth.signOut()
        val googleUser = auth.signInWithCredential(credential).await().user
        
        // Now merge with actual Google user ID
        if (googleUser != null) {
            mergeFunction.call(hashMapOf(
                "fromUserId" to anonymousUid,
                "toUserId" to googleUser.uid
            )).await()
        }
        
        return Result.success(googleUser)
    }
}
```

---

## ✅ Current Implementation Summary

**What we have now:**
- ✅ Detects when Google account is already linked
- ✅ Automatically signs out anonymous user
- ✅ Signs in with Google (gets existing account)
- ✅ User gets their original data
- ❌ Data from anonymous session is lost (by design)

**This is the correct behavior for most apps** because:
1. Users expect one account per Google email
2. Anonymous sessions are typically temporary
3. The Google account has the "real" data
4. It's simple and automatic

---

## 🆘 FAQ

### Q: Can I link one Google account to multiple anonymous users?
**A:** No, Firebase doesn't allow this. One Google credential = one Firebase user.

### Q: What happens to data from the anonymous session?
**A:** With current implementation, it stays with the anonymous user (which is signed out). The user gets their Google account data instead.

### Q: Can I merge data from multiple anonymous users?
**A:** Yes, but you need custom implementation (Option 2 above). Firebase doesn't do this automatically.

### Q: Should I implement data merging?
**A:** Only if:
- Users generate valuable content as anonymous
- Credits are purchased/spent as anonymous
- You want to preserve all user activity

For most apps, the current behavior (Option 1) is sufficient.

---

## 📚 Related Documentation

- `FIREBASE_AUTH_TESTING_GUIDE.md` - Testing authentication
- `ADD_USER_CREDITS_GUIDE.md` - Managing user credits
- `VIDEO_GENERATION_FLOW.md` - How video jobs are stored

