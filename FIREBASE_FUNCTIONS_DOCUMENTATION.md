# Firebase Functions Documentation
## Based on Reverse Engineered APK

This document details the Firebase Functions that the app calls and what they do.

---

## Overview

The app uses **2 main Firebase Functions**:

1. **`callReplicateVeoAPIV2`** - For video generation
2. **`generateVideoEffect`** - For applying video effects to images

Both functions are called via Firebase Functions HTTPS Callable API from the Android app.

---

## Function 1: `callReplicateVeoAPIV2`

### Purpose
Generates AI videos from text prompts using Replicate API (Veo model).

### Called From
- **Repository:** `VideoGenerateRepository.callReplicateVeoAPI()`
- **Location:** `com.decent.soraai.data.db.VideoGenerateRepository.java:165-167`

### Code Reference
```java
public Object callReplicateVeoAPI(Map<String, ? extends Object> map, Continuation<Object> continuation) {
    return TasksKt.await(
        this.functions.getHttpsCallable("callReplicateVeoAPIV2").call(map), 
        continuation
    );
}
```

### Request Parameters (Map<String, Object>)

Based on `VideoGenerateViewModel.onGenerate()`, the request includes:

```kotlin
Map<String, Object> {
    "prompt": String,                    // User's text prompt
    "aspectRatio": String,               // e.g., "16:9", "9:16", "1:1"
    "duration": Int,                     // Video duration in seconds
    "replicateName": String,            // Replicate model name (from AIModel)
    "userId": String,                    // Firebase user ID
    "firstFrameUrl": String?,            // Optional: First frame image URL
    "lastFrameUrl": String?,            // Optional: Last frame image URL
    "promptOptimizer": Boolean?          // Optional: Enable prompt optimization
}
```

### Parameter Details

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `prompt` | String | ✅ Yes | Text description for video generation |
| `aspectRatio` | String | ✅ Yes | Video aspect ratio (e.g., "16:9", "9:16", "1:1") |
| `duration` | Int | ✅ Yes | Video duration in seconds |
| `replicateName` | String | ✅ Yes | Replicate API model identifier (e.g., "google/veo-2") |
| `userId` | String | ✅ Yes | Firebase user ID |
| `firstFrameUrl` | String | ❌ Optional | URL of first frame image (if model requires) |
| `lastFrameUrl` | String | ❌ Optional | URL of last frame image (if model requires) |
| `promptOptimizer` | Boolean | ❌ Optional | Whether to optimize the prompt before generation |

### What the Function Does (Server-Side)

**Expected Behavior:**
1. **Validates Request:**
   - Checks user authentication
   - Validates parameters
   - Checks user credits (if applicable)

2. **Calls Replicate API:**
   - Uses `replicateName` to identify the model
   - Sends prompt, aspect ratio, duration to Replicate
   - Includes `firstFrameUrl` and `lastFrameUrl` if provided
   - Optionally optimizes prompt if `promptOptimizer` is true

3. **Creates Firestore Document:**
   - Creates document in `users/{userId}/videos/{videoId}`
   - Sets initial status: `status = "inprogress"`
   - Stores request parameters
   - Stores Replicate job ID

4. **Returns Response:**
   - Returns video job ID or initial response
   - May return Firestore document ID

5. **Background Processing:**
   - Function or separate worker monitors Replicate job
   - When complete, updates Firestore:
     - `status = "processed"`
     - `storage_url = {video_url}`
     - `preview_image = {preview_url}`
   - Sends FCM notification to user

### Response Format

The function returns an `Object` (likely a Map or custom object) containing:
- Video ID or job ID
- Status information
- Possibly Firestore document reference

### Example Request

```json
{
  "prompt": "A cat walking on the beach at sunset",
  "aspectRatio": "16:9",
  "duration": 5,
  "replicateName": "google/veo-2",
  "userId": "abc123xyz",
  "firstFrameUrl": "https://firebasestorage.googleapis.com/.../image1.jpeg",
  "lastFrameUrl": "https://firebasestorage.googleapis.com/.../image2.jpeg",
  "promptOptimizer": true
}
```

