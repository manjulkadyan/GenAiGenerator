#!/usr/bin/env python3
"""
Pixverse Effects Scraper
Scrapes video effect templates from Pixverse website and API
"""

import requests
import json
from dataclasses import dataclass
from typing import Optional, List

@dataclass
class VideoEffect:
    effect_id: str
    name: str
    prompt: str
    credits: int
    aspect_ratio: str
    preview_url: str
    endpoint: str
    identifier: str

# Effects extracted from VED AI APK (hardcoded known effects)
KNOWN_PIXVERSE_EFFECTS = [
    VideoEffect(
        effect_id="earth-zoom",
        name="Earth Zoom",
        prompt="earth zoom out",
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_earthzoom_250716.gif",
        endpoint="pixverse",
        identifier="Earth Zoom"
    ),
    VideoEffect(
        effect_id="muscle",
        name="Muscle Surge",
        prompt="muscle surge",
        credits=30,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_muscle_0610.gif",
        endpoint="pixverse",
        identifier="Muscle Surge"
    ),
    VideoEffect(
        effect_id="360-rotate-microwave",
        name="Rotate 360",
        prompt="rotate 360",
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_360_250227.gif",
        endpoint="pixverse",
        identifier="Microwave"
    ),
    VideoEffect(
        effect_id="Bikini",
        name="Bikini Up",
        prompt="put on bikini",
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_bikini_250218.gif",
        endpoint="pixverse",
        identifier="Bikini Up"
    ),
    VideoEffect(
        effect_id="mermaid",
        name="Fin-tastic Mermaid",
        prompt="Mermaid me",
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_mermaid_250528.gif",
        endpoint="pixverse",
        identifier="Fin-tastic Mermaid"
    ),
    VideoEffect(
        effect_id="metal",
        name="Liquid Metal",
        prompt="",  # Uses default prompt
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_metal_250606.gif",
        endpoint="pixverse",
        identifier="Liquid Metal"
    ),
    VideoEffect(
        effect_id="wings-angle",
        name="Angel Wings",
        prompt="put on angle wings",
        credits=40,
        aspect_ratio="1:1",
        preview_url="https://media.pixverse.ai/asset%2Ftemplate%2Fweb_anglewings_250208.gif",
        endpoint="pixverse",
        identifier="Holy Wings"
    ),
]


class PixverseAPI:
    """Official Pixverse API Client"""
    
    BASE_URL = "https://api.pixverse.ai/v2"
    
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.headers = {
            "Api-Key": api_key,
            "Content-Type": "application/json"
        }
    
    def get_effects_list(self) -> dict:
        """Get all available effects from Pixverse API"""
        response = requests.get(
            f"{self.BASE_URL}/effects/list",
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()
    
    def upload_image(self, image_path: str) -> str:
        """Upload an image to Pixverse and get the URL"""
        with open(image_path, 'rb') as f:
            files = {'file': f}
            response = requests.post(
                f"{self.BASE_URL}/image/upload",
                headers={"Api-Key": self.api_key},
                files=files
            )
        response.raise_for_status()
        return response.json()['Img_url']
    
    def generate_effect(self, image_url: str, effect_id: str, model: str = "v3.5") -> dict:
        """Generate a video with an effect applied to the image"""
        payload = {
            "image": image_url,
            "effect_id": effect_id,
            "model": model
        }
        response = requests.post(
            f"{self.BASE_URL}/effects/generate",
            headers=self.headers,
            json=payload
        )
        response.raise_for_status()
        return response.json()
    
    def get_video_status(self, video_id: str) -> dict:
        """Check the status of a video generation task"""
        response = requests.get(
            f"{self.BASE_URL}/video/result/{video_id}",
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()


def scrape_pixverse_templates():
    """
    Scrape effect templates from Pixverse CDN
    Note: These are the preview GIFs, not the actual effect generation
    """
    # Known CDN patterns from Pixverse
    cdn_base = "https://media.pixverse.ai/asset%2Ftemplate%2F"
    
    # Known effect prefixes discovered from VED AI
    known_effects = [
        ("web_earthzoom", "Earth Zoom"),
        ("web_muscle", "Muscle Surge"),
        ("web_360", "Rotate 360"),
        ("web_bikini", "Bikini Up"),
        ("web_mermaid", "Mermaid"),
        ("web_metal", "Liquid Metal"),
        ("web_anglewings", "Angel Wings"),
        ("web_hug", "Hug"),
        ("web_kiss", "Kiss"),
        ("web_swim", "Swimming"),
        ("web_fly", "Flying"),
        ("web_dance", "Dancing"),
        ("web_fire", "Fire Effect"),
        ("web_ice", "Ice Effect"),
        ("web_explosion", "Explosion"),
        ("web_transform", "Transform"),
    ]
    
    effects = []
    for prefix, name in known_effects:
        # Try common date suffixes
        for suffix in ["_250716", "_250606", "_250528", "_250227", "_250218", "_250208", "_0610"]:
            url = f"{cdn_base}{prefix}{suffix}.gif?x-oss-process=style/cover-webp-small"
            try:
                response = requests.head(url, timeout=5)
                if response.status_code == 200:
                    effects.append({
                        "name": name,
                        "preview_url": url,
                        "effect_prefix": prefix
                    })
                    print(f"✅ Found: {name} - {url}")
                    break
            except:
                continue
    
    return effects


def save_effects_to_json(effects: List[VideoEffect], filename: str = "pixverse_effects.json"):
    """Save effects to a JSON file for use in your app"""
    data = [
        {
            "id": e.effect_id,
            "name": e.name,
            "prompt": e.prompt,
            "credits": e.credits,
            "aspectRatio": e.aspect_ratio,
            "previewUrl": e.preview_url,
            "endpoint": e.endpoint,
            "identifier": e.identifier
        }
        for e in effects
    ]
    
    with open(filename, 'w') as f:
        json.dump(data, f, indent=2)
    
    print(f"💾 Saved {len(effects)} effects to {filename}")


def main():
    print("=" * 60)
    print("🎬 Pixverse Effects Scraper")
    print("=" * 60)
    
    # Option 1: Use known effects from VED AI
    print("\n📋 Known effects from VED AI reverse engineering:")
    for effect in KNOWN_PIXVERSE_EFFECTS:
        print(f"  • {effect.name} ({effect.effect_id})")
    
    # Save known effects
    save_effects_to_json(KNOWN_PIXVERSE_EFFECTS)
    
    # Option 2: Try to discover more effects from CDN
    print("\n🔍 Attempting to discover more effects from CDN...")
    discovered = scrape_pixverse_templates()
    
    # Option 3: Use official API (requires API key)
    print("\n" + "=" * 60)
    print("🔑 To get MORE effects, use the official Pixverse API:")
    print("=" * 60)
    print("""
1. Sign up at: https://platform.pixverse.ai/
2. Get your API key from the dashboard
3. Run this script with your API key:

   api = PixverseAPI("YOUR_API_KEY")
   effects = api.get_effects_list()
   print(effects)
    """)
    
    # Example usage with API key
    api_key = None  # Set your API key here
    if api_key:
        print("\n🚀 Fetching effects from official API...")
        api = PixverseAPI(api_key)
        try:
            effects_list = api.get_effects_list()
            print(f"Found {len(effects_list.get('effects', []))} effects!")
            print(json.dumps(effects_list, indent=2))
        except Exception as e:
            print(f"❌ API Error: {e}")


if __name__ == "__main__":
    main()


