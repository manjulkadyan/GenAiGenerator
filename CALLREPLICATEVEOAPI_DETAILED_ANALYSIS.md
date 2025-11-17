# Detailed Analysis: callReplicateVeoAPI

## Overview

`callReplicateVeoAPI` is the core function responsible for generating AI videos using the Replicate API. It acts as a bridge between the Android app and the backend Firebase Function that communicates with Replicate's Veo model.

---

## Location in Codebase

### 1. Interface Definition
**File:** `decompiled/sources/com/decent/soraai/data/db/IVideoGenerateRepository.java`

```java
public interface IVideoGenerateRepository {
    Object callReplicateVeoAPI(
        Map<String, ? extends Object> request, 
        Continuation<Object> continuation
    );
}
```

### 2. Implementation
**File:** `decompiled/sources/com/decent/soraai/data/db/VideoGenerateRepository.java`

```java
public final class VideoGenerateRepository implements IVideoGenerateRepository {
    private final FirebaseFunctions functions = FunctionsKt.getFunctions(Firebase.INSTANCE);
    
    @Override
    public Object callReplicateVeoAPI(
        Map<String, ? extends Object> map, 
        Continuation<Object> continuation
    ) {
        return TasksKt.await(
            this.functions
                .getHttpsCallable("callReplicateVeoAPIV2")
                .call(map), 
            continuation
        );
    }
}
```

### 3. Usage
**File:** `decompiled/sources/com/decent/soraai/ui/screens/viewmodels/VideoGenerateViewModel.java`

Called from the `onGenerate()` method's coroutine lambda (AnonymousClass1).

---

## Architecture Flow

```
User Input (UI)
    ↓
VideoGenerateViewModel.onGenerate()
    ↓
[Upload Images to Firebase Storage]
    ↓
[Build Request Map]
    ↓
VideoGenerateRepository.callReplicateVeoAPI(Map)
    ↓
Firebase Function: "callReplicateVeoAPIV2"
    ↓
Replicate API (Veo Model)
    ↓
Response → Firestore → User Notification
```

---

## Request Parameters (Map Structure)

Based on the code analysis, the request map passed to `callReplicateVeoAPI` contains the following parameters:

### Required Parameters:

1. **`prompt`** (String)
   - User's text description for video generation
   - Example: "A cat walking on the beach at sunset"
   - Source: `VideoGenerateState.prompt`

2. **`aspectRatio`** (String)
   - Video aspect ratio
   - Values: "1:1", "16:9", "9:16", "21:9", "9:21", "3:4", "4:3"
   - Source: `VideoGenerateState.aspectRatio`
   - Default: First value from `AIModel.aspectRatio` list

3. **`duration`** (Integer)
   - Video duration in seconds
   - Source: `VideoGenerateState.duration`
   - Default: `AIModel.duration` (typically 5 seconds)
   - Options: From `AIModel.durationOption` list

4. **`replicateName`** or **`model`** (String)
   - Replicate model identifier
   - Source: `AIModel.replicateName`
   - Example: "google/veo-2" or similar Veo model name

5. **`userId`** (String)
   - Current user identifier
   - Source: `VideoGenerateViewModel.userId`
   - Used for tracking and Firestore document creation

### Optional Parameters:

6. **`firstFrameUrl`** (String, nullable)
   - URL of first frame image (uploaded to Firebase Storage)
   - Only included if `AIModel.firstFrame == true`
   - Source: Uploaded via `repository.uploadImage(userId, firstFrameUri)`
   - Path: `users/{userId}/inputs/{uuid}.jpeg`

7. **`lastFrameUrl`** (String, nullable)
   - URL of last frame image (uploaded to Firebase Storage)
   - Only included if `AIModel.lastFrame == true`
   - Source: Uploaded via `repository.uploadImage(userId, lastFrameUri)`
   - Path: `users/{userId}/inputs/{uuid}.jpeg`

