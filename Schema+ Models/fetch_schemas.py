#!/usr/bin/env python3
"""
Script to fetch input/output schemas and pricing from Replicate models
"""

import json
import requests
from bs4 import BeautifulSoup
import time
import re
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

def extract_nextjs_data(html_content: str) -> Optional[Dict[str, Any]]:
    """Extract Next.js __NEXT_DATA__ from HTML"""
    # Method 1: Find script tag with id="__NEXT_DATA__"
    soup = BeautifulSoup(html_content, 'html.parser')
    next_script = soup.find('script', id='__NEXT_DATA__')
    if next_script and next_script.string:
        try:
            return json.loads(next_script.string)
        except json.JSONDecodeError:
            pass
    
    # Method 2: Try regex pattern for script tag
    match = re.search(r'<script[^>]*id=["\']__NEXT_DATA__["\'][^>]*>(.*?)</script>', html_content, re.DOTALL | re.IGNORECASE)
    if match:
        try:
            return json.loads(match.group(1))
        except json.JSONDecodeError:
            pass
    
    # Method 3: Try to find __NEXT_DATA__ assignment in any script
    match = re.search(r'__NEXT_DATA__\s*=\s*({.+?});', html_content, re.DOTALL)
    if match:
        try:
            data_str = match.group(1)
            return json.loads(data_str)
        except json.JSONDecodeError:
            pass
    
    # Method 4: Try to find it as a variable in script content
    scripts = soup.find_all('script')
    for script in scripts:
        if script.string and '__NEXT_DATA__' in script.string:
            # Try to extract the JSON object
            # Look for the pattern: __NEXT_DATA__ = {...};
            match = re.search(r'__NEXT_DATA__\s*=\s*(\{.*?\});', script.string, re.DOTALL)
            if match:
                try:
                    return json.loads(match.group(1))
                except json.JSONDecodeError:
                    # Try to find balanced braces
                    start = script.string.find('__NEXT_DATA__')
                    if start != -1:
                        # Find the opening brace after the assignment
                        brace_start = script.string.find('{', start)
                        if brace_start != -1:
                            # Count braces to find the end
                            brace_count = 0
                            for i in range(brace_start, len(script.string)):
                                if script.string[i] == '{':
                                    brace_count += 1
                                elif script.string[i] == '}':
                                    brace_count -= 1
                                    if brace_count == 0:
                                        try:
                                            return json.loads(script.string[brace_start:i+1])
                                        except json.JSONDecodeError:
                                            break
    
    return None

