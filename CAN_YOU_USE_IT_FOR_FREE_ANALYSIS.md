# Can You Call callReplicateVeoAPIV2 Directly and Use It For Free?

## Short Answer

**No, it's NOT free.** Replicate API charges approximately **$0.75 per second** for video generation (about **$6.00 for an 8-second video**).

However, you CAN call it directly from your app, but you'll need:
1. Your own Replicate API key (paid)
2. To bypass the Firebase Function
3. To handle billing yourself

---

## Current Architecture

### How It Works Now:

```
Your App
  ↓
Firebase Function: "callReplicateVeoAPIV2"
  ↓ (hides API key, validates, bills)
Replicate API
  ↓
Video Generated
```

**The Firebase Function:**
- ✅ Hides the Replicate API key (security)
- ✅ Validates user requests
- ✅ Manages credits/billing
- ✅ Tracks usage in Firestore
- ✅ Prevents abuse

---

## Can You Call Replicate API Directly?

### Yes, Technically Possible

You can bypass the Firebase Function and call Replicate API directly, but you'll need:

### Requirements:

1. **Replicate API Account & Key**
   - Sign up at https://replicate.com
   - Get your API token
   - Add payment method (credit card required)

2. **Direct API Implementation**

Instead of:
```kotlin
// Current (via Firebase Function)
repository.callReplicateVeoAPI(requestMap)
```

You'd need:
```kotlin
// Direct Replicate API call
suspend fun callReplicateDirectly(
    prompt: String,
    aspectRatio: String,
    duration: Int,
    modelName: String,
    firstFrameUrl: String?,
    lastFrameUrl: String?
): Result<String> {
    val client = OkHttpClient()
    val json = JSONObject().apply {
        put("version", modelName) // e.g., "google/veo-2"
        put("input", JSONObject().apply {
            put("prompt", prompt)
            put("aspect_ratio", aspectRatio)
            put("duration", duration)
            firstFrameUrl?.let { put("first_frame", it) }
            lastFrameUrl?.let { put("last_frame", it) }
        })
    }
    
    val request = Request.Builder()
        .url("https://api.replicate.com/v1/predictions")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .addHeader("Authorization", "Token YOUR_REPLICATE_API_KEY")
        .addHeader("Content-Type", "application/json")
        .build()
    
    val response = client.newCall(request).execute()
    // Handle response...
}
```

3. **API Endpoint**
   ```
   POST https://api.replicate.com/v1/predictions
   ```

4. **Authentication Header**
   ```
   Authorization: Token r8_xxxxxxxxxxxxx
   ```

---

## Cost Analysis

### Replicate API Pricing:

| Service | Cost per Second | 8-Second Video |
|---------|----------------|----------------|
| **Replicate (Official)** | **$0.75** | **$6.00** |
| Veo3Gen | $0.12 | $0.96 |
| Kie.ai | $0.05 | $0.40 |
| Veo3API.ai | $0.05 | $0.40 |

### Free Tier Options:

**Veo3API.com** offers:
- 100 free credits per month
- ~20 Veo 3.1 Fast generations monthly
- After that, paid plans

**Replicate itself:**
- ❌ No free tier for Veo model
- Requires payment method upfront
- Pay-as-you-go pricing

---

## What You'd Need to Change

### 1. Modify VideoGenerateRepository

**Current Code:**
```kotlin
// VideoGenerateRepository.kt
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
```

**Direct API Version:**
```kotlin
// DirectReplicateRepository.kt
public Object callReplicateDirectly(
    Map<String, ? extends Object> map, 
    Continuation<Object> continuation
) {
    // Build Replicate API request
    val requestBody = buildReplicateRequest(map)
    
    // Make HTTP POST to Replicate API
    val response = httpClient.post(
        "https://api.replicate.com/v1/predictions",
        headers = mapOf(
            "Authorization" to "Token YOUR_API_KEY",
            "Content-Type" to "application/json"
        ),
        body = requestBody
    )
    
    return parseResponse(response)
}
```

### 2. Store API Key Securely

**⚠️ CRITICAL SECURITY WARNING:**

**DO NOT hardcode API key in your app!**

