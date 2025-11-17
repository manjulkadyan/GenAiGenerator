#!/usr/bin/env python3
"""
Extract schema by getting version details using the version ID
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

async def extract_schema_via_version(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Extract schema by getting version ID and fetching version details"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)
        
        # Get version ID from React component props
        version_info = await page.evaluate("""
            () => {
                const reactScripts = document.querySelectorAll('script[id^="react-component-props"]');
                for (const script of reactScripts) {
                    try {
                        const props = JSON.parse(script.textContent);
                        if (props.model?.latest_version?.id) {
                            return {
                                version_id: props.model.latest_version.id,
                                model_name: props.model.name,
                                owner: props.model.owner
                            };
                        }
                    } catch (e) {}
                }
                return null;
            }
        """)
        
        if not version_info:
            return None
        
        # Try to fetch version details via API
        # The schema might be available at a version endpoint
        version_url = f"https://replicate.com/api/models/{version_info['owner']}/{version_info['model_name']}/versions/{version_info['version_id']}"
        
        # Try to get schema from version page or API
        response = await page.goto(version_url, wait_until="networkidle", timeout=30000)
        if response and response.status == 200:
            await page.wait_for_timeout(3000)
            
            schema = await page.evaluate("""
                () => {
                    // Check for schema in React props
                    const reactScripts = document.querySelectorAll('script[id^="react-component-props"]');
                    for (const script of reactScripts) {
                        try {
                            const props = JSON.parse(script.textContent);
                            if (props.version?.openapi_schema) {
                                return props.version.openapi_schema;
                            }
                            if (props.model?.latest_version?.openapi_schema) {
                                return props.model.latest_version.openapi_schema;
                            }
                        } catch (e) {}
                    }
                    return null;
                }
            """)
            
            if schema:
                return schema
        
        # Alternative: Try intercepting network requests for version data
        schema_data = None
        
        async def handle_response(response):
            nonlocal schema_data
            url = response.url
            if 'version' in url.lower() or 'api' in url.lower():
                try:
                    if response.headers.get('content-type', '').startswith('application/json'):
                        data = await response.json()
                        if isinstance(data, dict):
                            if 'openapi_schema' in data:
                                schema_data = data['openapi_schema']
                            elif 'version' in data and isinstance(data['version'], dict):
                                if 'openapi_schema' in data['version']:
                                    schema_data = data['version']['openapi_schema']
                except:
                    pass
        
        page.on('response', handle_response)
        
        # Navigate to version page again to trigger requests
        await page.goto(version_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)
        
        page.remove_listener('response', handle_response)
        
        return schema_data
        
    except Exception as e:
        print(f"  Error: {e}")
        return None

async def main():
    """Test with one model first"""
    model_name = "openai/sora-2-pro"
    print(f"Testing {model_name}...")
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        schema = await extract_schema_via_version(model_name, page)
        if schema:
            print("✓ Found schema!")
            print(f"Schema keys: {list(schema.keys())}")
            with open('test_version_schema.json', 'w') as f:
                json.dump(schema, f, indent=2)
        else:
            print("✗ No schema found")
        
        await browser.close()

if __name__ == "__main__":
    asyncio.run(main())

