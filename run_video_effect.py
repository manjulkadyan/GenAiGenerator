#!/usr/bin/env python3
"""
🎬 DIY Video Effects Generator
Create your own video effects like Pixverse, VED AI, etc.

Requirements:
    pip install torch diffusers transformers accelerate pillow imageio

GPU Required: NVIDIA GPU with 16GB+ VRAM (RTX 3090, A10G, etc.)
"""

import torch
from PIL import Image
import imageio
import numpy as np
from pathlib import Path

# ============================================================================
# CONFIGURATION
# ============================================================================

# Pre-defined effects (like Pixverse)
EFFECTS = {
    "muscle_surge": {
        "prompt": "muscle transformation, muscles growing larger and more defined, body building pose, dramatic lighting, smooth motion",
        "negative_prompt": "static, no motion, blurry, distorted",
        "motion_bucket_id": 100,
        "noise_aug_strength": 0.02,
    },
    "mermaid": {
        "prompt": "magical transformation into a mermaid, growing beautiful fish tail, underwater bubbles, sparkles and magic particles",
        "negative_prompt": "ugly, deformed, static",
        "motion_bucket_id": 120,
        "noise_aug_strength": 0.03,
    },
    "earth_zoom": {
        "prompt": "dramatic camera zoom out to space, earth getting smaller, stars appearing, cinematic space view",
        "negative_prompt": "static, no motion",
        "motion_bucket_id": 180,  # High motion
        "noise_aug_strength": 0.02,
    },
    "liquid_metal": {
        "prompt": "transforming into liquid chrome metal, T-1000 style, reflective mercury surface, morphing",
        "negative_prompt": "static, blurry",
        "motion_bucket_id": 140,
        "noise_aug_strength": 0.02,
    },
    "angel_wings": {
        "prompt": "beautiful angel wings growing from back, white feathers appearing, heavenly glow, magical transformation",
        "negative_prompt": "ugly, deformed",
        "motion_bucket_id": 110,
        "noise_aug_strength": 0.02,
    },
    "rotate_360": {
        "prompt": "smooth 360 degree rotation, object spinning, turntable view, continuous rotation",
        "negative_prompt": "static, jerky motion",
        "motion_bucket_id": 150,
        "noise_aug_strength": 0.01,
    },
    "fire_effect": {
        "prompt": "engulfed in flames, fire spreading across body, burning effect, dramatic fire",
        "negative_prompt": "static",
        "motion_bucket_id": 130,
        "noise_aug_strength": 0.03,
    },
    "ice_freeze": {
        "prompt": "freezing into ice, frost spreading, turning into ice sculpture, crystallization",
        "negative_prompt": "static, melting",
        "motion_bucket_id": 100,
        "noise_aug_strength": 0.02,
    },
}


# ============================================================================
# OPTION 1: Using Stable Video Diffusion (Open Source)
# ============================================================================

def generate_with_svd(image_path: str, effect_name: str, output_path: str = "output.mp4"):
    """
    Generate video using Stable Video Diffusion (requires ~16GB VRAM)
    
    Model: stabilityai/stable-video-diffusion-img2vid-xt
    """
    from diffusers import StableVideoDiffusionPipeline
    from diffusers.utils import load_image, export_to_video
    
    print(f"🎬 Generating '{effect_name}' effect...")
    
    # Load the model
    pipe = StableVideoDiffusionPipeline.from_pretrained(
        "stabilityai/stable-video-diffusion-img2vid-xt",
        torch_dtype=torch.float16,
        variant="fp16"
    )
    pipe.to("cuda")
    
    # Enable memory optimizations
    pipe.enable_model_cpu_offload()
    # pipe.enable_vae_slicing()  # Uncomment if OOM
    
    # Load and preprocess image
    image = load_image(image_path)
    image = image.resize((1024, 576))  # SVD optimal resolution
    
    # Get effect config
    effect = EFFECTS.get(effect_name, EFFECTS["muscle_surge"])
    
    # Generate frames
    generator = torch.Generator(device="cuda").manual_seed(42)
    
    frames = pipe(
        image=image,
        num_frames=25,
        num_inference_steps=25,
        motion_bucket_id=effect["motion_bucket_id"],
        noise_aug_strength=effect["noise_aug_strength"],
        decode_chunk_size=4,
        generator=generator
    ).frames[0]
    
    # Export to video
    export_to_video(frames, output_path, fps=8)
    print(f"✅ Video saved to: {output_path}")
    
    return output_path


