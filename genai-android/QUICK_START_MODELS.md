# Quick Start: Add Models to Your App

## Problem
Your app shows empty models because Firestore `video_features` collection is empty.

## ✅ Quickest Solution (2 minutes)

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Click **Firestore Database** (left sidebar)

### Step 2: Create Collection
1. Click **"Start collection"** (or **"Add collection"**)
2. Collection ID: `video_features` (exact match, case-sensitive)
3. Click **"Next"**

### Step 3: Add First Model Document
1. Document ID: `veo-2` (or leave auto-generated)
2. Add these fields (click "Add field" for each):

| Field | Type | Value |
|-------|------|-------|
| `id` | string | `veo-2` |
| `name` | string | `Veo 2` |
| `description` | string | `Google's latest video generation model` |
| `price_per_sec` | number | `15` |
| `default_duration` | number | `5` |
| `duration_options` | array | `[3, 5, 10]` |
| `aspect_ratios` | array | `["16:9", "9:16", "1:1"]` |
| `requires_first_frame` | boolean | `false` |
| `requires_last_frame` | boolean | `false` |
| `preview_url` | string | `` (empty for now) |
| `replicate_name` | string | `google/veo-2` |
| `index` | number | `0` |
| `trending` | boolean | `true` |

3. Click **"Save"**

### Step 4: Verify
1. Restart your Android app
2. Go to Models screen
3. You should see "Veo 2" model! ✅

---

## 📋 Copy-Paste JSON (Even Faster)

Instead of adding fields one by one, you can:

1. Create document with ID: `veo-2`
2. Click the **"</>"** (code view) button in Firestore
3. Paste this JSON:

```json
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "Google's latest high-quality video generation model",
  "price_per_sec": 15,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "google/veo-2",
  "index": 0,
  "trending": true
}
```

4. Click **"Save"**

---

## 🎯 Finding Real Replicate Models

### Method 1: Replicate Website
1. Go to [replicate.com/search?query=text%20to%20video](https://replicate.com/search?query=text%20to%20video)
2. Browse popular models like:
   - [google/veo-3.1](https://replicate.com/google/veo-3.1) - Latest Veo with audio
   - [kwaivgi/kling-v2.5-turbo-pro](https://replicate.com/kwaivgi/kling-v2.5-turbo-pro) - Pro-level generation
3. Click on a model and copy the model name (e.g., `google/veo-3.1`)

### Method 2: Replicate API
```bash
curl -H "Authorization: Bearer r8_your_token" \
  https://api.replicate.com/v1/models?search=video
```

### Method 3: Use Our Fetch Script
```bash
cd genai-android/functions
export REPLICATE_API_TOKEN=r8_your_token
npm run seed:fetch
```

This will:
- Fetch all video models from Replicate
- Automatically seed Firestore
- Filter for video-related models

---

## 📝 Example Models to Add

### Model 1: Veo 3.1 (Google) - Latest & Best
```json
{
  "id": "veo-3-1",
  "name": "Veo 3.1",
  "description": "Google's state-of-the-art video generation with synchronized native audio",
  "price_per_sec": 20,
  "default_duration": 6,
  "duration_options": [4, 6, 8],
  "aspect_ratios": ["16:9", "9:16"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "google/veo-3.1",
  "index": 0,
  "trending": true
}
```

### Model 2: Kling 2.5 Turbo Pro - Popular Pro Model
```json
{
  "id": "kling-v2-5-turbo-pro",
  "name": "Kling 2.5 Turbo Pro",
  "description": "Pro-level text-to-video with smooth motion and cinematic depth",
  "price_per_sec": 18,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "kwaivgi/kling-v2.5-turbo-pro",
  "index": 1,
  "trending": true
}
```

### Model 3: Veo 2 (Google) - Previous Generation
```json
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "Google's high-quality video generation model",
  "price_per_sec": 15,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1", "21:9"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "google/veo-2",
  "index": 2,
  "trending": false
}
```

### Model 4: FLUX.1 [schnell]
```json
{
  "id": "flux-schnell",
  "name": "FLUX.1 [schnell]",
  "description": "Fast image-to-video generation",
  "price_per_sec": 12,
  "default_duration": 4,
  "duration_options": [3, 4, 5],
  "aspect_ratios": ["16:9", "9:16", "1:1"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "black-forest-labs/flux-schnell",
  "index": 1,
  "trending": true
}
```

### Model 5: Zeroscope v2 XL
```json
{
  "id": "zeroscope-v2-xl",
  "name": "Zeroscope v2 XL",
  "description": "High-quality video generation",
  "price_per_sec": 10,
  "default_duration": 3,
  "duration_options": [2, 3, 4],
  "aspect_ratios": ["16:9", "9:16"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "",
  "replicate_name": "anotherjesse/zeroscope-v2-xl",
  "index": 2,
  "trending": false
}
```

---

## ⚠️ Important Notes

1. **Collection Name:** Must be exactly `video_features` (case-sensitive)
2. **Field Names:** Must match exactly (use underscores: `price_per_sec`, not `pricePerSec`)
3. **replicate_name:** Must be the exact model name from Replicate (format: `owner/model-name`)
4. **Arrays:** Use Firestore array type, not strings
5. **Restart App:** After adding models, restart your Android app

---

## 🔍 Verify Model Exists on Replicate

Before adding a model, verify it exists:
1. Go to: `https://replicate.com/{replicate_name}`
2. Example: `https://replicate.com/google/veo-2`
3. If page loads, model exists ✅
4. If 404, model doesn't exist ❌

---

## 🚀 After Seeding

Once you've added at least one model:
1. ✅ Restart Android app
2. ✅ Go to Models screen
3. ✅ You should see your models
4. ✅ Select a model
5. ✅ Try generating a video!

---

## 📚 More Info

See `MODELS_SEEDING_GUIDE.md` for:
- Automated seeding scripts
- Fetching from Replicate API
- Advanced configuration

**Quickest path:** Just add one model manually in Firebase Console and you're done! 🎉

