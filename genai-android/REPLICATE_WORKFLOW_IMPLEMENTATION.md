# Replicate → Firebase Workflow Implementation

## Overview

This document describes the complete workflow for handling Replicate API jobs and updating Firestore documents, matching the behavior from the reverse-engineered APK.

## Architecture

```
Android App
    ↓
Firebase Function: callReplicateVeoAPIV2
    ↓
Replicate API (creates prediction)
    ↓
Firestore: users/{uid}/jobs/{predictionId} (status: PROCESSING)
    ↓
Scheduled Function: checkReplicateJobs (runs every 30s)
    ↓
Replicate API (checks prediction status)
    ↓
Firestore: Update status to COMPLETE/FAILED + store video URL
    ↓
FCM Notification (optional)
    ↓
Android App (real-time listener updates UI)
```

## Implementation Details

### 1. Job Creation (`callReplicateVeoAPIV2`)

**Location:** `functions/src/index.ts`

**What it does:**
1. Receives video generation request from Android app
2. Calls Replicate API to create prediction
3. Creates Firestore document in `users/{uid}/jobs/{predictionId}`
4. Sets initial status: `PROCESSING`
5. Stores `replicate_prediction_id` for later polling

**Firestore Document Structure:**
```json
{
  "prompt": "A cat walking on the beach",
  "model_id": "veo-2",
  "model_name": "google/veo-2",
  "duration_seconds": 5,
  "aspect_ratio": "16:9",
  "status": "PROCESSING",
  "replicate_prediction_id": "abc123xyz",
  "created_at": Timestamp
}
```

### 2. Status Polling (`checkReplicateJobs`)

**Location:** `functions/src/index.ts`

**Schedule:** Runs every 30 seconds

**What it does:**
1. Queries Firestore for all jobs with:
   - `status == "PROCESSING"`
   - `replicate_prediction_id != null`
2. For each job, calls Replicate API to check status
3. Updates Firestore based on Replicate status:
   - **succeeded** → `COMPLETE` + `storage_url` + `preview_url`
   - **failed/canceled** → `FAILED` + `error_message`
   - **starting/processing** → Keep `PROCESSING` (update timestamp)
4. Sends FCM notification when job completes

**Status Mapping:**

| Replicate Status | Firestore Status | Additional Fields |
|------------------|------------------|-------------------|
| `succeeded` | `COMPLETE` | `storage_url`, `preview_url`, `completed_at` |
| `failed` | `FAILED` | `error_message`, `failed_at` |
| `canceled` | `FAILED` | `error_message`, `failed_at` |
| `starting` | `PROCESSING` | `updated_at` |
| `processing` | `PROCESSING` | `updated_at` |

### 3. Firestore Document Updates

**On Success:**
```json
{
  "status": "COMPLETE",
  "storage_url": "https://replicate.delivery/.../video.mp4",
  "preview_url": "https://replicate.delivery/.../video.mp4",
  "completed_at": Timestamp,
  "updated_at": Timestamp
}
```

**On Failure:**
```json
{
  "status": "FAILED",
  "error_message": "Prediction failed: ...",
  "failed_at": Timestamp,
  "updated_at": Timestamp
}
```

### 4. FCM Notifications

**Function:** `sendJobCompleteNotification()`

**What it does:**
- Retrieves user's FCM token from `users/{uid}/fcm_token`
- Sends notification with:
  - Title: "Video Ready!"
  - Body: "Your video generation is complete."
  - Data: `{type: "video_complete", job_id: "...", video_url: "..."}`

**Note:** FCM token must be stored in Firestore by the Android app when user logs in.

## Android App Integration

### Expected Status Values

The Android app expects these status values (from `VideoJobStatus` enum):
- `QUEUED` - Job queued but not started
- `PROCESSING` - Job in progress
- `COMPLETE` - Job completed successfully
- `FAILED` - Job failed

### Real-time Listeners

The app listens to `users/{uid}/jobs` collection:
```kotlin
firestore.collection("users")
    .document(uid)
    .collection("jobs")
    .orderBy("created_at", Query.Direction.DESCENDING)
    .addSnapshotListener { snapshot, error ->
        // Update UI when jobs change
    }
```

### Expected Fields

