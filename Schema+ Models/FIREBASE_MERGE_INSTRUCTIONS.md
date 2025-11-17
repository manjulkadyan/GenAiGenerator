# Firebase Duplicate Models Merger

## Problem
In Firebase, you have duplicate models:
- **Models WITH owner prefix** (e.g., `google-veo-3-fast`) → have **pricing** (`price_per_sec`)
- **Models WITHOUT owner prefix** (e.g., `veo-3-fast`) → have **schema details** (`input_schema`, `schema_metadata`, `schema_parameters`)

Both have the same fields, but different data in them.

## Solution
The script `analyze_and_merge_firebase.py`:
1. **First analyzes** the Firebase data to understand the structure
2. **Then merges** duplicates intelligently

## Usage

```bash
python3 analyze_and_merge_firebase.py <firebase_export.json> [output.json]
```

**Example:**
```bash
python3 analyze_and_merge_firebase.py firebase_models.json merged_models.json
```

## Merge Strategy

For duplicate pairs:
- **Base**: Model WITHOUT owner prefix (has detailed schema)
- **ID**: Updated to model WITH owner prefix (more specific)
- **Pricing**: Taken from model WITH owner prefix
- **Schema fields**: Preserved from model WITHOUT owner prefix
- **Other fields**: Merged intelligently (prefer non-empty values)

## Result

After merging:
- ✅ One complete model per base ID
- ✅ ID with owner prefix (e.g., `google-veo-3-fast`)
- ✅ Pricing from model with owner
- ✅ Complete schema details from model without owner
- ✅ All other fields merged

## Example

**Before (Firebase duplicates):**
```json
[
  {
    "id": "google-veo-3-fast",
    "price_per_sec": 20,
    "schema_parameters": []
  },
  {
    "id": "veo-3-fast",
    "price_per_sec": 15,
    "input_schema": "{...}",
    "schema_metadata": {...},
    "schema_parameters": [...]
  }
]
```

**After (merged):**
```json
[
  {
    "id": "google-veo-3-fast",
    "price_per_sec": 20,
    "input_schema": "{...}",
    "schema_metadata": {...},
    "schema_parameters": [...]
  }
]
```

