# Video Generation Flow - Complete Implementation Guide

## Architecture Overview

```
┌─────────────────┐
│  Android App    │
│  GenerateScreen │
└────────┬────────┘
         │
         │ 1. User clicks "Generate"
         │    - Validates inputs
         │    - Calculates cost (with audio 2x multiplier)
         │    - Checks credits
         │
         ▼
┌─────────────────────────────┐
│  VideoGenerateViewModel    │
│  - generate()               │
│  - Uploads first/last frame │
│  - Calls Firebase Function  │
└────────┬────────────────────┘
         │
         │ 2. Calls Firebase Function
         │    callReplicateVeoAPIV2
         │
         ▼
┌─────────────────────────────┐
│  Firebase Function           │
│  callReplicateVeoAPIV2      │
│  ┌─────────────────────────┐│
│  │ 1. Check user credits    ││
│  │ 2. Deduct credits        ││
│  │ 3. Call Replicate API   ││
│  │ 4. Create job document   ││
│  │    status: PROCESSING   ││
│  └─────────────────────────┘│
└────────┬────────────────────┘
         │
         │ 3. Replicate processes video
         │
         ▼
┌─────────────────────────────┐
│  Replicate API              │
│  - Processes video          │
│  - Calls webhook on update  │
└────────┬────────────────────┘
         │
         │ 4. Webhook called
         │
         ▼
┌─────────────────────────────┐
│  Firebase Function          │
│  replicateWebhook            │
│  ┌─────────────────────────┐│
│  │ - Updates job status    ││
│  │ - COMPLETE: Store URL   ││
│  │ - FAILED: Refund credits││
│  └─────────────────────────┘│
└────────┬────────────────────┘
         │
         │ 5. Firestore updates
         │
         ▼
┌─────────────────────────────┐
│  Android App                │
│  HistoryScreen               │
│  - Real-time listener        │
│  - Auto-updates UI           │
└─────────────────────────────┘
```

## Credit Storage & Management

### Firestore Structure

**Collection:** `users`
**Document:** `{userId}`
**Fields:**
```json
{
  "credits": 1000  // Integer - user's credit balance
}
```

### Credit Flow

1. **Before Generation:**
   - Function checks: `currentCredits >= cost`
   - If insufficient: Throws error, no deduction

2. **During Generation:**
   - Function deducts: `credits -= cost`
   - Job created with `credits_deducted: cost`

3. **On Success:**
   - Credits remain deducted
   - Job status: `COMPLETE`
   - Video URL stored

4. **On Failure:**
   - Function refunds: `credits += credits_deducted`
   - Job status: `FAILED`
   - Error message stored

## Job Status Lifecycle

### Status Flow

```
QUEUED (initial for effects)
    ↓
PROCESSING (when Replicate prediction created)
    ↓
    ├─→ COMPLETE (webhook updates when succeeded)
    │   - storage_url: Video URL
    │   - preview_url: Video URL
    │   - completed_at: Timestamp
    │
    └─→ FAILED (webhook updates when failed)
        - error_message: Error details
        - failed_at: Timestamp
        - credits_refunded: Amount refunded
```

### Firestore Job Document

**Collection:** `users/{userId}/jobs`
**Document:** `{predictionId}` (Replicate prediction ID)

**Fields:**
```json
{
  "prompt": "A cat walking on the beach",
  "model_id": "veo-3.1",
  "model_name": "google/veo-3.1",
  "duration_seconds": 8,
  "aspect_ratio": "16:9",
  "status": "PROCESSING",  // QUEUED | PROCESSING | COMPLETE | FAILED
  "replicate_prediction_id": "abc123xyz",
  "cost": 40,  // Credits cost
  "credits_deducted": 40,  // Credits deducted (for refund tracking)
  "enable_audio": false,
  "first_frame_url": "https://...",
  "last_frame_url": "https://...",
  "storage_url": "https://...",  // Set when COMPLETE
  "preview_url": "https://...",  // Set when COMPLETE
  "error_message": "...",  // Set when FAILED
  "credits_refunded": 40,  // Set when FAILED
  "created_at": Timestamp,
  "completed_at": Timestamp,  // Set when COMPLETE
  "failed_at": Timestamp,  // Set when FAILED
  "updated_at": Timestamp
}
```

## Implementation Details

### 1. Credit Calculation

**Formula:** `credits = dollars_per_second * 100 * duration`

**With Audio:** `credits = (dollars_per_second * 100 * duration) * 2`

**Example:**
- Base: $0.4/sec × 8s = $3.2 → 320 credits
- With Audio: 320 × 2 = 640 credits

### 2. Firebase Function: `callReplicateVeoAPIV2`

**Steps:**
1. ✅ Validate authentication
2. ✅ Check user credits
3. ✅ Deduct credits immediately
4. ✅ Build Replicate input payload (with audio support)
5. ✅ Call Replicate API
6. ✅ Create job document with status `PROCESSING`
7. ✅ Return prediction ID

**Error Handling:**
- If Replicate API fails → Refund credits
- If insufficient credits → Throw error (no deduction)

### 3. Firebase Function: `replicateWebhook`

**Steps:**
1. ✅ Receive webhook from Replicate
2. ✅ Find job document by `replicate_prediction_id`
3. ✅ Update status based on Replicate status:
   - `succeeded` → `COMPLETE` + store video URL
   - `failed/canceled` → `FAILED` + refund credits
   - `starting/processing` → Keep `PROCESSING`
4. ✅ Send FCM notification (optional)

### 4. Android App: Real-time Updates

**Location:** `FirebaseVideoHistoryRepository.observeJobs()`

**How it works:**
- Firestore snapshot listener watches `users/{uid}/jobs`
- Automatically fires when any job document changes
- Converts Firestore status to `VideoJobStatus` enum
- UI automatically updates via StateFlow

## Testing Checklist

### Credit Management
- [ ] User with sufficient credits can generate
- [ ] User with insufficient credits gets error
- [ ] Credits are deducted immediately
- [ ] Credits are refunded on failure
- [ ] Credits are NOT refunded on success
- [ ] Audio doubles the cost correctly

### Job Status Flow
- [ ] Job starts as `PROCESSING`
- [ ] Job updates to `COMPLETE` when video ready
- [ ] Job updates to `FAILED` when error occurs
- [ ] Video URL is stored on completion
- [ ] Error message is stored on failure

### UI Updates
- [ ] History screen shows all jobs
- [ ] Status updates in real-time
- [ ] Completed videos show preview
- [ ] Failed jobs show error message
- [ ] Credits balance updates in real-time

## Credit Pricing Examples

| Model | Price/sec | Duration | Audio | Total Credits |
|-------|-----------|----------|-------|---------------|
| Veo 3.1 | $0.4 | 8s | No | 320 |
| Veo 3.1 | $0.4 | 8s | Yes | 640 |
| Sora 2 | $0.1 | 5s | No | 50 |
| Sora 2 | $0.1 | 5s | Yes | 100 |

**Formula:** `credits = (price_per_second * 100) * duration * (audio ? 2 : 1)`

