# Seed ALL Text-to-Video Models from Replicate

## Overview

This guide shows you how to fetch and seed **ALL** text-to-video models from Replicate, including:
- ✅ Complete model information
- ✅ API schema details (parameters, aspect ratios, durations)
- ✅ Pricing information
- ✅ Model capabilities (first frame, last frame, image input)

---

## Quick Start

### Step 1: Get Your Replicate API Token

1. Go to [Replicate Account API Tokens](https://replicate.com/account/api-tokens)
2. Copy your API token (starts with `r8_`)

### Step 2: Run the Comprehensive Seed Script

```bash
cd genai-android/functions
export REPLICATE_API_TOKEN=r8_your_token_here
npm run seed:all
```

This will:
1. ✅ Fetch **ALL** models from Replicate API (with pagination)
2. ✅ Filter for text-to-video models
3. ✅ Fetch detailed info for each model (schema, pricing)
4. ✅ Extract parameters (aspect ratios, durations, capabilities)
5. ✅ Seed all models to Firestore `video_features` collection

---

## What Gets Fetched

### Model Information
- **Name & Description** - From Replicate model page
- **Replicate Name** - Full model identifier (e.g., `google/veo-3.1`)
- **Visibility** - Public/private status
- **Links** - GitHub, paper, license URLs

### API Schema Details
The script fetches the OpenAPI schema for each model to extract:
- ✅ **Aspect Ratios** - Supported ratios (16:9, 9:16, etc.)
- ✅ **Durations** - Available duration options
- ✅ **First Frame Support** - Whether model accepts first frame
- ✅ **Last Frame Support** - Whether model accepts last frame
- ✅ **Image Input** - Whether model accepts image input

### Pricing
- Estimated based on model type and name
- Can be adjusted manually in Firestore after seeding

---

## Example Models That Will Be Seeded

Based on [Replicate's text-to-video search](https://replicate.com/search?query=text%20to%20video):

| Model | Replicate Name | Features |
|-------|---------------|----------|
| Veo 3.1 | `google/veo-3.1` | Audio, reference images |
| Kling 2.5 Turbo Pro | `kwaivgi/kling-v2.5-turbo-pro` | Pro-level generation |
| Wan 2.5 T2V Fast | `wan-video/wan-2.5-t2v-fast` | Optimized for speed |
| Veo 2 | `google/veo-2` | High-quality |
| FLUX.1 [schnell] | `black-forest-labs/flux-schnell` | Fast generation |
| Zeroscope v2 XL | `anotherjesse/zeroscope-v2-xl` | Community favorite |
| Runway Gen-3 | `runway/gen3` | Professional |
| ... and **ALL** others | | |

---

## Firestore Document Structure

Each model is stored with this structure:

```json
{
  "id": "veo-3-1",
  "name": "Veo 3.1",
  "description": "Google's state-of-the-art video generation...",
  "price_per_sec": 20,
  "default_duration": 6,
  "duration_options": [4, 6, 8],
  "aspect_ratios": ["16:9", "9:16"],
  "requires_first_frame": false,
  "requires_last_frame": false,
  "supports_image_input": true,
  "preview_url": "",
  "replicate_name": "google/veo-3.1",
  "index": 0,
  "trending": false,
  "official": true,
  "github_url": "https://...",
  "paper_url": "https://...",
  "license_url": "https://...",
  "created_at": "2024-01-01T00:00:00Z"
}
```

---

## Understanding Model Schemas

The script fetches API schemas from URLs like:
- `https://replicate.com/{model}/api/schema`
- `https://replicate.com/{model}/api/api-reference`

Example: [wan-video/wan-2.5-t2v-fast/api/schema](https://replicate.com/wan-video/wan-2.5-t2v-fast/api/schema)

From these schemas, we extract:
- **Input parameters** - What the model accepts
- **Aspect ratio options** - From `aspect_ratio` parameter
- **Duration options** - From `duration` parameter
- **Image inputs** - From `image`, `first_frame`, `last_frame` parameters

---

## Understanding Pricing

Replicate uses a **pay-as-you-go** model. Pricing is based on:

1. **Compute Time** - Time the model is active processing
2. **Hardware Type** - Different models use different hardware
3. **Model Type** - Public vs private models

### Public Models
- ✅ Only pay for **active time** (processing requests)
- ✅ Setup and idle time is **free**
- ✅ Share hardware pool with other customers

### Private Models
- ⚠️ Pay for **all time** (setup + idle + active)
- ⚠️ Dedicated hardware (no sharing)

### Pricing Estimation

The script estimates pricing based on model characteristics:
- **Premium models** (Veo 3.1, Kling Pro): 18-20 credits/sec
- **Mid-tier models** (Veo 2, Runway): 14-16 credits/sec
- **Budget models** (Zeroscope, Wan): 10-12 credits/sec

**Note:** You can adjust pricing in Firestore after seeding based on actual Replicate pricing.

---

## Filtering Logic

The script filters models using these criteria:

### ✅ Included Keywords
- `veo`, `video`, `animate`, `motion`
- `flux`, `runway`, `pika`, `kling`
- `zeroscope`, `wan`, `t2v`, `text2video`
- `gen3`, `gen-3`, `video-gen`

### ❌ Excluded Keywords
- `image` (unless part of "image-to-video")
- `upscale`, `enhance`, `remove`
- `background`, `face`, `detect`
- `audio`, `music`, `speech`, `tts`

---

## Troubleshooting

### Issue: No models found
**Solution:**
- Check your API token is valid
- Verify token has access to models
- Check network connection

### Issue: Rate limiting
**Solution:**
- Script includes 500ms delay between requests
- If still rate limited, increase delay in code
- Run script during off-peak hours

### Issue: Some models fail to seed
**Solution:**
- Check error messages in output
- Some models may have private APIs
- Verify model names are correct

### Issue: Wrong parameters extracted
**Solution:**
- Some models have non-standard schemas
- Manually adjust in Firestore after seeding
- Check model's API documentation

---

## Manual Adjustments

After seeding, you may want to:

1. **Adjust Pricing**
   - Check actual Replicate pricing
   - Update `price_per_sec` in Firestore

2. **Refine Parameters**
   - Check model's actual API schema
   - Update `aspect_ratios`, `duration_options`

3. **Mark Trending**
   - Set `trending: true` for popular models

4. **Add Preview Images**
   - Find preview images from model pages
   - Update `preview_url` in Firestore

---

## Verification

After seeding:

1. ✅ Go to Firebase Console → Firestore
2. ✅ Check `video_features` collection
3. ✅ You should see many models (50+ depending on Replicate)
4. ✅ Restart your Android app
5. ✅ Models should appear in ModelsScreen

---

## Performance

- **Time:** ~5-10 minutes for 50+ models
- **API Calls:** ~100-200 requests (model list + details)
- **Rate Limits:** Script includes delays to avoid limits

---

## Next Steps

1. ✅ Run `npm run seed:all`
2. ✅ Verify models in Firestore
3. ✅ Test in your app
4. ✅ Adjust pricing/parameters as needed
5. ✅ Mark trending models
6. ✅ Add preview images

---

## References

- [Replicate Text-to-Video Search](https://replicate.com/search?query=text%20to%20video)
- [Replicate API Documentation](https://replicate.com/docs)
- [Replicate Billing Information](https://replicate.com/docs/topics/billing)
- [Model API Schema Example](https://replicate.com/wan-video/wan-2.5-t2v-fast/api/schema)

---

## Summary

This comprehensive script will:
- ✅ Fetch **ALL** text-to-video models from Replicate
- ✅ Extract detailed information (schema, pricing)
- ✅ Seed them all to Firestore
- ✅ Make them available in your app

**Run it once and you'll have all models ready to use!** 🎉