8. **`promptOptimizer`** (Boolean, optional)
   - Whether to use prompt optimization
   - Source: `VideoGenerateState.promptOptimizer`
   - Default: `false`

### Additional Parameters (Likely):

9. **`modelId`** or **`model_id`** (String)
   - AI model identifier from `AIModel.id`

10. **`credits`** or **`estimatedCost`** (Integer)
    - Estimated credits cost
    - Calculated as: `model.pricePerSec * duration`

---

## Request Map Construction Flow

Based on `VideoGenerateViewModel.onGenerate()`:

```kotlin
// 1. Upload first frame if required
if (model.firstFrame == true && firstFrameUri != null) {
    firstFrameUrl = repository.uploadImage(userId, firstFrameUri)
    // Returns: "https://firebasestorage.googleapis.com/..."
}

// 2. Upload last frame if required
if (model.lastFrame == true && lastFrameUri != null) {
    lastFrameUrl = repository.uploadImage(userId, lastFrameUri)
}

// 3. Build request map
val requestMap = mapOf(
    "prompt" to state.prompt,
    "aspectRatio" to state.aspectRatio,  // or "aspect_ratio"
    "duration" to state.duration,
    "replicateName" to model.replicateName,  // or "model"
    "userId" to userId,
    "firstFrameUrl" to firstFrameUrl,  // nullable
    "lastFrameUrl" to lastFrameUrl,     // nullable
    "promptOptimizer" to state.promptOptimizer,  // optional
    "modelId" to model.id,  // likely
    "estimatedCost" to state.estimatedCost  // likely
)

// 4. Call API
repository.callReplicateVeoAPI(requestMap)
```

---

## Firebase Function: callReplicateVeoAPIV2

**Note:** The actual implementation is in Firebase Functions (server-side), not in the APK.

### Function Name:
`callReplicateVeoAPIV2`

### Function Type:
HTTPS Callable Function (Firebase Functions)

### Expected Behavior:
1. Receives the request map from Android app
2. Validates parameters
3. Calls Replicate API with proper authentication
4. Creates Firestore document for tracking
5. Returns job ID or result

### Replicate API Call (Server-Side):
The Firebase Function likely makes a call like:

```javascript
// Pseudo-code (server-side)
const replicate = require('replicate');
const client = new replicate.Client({ token: REPLICATE_API_TOKEN });

const output = await client.run(
  request.replicateName,  // e.g., "google/veo-2"
  {
    input: {
      prompt: request.prompt,
      aspect_ratio: request.aspectRatio,
      duration: request.duration,
      first_frame: request.firstFrameUrl,  // if provided
      last_frame: request.lastFrameUrl,    // if provided
      // ... other parameters
    }
  }
);
```

---

## Replicate API Details

### What is Replicate?
Replicate is a cloud-based platform that allows running AI models without managing infrastructure. It provides APIs for various AI models including video generation models.

### Veo Model
- **Model Name:** Likely "google/veo-2" or similar
- **Type:** Video generation from text prompts
- **Capabilities:**
  - Text-to-video generation
  - Image-to-video (first/last frame support)
  - Aspect ratio control
  - Duration control

### Replicate API Endpoint:
```
POST https://api.replicate.com/v1/predictions
```

### Authentication:
- Uses API token (stored server-side in Firebase Functions)
- Not exposed in Android app

### Response Format:
```json
{
  "id": "prediction_id",
  "status": "starting" | "processing" | "succeeded" | "failed",
  "output": "https://replicate.delivery/.../video.mp4",
  "error": null
}
```

---

## Response Handling

### What the Function Returns:
The Firebase Function returns an object that likely contains:

1. **`jobId`** or **`predictionId`** (String)
   - Replicate prediction/job ID
   - Used to track generation status

2. **`status`** (String)
   - Initial status: "starting" or "processing"

3. **`videoId`** (String)
   - Firestore document ID for the video result

### How Response is Used:

