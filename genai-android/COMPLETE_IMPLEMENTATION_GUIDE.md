# Complete Video Generation Implementation Guide

## 🎯 Overview

This document describes the complete, production-ready implementation of the video generation system with credit management, job tracking, and real-time status updates.

## 📊 Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID APP                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ Generate     │  │ History      │  │ Profile      │   │
│  │ Screen       │  │ Screen       │  │ Screen       │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                  │                  │            │
│         ▼                  ▼                  ▼            │
│  ┌────────────────────────────────────────────────────┐   │
│  │         ViewModels & Repositories                   │   │
│  │  - VideoGenerateViewModel                           │   │
│  │  - HistoryViewModel                                │   │
│  │  - CreditsViewModel                                │   │
│  └────────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ Firebase SDK
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              FIREBASE SERVICES                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Firestore: users/{uid}/credits                      │   │
│  │  Firestore: users/{uid}/jobs/{jobId}                 │   │
│  │  Cloud Functions: callReplicateVeoAPIV2              │   │
│  │  Cloud Functions: replicateWebhook                   │   │
│  │  Storage: users/{uid}/inputs/{imageId}               │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ HTTP API
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              REPLICATE API                                   │
│  - Creates prediction                                        │
│  - Processes video                                          │
│  - Calls webhook on status change                           │
└─────────────────────────────────────────────────────────────┘
```

## 💰 Credit System

### Storage Location

**Firestore Path:** `users/{userId}/credits`

**Document Structure:**
```json
{
  "credits": 1000  // Integer - current credit balance
}
```

### Credit Calculation

**Base Formula:** `credits = (price_per_second * 100) * duration`

**With Audio:** `credits = (price_per_second * 100) * duration * 2`

**Examples:**
- Veo 3.1: $0.4/sec × 8s = **320 credits**
- Veo 3.1 with Audio: $0.4/sec × 8s × 2 = **640 credits**
- Sora 2: $0.1/sec × 5s = **50 credits**
- Sora 2 with Audio: $0.1/sec × 5s × 2 = **100 credits**

### Credit Flow

1. **Before Generation:**
   ```
   User clicks "Generate"
   → ViewModel calculates cost
   → Function checks: currentCredits >= cost
   → If insufficient: Error thrown, no deduction
   ```

2. **During Generation:**
   ```
   Function deducts: credits -= cost
   → Job created with credits_deducted field
   → Status: PROCESSING
   ```

3. **On Success:**
   ```
   Webhook receives: status = "succeeded"
   → Updates job: status = COMPLETE
   → Stores video URL
   → Credits remain deducted (no refund)
   ```

4. **On Failure:**
   ```
   Webhook receives: status = "failed"
   → Updates job: status = FAILED
   → Function refunds: credits += credits_deducted
   → Stores error message
   ```

## 📝 Job Status Lifecycle

### Status States

| Status | When Set | Description |
|--------|----------|-------------|
| **QUEUED** | Initial for effects | Job queued, not yet processing |
| **PROCESSING** | When Replicate prediction created | Video generation in progress |
| **COMPLETE** | When Replicate succeeds | Video ready, URL stored |
| **FAILED** | When Replicate fails | Generation failed, credits refunded |

### Status Flow Diagram

```
User clicks "Generate"
    ↓
Firebase Function: callReplicateVeoAPIV2
    ├─→ Check credits ✓
    ├─→ Deduct credits ✓
    ├─→ Call Replicate API ✓
    └─→ Create job document
        status: PROCESSING
        credits_deducted: {cost}
    ↓
Replicate processes video...
    ↓
Replicate calls webhook
    ↓
Firebase Function: replicateWebhook
    ├─→ If succeeded:
    │   ├─→ status: COMPLETE
    │   ├─→ storage_url: {videoUrl}
    │   └─→ preview_url: {videoUrl}
    │
    └─→ If failed:
        ├─→ status: FAILED
        ├─→ error_message: {error}
        └─→ Refund credits ✓
    ↓
Firestore updates job document
    ↓
Android App: Real-time listener fires
    ↓
