#!/usr/bin/env python3
"""
Download m3u8 video in multiple resolutions and trim the last frames.
This script downloads all available resolutions from the m3u8 playlist,
trims the last 10-15 frames (watermark), and saves them as MP4 files.
"""

import subprocess
import os
import sys
import json
from pathlib import Path

# Configuration
M3U8_URL = "https://video.twimg.com/amplify_video/1858525650694635520/pl/M1N2AhZP1we_u-at.m3u8?variant_version=1&tag=14"
OUTPUT_DIR = Path("downloaded_videos")
TARGET_DURATION = 63  # Target duration in seconds (1 minute 3 seconds = 63s)
# Alternative: Use FRAMES_TO_TRIM if you prefer frame-based trimming
FRAMES_TO_TRIM = None  # Set to None to use TARGET_DURATION instead
FPS = 30  # Approximate FPS (will be detected automatically if possible)

# Available resolutions from the m3u8 file
RESOLUTIONS = [
    {"name": "1280x720", "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/1280x720/mMcIfeVfjw-Z1J_D.m3u8"},
    {"name": "640x360", "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/640x360/cbYJT2bxdEuam1A5.m3u8"},
    {"name": "480x270", "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/480x270/P0RHyJWr6wy0B68H.m3u8"},
]

def check_ffmpeg():
    """Check if ffmpeg is installed."""
    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("ERROR: ffmpeg is not installed or not in PATH")
        print("Please install ffmpeg:")
        print("  macOS: brew install ffmpeg")
        print("  Linux: sudo apt-get install ffmpeg")
        print("  Windows: Download from https://ffmpeg.org/download.html")
        return False

