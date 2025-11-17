# Models Seeding Guide

## Problem
Your app is showing empty models because the `video_features` collection in Firestore is empty.

## Solution
You need to seed the Firestore `video_features` collection with AI model data.

---

## Option 1: Manual Seeding (Recommended for Testing)

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Firestore Database**

### Step 2: Create Collection
1. Click **"Start collection"**
2. Collection ID: `video_features`
3. Click **"Next"**

### Step 3: Add Model Documents

Create documents with these fields:

#### Model 1: Veo 2
**Document ID:** `veo-2`

```json
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "Google's latest high-quality video generation model",
  "price_per_sec": 15,
  "default_duration": 5,
  "duration_options": [3, 5, 10],
  "aspect_ratios": ["16:9", "9:16", "1:1", "21:9"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "preview_url": "https://replicate.delivery/pbxt/example.jpg",
  "replicate_name": "google/veo-2",
  "index": 0,
  "trending": true
}
```

#### Model 2: FLUX.1 [schnell]
**Document ID:** `flux-schnell`

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

#### Model 3: Veo 2 Advanced
**Document ID:** `veo-2-advanced`

```json
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
  "preview_url": "",
  "replicate_name": "google/veo-2",
  "index": 2,
  "trending": false
}
```

---

## Option 2: Use Seeding Script

### Step 1: Install Dependencies
```bash
cd genai-android/functions
npm install
```

### Step 2: Set Environment Variable
```bash
export REPLICATE_API_TOKEN=r8_your_token_here
```

### Step 3: Run Seeding Script

**Option A: Use Simple Seed Script**
```bash
# Compile TypeScript
npm run build

# Run seed script (if you add it to package.json)
node lib/seedModels.js
```

**Option B: Use Fetch Script (Fetches from Replicate API)**
```bash
# Set token
export REPLICATE_API_TOKEN=r8_your_token_here

# Run fetch script
npx ts-node src/fetchReplicateModels.ts
```

---

## Option 3: Firebase Function to Seed (One-time)

Create a temporary Firebase Function to seed models:

```typescript
// functions/src/index.ts - Add this temporarily

export const seedModels = onCall(async ({auth}) => {
  if (!auth) {
    throw new Error("Unauthorized");
  }

  const models = [
    {
      id: "veo-2",
      name: "Veo 2",
      description: "Google's latest video generation model",
      price_per_sec: 15,
      default_duration: 5,
      duration_options: [3, 5, 10],
      aspect_ratios: ["16:9", "9:16", "1:1"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "google/veo-2",
      index: 0,
      trending: true,
    },
    // Add more models...
  ];

  for (const model of models) {
    await firestore.collection("video_features").doc(model.id).set(model);
  }

  return {seeded: models.length};
});
```

Then call it once from your app or Firebase Console, then delete the function.

---

## Finding Real Replicate Models

### Method 1: Replicate Website
1. Go to [replicate.com/models](https://replicate.com/models)
2. Search for "video" or "veo"
3. Find model names like:
   - `google/veo-2`
   - `black-forest-labs/flux-schnell`
   - `anotherjesse/zeroscope-v2-xl`

### Method 2: Replicate API
```bash
curl -H "Authorization: Bearer r8_your_token" \
  https://api.replicate.com/v1/models
```

### Method 3: Python Script
```python
import replicate

client = replicate.Client(api_token="r8_your_token")
models = client.models.list()

for model in models:
    if "video" in model.name.lower() or "veo" in model.name.lower():
        print(f"{model.name}: {model.description}")
```

---

## Field Mappings

| Firestore Field | Type | Description | Example |
|----------------|------|-------------|---------|
| `id` | String | Unique model ID | `"veo-2"` |
| `name` | String | Display name | `"Veo 2"` |
| `description` | String | Model description | `"High-quality video generation"` |
| `price_per_sec` | Number | Credits per second | `15` |
| `default_duration` | Number | Default video length | `5` |
| `duration_options` | Array<Number> | Available durations | `[3, 5, 10]` |
| `aspect_ratios` | Array<String> | Supported ratios | `["16:9", "9:16"]` |
| `requires_first_frame` | Boolean | Needs first frame | `false` |
| `requires_last_frame` | Boolean | Needs last frame | `false` |
| `preview_url` | String | Preview image URL | `"https://..."` |
| `replicate_name` | String | Full Replicate name | `"google/veo-2"` |
| `index` | Number | Display order | `0` |
| `trending` | Boolean | Show as trending | `true` |

---

## Quick Start (Recommended)

**Fastest way to get models showing:**

1. Go to Firebase Console → Firestore
2. Create collection: `video_features`
3. Add at least one document with the fields above
4. Restart your app
5. Models should appear!

**Minimal example (copy-paste into Firestore):**

```json
{
  "id": "veo-2",
  "name": "Veo 2",
  "description": "AI video generation",
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

---

## Verifying Models

After seeding, verify in your app:
1. Open ModelsScreen
2. You should see the models you added
3. Check logcat for any errors

If still empty:
- Check Firestore collection name: `video_features` (exact match)
- Check field names match exactly (case-sensitive)
- Check `replicate_name` is correct (used in function calls)

---

## Common Issues

### Issue: Models still empty after seeding
**Solution:**
- Check collection name is exactly `video_features`
- Check document has `name` and `replicate_name` fields
- Restart app to clear cache

### Issue: Models show but can't generate
**Solution:**
- Verify `replicate_name` matches actual Replicate model
- Check model exists: https://replicate.com/{replicate_name}
- Verify API token has access

### Issue: Wrong model names from Replicate
**Solution:**
- Use the exact model name from Replicate
- Format: `{owner}/{model-name}`
- Example: `google/veo-2` not `veo-2`

---

## Next Steps

1. ✅ Seed at least one model in Firestore
2. ✅ Verify models appear in app
3. ✅ Test video generation
4. ⏳ Add more models as needed
5. ⏳ Customize pricing and features

Your app reads from `video_features` collection, so once you add documents there, models will appear! 🎉