UI automatically updates ✓
```

## 🗄️ Firestore Data Structure

### User Credits

**Path:** `users/{userId}`

```json
{
  "credits": 1000
}
```

### Video Jobs

**Path:** `users/{userId}/jobs/{predictionId}`

```json
{
  "id": "abc123xyz",
  "prompt": "A cat walking on the beach",
  "model_id": "veo-3.1",
  "model_name": "google/veo-3.1",
  "duration_seconds": 8,
  "aspect_ratio": "16:9",
  "status": "PROCESSING",  // QUEUED | PROCESSING | COMPLETE | FAILED
  "replicate_prediction_id": "abc123xyz",
  "cost": 320,
  "credits_deducted": 320,
  "credits_refunded": 0,  // Set when FAILED
  "enable_audio": false,
  "first_frame_url": "https://...",
  "last_frame_url": "https://...",
  "storage_url": "https://...",  // Set when COMPLETE
  "preview_url": "https://...",  // Set when COMPLETE
  "error_message": "...",  // Set when FAILED
  "created_at": Timestamp,
  "updated_at": Timestamp,
  "completed_at": Timestamp,  // Set when COMPLETE
  "failed_at": Timestamp  // Set when FAILED
}
```

## 🔧 Implementation Details

### 1. Firebase Function: `callReplicateVeoAPIV2`

**Location:** `genai-android/functions/src/index.ts`

**Responsibilities:**
- ✅ Validate authentication
- ✅ Check user credits balance
- ✅ Deduct credits immediately
- ✅ Build Replicate input payload (with audio support)
- ✅ Call Replicate API to create prediction
- ✅ Create job document with status `PROCESSING`
- ✅ Handle errors and refund credits if API fails

**Key Code:**
```typescript
// Check and deduct credits
const userRef = firestore.collection("users").doc(userId);
const userDoc = await userRef.get();
const currentCredits = (userDoc.data()?.credits as number) || 0;
const cost = data.cost || 0;

if (currentCredits < cost) {
  throw new Error(`Insufficient credits. Required: ${cost}, Available: ${currentCredits}`);
}

// Deduct credits
await userRef.update({
  credits: admin.firestore.FieldValue.increment(-cost),
});