The app reads these fields from job documents:
- `prompt` - Video prompt
- `model_name` - Model name
- `duration_seconds` - Video duration
- `aspect_ratio` - Aspect ratio
- `status` - Job status (QUEUED, PROCESSING, COMPLETE, FAILED)
- `preview_url` - Preview/video URL (when complete)
- `created_at` - Creation timestamp

## Replicate API Integration

### Creating Predictions

**Endpoint:** `POST https://api.replicate.com/v1/predictions`

**Request:**
```json
{
  "version": "google/veo-2",
  "input": {
    "prompt": "A cat walking on the beach",
    "duration": 5,
    "aspect_ratio": "16:9",
    "first_frame": "https://...",
    "last_frame": "https://..."
  }
}
```

**Response:**
```json
{
  "id": "abc123xyz",
  "status": "starting",
  "urls": {
    "get": "https://api.replicate.com/v1/predictions/abc123xyz"
  }
}
```

### Checking Prediction Status

**Endpoint:** `GET https://api.replicate.com/v1/predictions/{id}`

**Response (Processing):**
```json
{
  "id": "abc123xyz",
  "status": "processing",
  "output": null
}
```

**Response (Success):**
```json
{
  "id": "abc123xyz",
  "status": "succeeded",
  "output": "https://replicate.delivery/.../video.mp4"
}
```

**Response (Failed):**
```json
{
  "id": "abc123xyz",
  "status": "failed",
  "error": "Prediction failed: ..."
}
```

## Deployment Steps

### 1. Set Up Replicate API Token

```bash
# In Firebase Console or via CLI
firebase functions:secrets:set REPLICATE_API_TOKEN
# Enter your Replicate API token when prompted
```

### 2. Deploy Functions

```bash
cd genai-android/functions
npm run build
firebase deploy --only functions
```

### 3. Verify Scheduled Function

After deployment, the `checkReplicateJobs` function will automatically:
- Run every 30 seconds
- Check all PROCESSING jobs
- Update Firestore documents

### 4. Test the Workflow

1. **Create a job** from Android app
2. **Check Firestore** - Should see job with `status: PROCESSING`
3. **Wait 30-60 seconds** - Scheduled function should run
4. **Check Firestore again** - Status should update to `COMPLETE` or `FAILED`
5. **Check Android app** - UI should update via real-time listener

## Monitoring

### View Function Logs

```bash
firebase functions:log --only checkReplicateJobs
```

### Check Function Execution

In Firebase Console:
1. Go to Functions
2. Click on `checkReplicateJobs`
3. View execution history and logs

### Common Issues

1. **Jobs stuck in PROCESSING:**
   - Check Replicate API token is valid
   - Check function logs for errors
   - Verify Replicate prediction ID is correct

2. **Notifications not sending:**
   - Ensure FCM token is stored in `users/{uid}/fcm_token`
   - Check Firebase Messaging is enabled
   - Verify notification permissions in Android app

3. **Status not updating:**
   - Check scheduled function is deployed
   - Verify Firestore indexes are created
   - Check function execution logs

## Firestore Indexes Required

The scheduled function uses a collection group query that requires an index:

**Collection:** `jobs` (collection group)
**Fields:**
- `status` (Ascending)
- `replicate_prediction_id` (Ascending)

Firebase will prompt you to create this index on first deployment, or you can create it manually in Firebase Console.

## Cost Considerations

- **Scheduled Function:** Runs every 30 seconds
- **Replicate API:** Charged per prediction (~$0.75/second for Veo)
- **Firestore Reads:** One read per job check
- **Firestore Writes:** One write per status update

**Optimization Tips:**
- Increase schedule interval if needed (e.g., every 1 minute)
- Limit query results (currently 50 jobs per run)
- Consider using Replicate webhooks instead of polling (more efficient)

## Alternative: Replicate Webhooks

For better efficiency, you could use Replicate webhooks instead of polling:

1. Create a webhook endpoint in Firebase Functions
2. Configure Replicate to call webhook when prediction completes
3. Update Firestore directly from webhook

This eliminates the need for scheduled polling and reduces costs.

## Summary

✅ **Completed:**
- Job creation with Replicate API
- Scheduled function for status polling
- Firestore document updates
- Status mapping (Replicate → App)
- FCM notifications
- Error handling

✅ **Matches APK Behavior:**
- Status transitions: PROCESSING → COMPLETE/FAILED
- Video URL storage in `storage_url` and `preview_url`
- Error message storage
- Real-time updates via Firestore listeners

The workflow is now complete and ready for deployment!

