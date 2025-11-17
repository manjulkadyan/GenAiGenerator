#!/usr/bin/env python3
"""
First analyze Firebase data structure, then create merge logic
"""

import json
import sys
from typing import Dict, List, Any, Set

def analyze_firebase_structure(data: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Analyze Firebase data to understand the structure of duplicates"""
    analysis = {
        'total_models': len(data),
        'models_with_owner_prefix': {},
        'models_without_owner_prefix': {},
        'common_fields': set(),
        'fields_only_in_with_owner': set(),
        'fields_only_in_without_owner': set(),
        'duplicate_pairs': []
    }
    
    # Separate models by ID pattern
    by_base_id = {}  # e.g., "veo-3-fast" -> [models]
    
    for model in data:
        model_id = model.get('id', '')
        if not model_id:
            continue
        
        # Check if ID has owner prefix (e.g., "google-veo-3-fast")
        # vs without (e.g., "veo-3-fast")
        parts = model_id.split('-')
        
        # Try to identify base ID (usually the last 2-3 parts)
        # For "google-veo-3-fast" -> base might be "veo-3-fast"
        # For "veo-3-fast" -> base is "veo-3-fast"
        
        # Simple heuristic: if it starts with known owner prefixes, it's with owner
        known_owners = ['google', 'openai', 'bytedance', 'wan', 'minimax', 'kwaivgi', 
                       'runwayml', 'lightricks', 'leonardoai', 'character', 'luma', 'pixverse']
        
        has_owner_prefix = any(model_id.startswith(owner + '-') for owner in known_owners)
        
        # Extract base ID (remove owner prefix if present)
        base_id = model_id
        if has_owner_prefix:
            for owner in known_owners:
                if model_id.startswith(owner + '-'):
                    base_id = model_id[len(owner) + 1:]  # Remove "owner-"
                    break
        
        if base_id not in by_base_id:
            by_base_id[base_id] = {'with_owner': [], 'without_owner': []}
        
        if has_owner_prefix:
            by_base_id[base_id]['with_owner'].append(model)
        else:
            by_base_id[base_id]['without_owner'].append(model)
    
    # Analyze duplicate pairs
    for base_id, models in by_base_id.items():
        with_owner = models['with_owner']
        without_owner = models['without_owner']
        
        if with_owner and without_owner:
            # Found a duplicate pair
            with_owner_model = with_owner[0]  # Model with owner prefix
            without_owner_model = without_owner[0]  # Model without owner prefix
            
            with_owner_fields = set(with_owner_model.keys())
            without_owner_fields = set(without_owner_model.keys())
            
            analysis['duplicate_pairs'].append({
                'base_id': base_id,
                'with_owner_id': with_owner[0].get('id'),
                'without_owner_id': without_owner[0].get('id'),
                'with_owner_fields': list(with_owner_fields),
                'without_owner_fields': list(without_owner_fields),
                'common_fields': list(with_owner_fields & without_owner_fields),
                'only_in_with_owner': list(with_owner_fields - without_owner_fields),
                'only_in_without_owner': list(without_owner_fields - with_owner_fields)
            })
            
            # Update field analysis
            analysis['common_fields'].update(with_owner_fields & without_owner_fields)
            analysis['fields_only_in_with_owner'].update(with_owner_fields - without_owner_fields)
            analysis['fields_only_in_without_owner'].update(without_owner_fields - with_owner_fields)
    
    return analysis

def merge_based_on_analysis(data: List[Dict[str, Any]], analysis: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Merge duplicates based on the analysis"""
    # Group by base ID
    by_base_id = {}
    known_owners = ['google', 'openai', 'bytedance', 'wan', 'minimax', 'kwaivgi', 
                   'runwayml', 'lightricks', 'leonardoai', 'character', 'luma', 'pixverse']
    
    for model in data:
        model_id = model.get('id', '')
        if not model_id:
            continue
        
        # Extract base ID
        base_id = model_id
        has_owner_prefix = any(model_id.startswith(owner + '-') for owner in known_owners)
        
        if has_owner_prefix:
            for owner in known_owners:
                if model_id.startswith(owner + '-'):
                    base_id = model_id[len(owner) + 1:]
                    break
        
        if base_id not in by_base_id:
            by_base_id[base_id] = {'with_owner': None, 'without_owner': None}
        
        if has_owner_prefix:
            by_base_id[base_id]['with_owner'] = model
        else:
            by_base_id[base_id]['without_owner'] = model
    
    # Merge
    merged = []
    for base_id, models in by_base_id.items():
        with_owner = models['with_owner']
        without_owner = models['without_owner']
        
        if with_owner and without_owner:
            # Merge strategy:
            # - Use model WITHOUT owner as base (has detailed schema: input_schema, schema_metadata, schema_parameters)
            # - Update ID to the one WITH owner prefix (more specific identifier)
            # - Take pricing from model WITH owner (has price_per_sec)
            # - Merge other fields intelligently
            
            merged_model = without_owner.copy()
            
            # Update ID to the one with owner prefix (more specific)
            merged_model['id'] = with_owner['id']
            
            # Pricing: model WITH owner has the pricing
            if with_owner.get('price_per_sec'):
                merged_model['price_per_sec'] = with_owner['price_per_sec']
            
            # Merge other fields from with_owner that might be more complete
            # But preserve schema fields from without_owner
            schema_fields = ['input_schema', 'output_schema', 'schema_metadata', 'schema_parameters']
            
            for key, value in with_owner.items():
                if key in schema_fields:
                    # Don't overwrite schema fields from without_owner
                    continue
                elif key == 'id':
                    # Already set
                    continue
                elif key not in merged_model:
                    # Add missing fields
                    merged_model[key] = value
                elif not merged_model[key] and value:
                    # Replace empty values
                    merged_model[key] = value
                elif isinstance(value, (list, dict)) and value:
                    # If both have values, prefer the one with owner for non-schema fields
                    if key not in schema_fields:
                        merged_model[key] = value
            
            merged.append(merged_model)
        elif with_owner:
            merged.append(with_owner)
        elif without_owner:
            merged.append(without_owner)
    
    return merged

def main():
    """Main function"""
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_and_merge_firebase.py <firebase_export.json> [output.json]")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'firebase_merged.json'
    
    print(f"Reading Firebase data from: {input_file}")
    
    try:
        with open(input_file, 'r') as f:
            data = json.load(f)
        
        print(f"Found {len(data)} models\n")
        
        # Step 1: Analyze structure
        print("=== Step 1: Analyzing Firebase Structure ===")
        analysis = analyze_firebase_structure(data)
        
        print(f"Total models: {analysis['total_models']}")
        print(f"Duplicate pairs found: {len(analysis['duplicate_pairs'])}")
        
        if analysis['duplicate_pairs']:
            print(f"\nCommon fields: {len(analysis['common_fields'])}")
            print(f"Fields only in models WITH owner prefix: {len(analysis['fields_only_in_with_owner'])}")
            print(f"Fields only in models WITHOUT owner prefix: {len(analysis['fields_only_in_without_owner'])}")
            
            print(f"\n=== Example Duplicate Pair ===")
            if analysis['duplicate_pairs']:
                example = analysis['duplicate_pairs'][0]
                print(f"Base ID: {example['base_id']}")
                print(f"  With owner: {example['with_owner_id']}")
                print(f"  Without owner: {example['without_owner_id']}")
                print(f"  Fields only in WITH owner: {example['only_in_with_owner'][:5]}")
                print(f"  Fields only in WITHOUT owner: {example['only_in_without_owner'][:5]}")
        else:
            print("\nNo duplicate pairs found. Checking if data needs different analysis...")
            # Show first model structure
            if data:
                print(f"\nFirst model structure:")
                first = data[0]
                print(f"  ID: {first.get('id')}")
                print(f"  Keys: {list(first.keys())[:10]}")
        
        # Step 2: Merge based on analysis
        print(f"\n=== Step 2: Merging Duplicates ===")
        merged = merge_based_on_analysis(data, analysis)
        
        print(f"Merged {len(data)} models into {len(merged)} models")
        
        # Save
        with open(output_file, 'w') as f:
            json.dump(merged, f, indent=2)
        
        print(f"\n✓ Saved merged data to: {output_file}")
        
        # Summary
        models_with_schema = sum(1 for m in merged if m.get('input_schema') or m.get('schema_parameters'))
        models_with_pricing = sum(1 for m in merged if m.get('price_per_sec'))
        
        print(f"\nSummary:")
        print(f"  Models with schema: {models_with_schema}/{len(merged)}")
        print(f"  Models with pricing: {models_with_pricing}/{len(merged)}")
        
    except FileNotFoundError:
        print(f"Error: File '{input_file}' not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()

