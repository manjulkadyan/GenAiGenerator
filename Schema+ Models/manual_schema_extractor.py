#!/usr/bin/env python3
"""
Helper script to open schema pages for manual extraction
Opens each model's schema page with console commands ready to copy
"""

import json
import webbrowser
import time
from typing import Dict, Any

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

# Console command to extract schema
CONSOLE_COMMAND = """
// Copy and paste this in browser console (F12) on the schema page:

// Method 1: Try React component props
const scripts = document.querySelectorAll('script[id^="react-component-props"]');
for (const script of scripts) {
    try {
        const props = JSON.parse(script.textContent);
        if (props.version?.openapi_schema) {
            console.log(JSON.stringify(props.version.openapi_schema, null, 2));
            break;
        }
        if (props.model?.latest_version?.openapi_schema) {
            console.log(JSON.stringify(props.model.latest_version.openapi_schema, null, 2));
            break;
        }
    } catch (e) {}
}

// Method 2: Check window object
if (window.__NEXT_DATA__) {
    const model = window.__NEXT_DATA__?.props?.pageProps?.model;
    if (model?.latest_version?.openapi_schema) {
        console.log(JSON.stringify(model.latest_version.openapi_schema, null, 2));
    }
}

// Method 3: Look for JSON in page
const preTags = document.querySelectorAll('pre');
for (const pre of preTags) {
    try {
        const json = JSON.parse(pre.textContent);
        if (json.input || json.output) {
            console.log(JSON.stringify(json, null, 2));
            break;
        }
    } catch (e) {}
}
"""

def create_extraction_guide():
    """Create a guide file with instructions"""
    guide = f"""
# Manual Schema Extraction Guide

## Instructions

1. Run this script to open each model's schema page
2. On each page, open browser console (F12 → Console tab)
3. Copy and paste the console command below
4. Copy the JSON output
5. Save it to the schemas.json file (format shown below)

## Console Command

{CONSOLE_COMMAND}

## Models to Process

"""
    for i, model in enumerate(MODELS, 1):
        guide += f"{i}. {model}\n   URL: https://replicate.com/{model}/api/schema\n\n"
    
    guide += """
## Output Format

Create a file `manual_schemas.json` with this structure:

{
  "openai/sora-2-pro": {
    "input": { ... },
    "output": { ... }
  },
  "openai/sora-2": {
    "input": { ... },
    "output": { ... }
  },
  ...
}

Then run: python3 merge_manual_schemas.py
"""
    
    with open('MANUAL_EXTRACTION_GUIDE.md', 'w') as f:
        f.write(guide)
    
    print("✓ Created MANUAL_EXTRACTION_GUIDE.md")

def open_schema_pages():
    """Open schema pages one by one"""
    print("This will open each model's schema page in your browser.")
    print("Press Enter after extracting each schema, or 'q' to quit.\n")
    
    for i, model in enumerate(MODELS, 1):
        url = f"https://replicate.com/{model}/api/schema"
        print(f"\n[{i}/{len(MODELS)}] Opening {model}...")
        print(f"URL: {url}")
        print("\nConsole command to run:")
        print("=" * 60)
        print(CONSOLE_COMMAND)
        print("=" * 60)
        
        webbrowser.open(url)
        
        user_input = input("\nPress Enter for next model, or 'q' to quit: ").strip().lower()
        if user_input == 'q':
            print("Stopped.")
            break
        
        time.sleep(1)  # Small delay between opens

def create_merge_script():
    """Create script to merge manually extracted schemas"""
    script = '''#!/usr/bin/env python3
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
'''
    
    with open('merge_manual_schemas.py', 'w') as f:
        f.write(script)
    
    print("✓ Created merge_manual_schemas.py")

if __name__ == "__main__":
    print("Manual Schema Extraction Helper")
    print("=" * 60)
    print("\nOptions:")
    print("1. Create extraction guide (recommended)")
    print("2. Open schema pages one by one")
    print("3. Create merge script")
    print("4. Do all of the above")
    
    choice = input("\nEnter choice (1-4): ").strip()
    
    if choice == '1':
        create_extraction_guide()
    elif choice == '2':
        open_schema_pages()
    elif choice == '3':
        create_merge_script()
    elif choice == '4':
        create_extraction_guide()
        create_merge_script()
        print("\nGuide created. Run 'python3 manual_schema_extractor.py' and choose option 2 to open pages.")
    else:
        print("Invalid choice")

