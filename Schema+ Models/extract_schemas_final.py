#!/usr/bin/env python3
"""
Final schema extraction - try main page and network interception
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
    """Try to get schema from main model page"""
    main_url = f"https://replicate.com/{model_name}"
    
    try:
        await page.goto(main_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(3000)
        
        schema = await page.evaluate("""
            () => {
                // Try __NEXT_DATA__ on main page
                const nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    try {
                        const data = JSON.parse(nextScript.textContent);
                        const model = data?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) return model.openapi_schema;
                            if (model.latest_version?.openapi_schema) return model.latest_version.openapi_schema;
                            // Check all versions
                            if (model.versions) {
                                for (const v of model.versions) {
                                    if (v.openapi_schema) return v.openapi_schema;
                                }
                            }
                        }
                    } catch (e) {}
                }
                
                // Try window object
                if (window.__NEXT_DATA__) {
                    try {
                        const model = window.__NEXT_DATA__?.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) return model.openapi_schema;
                            if (model.latest_version?.openapi_schema) return model.latest_version.openapi_schema;
                        }
                    } catch (e) {}
                }
                
                return null;
            }
        """)
        
        return schema
    except Exception as e:
        return None

async def extract_schema_with_network(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Extract schema by intercepting network requests"""
    schema_data = None
    
    async def handle_response(response):
        nonlocal schema_data
        url = response.url
        # Check if this is an API response with schema data
        if 'api' in url.lower() or 'schema' in url.lower() or 'model' in url.lower():
            try:
                if response.headers.get('content-type', '').startswith('application/json'):
                    data = await response.json()
                    # Look for schema in response
                    if isinstance(data, dict):
                        if 'input' in data or 'output' in data:
                            schema_data = data
                        elif 'openapi_schema' in data:
                            schema_data = data['openapi_schema']
                        elif 'model' in data and isinstance(data['model'], dict):
                            model = data['model']
                            if 'openapi_schema' in model:
                                schema_data = model['openapi_schema']
                            elif 'latest_version' in model and model['latest_version']:
                                if 'openapi_schema' in model['latest_version']:
                                    schema_data = model['latest_version']['openapi_schema']
            except:
                pass
    
    page.on('response', handle_response)
    
    # Try schema page
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    try:
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)
    except:
        pass
    
    # Also try main page
    if not schema_data:
        main_url = f"https://replicate.com/{model_name}"
        try:
            await page.goto(main_url, wait_until="networkidle", timeout=30000)
            await page.wait_for_timeout(5000)
        except:
            pass
    
    page.remove_listener('response', handle_response)
    return schema_data

async def extract_schema_comprehensive(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Comprehensive schema extraction"""
    # Method 1: Try network interception
    schema = await extract_schema_with_network(model_name, page)
    if schema:
        return schema
    
    # Method 2: Try main page
    schema = await extract_schema_from_main_page(model_name, page)
    if schema:
        return schema
    
    # Method 3: Try schema page with longer wait
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    try:
        await page.goto(schema_url, wait_until="load", timeout=30000)
        await page.wait_for_timeout(8000)  # Long wait
        
        schema = await page.evaluate("""
            () => {
                // Check all possible locations
                const checks = [
                    () => {
                        const script = document.getElementById('__NEXT_DATA__');
                        if (script) {
                            const data = JSON.parse(script.textContent);
                            return data?.props?.pageProps?.model?.openapi_schema || 
                                   data?.props?.pageProps?.model?.latest_version?.openapi_schema;
                        }
                    },
                    () => window.__NEXT_DATA__?.props?.pageProps?.model?.openapi_schema,
                    () => window.__NEXT_DATA__?.props?.pageProps?.model?.latest_version?.openapi_schema,
                    () => {
                        const scripts = document.querySelectorAll('script');
                        for (const s of scripts) {
                            if (s.textContent) {
                                try {
                                    const d = JSON.parse(s.textContent);
                                    if (d.props?.pageProps?.model) {
                                        const m = d.props.pageProps.model;
                                        return m.openapi_schema || m.latest_version?.openapi_schema;
                                    }
                                } catch {}
                            }
                        }
                    }
                ];
                
                for (const check of checks) {
                    try {
                        const result = check();
                        if (result) return result;
                    } catch {}
                }
                return null;
            }
        """)
        
        return schema
    except:
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
            schema = await extract_schema_comprehensive(model, page)
            if schema:
                all_schemas[model] = schema
                print("✓")
            else:
                print("✗")
            await asyncio.sleep(2)
        
        await browser.close()
    
    # Save schemas
    with open('extracted_schemas_final.json', 'w') as f:
        json.dump(all_schemas, f, indent=2)
    
    # Update normalized file
    try:
        with open('normalized_models_schema.json', 'r') as f:
            normalized = json.load(f)
        
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
    except Exception as e:
        print(f"\nError updating file: {e}")

if __name__ == "__main__":
    asyncio.run(main())

