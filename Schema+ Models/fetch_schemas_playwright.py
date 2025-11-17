#!/usr/bin/env python3
"""
Script to fetch input/output schemas and pricing from Replicate models using Playwright
This provides more reliable extraction from client-side rendered pages
"""

import json
import asyncio
from playwright.async_api import async_playwright
from typing import Dict, List, Any, Optional

# List of all models from the file
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

async def fetch_schema_playwright(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Fetch input and output schema using Playwright"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        
        # Wait for page to fully load
        await page.wait_for_timeout(3000)
        
        # Try to get Next.js data from the page
        schema_data = await page.evaluate("""
            () => {
                // Method 1: Try __NEXT_DATA__
                const nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    try {
                        const nextData = JSON.parse(nextScript.textContent);
                        if (nextData.props && nextData.props.pageProps) {
                            const pageProps = nextData.props.pageProps;
                            if (pageProps.model) {
                                const model = pageProps.model;
                                if (model.openapi_schema) {
                                    return model.openapi_schema;
                                }
                                if (model.latest_version && model.latest_version.openapi_schema) {
                                    return model.latest_version.openapi_schema;
                                }
                            }
                        }
                    } catch (e) {}
                }
                
                // Method 2: Look for schema in script tags with type="application/json"
                const scripts = document.querySelectorAll('script[type="application/json"]');
                for (const script of scripts) {
                    try {
                        const data = JSON.parse(script.textContent);
                        if (data.input || data.output) {
                            return data;
                        }
                    } catch (e) {}
                }
                
                // Method 3: Look for pre tags with JSON
                const preTags = document.querySelectorAll('pre');
                for (const pre of preTags) {
                    try {
                        const json = JSON.parse(pre.textContent);
                        if (json.input || json.output) {
                            return json;
                        }
                    } catch (e) {}
                }
                
                // Method 4: Try to find schema in window object
                if (window.__NEXT_DATA__) {
                    try {
                        const nextData = window.__NEXT_DATA__;
                        if (nextData.props && nextData.props.pageProps) {
                            const pageProps = nextData.props.pageProps;
                            if (pageProps.model) {
                                const model = pageProps.model;
                                if (model.openapi_schema) {
                                    return model.openapi_schema;
                                }
                                if (model.latest_version && model.latest_version.openapi_schema) {
                                    return model.latest_version.openapi_schema;
                                }
                            }
                        }
                    } catch (e) {}
                }
                
                return null;
            }
        """)
        
        return schema_data
    except Exception as e:
        print(f"  Exception fetching schema for {model_name}: {e}")
        return None

async def fetch_pricing_playwright(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Fetch pricing information using Playwright"""
    main_url = f"https://replicate.com/{model_name}"
    
    try:
        await page.goto(main_url, wait_until="networkidle", timeout=30000)
        
        # Wait for page to load
        await page.wait_for_timeout(2000)
        
        # Scroll to pricing section
        try:
            await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            await page.wait_for_timeout(1000)
        except:
            pass
        
        # Extract pricing data
        pricing_info = await page.evaluate("""
            () => {
                const result = {
                    variants: [],
                    raw_html: null,
                    raw_text: null,
                    billing_config: null
                };
                
                // Try to find pricing JSON in script tags
                const scripts = document.querySelectorAll('script[type="application/json"]');
                for (const script of scripts) {
                    try {
                        const data = JSON.parse(script.textContent);
                        if (data.billingConfig) {
                            result.billing_config = data.billingConfig;
                            // Parse tiers into variants
                            if (data.billingConfig.current_tiers) {
                                data.billingConfig.current_tiers.forEach(tier => {
                                    if (tier.prices && tier.prices.length > 0) {
                                        tier.prices.forEach(price => {
                                            const variant = {
                                                variant: tier.criteria && tier.criteria[0] ? tier.criteria[0].value : null,
                                                variant_name: tier.title || null,
                                                price_per_second: parseFloat(price.price.replace('$', '')),
                                                description: price.description || tier.description || '',
                                                metric: price.metric || null
                                            };
                                            result.variants.push(variant);
                                        });
                                    }
                                });
                            }
                        }
                    } catch (e) {}
                }
                
                // Also get raw HTML and text
                const section = document.getElementById('pricing') || 
                               document.querySelector('[id*="pricing"]') ||
                               document.querySelector('[class*="pricing"]');
                if (section) {
                    result.raw_html = section.innerHTML;
                    result.raw_text = section.innerText;
                }
                
                return result;
            }
        """)
        
        if pricing_info:
            pricing_info["url"] = f"https://replicate.com/{model_name}#pricing"
            return pricing_info
        
        return None
        
    except Exception as e:
        print(f"  Exception fetching pricing for {model_name}: {e}")
        return None

def normalize_model_data(model_name: str, schema_data: Optional[Dict], pricing_data: Optional[Dict]) -> Dict[str, Any]:
    """Normalize model data into consistent format matching user's example structure"""
    
    model_id = model_name.split("/")[-1]
    name_parts = model_id.split("-")
    display_name = " ".join(word.capitalize() for word in name_parts)
    
    input_schema = {}
    output_schema = {}
    
    if schema_data:
        if isinstance(schema_data, dict):
            input_schema = schema_data.get("input", {})
            output_schema = schema_data.get("output", {})
    
    pricing_variants = []
    if pricing_data:
        if "variants" in pricing_data and pricing_data["variants"]:
            pricing_variants = pricing_data["variants"]
        elif isinstance(pricing_data, list):
            pricing_variants = pricing_data
    
    normalized = {
        "id": model_id,
        "name": display_name,
        "replicate_name": model_name,
        "url": f"https://replicate.com/{model_name}",
        "schema_url": f"https://replicate.com/{model_name}/api/schema",
        "pricing_url": f"https://replicate.com/{model_name}#pricing",
        "input_schema": input_schema,
        "output_schema": output_schema,
        "pricing": {
            "variants": pricing_variants,
            "raw_data": pricing_data if pricing_data else {}
        }
    }
    
    return normalized

async def main():
    """Main function to fetch all model data"""
    all_models_data = []
    
    print(f"Fetching data for {len(MODELS)} models using Playwright...")
    print("Note: This requires Playwright to be installed: pip install playwright && playwright install chromium")
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        for i, model in enumerate(MODELS, 1):
            print(f"\n[{i}/{len(MODELS)}] Processing {model}...")
            
            # Fetch schema
            print(f"  Fetching schema...")
            schema_data = await fetch_schema_playwright(model, page)
            
            # Fetch pricing
            print(f"  Fetching pricing...")
            pricing_data = await fetch_pricing_playwright(model, page)
            
            # Normalize data
            normalized = normalize_model_data(model, schema_data, pricing_data)
            all_models_data.append(normalized)
            
            print(f"  ✓ Completed {model}")
            await asyncio.sleep(1)  # Rate limiting
        
        await browser.close()
    
    # Save to JSON file
    output_file = "normalized_models_schema.json"
    with open(output_file, 'w') as f:
        json.dump(all_models_data, f, indent=2)
    
    print(f"\n✓ Saved normalized data to {output_file}")
    print(f"Total models processed: {len(all_models_data)}")
    
    return all_models_data

if __name__ == "__main__":
    asyncio.run(main())

