#!/usr/bin/env python3
"""
View Firebase data without merging - just analyze and display
"""

import json
import sys

def view_firebase_data(input_file: str):
    """View Firebase data structure"""
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    print(f"Total models in Firebase: {len(data)}\n")
    
    # Group by base ID to find duplicates
    known_owners = ['google', 'openai', 'bytedance', 'wan', 'minimax', 'kwaivgi', 
                   'runwayml', 'lightricks', 'leonardoai', 'character', 'luma', 'pixverse']
    
    def extract_base_id(model_id: str) -> str:
        for owner in known_owners:
            if model_id.startswith(owner + '-'):
                return model_id[len(owner) + 1:]
        return model_id
    
    by_base_id = {}
    for model in data:
        model_id = model.get('id', '')
        if model_id:
            base_id = extract_base_id(model_id)
            if base_id not in by_base_id:
                by_base_id[base_id] = []
            by_base_id[base_id].append(model)
    
    # Find duplicates
    duplicates = {k: v for k, v in by_base_id.items() if len(v) > 1}
    
    print(f"=== Duplicate Groups Found: {len(duplicates)} ===\n")
    
    for base_id, models in duplicates.items():
        print(f"Base ID: {base_id}")
        print(f"  Found {len(models)} entries:\n")
        
        for i, model in enumerate(models, 1):
            model_id = model.get('id', 'N/A')
            has_owner = any(model_id.startswith(owner + '-') for owner in known_owners)
            
            print(f"  Entry {i}: {model_id} {'(WITH owner prefix)' if has_owner else '(WITHOUT owner prefix)'}")
            print(f"    Fields: {list(model.keys())[:15]}")
            
            # Show key differences
            if has_owner:
                print(f"    price_per_sec: {model.get('price_per_sec', 'N/A')}")
                print(f"    has_schema_parameters: {bool(model.get('schema_parameters'))}")
                print(f"    has_input_schema: {bool(model.get('input_schema'))}")
                print(f"    has_schema_metadata: {bool(model.get('schema_metadata'))}")
            else:
                print(f"    price_per_sec: {model.get('price_per_sec', 'N/A')}")
                print(f"    has_schema_parameters: {bool(model.get('schema_parameters'))}")
                print(f"    has_input_schema: {bool(model.get('input_schema'))}")
                print(f"    has_schema_metadata: {bool(model.get('schema_metadata'))}")
            print()
        
        print("-" * 60)
        print()
    
    # Show first duplicate in detail
    if duplicates:
        print("=== Detailed View of First Duplicate ===\n")
        base_id = list(duplicates.keys())[0]
        models = duplicates[base_id]
        
        for i, model in enumerate(models, 1):
            print(f"Model {i}: {model.get('id')}")
            print(json.dumps(model, indent=2)[:2000])  # First 2000 chars
            print("\n" + "="*60 + "\n")
    
    # Save unmerged data for inspection
    output_file = input_file.replace('.json', '_unmerged.json')
    with open(output_file, 'w') as f:
        json.dump(data, f, indent=2)
    
    print(f"✓ Saved unmerged data to: {output_file}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 view_firebase_data.py <firebase_export.json>")
        sys.exit(1)
    
    view_firebase_data(sys.argv[1])

