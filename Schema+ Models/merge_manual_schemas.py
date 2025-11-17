#!/usr/bin/env python3
"""
Merge manually extracted schemas into normalized_models_schema.json
"""

import json
import sys

def merge_schemas():
    try:
        # Load manual schemas
        with open('manual_schemas.json', 'r') as f:
            manual_schemas = json.load(f)
    except FileNotFoundError:
        print("Error: manual_schemas.json not found")
        print("Please create it with the extracted schemas first.")
        sys.exit(1)
    
    # Load normalized data
    with open('normalized_models_schema.json', 'r') as f:
        normalized = json.load(f)
    
    # Merge schemas
    updated = 0
    for model_data in normalized:
        model_name = model_data['replicate_name']
        if model_name in manual_schemas:
            schema = manual_schemas[model_name]
            if isinstance(schema, dict):
                model_data['input_schema'] = schema.get('input', {})
                model_data['output_schema'] = schema.get('output', {})
                updated += 1
    
    # Save updated data
    with open('normalized_models_schema.json', 'w') as f:
        json.dump(normalized, f, indent=2)
    
    print(f"✓ Merged schemas for {updated}/{len(normalized)} models")
    print("✓ Updated normalized_models_schema.json")

if __name__ == "__main__":
    merge_schemas()
