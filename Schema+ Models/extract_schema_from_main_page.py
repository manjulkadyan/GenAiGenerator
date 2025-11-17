#!/usr/bin/env python3
"""
Extract schema from main model page (not schema page) - it has __NEXT_DATA__
"""

import json
import asyncio
from playwright.async_api import async_playwright
from typing import Dict, Any, Optional

MODELS = [
    "openai/sora-2-pro",
    "openai/sora-2",
    "google/veo-3.1",
    "google/veo-3.1-fast",
    "google/veo-3",
    "wan-video/wan-2.5-t2v-fast",
    "bytedance/seedance-1-lite",
    "wan-video/wan-2.5-i2v",
    "google/veo-3-fast",
    "minimax/hailuo-2.3-fast",
    "bytedance/seedance-1-pro",
    "minimax/hailuo-02",
    "kwaivgi/kling-v2.1-master",
    "wan-video/wan-2.2-t2v-fast",
    "pixverse/pixverse-v5",
    "kwaivgi/kling-v2.5-turbo-pro",
    "runwayml/gen4-image-turbo",
    "lightricks/ltx-2-fast",
    "leonardoai/motion-2.0",
    "lightricks/ltx-2-pro",
    "character-ai/ovi-i2v",
    "luma/ray-2-720p"
]

async def extract_schema_from_main_page(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Extract schema from main model page which has __NEXT_DATA__"""
    main_url = f"https://replicate.com/{model_name}"
    
    try:
        await page.goto(main_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(3000)
        
        schema = await page.evaluate("""
            () => {
                // Get __NEXT_DATA__ from main page
                const nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    try {
                        const data = JSON.parse(nextScript.textContent);
                        const model = data?.props?.pageProps?.model;
                        if (model) {
                            // Try latest_version first
                            if (model.latest_version?.openapi_schema) {
                                return model.latest_version.openapi_schema;
                            }
                            // Try direct openapi_schema
                            if (model.openapi_schema) {
                                return model.openapi_schema;
                            }
                            // Try versions array
                            if (model.versions && Array.isArray(model.versions)) {
                                // Get the latest version
                                const latest = model.versions[0];
                                if (latest?.openapi_schema) {
                                    return latest.openapi_schema;
                                }
                                // Or check all versions
                                for (const v of model.versions) {
                                    if (v.openapi_schema) {
                                        return v.openapi_schema;
                                    }
                                }
                            }
                        }
                    } catch (e) {
                        console.error('Error parsing __NEXT_DATA__:', e);
                    }
                }
                
                // Fallback: check window object
                if (window.__NEXT_DATA__) {
                    try {
                        const model = window.__NEXT_DATA__?.props?.pageProps?.model;
                        if (model) {
                            if (model.latest_version?.openapi_schema) {
                                return model.latest_version.openapi_schema;
                            }
                            if (model.openapi_schema) {
                                return model.openapi_schema;
                            }
                        }
                    } catch (e) {}
                }
                
                return null;
            }
        """)
        
        return schema
    except Exception as e:
        print(f"  Error: {e}")
        return None

async def main():
    """Extract schemas for all models"""
    all_schemas = {}
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context()
        page = await context.new_page()
        
        for i, model in enumerate(MODELS, 1):
            print(f"[{i}/{len(MODELS)}] {model}...", end=" ", flush=True)
            schema = await extract_schema_from_main_page(model, page)
            if schema:
                all_schemas[model] = schema
                print("✓")
            else:
                print("✗")
            await asyncio.sleep(2)
        
        await browser.close()
    
    # Save schemas
    with open('extracted_schemas_from_main.json', 'w') as f:
        json.dump(all_schemas, f, indent=2)
    
    # Update normalized file
    try:
        with open('normalized_models_schema.json', 'r') as f:
            normalized = json.load(f)
        
        updated = 0
        for model_data in normalized:
            model_name = model_data['replicate_name']
            if model_name in all_schemas:
                schema = all_schemas[model_name]
                if isinstance(schema, dict):
                    model_data['input_schema'] = schema.get('input', {})
                    model_data['output_schema'] = schema.get('output', {})
                    updated += 1
        
        with open('normalized_models_schema.json', 'w') as f:
            json.dump(normalized, f, indent=2)
        
        print(f"\n✓ Extracted schemas for {len(all_schemas)}/{len(MODELS)} models")
        print(f"✓ Updated {updated} models in normalized_models_schema.json")
    except Exception as e:
        print(f"\nError updating file: {e}")

if __name__ == "__main__":
    asyncio.run(main())

