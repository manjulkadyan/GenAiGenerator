# Replicate Models Schema Extraction

This directory contains scripts to extract input/output schemas and pricing information from Replicate models.

## Scripts

1. **fetch_schemas.py** - Uses HTTP requests and HTML parsing (faster, but may miss client-side rendered data)
2. **fetch_schemas_playwright.py** - Uses Playwright headless browser (more reliable, requires Playwright installation)

## Usage

### ⚠️ Important Note
Replicate's pages are client-side rendered, so the HTTP-based script (`fetch_schemas.py`) may not extract all data correctly. **Use the Playwright-based script for reliable extraction.**

### Recommended: Playwright-based extraction
```bash
# Install Playwright first (one-time setup)
pip install playwright
playwright install chromium

# Run the script
python3 fetch_schemas_playwright.py
```

This will:
- Use a headless browser to fully render the pages
- Extract input/output schemas from `/api/schema` pages
- Extract pricing data from main model pages
- Save normalized data to `normalized_models_schema.json`

### Alternative: HTTP-based extraction (may miss data)
```bash
python3 fetch_schemas.py
```

**Note:** This may not work for all models due to client-side rendering. Use Playwright version for accurate results.

## Manual Extraction (for exact data)

If the automated scripts don't capture all data accurately, you can manually extract:

### Getting Input/Output Schema

1. Visit `https://replicate.com/{model_name}/api/schema`
2. Open browser DevTools (F12)
3. Go to Console tab
4. Run:
   ```javascript
   const script = document.getElementById('__NEXT_DATA__');
   const data = JSON.parse(script.textContent);
   const model = data.props.pageProps.model;
   const schema = model.openapi_schema || model.latest_version?.openapi_schema;
   console.log(JSON.stringify(schema, null, 2));
   ```
5. Copy the output JSON

### Getting Pricing

1. Visit `https://replicate.com/{model_name}#pricing`
2. The pricing section shows:
   - Variant names (e.g., "sora-2-pro-standard", "sora-2-pro-high")
   - Price per second (e.g., "$0.30 per second")
   - Quality descriptions

## Output Format

The normalized output follows this structure:

```json
{
  "id": "model-id",
  "name": "Model Name",
  "replicate_name": "owner/model-name",
  "url": "https://replicate.com/owner/model-name",
  "schema_url": "https://replicate.com/owner/model-name/api/schema",
  "pricing_url": "https://replicate.com/owner/model-name#pricing",
  "input_schema": {
    "type": "object",
    "properties": { ... }
  },
  "output_schema": {
    "type": "string",
    "format": "uri"
  },
  "pricing": {
    "variants": [
      {
        "variant": "standard",
        "price_per_second": 0.30,
        "description": "Standard quality"
      }
    ],
    "raw_data": { ... }
  }
}
```

## Models List

The scripts process these 22 models:
1. openai/sora-2-pro
2. openai/sora-2
3. google/veo-3.1
4. google/veo-3.1-fast
5. google/veo-3
6. wan-video/wan-2.5-t2v-fast
7. bytedance/seedance-1-lite
8. wan-video/wan-2.5-i2v
9. google/veo-3-fast
10. minimax/hailuo-2.3-fast
11. bytedance/seedance-1-pro
12. minimax/hailuo-02
13. kwaivgi/kling-v2.1-master
14. wan-video/wan-2.2-t2v-fast
15. pixverse/pixverse-v5
16. kwaivgi/kling-v2.5-turbo-pro
17. runwayml/gen4-image-turbo
18. lightricks/ltx-2-fast
19. leonardoai/motion-2.0
20. lightricks/ltx-2-pro
21. character-ai/ovi-i2v
22. luma/ray-2-720p

## Notes

- The scripts include rate limiting to avoid overwhelming Replicate's servers
- Some data may need manual verification for 100% accuracy
- Pricing information is extracted from the HTML, so verify against the actual pricing page
- Input/output schemas are extracted from the API schema pages