---

## Function 2: `generateVideoEffect`

### Purpose
Applies video effects to uploaded images.

### Called From
- **Repository:** `EffectRepository.callVideoEffectAPI()`
- **Location:** `com.decent.soraai.data.db.EffectRepository.java:165-167`

### Code Reference
```java
public Object callVideoEffectAPI(Map<String, ? extends Object> map, Continuation<Object> continuation) {
    return TasksKt.await(
        this.functions.getHttpsCallable("generateVideoEffect").call(map), 
        continuation
    );
}
```

### Request Parameters (Map<String, Object>)

Based on `EffectDetailViewModel.generateEffect()`, the request includes:

```kotlin
Map<String, Object> {
    "imageUrl": String,           // Uploaded image URL from Firebase Storage
    "effectId": String,           // Effect identifier
    "effectPrompt": String,       // Effect prompt/description
    "userId": String,             // Firebase user ID
    "appVersion": String,         // App version code
    "aspectRatio": String,        // Effect aspect ratio
    "credits": Int                // Credits required (optional)
}
```

### Parameter Details

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `imageUrl` | String | ✅ Yes | URL of uploaded image from Firebase Storage |
| `effectId` | String | ✅ Yes | Unique effect identifier |
| `effectPrompt` | String | ✅ Yes | Effect prompt/description |
| `userId` | String | ✅ Yes | Firebase user ID |
| `appVersion` | String | ✅ Yes | App version code |
| `aspectRatio` | String | ❌ Optional | Aspect ratio for effect |
| `credits` | Int | ❌ Optional | Credits required for effect |

### What the Function Does (Server-Side)

**Expected Behavior:**
1. **Validates Request:**
   - Checks user authentication
   - Validates image URL
   - Checks user credits (if applicable)

2. **Applies Effect:**
   - Downloads image from Firebase Storage
   - Applies effect using AI model or processing
   - Generates video with effect applied

3. **Creates Firestore Document:**
   - Creates document in `users/{userId}/videos/{videoId}` or similar
   - Sets status: `status = "inprogress"`
   - Stores effect information

4. **Returns Response:**
   - Returns job ID or initial response

5. **Background Processing:**
   - Processes effect application
   - When complete, updates Firestore:
     - `status = "processed"`
     - `storage_url = {video_url}`
   - Sends FCM notification

### Response Format

Similar to `callReplicateVeoAPIV2`, returns job ID or status information.

---

## Image Upload (Before Function Calls)

### Video Generation Images

**Function:** `VideoGenerateRepository.uploadImage()`

**Storage Path:**
```
users/{userId}/inputs/{uuid}.jpeg
```

**Process:**
1. Uploads image to Firebase Storage
2. Gets download URL
3. Returns URL string
4. URL is then passed to `callReplicateVeoAPIV2` as `firstFrameUrl` or `lastFrameUrl`

### Effect Images

**Function:** `EffectRepository.uploadEffectImage()`

**Storage Path:**
```
users/{userId}/effects/{uuid}.jpeg
```

**Process:**
1. Uploads image to Firebase Storage
2. Gets download URL
3. Returns URL string
4. URL is then passed to `generateVideoEffect` as `imageUrl`

---

## Firestore Structure

### Video Documents

**Collection:** `users/{userId}/videos`

**Document Structure:**
```json
{
  "id": "video_id",
  "preview_image": "https://...",
  "storage_url": "https://...",
  "status": "inprogress" | "processed" | "error",
  "request_credits": 25,
  "error_message": null,
  "content_type": "veo3",
  "created_at": Timestamp
}
```

**Status Values:**
- `"inprogress"` - Video generation in progress
- `"processed"` - Video generation completed
- `"error"` - Video generation failed

### App Configuration

**Collection:** `app/config`