def fetch_schema(model_name: str) -> Optional[Dict[str, Any]]:
    """Fetch input and output schema from Replicate API schema page"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        response = requests.get(schema_url, timeout=15, headers={
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })
        if response.status_code == 200:
            html_content = response.text
            
            # Try to extract Next.js data
            next_data = extract_nextjs_data(html_content)
            if next_data:
                try:
                    # Navigate to find schema
                    if 'props' in next_data and 'pageProps' in next_data['props']:
                        page_props = next_data['props']['pageProps']
                        if 'model' in page_props:
                            model_data = page_props['model']
                            # Try different possible schema locations
                            if 'openapi_schema' in model_data:
                                return model_data['openapi_schema']
                            if 'latest_version' in model_data:
                                latest_version = model_data['latest_version']
                                if 'openapi_schema' in latest_version:
                                    return latest_version['openapi_schema']
                except Exception as e:
                    print(f"    Error navigating Next.js data: {e}")
            
            # Fallback: try to find schema in the HTML directly
            soup = BeautifulSoup(html_content, 'html.parser')
            scripts = soup.find_all('script')
            for script in scripts:
                if script.string and ('input' in script.string.lower() or 'output' in script.string.lower()):
                    # Try to find JSON schema embedded in script
                    json_matches = re.findall(r'\{[^{}]*"(?:input|output)"[^{}]*\{[^{}]*\}[^{}]*\}', script.string, re.DOTALL)
                    for match in json_matches:
                        try:
                            parsed = json.loads(match)
                            if 'input' in parsed or 'output' in parsed:
                                return parsed
                        except:
                            continue
            
            print(f"  Warning: Could not extract JSON schema from HTML for {model_name}")
            return None
        else:
            print(f"  Error fetching schema for {model_name}: {response.status_code}")
            return None
    except Exception as e:
        print(f"  Exception fetching schema for {model_name}: {e}")
        return None

def fetch_pricing(model_name: str) -> Optional[Dict[str, Any]]:
    """Fetch pricing information from Replicate page"""
    pricing_url = f"https://replicate.com/{model_name}#pricing"
    main_url = f"https://replicate.com/{model_name}"
    
    try:
        # Try the main page first (pricing might be there)
        response = requests.get(main_url, timeout=15, headers={
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })
        if response.status_code == 200:
            html_content = response.text
            
            # Try to extract Next.js data first
            next_data = extract_nextjs_data(html_content)
            pricing_data = {
                "variants": [],
                "raw_html": None,
                "raw_text": None
            }
            
            if next_data:
                try:
                    # Navigate to find pricing in Next.js data
                    if 'props' in next_data and 'pageProps' in next_data['props']:
                        page_props = next_data['props']['pageProps']
                        if 'model' in page_props:
                            model_data = page_props['model']
                            # Check for pricing in model data
                            if 'pricing' in model_data:
                                pricing_info = model_data['pricing']
                                if isinstance(pricing_info, dict):
                                    pricing_data.update(pricing_info)
                            # Check for latest_version pricing
                            if 'latest_version' in model_data:
                                latest_version = model_data['latest_version']
                                if 'pricing' in latest_version:
                                    pricing_info = latest_version['pricing']
                                    if isinstance(pricing_info, dict):
                                        pricing_data.update(pricing_info)
                except Exception as e:
                    print(f"    Error extracting pricing from Next.js data: {e}")
            
            # Also try to parse HTML for pricing section
            soup = BeautifulSoup(html_content, 'html.parser')
            
            # Look for pricing section
            pricing_section = soup.find('section', id='pricing') or soup.find('div', id='pricing')
            if not pricing_section:
                # Try to find by class or text content
                all_sections = soup.find_all(['section', 'div'])
                for section in all_sections:
                    text = section.get_text().lower()
                    if 'pricing' in text and ('per second' in text or '$' in text):
                        pricing_section = section
                        break
            
            if pricing_section:
                pricing_data["raw_html"] = str(pricing_section)
                pricing_text = pricing_section.get_text()
                pricing_data["raw_text"] = pricing_text
                
                # Try to extract pricing variants more comprehensively
                # Look for patterns like "$0.30 per second" or "model variant is"
                price_pattern = r'\$([\d.]+)\s*(?:per second|/sec|per second of output)'
                variant_pattern = r'model variant is\s+([^\s,]+)'
                quality_pattern = r'(standard|high|low|medium)\s+quality'
                
                prices = re.findall(price_pattern, pricing_text, re.IGNORECASE)
                variants = re.findall(variant_pattern, pricing_text, re.IGNORECASE)
                qualities = re.findall(quality_pattern, pricing_text, re.IGNORECASE)
                
                # If we found prices but no variants, create variants
                if prices and not variants:
                    for i, price in enumerate(prices):
                        quality = qualities[i] if i < len(qualities) else f"variant_{i+1}"
                        pricing_data["variants"].append({
                            "variant": quality,
                            "price_per_second": float(price),
                            "description": ""
                        })
                elif prices:
                    for i, price in enumerate(prices):
                        variant_name = variants[i] if i < len(variants) else (qualities[i] if i < len(qualities) else f"variant_{i+1}")
                        pricing_data["variants"].append({
                            "variant": variant_name,
                            "price_per_second": float(price),
                            "description": ""
                        })
            
            pricing_data["url"] = pricing_url
            return pricing_data
        else:
            print(f"  Error fetching pricing for {model_name}: {response.status_code}")
            return None
    except Exception as e:
        print(f"  Exception fetching pricing for {model_name}: {e}")
        return None

def normalize_model_data(model_name: str, schema_data: Optional[Dict], pricing_data: Optional[Dict]) -> Dict[str, Any]:
    """Normalize model data into consistent format matching user's example structure"""
    
    # Extract model ID from name (e.g., "openai/sora-2-pro" -> "sora-2-pro")
    model_id = model_name.split("/")[-1]
    
    # Extract display name (capitalize properly)
    name_parts = model_id.split("-")
    display_name = " ".join(word.capitalize() for word in name_parts)
    
    # Extract input and output schemas
    input_schema = {}
    output_schema = {}
    
    if schema_data:
        if isinstance(schema_data, dict):
            input_schema = schema_data.get("input", {})
            output_schema = schema_data.get("output", {})
        elif "input" in str(schema_data) or "output" in str(schema_data):
            # Try to extract from nested structure
            if "input" in schema_data:
                input_schema = schema_data["input"]
            if "output" in schema_data:
                output_schema = schema_data["output"]
    
    # Extract pricing information
    pricing_variants = []
    if pricing_data:
        if "variants" in pricing_data and pricing_data["variants"]:
            pricing_variants = pricing_data["variants"]
        elif isinstance(pricing_data, list):
            pricing_variants = pricing_data
    
    # Build normalized structure matching user's example
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