# ============================================================================
# OPTION 2: Using AnimateDiff (More Controllable)
# ============================================================================

def generate_with_animatediff(image_path: str, effect_name: str, output_path: str = "output.mp4"):
    """
    Generate video using AnimateDiff (more prompt controllable)
    
    Requires: diffusers >= 0.24.0
    """
    from diffusers import AnimateDiffPipeline, MotionAdapter, DDIMScheduler
    from diffusers.utils import export_to_gif
    
    print(f"🎬 Generating '{effect_name}' effect with AnimateDiff...")
    
    # Load motion adapter
    adapter = MotionAdapter.from_pretrained(
        "guoyww/animatediff-motion-adapter-v1-5-2",
        torch_dtype=torch.float16
    )
    
    # Create pipeline
    pipe = AnimateDiffPipeline.from_pretrained(
        "runwayml/stable-diffusion-v1-5",
        motion_adapter=adapter,
        torch_dtype=torch.float16
    )
    pipe.scheduler = DDIMScheduler.from_config(pipe.scheduler.config)
    pipe.to("cuda")
    pipe.enable_model_cpu_offload()
    
    # Get effect config
    effect = EFFECTS.get(effect_name, EFFECTS["muscle_surge"])
    
    # Load init image
    from diffusers.utils import load_image
    init_image = load_image(image_path).resize((512, 512))
    
    # Generate with IP-Adapter for image conditioning
    # (requires additional setup)
    
    generator = torch.Generator(device="cuda").manual_seed(42)
    
    frames = pipe(
        prompt=effect["prompt"],
        negative_prompt=effect["negative_prompt"],
        num_frames=16,
        num_inference_steps=25,
        guidance_scale=7.5,
        generator=generator
    ).frames[0]
    
    # Export
    export_to_gif(frames, output_path.replace(".mp4", ".gif"))
    print(f"✅ GIF saved to: {output_path.replace('.mp4', '.gif')}")


# ============================================================================
# OPTION 3: Using Replicate API (No GPU Needed!)
# ============================================================================

def generate_with_replicate(image_path: str, effect_name: str, output_path: str = "output.mp4"):
    """
    Generate video using Replicate.com API (no local GPU needed!)
    
    Cost: ~$0.01-0.05 per video
    
    Get API key from: https://replicate.com/
    """
    import replicate
    import urllib.request
    
    print(f"🎬 Generating '{effect_name}' effect via Replicate API...")
    
    # Get effect config
    effect = EFFECTS.get(effect_name, EFFECTS["muscle_surge"])
    
    # Upload image or use URL
    with open(image_path, "rb") as f:
        image_data = f.read()
    
    # Use Stable Video Diffusion on Replicate
    output = replicate.run(
        "stability-ai/stable-video-diffusion:3f0457e4619daac51203dedb472816fd4af51f3149fa7a9e0b5ffcf1b8172438",
        input={
            "input_image": open(image_path, "rb"),
            "motion_bucket_id": effect["motion_bucket_id"],
            "noise_aug_strength": effect["noise_aug_strength"],
            "fps": 8,
            "num_frames": 25,
        }
    )
    
    # Download result
    urllib.request.urlretrieve(output, output_path)
    print(f"✅ Video saved to: {output_path}")
    
    return output_path


