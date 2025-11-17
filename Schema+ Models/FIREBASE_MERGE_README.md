# Firebase Duplicate Models Merger

This script merges duplicate models in Firebase where:
- **Models without owner** (e.g., `replicate_name: "sora-2-pro"`) have **pricing and parameters**
- **Models with owner** (e.g., `replicate_name: "openai/sora-2-pro"`) have **rest of the information**

## Usage

### Step 1: Export Firebase Data

Export your Firebase models collection to a JSON file. The file should be an array of model objects.

### Step 2: Run the Merge Script

```bash
python3 merge_firebase_duplicates.py <input_file> <output_file>
```

**Examples:**
```bash
# Merge Firebase export
python3 merge_firebase_duplicates.py firebase_export.json merged_models.json

# Use default files
python3 merge_firebase_duplicates.py
# Reads from: normalized_models_schema.json
# Writes to: normalized_models_schema_merged.json
```

## How It Works

1. **Identifies duplicates:**
   - Models with `replicate_name` containing `/` (e.g., `"openai/sora-2-pro"`) → has owner
   - Models with `replicate_name` without `/` (e.g., `"sora-2-pro"`) → no owner

2. **Merges data:**
   - Takes **pricing and schemas** from models without owner (these have the complete pricing/parameter data)
   - Takes **metadata** (name, URL, description, etc.) from models with owner
   - Combines into complete records

3. **Output:**
   - One complete model per unique model ID
   - All pricing variants and billing_config preserved
   - All input/output schemas preserved
   - All metadata from owner models preserved

## Example

**Input (Firebase duplicates):**
```json
[
  {
    "id": "sora-2-pro",
    "replicate_name": "sora-2-pro",
    "pricing": {
      "variants": [{"variant": "standard", "price_per_second": 0.3}],
      "billing_config": {...}
    },
    "input_schema": {...}
  },
  {
    "id": "sora-2-pro",
    "replicate_name": "openai/sora-2-pro",
    "name": "Sora 2 Pro",
    "url": "https://replicate.com/openai/sora-2-pro",
    "description": "OpenAI's model"
  }
]
```

**Output (Merged):**
```json
[
  {
    "id": "sora-2-pro",
    "replicate_name": "openai/sora-2-pro",
    "name": "Sora 2 Pro",
    "url": "https://replicate.com/openai/sora-2-pro",
    "description": "OpenAI's model",
    "pricing": {
      "variants": [{"variant": "standard", "price_per_second": 0.3}],
      "billing_config": {...}
    },
    "input_schema": {...}
  }
]
```

## Notes

- The script preserves the model with owner as the base (keeps URLs and metadata)
- Pricing and schemas from models without owner take precedence if they're more complete
- Standalone models (without a corresponding duplicate) are kept as-is

