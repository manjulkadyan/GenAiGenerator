#!/usr/bin/env python3
"""
Merge Firebase duplicate models:
- Models without owner (e.g., "sora-2-pro") have pricing and parameters
- Models with owner (e.g., "openai/sora-2-pro") have rest of the information
Merge them into complete records
"""

import json
from typing import Dict, List, Any

def extract_model_id_from_replicate_name(replicate_name: str) -> str:
    """Extract model ID from replicate name (e.g., 'openai/sora-2-pro' -> 'sora-2-pro')"""
    if '/' in replicate_name:
        return replicate_name.split('/')[-1]
    return replicate_name

def extract_base_id(model_id: str) -> str:
    """Extract base ID by removing owner prefix"""
    known_owners = ['google', 'openai', 'bytedance', 'wan', 'minimax', 'kwaivgi', 
                   'runwayml', 'lightricks', 'leonardoai', 'character', 'luma', 'pixverse']
    for owner in known_owners:
        if model_id.startswith(owner + '-'):
            return model_id[len(owner) + 1:]
    return model_id

def merge_firebase_models(firebase_data: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Merge duplicate Firebase models:
    - Models with just ID (no owner) have pricing and parameters
    - Models with full replicate_name have other info
    """
    # Separate models by ID pattern (with/without owner prefix)
    known_owners = ['google', 'openai', 'bytedance', 'wan', 'minimax', 'kwaivgi', 
                   'runwayml', 'lightricks', 'leonardoai', 'character', 'luma', 'pixverse']
    
    by_base_id = {}  # key: base_id -> {'with_owner': model, 'without_owner': model}
    
    for model in firebase_data:
        model_id = model.get('id', '')
        if not model_id:
            continue
        
        # Check if ID has owner prefix
        has_owner_prefix = any(model_id.startswith(owner + '-') for owner in known_owners)
        base_id = extract_base_id(model_id)
        
        if base_id not in by_base_id:
            by_base_id[base_id] = {'with_owner': None, 'without_owner': None}
        
        if has_owner_prefix:
            by_base_id[base_id]['with_owner'] = model
        else:
            by_base_id[base_id]['without_owner'] = model
    
    # Merge them
    merged_models = []
    
    for base_id, models in by_base_id.items():
        with_owner = models['with_owner']
        without_owner = models['without_owner']
        
        if with_owner and without_owner:
            # Merge: model WITHOUT owner has schema details, model WITH owner has pricing
            merged = without_owner.copy()
            
            # Update ID to the one with owner prefix
            merged['id'] = with_owner['id']
            
            # Take pricing from model WITH owner
            if with_owner.get('price_per_sec'):
                merged['price_per_sec'] = with_owner['price_per_sec']
            
            # Preserve schema fields from model WITHOUT owner
            schema_fields = ['input_schema', 'output_schema', 'schema_metadata', 'schema_parameters']
            
            # Merge other fields from with_owner
            for key, value in with_owner.items():
                if key not in schema_fields and key != 'id':
                    if key not in merged or not merged[key]:
                        merged[key] = value
            
            merged_models.append(merged)
        elif with_owner:
            merged_models.append(with_owner)
        elif without_owner:
            merged_models.append(without_owner)
    
    return merged_models

def main():
    """Main function to merge Firebase duplicates"""
    import sys
    
    # Check if input file is provided
    if len(sys.argv) > 1:
        input_file = sys.argv[1]
    else:
        input_file = 'normalized_models_schema.json'
    
    # Check if output file is provided
    if len(sys.argv) > 2:
        output_file = sys.argv[2]
    else:
        output_file = 'normalized_models_schema_merged.json'
    
    print(f"Reading from: {input_file}")
    
    try:
        with open(input_file, 'r') as f:
            firebase_data = json.load(f)
        
        print(f"Found {len(firebase_data)} models")
        
        # Analyze duplicates
        models_by_id = {}
        models_by_replicate_name = {}
        
        for model in firebase_data:
            model_id = model.get('id', '')
            replicate_name = model.get('replicate_name', '')
            
            if model_id:
                if model_id not in models_by_id:
                    models_by_id[model_id] = []
                models_by_id[model_id].append(model)
            
            if replicate_name:
                if replicate_name not in models_by_replicate_name:
                    models_by_replicate_name[replicate_name] = []
                models_by_replicate_name[replicate_name].append(model)
        
        # Find duplicates
        duplicates_by_id = {k: v for k, v in models_by_id.items() if len(v) > 1}
        duplicates_by_name = {k: v for k, v in models_by_replicate_name.items() if len(v) > 1}
        
        print(f"\nDuplicates found:")
        print(f"  By ID: {len(duplicates_by_id)}")
        print(f"  By replicate_name: {len(duplicates_by_name)}")
        
        if duplicates_by_id:
            print(f"\nDuplicate IDs:")
            for model_id, models in list(duplicates_by_id.items())[:5]:
                print(f"  {model_id}: {len(models)} entries")
                for m in models:
                    print(f"    - replicate_name: {m.get('replicate_name')}, has_pricing: {bool(m.get('pricing'))}, has_schema: {bool(m.get('input_schema'))}")
        
        # Merge duplicates
        merged_models = merge_firebase_models(firebase_data)
        
        print(f"\nAfter merging: {len(merged_models)} models")
        
        # Save merged data
        with open(output_file, 'w') as f:
            json.dump(merged_models, f, indent=2)
        
        print(f"✓ Saved merged data to: {output_file}")
        
        # Show summary
        models_with_schema = sum(1 for m in merged_models if m.get('input_schema') or m.get('output_schema'))
        models_with_pricing = sum(1 for m in merged_models if m.get('pricing', {}).get('variants'))
        
        print(f"\nSummary:")
        print(f"  Models with schemas: {models_with_schema}/{len(merged_models)}")
        print(f"  Models with pricing: {models_with_pricing}/{len(merged_models)}")
        
    except FileNotFoundError:
        print(f"Error: File '{input_file}' not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in '{input_file}': {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()