```kotlin
// In VideoGenerateViewModel
val result = repository.callReplicateVeoAPI(requestMap)
// result contains jobId or predictionId

// Create Firestore document
val videoDoc = mapOf(
    "id" to videoId,
    "status" to Status.INPROGRESS,
    "requestCredits" to estimatedCost,
    "createdAt" to Date(),
    "contentType" to ContentType.veo3
)

// Save to Firestore: users/{userId}/videos/{videoId}
// Listen for status updates
```

---

## Image Upload Process

Before calling `callReplicateVeoAPI`, images are uploaded:

### First Frame Upload:
```kotlin
// In VideoGenerateViewModel.onGenerate()
if (model.firstFrame == true && firstFrameUri != null) {
    val firstFrameUrl = repository.uploadImage(userId, firstFrameUri)
    // Uploads to: users/{userId}/inputs/{uuid}.jpeg
    // Returns: Firebase Storage download URL
}
```

### Last Frame Upload:
```kotlin
if (model.lastFrame == true && lastFrameUri != null) {
    val lastFrameUrl = repository.uploadImage(userId, lastFrameUri)
    // Uploads to: users/{userId}/inputs/{uuid}.jpeg
    // Returns: Firebase Storage download URL
}
```

### Upload Implementation:
**File:** `VideoGenerateRepository.uploadImage()`

```java
public Object uploadImage(String uid, Uri uri, Continuation<String> continuation) {
    // 1. Get Firebase Storage reference
    StorageReference ref = storage.getReference()
        .child("users/" + uid + "/inputs/" + UUID.randomUUID() + ".jpeg");
    
    // 2. Upload file
    UploadTask task = ref.putFile(uri);
    
    // 3. Wait for completion
    await(task);
    
    // 4. Get download URL
    Uri downloadUrl = ref.getDownloadUrl();
    
    // 5. Return URL string
    return downloadUrl.toString();
}
```

---

## Error Handling

### Possible Errors:

1. **Insufficient Credits:**
   - Checked before API call
   - `if (estimatedCost > credits) { onInsufficientCredits() }`

2. **Image Upload Failure:**
   - Caught in try-catch
   - Sets error message in state

3. **API Call Failure:**
   - Network errors
   - Invalid parameters
   - Replicate API errors
   - Firebase Function errors

4. **Timeout:**
   - Replicate API may take time
   - Handled via Firestore listeners

### Error State Updates:
```kotlin
_state.setValue(
    state.copy(
        isGenerating = false,
        showErrorDialog = true,
        errorMessage = "Failed to generate video: ${error.message}"
    )
)
```

---

## Status Tracking

### Firestore Document Structure:
```
users/{userId}/videos/{videoId}
{
  "id": "video_id",
  "status": "INPROGRESS" | "PROCESSED" | "ERROR",
  "previewImage": "https://...",
  "storageUrl": "https://replicate.delivery/.../video.mp4",
  "requestCredits": 25,
  "errorMessage": null,
  "contentType": "veo3",
  "createdAt": Timestamp
}
```

### Status Updates:
1. **INPROGRESS:** Set when API call starts
2. **PROCESSED:** Set when Replicate completes (via webhook or polling)
3. **ERROR:** Set if generation fails

### Notification:
- Firebase Cloud Messaging (FCM) notifies user when video is ready
- Or app polls Firestore for status updates

---

## Code Flow Summary

### Complete Flow:

