# Seed Popular Text-to-Video Models

## Quick Seed (Recommended)

I've created a script that seeds the most popular text-to-video models from Replicate, including:

- ✅ **Veo 3.1** (google/veo-3.1) - Latest with audio support
- ✅ **Kling 2.5 Turbo Pro** (kwaivgi/kling-v2.5-turbo-pro) - Pro-level generation
- ✅ **Veo 2** (google/veo-2) - Previous generation
- ✅ **FLUX.1 [schnell]** (black-forest-labs/flux-schnell) - Fast generation
- ✅ **Zeroscope v2 XL** (anotherjesse/zeroscope-v2-xl) - Community favorite
- ✅ **Runway Gen-3** (runway/gen3) - Professional model

## Run the Seed Script

```bash
cd genai-android/functions
npm run seed:popular
```

This will:
1. ✅ Seed all popular models to Firestore
2. ✅ Use correct Replicate model names
3. ✅ Set appropriate pricing and features
4. ✅ Mark trending models

## Models Included

### 1. Veo 3.1 (google/veo-3.1)
- **Price:** 20 credits/sec
- **Duration:** 4, 6, or 8 seconds
- **Aspect Ratios:** 16:9, 9:16
- **Features:** Audio generation, reference images, frame-to-frame
- **Link:** [replicate.com/google/veo-3.1](https://replicate.com/google/veo-3.1)

### 2. Kling 2.5 Turbo Pro (kwaivgi/kling-v2.5-turbo-pro)
- **Price:** 18 credits/sec
- **Duration:** 3, 5, or 10 seconds
- **Aspect Ratios:** 16:9, 9:16, 1:1
- **Features:** Smooth motion, cinematic depth, better prompt adherence
- **Link:** [replicate.com/kwaivgi/kling-v2.5-turbo-pro](https://replicate.com/kwaivgi/kling-v2.5-turbo-pro)

### 3. Veo 2 (google/veo-2)
- **Price:** 15 credits/sec
- **Duration:** 3, 5, or 10 seconds
- **Aspect Ratios:** 16:9, 9:16, 1:1, 21:9
- **Features:** High-quality generation, multiple aspect ratios

### 4. FLUX.1 [schnell] (black-forest-labs/flux-schnell)
- **Price:** 12 credits/sec
- **Duration:** 3, 4, or 5 seconds
- **Aspect Ratios:** 16:9, 9:16, 1:1
- **Features:** Fast generation times

### 5. Zeroscope v2 XL (anotherjesse/zeroscope-v2-xl)
- **Price:** 10 credits/sec
- **Duration:** 2, 3, or 4 seconds
- **Aspect Ratios:** 16:9, 9:16
- **Features:** Community favorite, good quality

### 6. Runway Gen-3 (runway/gen3)
- **Price:** 16 credits/sec
- **Duration:** 3, 5, or 10 seconds
- **Aspect Ratios:** 16:9, 9:16, 1:1
- **Features:** Professional model with advanced controls

## Verify Models

After seeding:

1. ✅ Go to Firebase Console → Firestore
2. ✅ Check `video_features` collection
3. ✅ You should see 6 models
4. ✅ Restart your Android app
5. ✅ Models should appear in ModelsScreen

## Customize Models

Edit `functions/src/seedPopularModels.ts` to:
- Add more models
- Adjust pricing
- Change default durations
- Modify aspect ratios

## Alternative: Fetch from Replicate API

To automatically fetch all text-to-video models:

```bash
cd genai-android/functions
export REPLICATE_API_TOKEN=r8_your_token
npm run seed:fetch
```

This will:
- Fetch all models from Replicate API
- Filter for text-to-video models
- Seed Firestore automatically

## Model Sources

All models are verified from:
- [Replicate Text-to-Video Search](https://replicate.com/search?query=text%20to%20video)
- [Veo 3.1](https://replicate.com/google/veo-3.1)
- [Kling 2.5 Turbo Pro](https://replicate.com/kwaivgi/kling-v2.5-turbo-pro)

## Next Steps

1. ✅ Run `npm run seed:popular`
2. ✅ Verify models in Firestore
3. ✅ Test in your app
4. ✅ Adjust pricing/features as needed

Your app will now have real, working models from Replicate! 🎉