**Document Structure:**
```json
{
  "paywall_type": "NORMAL" | "MODERATE" | "HARD",
  "test_version": 13,
  "review_id": "test_user_id"
}
```

---

## Implementation Guide

### For Your Firebase Functions

#### 1. `callReplicateVeoAPIV2` Function

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import Replicate from 'replicate';

const replicate = new Replicate({
  auth: process.env.REPLICATE_API_TOKEN,
});

export const callReplicateVeoAPIV2 = functions.https.onCall(async (data, context) => {
  // 1. Validate authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const userId = context.auth.uid;
  const { prompt, aspectRatio, duration, replicateName, firstFrameUrl, lastFrameUrl, promptOptimizer } = data;

  // 2. Validate parameters
  if (!prompt || !aspectRatio || !duration || !replicateName) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required parameters');
  }

  // 3. Check user credits (if applicable)
  const userDoc = await admin.firestore().collection('users').doc(userId).get();
  const credits = userDoc.data()?.credits || 0;
  const cost = calculateCost(duration); // pricePerSec * duration
  
  if (credits < cost) {
    throw new functions.https.HttpsError('failed-precondition', 'Insufficient credits');
  }

  // 4. Create Firestore document
  const videoRef = admin.firestore()
    .collection('users')
    .doc(userId)
    .collection('videos')
    .doc();
  
  await videoRef.set({
    id: videoRef.id,
    status: 'inprogress',
    request_credits: cost,
    content_type: 'veo3',
    created_at: admin.firestore.FieldValue.serverTimestamp(),
  });

  // 5. Optimize prompt if requested
  let finalPrompt = prompt;
  if (promptOptimizer) {
    finalPrompt = await optimizePrompt(prompt);
  }

  // 6. Call Replicate API
  const input = {
    prompt: finalPrompt,
    aspect_ratio: aspectRatio,
    duration: duration,
    ...(firstFrameUrl && { first_frame_url: firstFrameUrl }),
    ...(lastFrameUrl && { last_frame_url: lastFrameUrl }),
  };

  const output = await replicate.run(replicateName, { input });

  // 7. Update Firestore with job ID
  await videoRef.update({
    replicate_job_id: output.id || output,
    status: 'inprogress',
  });

  // 8. Deduct credits
  await admin.firestore().collection('users').doc(userId).update({
    credits: admin.firestore.FieldValue.increment(-cost),
  });

  // 9. Return response
  return {
    videoId: videoRef.id,
    jobId: output.id || output,
    status: 'inprogress',
  };
});
```

#### 2. Background Worker for Status Updates

```typescript
// Separate function to check Replicate job status
export const checkReplicateJob = functions.pubsub
  .schedule('every 30 seconds')
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // Get all in-progress videos
    const inProgressVideos = await db
      .collectionGroup('videos')
      .where('status', '==', 'inprogress')
      .where('replicate_job_id', '!=', null)
      .get();

    for (const doc of inProgressVideos.docs) {
      const videoData = doc.data();
      const jobId = videoData.replicate_job_id;

      try {
        // Check Replicate job status
        const prediction = await replicate.predictions.get(jobId);
        
        if (prediction.status === 'succeeded') {
          // Update Firestore
          await doc.ref.update({
            status: 'processed',
            storage_url: prediction.output,
            preview_image: prediction.output[0], // First frame
          });

          // Send FCM notification
          await sendNotification(videoData.userId, 'Video ready!');
        } else if (prediction.status === 'failed') {
          await doc.ref.update({
            status: 'error',
            error_message: prediction.error || 'Generation failed',
          });
        }
      } catch (error) {
        console.error(`Error checking job ${jobId}:`, error);
      }
    }
  });
