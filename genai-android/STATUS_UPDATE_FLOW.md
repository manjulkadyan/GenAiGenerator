# Status Update Flow Documentation

## Current Flow

### 1. Initial Status: "PROCESSING"

**Location 1: Android App** (`FirebaseRepositories.kt:192`)
```kotlin
val jobData = mapOf(
    "status" to VideoJobStatus.PROCESSING.name,
    // ... other fields
)
firestore.collection("users")
    .document(uid)
    .collection("jobs")
    .document(predictionId)
    .set(jobData)
```

**Location 2: Firebase Function** (`index.ts:108`)
```typescript
await writeJobDocument({
  uid: data.userId,
  jobId: result.id,
  payload: {
    status: "PROCESSING",
    // ... other fields
  },
});
```

**⚠️ ISSUE:** Both the app and function are writing the job document. This could cause conflicts.

---

### 2. Status Updates: "COMPLETE" or "FAILED"

**Location: Firebase Function Webhook** (`index.ts:238-276`)

**When Replicate calls webhook:**

```typescript
// In replicateWebhook function
if (prediction.status === "succeeded") {
  updateData.status = "COMPLETE";
  updateData.storage_url = outputUrl;
  updateData.preview_url = outputUrl;
  // Updates Firestore document
  await jobDoc.ref.update(updateData);
} else if (prediction.status === "failed" || prediction.status === "canceled") {
  updateData.status = "FAILED";
  updateData.error_message = prediction.error;
  // Updates Firestore document
  await jobDoc.ref.update(updateData);
}
```

---

### 3. App Reading Status Updates

**Location: Android App** (`FirebaseRepositories.kt:95-115`)

```kotlin
override fun observeJobs(): Flow<List<VideoJob>> = callbackFlow {
    val registration = firestore.collection("users")
        .document(uid)
        .collection("jobs")
        .orderBy("created_at", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            // Automatically called when Firestore document changes
            val jobs = snapshot?.documents.orEmpty().mapNotNull { it.toVideoJob() }
            trySend(jobs) // Updates UI automatically
        }
}
```

**How it works:**
- Firestore snapshot listener watches `users/{uid}/jobs` collection
- When webhook updates status in Firestore → listener fires
- UI automatically updates via StateFlow

---

## Status Flow Diagram

```
1. User clicks "Generate"
   ↓
2. VideoGenerateViewModel.generate()
   ↓
3. FirebaseRepositories.requestVideoGeneration()
   ↓
4. Calls Firebase Function: callReplicateVeoAPIV2
   ↓
5. Function creates Replicate prediction
   ↓
6. Function writes Firestore: status = "PROCESSING" ✅
   ↓
7. App ALSO writes Firestore: status = "PROCESSING" ⚠️ (DUPLICATE)
   ↓
8. Replicate processes video...
   ↓
9. Replicate calls webhook: replicateWebhook
   ↓
10. Webhook updates Firestore: status = "COMPLETE" or "FAILED" ✅
   ↓
11. Firestore snapshot listener fires (in app)
   ↓
12. UI updates automatically ✅
```

---

## Problem: Duplicate Job Creation

**Current Issue:**
- Both Android app (line 196-200) and Firebase Function (line 99-112) create the job document
- This could cause:
  - Race conditions
  - Duplicate writes
  - Conflicting data

**Solution:**
Remove job creation from Android app. Let the Firebase Function be the single source of truth.

---

## Recommended Fix

**Remove job creation from Android app** - The function already creates it with all the data.

**Change in `FirebaseRepositories.kt`:**

```kotlin
// REMOVE this code (lines 186-201):
val jobData = mapOf(...)
firestore.collection("users")
    .document(uid)
    .collection("jobs")
    .document(predictionId)
    .set(jobData)
    .await()

// The function already creates the job document
// Just wait for the function call to complete
```

The function's `writeJobDocument()` already creates the job with status "PROCESSING", so the app doesn't need to create it again.

---

## Status Update Locations Summary

| Status | Where Set | When |
|--------|-----------|------|
| **PROCESSING** | `index.ts:108` (Function) | When prediction created |
| **PROCESSING** | `FirebaseRepositories.kt:192` (App) ⚠️ | When function called (DUPLICATE) |
| **COMPLETE** | `index.ts:245` (Webhook) | When Replicate succeeds |
| **FAILED** | `index.ts:264` (Webhook) | When Replicate fails |
| **QUEUED** | `index.ts:155` (Function) | For effect jobs |

---

## How App Reads Status

1. **Real-time Listener:** `FirebaseVideoHistoryRepository.observeJobs()`
   - Watches `users/{uid}/jobs` collection
   - Automatically fires when any job document changes
   - Converts Firestore status string to `VideoJobStatus` enum

2. **Status Mapping:**
   ```kotlin
   val statusRaw = getString("status") ?: VideoJobStatus.QUEUED.name
   val status = VideoJobStatus.valueOf(statusRaw.uppercase())
   ```

3. **UI Updates:**
   - `HistoryViewModel` collects the Flow
   - UI automatically recomposes when status changes

---

## Complete Status Lifecycle

```
QUEUED (initial for effects)
    ↓
PROCESSING (when Replicate prediction created)
    ↓
    ├─→ COMPLETE (webhook updates when succeeded)
    └─→ FAILED (webhook updates when failed)
```

All status updates happen in **Firebase Functions** (webhook), and the **Android app reads them** via Firestore real-time listeners.