def get_video_duration(video_path):
    """Get video duration in seconds using ffprobe."""
    try:
        cmd = [
            "ffprobe",
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(video_path)
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return float(result.stdout.strip())
    except Exception as e:
        print(f"Warning: Could not get video duration: {e}")
        return None

def get_video_fps(video_path):
    """Get video FPS using ffprobe."""
    try:
        cmd = [
            "ffprobe",
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=r_frame_rate",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(video_path)
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        fps_str = result.stdout.strip()
        if "/" in fps_str:
            num, den = map(int, fps_str.split("/"))
            return num / den if den != 0 else 30.0
        return float(fps_str) if fps_str else 30.0
    except Exception as e:
        print(f"Warning: Could not get video FPS, using default {FPS}: {e}")
        return FPS

def download_m3u8(m3u8_url, output_path):
    """Download m3u8 video and convert to MP4."""
    print(f"\n📥 Downloading: {output_path.name}")
    print(f"   Resolution URL: {m3u8_url}")
    
    # Use master playlist URL - it contains both video and audio streams
    # Individual resolution URLs only have video, no audio
    master_url = "https://video.twimg.com/amplify_video/1858525650694635520/pl/M1N2AhZP1we_u-at.m3u8?variant_version=1&tag=14"
    
    # Determine which video/audio stream pair to select based on resolution
    # From master playlist: audio streams are 0,1,2 and video streams are 3,4,5
    if "1280x720" in m3u8_url:
        # 1280x720 video (stream 3) + 128k audio (stream 0)
        map_video = "0:3"
        map_audio = "0:0"
    elif "640x360" in m3u8_url:
        # 640x360 video (stream 4) + 64k audio (stream 1)
        map_video = "0:4"
        map_audio = "0:1"
    elif "480x270" in m3u8_url:
        # 480x270 video (stream 5) + 32k audio (stream 2)
        map_video = "0:5"
        map_audio = "0:2"
    else:
        # Default: use first video and first audio
        map_video = "0:v:0"
        map_audio = "0:a:0"
    
    print(f"   Using master playlist and mapping video stream {map_video} + audio stream {map_audio}")
    
    cmd = [
        "ffmpeg",
        "-i", master_url,
        "-map", map_video,  # Map specific video stream
        "-map", map_audio,  # Map specific audio stream
        "-c:v", "copy",  # Copy video codec
        "-c:a", "copy",  # Copy audio codec (preserve audio)
        "-bsf:a", "aac_adtstoasc",  # Fix AAC audio
        "-y",  # Overwrite output file
        str(output_path)
    ]
    
    try:
        subprocess.run(cmd, check=True)
        print(f"✅ Downloaded: {output_path.name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Error downloading {output_path.name}: {e}")
        return False

def trim_video(input_path, output_path, frames_to_trim=None, target_duration=None):
    """Trim video to specific duration or by frames."""
    # Get video duration and FPS
    duration = get_video_duration(input_path)
    fps = get_video_fps(input_path) if frames_to_trim else None
    
    if duration is None:
        print("⚠️  Could not determine video duration. Skipping trim.")
        return False
    
    # Determine target duration
    if target_duration is not None:
        # Use exact duration (trim to specific length)
        new_duration = min(target_duration, duration)
        trim_method = f"to {new_duration}s (0:00 - {int(new_duration // 60)}:{int(new_duration % 60):02d})"
    elif frames_to_trim is not None and fps:
        # Use frame-based trimming
        time_to_trim = frames_to_trim / fps
        new_duration = duration - time_to_trim
        trim_method = f"last {frames_to_trim} frames ({time_to_trim:.2f}s)"
    else:
        print("⚠️  No trim method specified. Skipping trim.")
        return False
    
    if new_duration <= 0:
        print(f"⚠️  Invalid target duration. Original: {duration:.2f}s. Skipping trim.")
        return False
    
    if new_duration >= duration:
        print(f"⚠️  Target duration ({new_duration:.2f}s) >= original ({duration:.2f}s). Skipping trim.")
        return False
    
    print(f"\n✂️  Trimming video: {trim_method}")
    print(f"   Original duration: {duration:.2f}s ({int(duration // 60)}:{int(duration % 60):02d})")
    if fps:
        print(f"   FPS: {fps:.2f}")
    print(f"   New duration: {new_duration:.2f}s ({int(new_duration // 60)}:{int(new_duration % 60):02d})")
    
    cmd = [
        "ffmpeg",
        "-i", str(input_path),
        "-t", str(new_duration),  # Keep first N seconds
        "-c:v", "copy",  # Copy video codec
        "-c:a", "copy",  # Copy audio codec (preserve audio)
        "-map", "0",  # Map all streams (video + audio)
        "-y",  # Overwrite output file
        str(output_path)
    ]
    
    try:
        subprocess.run(cmd, check=True)
        print(f"✅ Trimmed: {output_path.name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Error trimming {output_path.name}: {e}")
        return False

def main():
    """Main function to download and process all resolutions."""
    print("=" * 60)
    print("M3U8 Video Downloader and Trimmer")
    print("=" * 60)
    
    # Check ffmpeg
    if not check_ffmpeg():
        sys.exit(1)
    
    # Create output directory
    OUTPUT_DIR.mkdir(exist_ok=True)
    print(f"\n📁 Output directory: {OUTPUT_DIR.absolute()}")
    
    results = []
    
    # Process each resolution
    for resolution in RESOLUTIONS:
        name = resolution["name"]
        url = resolution["url"]
        
        # File paths
        temp_file = OUTPUT_DIR / f"temp_{name}.mp4"
        final_file = OUTPUT_DIR / f"landing_video_{name}.mp4"
        
        # Step 1: Download
        if not download_m3u8(url, temp_file):
            print(f"⚠️  Skipping {name} due to download error")
            continue
        
        # Step 2: Trim
        trim_success = trim_video(
            temp_file, 
            final_file, 
            frames_to_trim=FRAMES_TO_TRIM,
            target_duration=TARGET_DURATION
        )
        if not trim_success:
            print(f"⚠️  Trim failed for {name}, keeping original")
            # If trim fails, use the downloaded file
            if temp_file.exists():
                temp_file.rename(final_file)
        
        # Clean up temp file
        if temp_file.exists() and final_file.exists():
            temp_file.unlink()
        
        results.append({
            "resolution": name,
            "file": str(final_file),
            "url": url
        })
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 Summary")
    print("=" * 60)
    
    for result in results:
        file_path = Path(result["file"])
        if file_path.exists():
            size_mb = file_path.stat().st_size / (1024 * 1024)
            print(f"✅ {result['resolution']}: {file_path.name} ({size_mb:.2f} MB)")
        else:
            print(f"❌ {result['resolution']}: File not found")
    
    # Create a JSON file with all video URLs (for Firebase Storage upload)
    json_output = OUTPUT_DIR / "video_info.json"
    with open(json_output, "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📄 Video info saved to: {json_output}")
    print("\n💡 Next steps:")
    print("   1. Upload the MP4 files to Firebase Storage or your CDN")
    print("   2. Update the 'backgroundVideoUrl' in Firebase Firestore")
    print("   3. Recommended: Use the 1280x720 version for best quality")
    print(f"\n   Example Firebase Storage path: videos/landing/landing_video_1280x720.mp4")

if __name__ == "__main__":
    main()