```

#### 3. `generateVideoEffect` Function

```typescript
export const generateVideoEffect = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const userId = context.auth.uid;
  const { imageUrl, effectId, effectPrompt, aspectRatio, credits } = data;

  // Validate and check credits
  // Apply effect (using Replicate or other service)
  // Create Firestore document
  // Return job ID

  // Similar structure to callReplicateVeoAPIV2
});
```

---

## Firestore Seeding Guide

### 1. Seed `video_features` Collection

**Collection:** `video_features`

**Purpose:** Stores available AI models/features

**Document Structure:**
```json
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "Google's latest video generation model",
  "price_per_sec": 15,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "https://...",
  "replicate_name": "google/veo-2",
  "index": 0,
  "trending": true
}
```

**Example Documents:**
```json
// Document 1
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "High-quality video generation",
  "price_per_sec": 15,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1", "21:9"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "https://example.com/preview1.jpg",
  "replicate_name": "google/veo-2",
  "index": 0,
  "trending": true
}

// Document 2
{
  "id": "veo-2-advanced",
  "name": "Veo 2 Advanced",
  "description": "Advanced video generation with frame control",
  "price_per_sec": 20,
  "default_duration": 5,
  "duration_options": [5, 10, 15],
  "aspect_ratios": ["16:9", "9:16"],
  "requires_first_frame": true,
  "requires_last_frame": true,
  "preview_url": "https://example.com/preview2.jpg",
  "replicate_name": "google/veo-2",
  "index": 1,
  "trending": false
}
```

### 2. Seed `users/{uid}` Document

**Collection:** `users`

**Document ID:** User's Firebase UID (from anonymous auth)

**Document Structure:**
```json
{
  "credits": 120
}
```

**How to Get UID:**
1. Run the app
2. Check logcat for Firebase Auth logs
3. Look for: `FirebaseAuth: User signed in anonymously: {uid}`
4. Copy the UID

**Example:**
```json
// users/abc123xyz456
{
  "credits": 120
}
```

### 3. Seed `users/{uid}/videos` (Optional)

**Collection:** `users/{uid}/videos`

**Purpose:** Pre-populate history tab

**Document Structure:**
```json
{
  "id": "video_123",
  "preview_image": "https://example.com/preview.jpg",
  "storage_url": "https://example.com/video.mp4",
  "status": "processed",
  "request_credits": 25,
  "error_message": null,
  "content_type": "veo3",
  "created_at": "2024-01-15T10:30:00Z"
}
```

**Example:**
```json
// users/abc123xyz456/videos/video_123
{
  "id": "video_123",
  "preview_image": "https://example.com/preview1.jpg",
  "storage_url": "https://example.com/video1.mp4",
  "status": "processed",
  "request_credits": 25,
  "content_type": "veo3",
  "created_at": "2024-01-15T10:30:00Z"
}
```

### 4. Seed `app/config` Document

**Collection:** `app`

**Document ID:** `config`

**Document Structure:**
```json
{
  "paywall_type": "NORMAL",
  "test_version": 13,
  "review_id": null
}
```

---

## Summary

### Firebase Functions Called:

1. **`callReplicateVeoAPIV2`**
   - **Purpose:** Generate videos from text prompts
   - **Parameters:** prompt, aspectRatio, duration, replicateName, userId, firstFrameUrl?, lastFrameUrl?, promptOptimizer?
   - **Does:** Calls Replicate API, creates Firestore document, tracks status

2. **`generateVideoEffect`**
   - **Purpose:** Apply effects to images
   - **Parameters:** imageUrl, effectId, effectPrompt, userId, appVersion, aspectRatio?, credits?
   - **Does:** Applies effect, creates Firestore document, tracks status

### Firestore Collections:

- `video_features` - AI models/features configuration
- `users/{uid}` - User data (credits)
- `users/{uid}/videos` - Generated videos
- `app/config` - App configuration

### Storage Paths:

- `users/{uid}/inputs/{uuid}.jpeg` - Input images for video generation
- `users/{uid}/effects/{uuid}.jpeg` - Images for effect application

---

This documentation is based on reverse engineering the APK. The actual Firebase Functions implementation is server-side and not visible in the APK, but this represents what the functions should do based on how the app calls them.