def main():
    """Main function to fetch all model data"""
    all_models_data = []
    raw_data = []  # Store raw data for manual verification
    
    print(f"Fetching data for {len(MODELS)} models...")
    print("Note: Some data may need manual verification for accuracy\n")
    
    for i, model in enumerate(MODELS, 1):
        print(f"\n[{i}/{len(MODELS)}] Processing {model}...")
        
        # Fetch schema
        print(f"  Fetching schema...")
        schema_data = fetch_schema(model)
        time.sleep(0.5)  # Rate limiting
        
        # Fetch pricing
        print(f"  Fetching pricing...")
        pricing_data = fetch_pricing(model)
        time.sleep(0.5)  # Rate limiting
        
        # Store raw data for verification
        raw_data.append({
            "model": model,
            "schema": schema_data,
            "pricing": pricing_data
        })
        
        # Normalize data
        normalized = normalize_model_data(model, schema_data, pricing_data)
        all_models_data.append(normalized)
        
        # Show summary
        has_input = bool(normalized.get("input_schema"))
        has_output = bool(normalized.get("output_schema"))
        has_pricing = bool(normalized.get("pricing", {}).get("variants"))
        print(f"  ✓ Completed - Input: {'✓' if has_input else '✗'}, Output: {'✓' if has_output else '✗'}, Pricing: {'✓' if has_pricing else '✗'}")
    
    # Save normalized data
    output_file = "normalized_models_schema.json"
    with open(output_file, 'w') as f:
        json.dump(all_models_data, f, indent=2)
    
    # Save raw data for manual verification
    raw_output_file = "raw_models_data.json"
    with open(raw_output_file, 'w') as f:
        json.dump(raw_data, f, indent=2)
    
    print(f"\n✓ Saved normalized data to {output_file}")
    print(f"✓ Saved raw data for verification to {raw_output_file}")
    print(f"Total models processed: {len(all_models_data)}")
    
    # Summary
    models_with_schema = sum(1 for m in all_models_data if m.get("input_schema") or m.get("output_schema"))
    models_with_pricing = sum(1 for m in all_models_data if m.get("pricing", {}).get("variants"))
    print(f"\nSummary:")
    print(f"  Models with schema: {models_with_schema}/{len(all_models_data)}")
    print(f"  Models with pricing: {models_with_pricing}/{len(all_models_data)}")
    print(f"\n⚠️  Please verify the data in {raw_output_file} and update {output_file} if needed")
    
    return all_models_data

if __name__ == "__main__":
    main()