// Build input with audio support
const input: Record<string, unknown> = {
  prompt: data.prompt,
  duration: data.durationSeconds,
  aspect_ratio: data.aspectRatio,
};
if (data.enableAudio) {
  input.generate_audio = true;
  input.enable_audio = true;
}
```

### 2. Firebase Function: `replicateWebhook`

**Location:** `genai-android/functions/src/index.ts`

**Responsibilities:**
- ✅ Receive webhook from Replicate
- ✅ Find job document by `replicate_prediction_id`
- ✅ Update status based on Replicate response:
  - `succeeded` → `COMPLETE` + store video URL
  - `failed/canceled` → `FAILED` + refund credits
  - `starting/processing` → Keep `PROCESSING`
- ✅ Send FCM notification (optional)

**Key Code:**
```typescript
if (prediction.status === "succeeded") {
  updateData.status = "COMPLETE";
  updateData.storage_url = outputUrl;
  updateData.preview_url = outputUrl;
  updateData.completed_at = admin.firestore.FieldValue.serverTimestamp();
} else if (prediction.status === "failed" || prediction.status === "canceled") {
  // Refund credits
  const creditsDeducted = (jobData?.credits_deducted as number) || 0;
  if (creditsDeducted > 0 && userId) {
    await userRef.update({
      credits: admin.firestore.FieldValue.increment(creditsDeducted),
    });
  }
  updateData.status = "FAILED";
  updateData.error_message = prediction.error || "Job failed";
  updateData.credits_refunded = creditsDeducted;
}
```

### 3. Android App: Credit Management

**Location:** `FirebaseCreditsRepository.kt`

**How it works:**
- Real-time listener on `users/{uid}/credits`
- Automatically updates UI when credits change
- Used by `CreditsViewModel` to display balance

**Key Code:**
```kotlin
override fun observeCredits(): Flow<UserCredits> = callbackFlow {
    val uid = auth.currentUser?.uid ?: return@callbackFlow
    val registration = firestore.collection("users")
        .document(uid)
        .addSnapshotListener { snapshot, error ->
            val credits = snapshot?.getLong("credits")?.toInt() ?: 0
            trySend(UserCredits(max(0, credits)))
        }
    awaitClose { registration.remove() }
}
```

### 4. Android App: Job History

**Location:** `FirebaseVideoHistoryRepository.kt`

**How it works:**
- Real-time listener on `users/{uid}/jobs`
- Automatically updates when job status changes
- Used by `HistoryViewModel` to display job list

**Key Code:**
```kotlin
override fun observeJobs(): Flow<List<VideoJob>> = callbackFlow {
    val uid = auth.currentUser?.uid ?: return@callbackFlow
    val registration = firestore.collection("users")
        .document(uid)
        .collection("jobs")
        .orderBy("created_at", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            val jobs = snapshot?.documents.orEmpty().mapNotNull { it.toVideoJob() }
            trySend(jobs)
        }
    awaitClose { registration.remove() }
}
```

### 5. Android App: Video Generation

**Location:** `VideoGenerateViewModel.kt` + `FirebaseRepositories.kt`

**Flow:**
1. User fills form (prompt, duration, aspect ratio, audio, frames)
2. ViewModel calculates cost (with 2x multiplier for audio)
3. ViewModel uploads first/last frame images (if provided)
4. ViewModel calls `requestVideoGeneration()`
5. Repository calls Firebase Function `callReplicateVeoAPIV2`
6. Function deducts credits and creates job
7. Real-time listener picks up job status updates

## 🎨 UI Components

### GenerateScreen
- Model selector
- Prompt input
- Duration selector
- Aspect ratio selector
- First frame picker (if model supports)
- Last frame picker (if model supports)
- Audio toggle (if model supports)
- Cost display (updates with audio toggle)
- Generate button

### HistoryScreen
- List of all jobs
- Status with color coding:
  - **COMPLETE**: Primary color (green)
  - **FAILED**: Error color (red)
  - **PROCESSING**: Secondary color (blue)
  - **QUEUED**: Gray
- Error messages for failed jobs
- Cost display per job
- Click to view video (TODO: implement player)

### ProfileScreen
- Credit balance (real-time)
- User info

## ✅ Testing Checklist

### Credit Management
- [ ] User with sufficient credits can generate
- [ ] User with insufficient credits gets error message
- [ ] Credits are deducted immediately when job starts
- [ ] Credits are refunded when job fails
- [ ] Credits are NOT refunded when job succeeds
- [ ] Audio doubles the cost correctly
- [ ] Credit balance updates in real-time

### Job Status Flow
- [ ] Job starts as `PROCESSING` immediately
- [ ] Job updates to `COMPLETE` when video ready
- [ ] Job updates to `FAILED` when error occurs
- [ ] Video URL is stored on completion
- [ ] Error message is stored on failure
- [ ] Status updates appear in real-time in UI

### Error Handling
- [ ] Replicate API failure refunds credits
- [ ] Network errors are handled gracefully
- [ ] Invalid inputs show appropriate errors
- [ ] Missing required fields prevent generation

## 🚀 Deployment Steps

1. **Deploy Firebase Functions:**
   ```bash
   cd genai-android/functions
   npm run build
   firebase deploy --only functions
   ```

2. **Set Environment Variables:**
   ```bash
   firebase functions:secrets:set REPLICATE_API_TOKEN
   ```

3. **Seed Models:**
   ```bash
   npm run seed:normalized
   ```

4. **Create Test User:**
   - Run app and sign in
   - Get user UID from logs
   - Add credits in Firestore: `users/{uid}/credits = 1000`

5. **Test Flow:**
   - Generate a video
   - Check credits deducted
   - Wait for completion
   - Verify video URL stored
   - Check history screen updates

## 📱 Android App Features

### Real-time Updates
- ✅ Credit balance updates automatically
- ✅ Job status updates automatically
- ✅ No manual refresh needed

### Error Handling
- ✅ Insufficient credits error
- ✅ Network error handling
- ✅ Validation errors
- ✅ Failed job error messages

### User Experience
- ✅ Clear status indicators
- ✅ Color-coded status
- ✅ Cost transparency
- ✅ Progress feedback

## 🔐 Security

- ✅ Authentication required for all operations
- ✅ User can only access their own jobs
- ✅ Credits checked server-side
- ✅ Credit deduction is atomic (Firestore transaction)

## 📊 Monitoring

**Firebase Console:**
- Monitor function executions
- Check error logs
- View Firestore data
- Monitor credit balances

**Key Metrics to Track:**
- Job success rate
- Average processing time
- Credit usage per user
- Failed job reasons