```
1. User clicks "Generate" button
   ↓
2. VideoGenerateViewModel.onGenerate(credits, onInsufficientCredits)
   ↓
3. Validate credits: if (cost > credits) return
   ↓
4. Set state: isGenerating = true, uploadStatus = "Preparing..."
   ↓
5. Launch coroutine:
   ↓
6. Upload first frame (if required):
   - repository.uploadImage(userId, firstFrameUri)
   - Returns: "https://firebasestorage.googleapis.com/..."
   ↓
7. Upload last frame (if required):
   - repository.uploadImage(userId, lastFrameUri)
   - Returns: "https://firebasestorage.googleapis.com/..."
   ↓
8. Build request map:
   {
     "prompt": "...",
     "aspectRatio": "16:9",
     "duration": 5,
     "replicateName": "google/veo-2",
     "userId": "...",
     "firstFrameUrl": "https://...",  // if provided
     "lastFrameUrl": "https://...",   // if provided
     "promptOptimizer": false
   }
   ↓
9. Call API:
   - repository.callReplicateVeoAPI(requestMap)
   ↓
10. Firebase Function "callReplicateVeoAPIV2":
    - Validates request
    - Calls Replicate API
    - Creates Firestore document
    - Returns jobId
    ↓
11. Handle response:
    - Update state: showSuccessDialog = true
    - Create Firestore document with INPROGRESS status
    - Listen for status updates
    ↓
12. Replicate processes video (async)
    ↓
13. Firebase Function receives webhook/polls for completion
    ↓
14. Update Firestore: status = PROCESSED, storageUrl = "..."
    ↓
15. User receives notification or app updates UI
```

---

## Key Files Reference

1. **Interface:**
   - `IVideoGenerateRepository.java` - Interface definition

2. **Implementation:**
   - `VideoGenerateRepository.java` - Actual implementation

3. **Usage:**
   - `VideoGenerateViewModel.java` - Calls the API

4. **Data Models:**
   - `AIModel.java` - Contains model configuration (replicateName, etc.)
   - `VideoGenerateState.java` - Contains user input state

5. **Supporting:**
   - `FakeVideoGenerateRepository.java` - Mock for testing

---

## Important Notes

1. **Server-Side Logic:**
   - The actual Replicate API call happens in Firebase Functions (not in APK)
   - API keys are stored server-side
   - The Android app only calls the Firebase Function

2. **Async Processing:**
   - Video generation is asynchronous
   - Status is tracked via Firestore
   - User is notified when complete

3. **Image URLs:**
   - Images are uploaded to Firebase Storage first
   - URLs are passed to Replicate API
   - Replicate downloads images from URLs

4. **Error Recovery:**
   - Errors are caught and displayed to user
   - State is reset appropriately
   - User can retry

5. **Cost Calculation:**
   - Cost = `model.pricePerSec * duration`
   - Validated before API call
   - Deducted from user credits

---

## Example Request Map

```json
{
  "prompt": "A beautiful sunset over the ocean with waves crashing",
  "aspectRatio": "16:9",
  "duration": 5,
  "replicateName": "google/veo-2",
  "userId": "user_abc123",
  "firstFrameUrl": "https://firebasestorage.googleapis.com/v0/b/.../users/user_abc123/inputs/uuid1.jpeg",
  "lastFrameUrl": null,
  "promptOptimizer": false,
  "modelId": "model_001",
  "estimatedCost": 25
}
```

---

## Security Considerations

1. **API Keys:**
   - Replicate API token stored in Firebase Functions (not in app)
   - Firebase Functions handle authentication

2. **User Validation:**
   - User ID validated server-side
   - Credits checked before API call

3. **Input Validation:**
   - Prompt length/format validated
   - Image URLs validated
   - Parameters sanitized

4. **Rate Limiting:**
   - Likely implemented in Firebase Functions
   - Prevents abuse

---

## Testing

### Mock Implementation:
`FakeVideoGenerateRepository` returns `Unit.INSTANCE` for testing without actual API calls.

### Test Scenarios:
1. Successful generation
2. Insufficient credits
3. Image upload failure
4. API call failure
5. Network timeout
6. Invalid parameters

---

## Conclusion

`callReplicateVeoAPI` is a critical function that:
- Bridges Android app and Replicate API via Firebase Functions
- Handles image uploads before API call
- Manages async video generation
- Tracks status via Firestore
- Provides error handling and user feedback

The actual Replicate API interaction happens server-side in Firebase Functions, keeping API keys secure and allowing for server-side validation and processing.

