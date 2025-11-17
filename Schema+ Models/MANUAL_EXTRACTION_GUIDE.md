
# Manual Schema Extraction Guide

## Instructions

1. Run this script to open each model's schema page
2. On each page, open browser console (F12 → Console tab)
3. Copy and paste the console command below
4. Copy the JSON output
5. Save it to the schemas.json file (format shown below)

## Console Command


// Copy and paste this in browser console (F12) on the schema page:

// Method 1: Try React component props
const scripts = document.querySelectorAll('script[id^="react-component-props"]');
for (const script of scripts) {
    try {
        const props = JSON.parse(script.textContent);
        if (props.version?.openapi_schema) {
            console.log(JSON.stringify(props.version.openapi_schema, null, 2));
            break;
        }
        if (props.model?.latest_version?.openapi_schema) {
            console.log(JSON.stringify(props.model.latest_version.openapi_schema, null, 2));
            break;
        }
    } catch (e) {}
}

// Method 2: Check window object
if (window.__NEXT_DATA__) {
    const model = window.__NEXT_DATA__?.props?.pageProps?.model;
    if (model?.latest_version?.openapi_schema) {
        console.log(JSON.stringify(model.latest_version.openapi_schema, null, 2));
    }
}

// Method 3: Look for JSON in page
const preTags = document.querySelectorAll('pre');
for (const pre of preTags) {
    try {
        const json = JSON.parse(pre.textContent);
        if (json.input || json.output) {
            console.log(JSON.stringify(json, null, 2));
            break;
        }
    } catch (e) {}
}


## Models to Process

1. openai/sora-2-pro
   URL: https://replicate.com/openai/sora-2-pro/api/schema

2. openai/sora-2
   URL: https://replicate.com/openai/sora-2/api/schema

3. google/veo-3.1
   URL: https://replicate.com/google/veo-3.1/api/schema

4. google/veo-3.1-fast
   URL: https://replicate.com/google/veo-3.1-fast/api/schema

5. google/veo-3
   URL: https://replicate.com/google/veo-3/api/schema

6. wan-video/wan-2.5-t2v-fast
   URL: https://replicate.com/wan-video/wan-2.5-t2v-fast/api/schema

7. bytedance/seedance-1-lite
   URL: https://replicate.com/bytedance/seedance-1-lite/api/schema

8. wan-video/wan-2.5-i2v
   URL: https://replicate.com/wan-video/wan-2.5-i2v/api/schema

9. google/veo-3-fast
   URL: https://replicate.com/google/veo-3-fast/api/schema

10. minimax/hailuo-2.3-fast
   URL: https://replicate.com/minimax/hailuo-2.3-fast/api/schema

11. bytedance/seedance-1-pro
   URL: https://replicate.com/bytedance/seedance-1-pro/api/schema

12. minimax/hailuo-02
   URL: https://replicate.com/minimax/hailuo-02/api/schema

13. kwaivgi/kling-v2.1-master
   URL: https://replicate.com/kwaivgi/kling-v2.1-master/api/schema

14. wan-video/wan-2.2-t2v-fast
   URL: https://replicate.com/wan-video/wan-2.2-t2v-fast/api/schema

15. pixverse/pixverse-v5
   URL: https://replicate.com/pixverse/pixverse-v5/api/schema

16. kwaivgi/kling-v2.5-turbo-pro
   URL: https://replicate.com/kwaivgi/kling-v2.5-turbo-pro/api/schema

17. runwayml/gen4-image-turbo
   URL: https://replicate.com/runwayml/gen4-image-turbo/api/schema

18. lightricks/ltx-2-fast
   URL: https://replicate.com/lightricks/ltx-2-fast/api/schema

19. leonardoai/motion-2.0
   URL: https://replicate.com/leonardoai/motion-2.0/api/schema

20. lightricks/ltx-2-pro
   URL: https://replicate.com/lightricks/ltx-2-pro/api/schema

21. character-ai/ovi-i2v
   URL: https://replicate.com/character-ai/ovi-i2v/api/schema

22. luma/ray-2-720p
   URL: https://replicate.com/luma/ray-2-720p/api/schema


## Output Format

Create a file `manual_schemas.json` with this structure:

{
  "openai/sora-2-pro": {
    "input": { ... },
    "output": { ... }
  },
  "openai/sora-2": {
    "input": { ... },
    "output": { ... }
  },
  ...
}

Then run: python3 merge_manual_schemas.py
