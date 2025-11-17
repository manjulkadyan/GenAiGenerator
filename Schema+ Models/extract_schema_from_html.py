#!/usr/bin/env python3
"""
Extract schema from HTML response - the schema is embedded in script tags
"""

import json
import re
import requests
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

def extract_schema_from_html(html_content: str) -> Optional[Dict[str, Any]]:
    """Extract input and output schema from HTML"""
    try:
        # The schema is in version._extras.dereferenced_openapi_schema
        # Find script tags with JSON
        script_pattern = r'<script[^>]*type=["\']application/json["\'][^>]*>(.*?)</script>'
        scripts = re.findall(script_pattern, html_content, re.DOTALL)
        
        for script_content in scripts:
            try:
                data = json.loads(script_content)
                if isinstance(data, dict):
                    # Check if this script has version with dereferenced_openapi_schema
                    if 'version' in data:
                        version = data['version']
                        if isinstance(version, dict) and '_extras' in version:
                            extras = version['_extras']
                            if 'dereferenced_openapi_schema' in extras:
                                openapi_schema = extras['dereferenced_openapi_schema']
                                
                                # Extract input from requestBody
                                paths = openapi_schema.get('paths', {})
                                predictions_path = paths.get('/predictions', {})
                                post_op = predictions_path.get('post', {})
                                
                                input_schema = {}
                                output_schema = {}
                                
                                # Get input from requestBody
                                request_body = post_op.get('requestBody', {})
                                if request_body:
                                    content = request_body.get('content', {})
                                    app_json = content.get('application/json', {})
                                    req_schema = app_json.get('schema', {})
                                    input_schema = req_schema.get('properties', {}).get('input', {})
                                
                                # Get output from responses
                                responses = post_op.get('responses', {})
                                response_200 = responses.get('200', {})
                                if response_200:
                                    resp_content = response_200.get('content', {})
                                    resp_app_json = resp_content.get('application/json', {})
                                    resp_schema = resp_app_json.get('schema', {})
                                    output_schema = resp_schema.get('properties', {}).get('output', {})
                                
                                if input_schema or output_schema:
                                    return {
                                        'input': input_schema,
                                        'output': output_schema
                                    }
            except json.JSONDecodeError:
                continue
        
        # Alternative: Look for JSON in pre/code tags
        pre_pattern = r'<pre[^>]*>(.*?)</pre>'
        code_pattern = r'<code[^>]*>(.*?)</code>'
        
        for pattern in [pre_pattern, code_pattern]:
            matches = re.findall(pattern, html_content, re.DOTALL)
            for match in matches:
                # Clean up HTML entities
                match = match.replace('&quot;', '"').replace('&lt;', '<').replace('&gt;', '>')
                try:
                    data = json.loads(match)
                    if isinstance(data, dict) and ('input' in data or 'output' in data or 'properties' in data):
                        # Check if it's the schema format
                        if 'type' in data and data.get('type') == 'object' and 'properties' in data:
                            # This is the input schema
                            return {
                                'input': data,
                                'output': {}  # Will need to find output separately
                            }
                        elif 'input' in data and 'output' in data:
                            return data
                except json.JSONDecodeError:
                    continue
        
        return None
    except Exception as e:
        print(f"Error extracting schema: {e}")
        return None

