# Replicate Webhook Setup Guide

## Overview

We're using **Replicate webhooks** instead of polling for better efficiency:
- ✅ **Instant updates** - No 30-second delay
- ✅ **Lower costs** - No scheduled function runs
- ✅ **More reliable** - Replicate handles retries
- ✅ **Better performance** - Event-driven instead of polling

## Architecture

```
Android App
    ↓
Firebase Function: callReplicateVeoAPIV2
    ↓
Replicate API (creates prediction with webhook URL)
    ↓
Firestore: users/{uid}/jobs/{predictionId} (status: PROCESSING)
    ↓
[Video generation happens on Replicate...]
    ↓
Replicate calls webhook: replicateWebhook
    ↓
Firestore: Update status to COMPLETE/FAILED + store video URL
    ↓
FCM Notification
    ↓
Android App (real-time listener updates UI)
```

## Setup Steps

### 1. Deploy Functions First

```bash
cd genai-android/functions
npm run build
firebase deploy --only functions
```

### 2. Get Webhook URL

After deployment, get your webhook function URL:

**Option A: From Firebase Console**
1. Go to Firebase Console → Functions
2. Find `replicateWebhook` function
3. Copy the function URL

**Option B: From CLI**
```bash
firebase functions:list
# Look for replicateWebhook URL
```

**Format:** `https://{region}-{project-id}.cloudfunctions.net/replicateWebhook`

### 3. Set Webhook URL Environment Variable

**Option A: Set in Firebase Console**
1. Go to Firebase Console → Functions → Configuration
2. Add environment variable:
   - Key: `WEBHOOK_URL`
   - Value: `https://us-central1-your-project.cloudfunctions.net/replicateWebhook`

**Option B: Set via CLI**
```bash
firebase functions:config:set webhook.url="https://us-central1-your-project.cloudfunctions.net/replicateWebhook"
```

**Option C: Update code directly** (temporary)
Edit `functions/src/index.ts` line 62-63 and replace with your actual URL.

### 4. Redeploy Functions

```bash
firebase deploy --only functions
```

## How It Works

### 1. Creating a Prediction with Webhook

When `callReplicateVeoAPIV2` is called:

```typescript
const payload = {
  version: "google/veo-2",
  input: { prompt: "...", ... },
  webhook: "https://...cloudfunctions.net/replicateWebhook",
  webhook_events_filter: ["start", "output", "logs", "completed"]
};
```

Replicate will call the webhook when:
- Prediction starts (`start`)
- New output is available (`output`)
- Logs are available (`logs`)
- Prediction completes (`completed`)

### 2. Webhook Receives Updates

The `replicateWebhook` function:
1. Receives POST request from Replicate
2. Extracts prediction data from request body
3. Finds job document by `replicate_prediction_id`
4. Updates Firestore based on status:
   - `succeeded` → `COMPLETE` + video URL
   - `failed`/`canceled` → `FAILED` + error message
5. Sends FCM notification (if job completed)
6. Returns 200 OK to Replicate

### 3. Webhook Payload

Replicate sends this structure:

```json
{
  "id": "abc123xyz",
  "status": "succeeded",
  "output": "https://replicate.delivery/.../video.mp4",
  "error": null,
  "urls": {
    "get": "https://api.replicate.com/v1/predictions/abc123xyz",
    "cancel": "https://api.replicate.com/v1/predictions/abc123xyz/cancel"
  }
}
```

## Testing

### 1. Test Webhook Locally (Optional)

You can use a tool like [ngrok](https://ngrok.com/) to test webhooks locally:

```bash
# Start local emulator
firebase emulators:start --only functions

# In another terminal, expose local function
ngrok http 5001

# Use ngrok URL as webhook URL
# https://abc123.ngrok.io/replicateWebhook
```

### 2. Test with Replicate

1. Create a test prediction from your app
2. Check Firebase Functions logs:
   ```bash
   firebase functions:log --only replicateWebhook
   ```
3. You should see: `Webhook received for prediction {id}, status: {status}`
4. Check Firestore - job status should update automatically

## Troubleshooting

### Webhook Not Being Called

1. **Check webhook URL is correct:**
   - Must be publicly accessible
   - Must use HTTPS
   - Must return 200 OK

2. **Check Replicate dashboard:**
   - Go to your Replicate account
   - View prediction details
   - Check if webhook was called

3. **Check Firebase Functions logs:**
   ```bash
   firebase functions:log --only replicateWebhook
   ```

### Webhook Returns Errors

The webhook always returns 200 OK to prevent Replicate retries. Check logs for actual errors:

```bash
firebase functions:log --only replicateWebhook
```

### Job Not Found

If webhook receives prediction but can't find job:
- Check `replicate_prediction_id` is stored correctly
- Verify Firestore collection structure
- Check collection group index is created

## Firestore Index Required

The webhook uses a collection group query that requires an index:

**Collection:** `jobs` (collection group)  
**Fields:**
- `replicate_prediction_id` (Ascending)

Firebase will prompt you to create this index on first webhook call, or create it manually in Firebase Console.

## Comparison: Webhooks vs Polling

| Feature | Webhooks ✅ | Polling ❌ |
|---------|------------|-----------|
| **Update Speed** | Instant | 30s delay |
| **Cost** | Only when event occurs | Every 30s |
| **Efficiency** | Event-driven | Constant polling |
| **Reliability** | Replicate handles retries | Manual retry logic |
| **Scalability** | Better | Worse (more jobs = more polling) |

## Security Considerations

1. **Webhook Verification (Optional):**
   - Replicate doesn't require webhook verification by default
   - You can add custom verification if needed
   - Check Replicate documentation for verification methods

2. **Function Access:**
   - Webhook function is public (Replicate needs to call it)
   - Validate prediction IDs to prevent unauthorized updates
   - Current implementation validates prediction ID exists in Firestore

## Next Steps

1. ✅ Deploy functions
2. ✅ Get webhook URL
3. ✅ Set WEBHOOK_URL environment variable
4. ✅ Test with a real prediction
5. ✅ Monitor logs
6. ✅ Verify Firestore updates

## Summary

✅ **Removed:** Scheduled polling function (`checkReplicateJobs`)  
✅ **Added:** Webhook endpoint (`replicateWebhook`)  
✅ **Updated:** `callReplicateVeoAPIV2` to include webhook URL  
✅ **Result:** More efficient, instant updates, lower costs

The workflow is now event-driven and matches production best practices!