# ============================================================================
# OPTION 4: Using CogVideoX (Open Source, High Quality)
# ============================================================================

def generate_with_cogvideo(image_path: str, effect_name: str, output_path: str = "output.mp4"):
    """
    Generate video using CogVideoX (Tsinghua's open model)
    
    Requires ~24GB VRAM for best quality
    """
    from diffusers import CogVideoXImageToVideoPipeline
    from diffusers.utils import export_to_video, load_image
    
    print(f"🎬 Generating '{effect_name}' effect with CogVideoX...")
    
    # Load pipeline
    pipe = CogVideoXImageToVideoPipeline.from_pretrained(
        "THUDM/CogVideoX-5b-I2V",
        torch_dtype=torch.bfloat16
    )
    pipe.to("cuda")
    pipe.enable_model_cpu_offload()
    pipe.vae.enable_tiling()
    
    # Get effect config
    effect = EFFECTS.get(effect_name, EFFECTS["muscle_surge"])
    
    # Load image
    image = load_image(image_path)
    
    # Generate
    video = pipe(
        prompt=effect["prompt"],
        image=image,
        num_videos_per_prompt=1,
        num_inference_steps=50,
        num_frames=49,
        guidance_scale=6,
    ).frames[0]
    
    export_to_video(video, output_path, fps=8)
    print(f"✅ Video saved to: {output_path}")


# ============================================================================
# MAIN: Choose Your Method
# ============================================================================

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Generate AI Video Effects")
    parser.add_argument("image", help="Path to input image")
    parser.add_argument("--effect", "-e", default="muscle_surge", 
                       choices=list(EFFECTS.keys()),
                       help="Effect to apply")
    parser.add_argument("--output", "-o", default="output.mp4",
                       help="Output video path")
    parser.add_argument("--method", "-m", default="replicate",
                       choices=["svd", "animatediff", "replicate", "cogvideo"],
                       help="Generation method")
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("🎬 AI Video Effect Generator")
    print("=" * 60)
    print(f"Input: {args.image}")
    print(f"Effect: {args.effect}")
    print(f"Method: {args.method}")
    print("=" * 60)
    
    if args.method == "svd":
        generate_with_svd(args.image, args.effect, args.output)
    elif args.method == "animatediff":
        generate_with_animatediff(args.image, args.effect, args.output)
    elif args.method == "replicate":
        generate_with_replicate(args.image, args.effect, args.output)
    elif args.method == "cogvideo":
        generate_with_cogvideo(args.image, args.effect, args.output)


if __name__ == "__main__":
    # Quick test without CLI
    print("""
╔══════════════════════════════════════════════════════════════╗
║           🎬 AI Video Effects - Available Effects            ║
╠══════════════════════════════════════════════════════════════╣
║  • muscle_surge  - Body transformation, muscles growing      ║
║  • mermaid       - Transform into a mermaid                  ║
║  • earth_zoom    - Zoom out to space                         ║
║  • liquid_metal  - Chrome/T-1000 transformation              ║
║  • angel_wings   - Grow angel wings                          ║
║  • rotate_360    - 360° rotation                             ║
║  • fire_effect   - Engulfed in flames                        ║
║  • ice_freeze    - Freeze into ice                           ║
╠══════════════════════════════════════════════════════════════╣
║  Usage:                                                       ║
║    python run_video_effect.py photo.jpg -e muscle_surge      ║
║    python run_video_effect.py photo.jpg -e mermaid -m svd    ║
╠══════════════════════════════════════════════════════════════╣
║  Methods:                                                     ║
║    • replicate   - Cloud API (no GPU needed) ~$0.05/video    ║
║    • svd         - Local GPU (16GB+ VRAM)                    ║
║    • cogvideo    - Local GPU (24GB+ VRAM) Best quality       ║
║    • animatediff - Local GPU (8GB+ VRAM)                     ║
╚══════════════════════════════════════════════════════════════╝
    """)
    main()