```kotlin
// ❌ BAD - Never do this!
val API_KEY = "r8_xxxxxxxxxxxxx" // Anyone can extract this!

// ✅ GOOD - Use secure storage
val apiKey = SecurePreferences.get("replicate_api_key")
// Or use Android Keystore
// Or use environment variables (for server-side)
```

**Better Approach:**
- Use Android Keystore
- Or keep it server-side (Firebase Functions)
- Or use environment variables (if server-side)

### 3. Handle Response Format

Replicate API returns:
```json
{
  "id": "prediction_id",
  "status": "starting",
  "output": null,
  "error": null,
  "urls": {
    "get": "https://api.replicate.com/v1/predictions/xxx",
    "cancel": "https://api.replicate.com/v1/predictions/xxx/cancel"
  }
}
```

You'll need to:
1. Poll the `get` URL for status updates
2. Wait for `status: "succeeded"`
3. Extract `output` URL (video URL)

---

## Security & Billing Implications

### Problems with Direct API Calls:

1. **API Key Exposure Risk**
   - If stored in app, can be extracted via reverse engineering
   - Anyone can use your API key → you pay for their usage
   - **Solution:** Keep key server-side

2. **No Billing Control**
   - Users can make unlimited requests
   - You pay for everything
   - **Solution:** Implement rate limiting & billing

3. **No Validation**
   - No user authentication
   - No input validation
   - **Solution:** Add validation layer

4. **Abuse Risk**
   - Malicious users can spam requests
   - High costs
   - **Solution:** Rate limiting, CAPTCHA, etc.

### Why Firebase Function Exists:

The Firebase Function provides:
- ✅ **Security:** API key hidden server-side
- ✅ **Billing Control:** Credits system prevents abuse
- ✅ **Validation:** User authentication & input validation
- ✅ **Rate Limiting:** Prevents spam
- ✅ **Tracking:** Firestore logs all requests
- ✅ **Error Handling:** Centralized error management

---

## Alternative: Use Cheaper Providers

If you want to reduce costs, consider these alternatives:

### 1. Veo3API.com (Free Tier Available)

```kotlin
// Free tier: 100 credits/month (~20 videos)
suspend fun callVeo3API(
    prompt: String,
    aspectRatio: String,
    duration: Int
): Result<String> {
    val request = Request.Builder()
        .url("https://api.veo3api.com/v1/generate")
        .post(buildRequestBody(prompt, aspectRatio, duration))
        .addHeader("Authorization", "Bearer YOUR_VEO3API_KEY")
        .build()
    // ...
}
```

**Pricing:**
- Free: 100 credits/month
- Paid: $0.12/second (cheaper than Replicate)

### 2. Kie.ai

**Pricing:** $0.05/second (much cheaper)

### 3. Veo3API.ai

**Pricing:** $0.05/second (much cheaper)

---

## Implementation Example

### Complete Direct Implementation:

```kotlin
class DirectReplicateRepository {
    private val apiKey = "YOUR_API_KEY" // Store securely!
    private val client = OkHttpClient()
    
    suspend fun generateVideo(
        prompt: String,
        aspectRatio: String,
        duration: Int,
        modelName: String = "google/veo-2",
        firstFrameUrl: String? = null,
        lastFrameUrl: String? = null
    ): Result<PredictionResponse> = withContext(Dispatchers.IO) {
        try {
            // Build request
            val input = JSONObject().apply {
                put("prompt", prompt)
                put("aspect_ratio", aspectRatio)
                put("duration", duration)
                firstFrameUrl?.let { put("first_frame", it) }
                lastFrameUrl?.let { put("last_frame", it) }
            }
            
            val body = JSONObject().apply {
                put("version", modelName)
                put("input", input)
            }
            
            // Make request
            val request = Request.Builder()
                .url("https://api.replicate.com/v1/predictions")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Token $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val prediction = PredictionResponse(
                    id = json.getString("id"),
                    status = json.getString("status"),
                    output = json.optString("output", null)
                )
                Result.success(prediction)
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Poll for completion
    suspend fun pollPrediction(predictionId: String): Result<String> {
        while (true) {
            val request = Request.Builder()
                .url("https://api.replicate.com/v1/predictions/$predictionId")
                .get()
                .addHeader("Authorization", "Token $apiKey")
                .build()
            
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            
            when (json.getString("status")) {
                "succeeded" -> {
                    val output = json.getString("output")
                    return Result.success(output)
                }
                "failed" -> {
                    val error = json.optString("error", "Unknown error")
                    return Result.failure(Exception(error))
                }
                "starting", "processing" -> {
                    delay(2000) // Wait 2 seconds before polling again
                }
            }
        }
    }
}
```

