#!/usr/bin/env python3
"""
Fetch models from Firestore video_features collection
"""

import json
from firebase_admin import firestore, credentials, initialize_app

def fetch_video_features():
    """Fetch all documents from video_features collection"""
    try:
        # Initialize Firebase (will use default credentials if available)
        try:
            initialize_app()
        except ValueError:
            # Already initialized
            pass
        
        db = firestore.client()
        collection_ref = db.collection('video_features')
        
        print("Fetching from Firestore collection: video_features...")
        
        # Get all documents
        docs = collection_ref.stream()
        
        models = []
        for doc in docs:
            model_data = doc.to_dict()
            model_data['_firestore_id'] = doc.id
            models.append(model_data)
        
        print(f"Fetched {len(models)} models from Firestore")
        
        # Save to file
        output_file = 'firestore_video_features.json'
        with open(output_file, 'w') as f:
            json.dump(models, f, indent=2)
        
        print(f"✓ Saved to {output_file}")
        
        # Analyze structure
        if models:
            print(f"\n=== First Model Structure ===")
            first = models[0]
            print(f"ID: {first.get('id', 'N/A')}")
            print(f"Keys: {list(first.keys())[:20]}")
            
            # Check for duplicates
            by_id = {}
            for model in models:
                model_id = model.get('id', '')
                if model_id:
                    if model_id not in by_id:
                        by_id[model_id] = []
                    by_id[model_id].append(model)
            
            duplicates = {k: v for k, v in by_id.items() if len(v) > 1}
            print(f"\nDuplicate IDs found: {len(duplicates)}")
            if duplicates:
                print("Duplicate examples:")
                for model_id, models_list in list(duplicates.items())[:3]:
                    print(f"  {model_id}: {len(models_list)} entries")
        
        return models
        
    except Exception as e:
        print(f"Error: {e}")
        print("\nNote: Make sure Firebase Admin SDK is configured.")
        print("You may need to set GOOGLE_APPLICATION_CREDENTIALS or use application default credentials.")
        return None

if __name__ == "__main__":
    fetch_video_features()

