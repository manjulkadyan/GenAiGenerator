#!/usr/bin/env python3
"""
Robust schema extraction with multiple strategies
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

async def extract_schema_robust(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Extract schema with multiple wait strategies"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        # Navigate and wait for page
        await page.goto(schema_url, wait_until="domcontentloaded", timeout=30000)
        
        # Wait for Next.js to hydrate
        await page.wait_for_selector('script#__NEXT_DATA__', timeout=10000)
        await page.wait_for_timeout(5000)  # Extra wait for React to render
        
        # Try multiple extraction methods
        schema = await page.evaluate("""
            async () => {
                // Wait a bit more for everything to load
                await new Promise(r => setTimeout(r, 2000));
                
                // Method 1: Direct __NEXT_DATA__ access
                let nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    try {
                        let data = JSON.parse(nextScript.textContent);
                        let model = data?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) {
                                return { source: 'next_data_model', schema: model.openapi_schema };
                            }
                            if (model.latest_version?.openapi_schema) {
                                return { source: 'next_data_latest_version', schema: model.latest_version.openapi_schema };
                            }
                            // Try to find schema in other places
                            if (model.versions && model.versions.length > 0) {
                                for (let v of model.versions) {
                                    if (v.openapi_schema) {
                                        return { source: 'next_data_versions', schema: v.openapi_schema };
                                    }
                                }
                            }
                        }
                    } catch (e) {
                        console.log('Error parsing __NEXT_DATA__:', e);
                    }
                }
                
                // Method 2: window.__NEXT_DATA__
                if (window.__NEXT_DATA__) {
                    try {
                        let model = window.__NEXT_DATA__?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) {
                                return { source: 'window_next_data', schema: model.openapi_schema };
                            }
                            if (model.latest_version?.openapi_schema) {
                                return { source: 'window_latest_version', schema: model.latest_version.openapi_schema };
                            }
                        }
                    } catch (e) {}
                }
                
                // Method 3: Look for all script tags with JSON
                let scripts = document.querySelectorAll('script');
                for (let script of scripts) {
                    if (script.textContent && script.textContent.length > 100) {
                        try {
                            let data = JSON.parse(script.textContent);
                            // Check if it's the model data
                            if (data.props?.pageProps?.model) {
                                let model = data.props.pageProps.model;
                                if (model.openapi_schema) {
                                    return { source: 'script_model', schema: model.openapi_schema };
                                }
                                if (model.latest_version?.openapi_schema) {
                                    return { source: 'script_latest_version', schema: model.latest_version.openapi_schema };
                                }
                            }
                            // Check if it's direct schema
                            if (data.input || data.output) {
                                return { source: 'script_direct', schema: data };
                            }
                        } catch (e) {}
                    }
                }
                
                // Method 4: Look for pre/code tags with JSON
                let codeTags = document.querySelectorAll('pre, code');
                for (let tag of codeTags) {
                    let text = tag.textContent.trim();
                    if (text.startsWith('{') && text.length > 50) {
                        try {
                            let json = JSON.parse(text);
                            if (json.input || json.output) {
                                return { source: 'code_tag', schema: json };
                            }
                        } catch (e) {}
                    }
                }
                
                return null;
            }
        """)
        
        if schema and 'schema' in schema:
            return schema['schema']
        
        return None
    except Exception as e:
        print(f"  Exception: {e}")
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
            schema = await extract_schema_robust(model, page)
            if schema:
                all_schemas[model] = schema
                print("✓")
            else:
                print("✗")
            await asyncio.sleep(2)  # Rate limiting
        
        await browser.close()
    
    # Save schemas
    with open('extracted_schemas_robust.json', 'w') as f:
        json.dump(all_schemas, f, indent=2)
    
    # Update the normalized file
    with open('normalized_models_schema.json', 'r') as f:
        normalized = json.load(f)
    
    # Merge schemas into normalized data
    for model_data in normalized:
        model_name = model_data['replicate_name']
        if model_name in all_schemas:
            schema = all_schemas[model_name]
            if isinstance(schema, dict):
                model_data['input_schema'] = schema.get('input', {})
                model_data['output_schema'] = schema.get('output', {})
    
    with open('normalized_models_schema.json', 'w') as f:
        json.dump(normalized, f, indent=2)
    
    print(f"\n✓ Extracted schemas for {len(all_schemas)}/{len(MODELS)} models")
    print("✓ Updated normalized_models_schema.json")

if __name__ == "__main__":
    asyncio.run(main())