---

## Recommendation

### Best Approach:

1. **Keep Using Firebase Function** (if you have access)
   - Most secure
   - Already handles billing
   - No code changes needed

2. **If You Must Go Direct:**
   - Use cheaper alternatives (Veo3API.com, Kie.ai)
   - Keep API key server-side (Firebase Functions)
   - Implement rate limiting
   - Add billing/credit system
   - Monitor usage closely

3. **Free Tier Option:**
   - Use Veo3API.com free tier (100 credits/month)
   - Good for testing/development
   - Upgrade to paid when needed

---

## Cost Comparison Example

### Scenario: 100 videos/month (5 seconds each)

| Provider | Cost per Video | Monthly Cost |
|----------|---------------|--------------|
| Replicate | $3.75 | $375.00 |
| Veo3Gen | $0.60 | $60.00 |
| Kie.ai | $0.25 | $25.00 |
| Veo3API.ai | $0.25 | $25.00 |
| Veo3API.com (Free) | $0.00 | $0.00 (first 20) |

**Savings:** Using alternatives can save **$350-375/month**!

---

## Conclusion

1. **Can you call it directly?** ✅ Yes, but requires API key
2. **Is it free?** ❌ No, Replicate charges $0.75/second
3. **Should you do it?** ⚠️ Only if:
   - You have your own Replicate account
   - You implement proper security (server-side key)
   - You handle billing/rate limiting
   - Or use cheaper alternatives

4. **Best Option:** Use cheaper alternatives like Veo3API.com (has free tier) or keep using Firebase Function if you have access to it.

---

## Security Checklist

If you implement direct API calls:

- [ ] Store API key server-side (Firebase Functions)
- [ ] Implement rate limiting
- [ ] Add user authentication
- [ ] Validate all inputs
- [ ] Monitor API usage
- [ ] Set spending limits
- [ ] Handle errors gracefully
- [ ] Log all requests
- [ ] Use HTTPS only
- [ ] Implement request signing (optional)

---

## Code Example: Secure Server-Side Implementation

**Firebase Function (Recommended):**

```javascript
// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const Replicate = require('replicate');

admin.initializeApp();
const replicate = new Replicate({
  auth: functions.config().replicate.api_key // Stored securely
});

exports.callReplicateVeoAPIV2 = functions.https.onCall(async (data, context) => {
  // 1. Authenticate user
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  
  // 2. Validate credits
  const userDoc = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .get();
  const credits = userDoc.data()?.credits || 0;
  const cost = data.duration * 0.75; // $0.75/second
  
  if (credits < cost) {
    throw new functions.https.HttpsError('failed-precondition', 'Insufficient credits');
  }
  
  // 3. Call Replicate API
  const output = await replicate.run(data.replicateName, {
    input: {
      prompt: data.prompt,
      aspect_ratio: data.aspectRatio,
      duration: data.duration,
      first_frame: data.firstFrameUrl,
      last_frame: data.lastFrameUrl
    }
  });
  
  // 4. Deduct credits
  await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .update({
      credits: admin.firestore.FieldValue.increment(-cost)
    });
  
  // 5. Save to Firestore
  const videoRef = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .collection('videos')
    .add({
      status: 'PROCESSED',
      storageUrl: output,
      requestCredits: cost,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  
  return { videoId: videoRef.id, videoUrl: output };
});
```

This approach:
- ✅ Keeps API key secure
- ✅ Validates user & credits
- ✅ Prevents abuse
- ✅ Tracks usage
- ✅ Handles billing

---

**Bottom Line:** You CAN call it directly, but it's NOT free and requires careful security implementation. Consider cheaper alternatives or keep using the Firebase Function if available.

