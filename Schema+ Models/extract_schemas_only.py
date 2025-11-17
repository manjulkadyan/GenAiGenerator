#!/usr/bin/env python3
"""
Focused script to extract schemas from Replicate model pages
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

async def extract_schema(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Extract schema using multiple methods"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)  # Wait longer for full render
        
        # Try comprehensive extraction
        schema = await page.evaluate("""
            () => {
                // Method 1: __NEXT_DATA__ script tag
                const nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    try {
                        const data = JSON.parse(nextScript.textContent);
                        const model = data?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) return model.openapi_schema;
                            if (model.latest_version?.openapi_schema) return model.latest_version.openapi_schema;
                        }
                    } catch (e) {}
                }
                
                // Method 2: window.__NEXT_DATA__
                if (window.__NEXT_DATA__) {
                    try {
                        const model = window.__NEXT_DATA__?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) return model.openapi_schema;
                            if (model.latest_version?.openapi_schema) return model.latest_version.openapi_schema;
                        }
                    } catch (e) {}
                }
                
                // Method 3: Look for JSON in all script tags
                const allScripts = document.querySelectorAll('script');
                for (const script of allScripts) {
                    if (script.textContent) {
                        try {
                            const data = JSON.parse(script.textContent);
                            if (data.input || data.output) return data;
                            if (data.props?.pageProps?.model) {
                                const model = data.props.pageProps.model;
                                if (model.openapi_schema) return model.openapi_schema;
                                if (model.latest_version?.openapi_schema) return model.latest_version.openapi_schema;
                            }
                        } catch (e) {}
                    }
                }
                
                // Method 4: Look for pre tags
                const preTags = document.querySelectorAll('pre, code');
                for (const tag of preTags) {
                    try {
                        const json = JSON.parse(tag.textContent);
                        if (json.input || json.output) return json;
                    } catch (e) {}
                }
                
                // Method 5: Try to find schema in page text/JSON-LD
                const jsonLd = document.querySelectorAll('script[type="application/ld+json"]');
                for (const script of jsonLd) {
                    try {
                        const data = JSON.parse(script.textContent);
                        if (data.input || data.output) return data;
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
    schemas = {}
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        for i, model in enumerate(MODELS, 1):
            print(f"[{i}/{len(MODELS)}] {model}...")
            schema = await extract_schema(model, page)
            if schema:
                schemas[model] = schema
                print(f"  ✓ Got schema")
            else:
                print(f"  ✗ No schema found")
            await asyncio.sleep(1)
        
        await browser.close()
    
    # Save schemas
    with open('extracted_schemas.json', 'w') as f:
        json.dump(schemas, f, indent=2)
    
    print(f"\n✓ Extracted schemas for {len(schemas)}/{len(MODELS)} models")
    print("Saved to extracted_schemas.json")

if __name__ == "__main__":
    asyncio.run(main())