def extract_pricing_from_html(html_content: str) -> Optional[Dict[str, Any]]:
    """Extract pricing information from HTML"""
    try:
        # Pricing is in React component props with billingConfig
        script_pattern = r'<script[^>]*type=["\']application/json["\'][^>]*>(.*?)</script>'
        scripts = re.findall(script_pattern, html_content, re.DOTALL)
        
        for script_content in scripts:
            try:
                data = json.loads(script_content)
                if isinstance(data, dict) and 'billingConfig' in data:
                    billing_config = data['billingConfig']
                    variants = []
                    
                    if 'current_tiers' in billing_config:
                        for tier in billing_config['current_tiers']:
                            if tier.get('prices') and len(tier['prices']) > 0:
                                for price in tier['prices']:
                                    variant = {
                                        'variant': tier.get('criteria', [{}])[0].get('value', '') if tier.get('criteria') else '',
                                        'variant_name': tier.get('title', ''),
                                        'price_per_second': float(price.get('price', '0').replace('$', '')),
                                        'description': price.get('description', tier.get('description', '')),
                                        'metric': price.get('metric', '')
                                    }
                                    variants.append(variant)
                    
                    if variants:
                        return {
                            'variants': variants,
                            'billing_config': billing_config
                        }
            except (json.JSONDecodeError, ValueError, KeyError):
                continue
        
        return None
    except Exception as e:
        return None

def fetch_and_extract_all(model_name: str) -> Optional[Dict[str, Any]]:
    """Fetch HTML from both pages and extract schema and pricing"""
    headers = {
        'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'user-agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36'
    }
    
    result = {}
    
    # Fetch schema page
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    try:
        response = requests.get(schema_url, headers=headers, timeout=30)
        if response.status_code == 200:
            schema = extract_schema_from_html(response.text)
            if schema:
                result['input_schema'] = schema.get('input', {})
                result['output_schema'] = schema.get('output', {})
    except Exception as e:
        pass
    
    # Fetch main page for pricing
    main_url = f"https://replicate.com/{model_name}"
    try:
        response = requests.get(main_url, headers=headers, timeout=30)
        if response.status_code == 200:
            pricing = extract_pricing_from_html(response.text)
            if pricing:
                result['pricing'] = pricing
    except Exception as e:
        pass
    
    return result if result else None

def main():
    """Extract schemas and pricing for all models"""
    all_data = {}
    
    print(f"Extracting schemas and pricing for {len(MODELS)} models...\n")
    
    for i, model in enumerate(MODELS, 1):
        print(f"[{i}/{len(MODELS)}] {model}...", end=" ", flush=True)
        data = fetch_and_extract_all(model)
        if data:
            all_data[model] = data
            has_schema = bool(data.get('input_schema') or data.get('output_schema'))
            has_pricing = bool(data.get('pricing', {}).get('variants'))
            status = "✓" if (has_schema and has_pricing) else "⚠"
            print(f"{status} (schema: {'✓' if has_schema else '✗'}, pricing: {'✓' if has_pricing else '✗'})")
        else:
            print("✗")
    
    # Save extracted data
    with open('extracted_all_data.json', 'w') as f:
        json.dump(all_data, f, indent=2)
    
    # Update normalized file
    try:
        with open('normalized_models_schema.json', 'r') as f:
            normalized = json.load(f)
        
        updated_schema = 0
        updated_pricing = 0
        for model_data in normalized:
            model_name = model_data['replicate_name']
            if model_name in all_data:
                data = all_data[model_name]
                
                # Update schemas
                if data.get('input_schema') or data.get('output_schema'):
                    model_data['input_schema'] = data.get('input_schema', {})
                    model_data['output_schema'] = data.get('output_schema', {})
                    updated_schema += 1
                
                # Update pricing
                if data.get('pricing'):
                    model_data['pricing'] = {
                        'variants': data['pricing'].get('variants', []),
                        'billing_config': data['pricing'].get('billing_config', {}),
                        'raw_data': data['pricing']
                    }
                    updated_pricing += 1
        
        with open('normalized_models_schema.json', 'w') as f:
            json.dump(normalized, f, indent=2)
        
        print(f"\n✓ Extracted data for {len(all_data)}/{len(MODELS)} models")
        print(f"✓ Updated schemas for {updated_schema} models")
        print(f"✓ Updated pricing for {updated_pricing} models")
        print(f"✓ Saved to normalized_models_schema.json")
    except Exception as e:
        print(f"\nError updating file: {e}")

if __name__ == "__main__":
    main()

