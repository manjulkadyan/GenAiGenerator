#!/usr/bin/env python3
"""
Simple script to download m3u8 video from main playlist URL.
Downloads the best quality available and trims the last frames.
"""

import subprocess
import sys
from pathlib import Path

# Configuration
M3U8_URL = "https://video.twimg.com/amplify_video/1858525650694635520/pl/M1N2AhZP1we_u-at.m3u8?variant_version=1&tag=14"
OUTPUT_DIR = Path("downloaded_videos")
TARGET_DURATION = 63  # Target duration in seconds (1 minute 3 seconds = 63s)
# Alternative: Use FRAMES_TO_TRIM if you prefer frame-based trimming
FRAMES_TO_TRIM = None  # Set to None to use TARGET_DURATION instead
OUTPUT_NAME = "landing_video.mp4"

def check_ffmpeg():
    """Check if ffmpeg is installed."""
    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("ERROR: ffmpeg is not installed")
        print("Install: brew install ffmpeg (macOS) or sudo apt-get install ffmpeg (Linux)")
        return False

def get_video_info(video_path):
    """Get video duration and FPS."""
    try:
        # Get duration
        cmd = [
            "ffprobe",
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(video_path)
        ]
        duration = float(subprocess.run(cmd, capture_output=True, text=True, check=True).stdout.strip())
        
        # Get FPS
        cmd = [
            "ffprobe",
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=r_frame_rate",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(video_path)
        ]
        fps_str = subprocess.run(cmd, capture_output=True, text=True, check=True).stdout.strip()
        if "/" in fps_str:
            num, den = map(int, fps_str.split("/"))
            fps = num / den if den != 0 else 30.0
        else:
            fps = float(fps_str) if fps_str else 30.0
        
        return duration, fps
    except Exception as e:
        print(f"Warning: Could not get video info: {e}")
        return None, None

def main():
    print("=" * 60)
    print("M3U8 Video Downloader (Simple)")
    print("=" * 60)
    
    if not check_ffmpeg():
        sys.exit(1)
    
    OUTPUT_DIR.mkdir(exist_ok=True)
    temp_file = OUTPUT_DIR / "temp_video.mp4"
    final_file = OUTPUT_DIR / OUTPUT_NAME
    
    # Step 1: Download (ffmpeg will automatically select best quality)
    print(f"\n📥 Downloading from: {M3U8_URL}")
    print("   (ffmpeg will automatically select the best available resolution)")
    
    cmd = [
        "ffmpeg",
        "-i", M3U8_URL,
        "-c", "copy",
        "-bsf:a", "aac_adtstoasc",
        "-y",
        str(temp_file)
    ]
    
    try:
        subprocess.run(cmd, check=True)
        print(f"✅ Downloaded: {temp_file.name}")
    except subprocess.CalledProcessError as e:
        print(f"❌ Download failed: {e}")
        sys.exit(1)
    
    # Step 2: Get video info and trim
    duration, fps = get_video_info(temp_file)
    
    if duration:
        # Determine target duration
        if TARGET_DURATION is not None:
            # Use exact duration (trim to specific length)
            target_duration = min(TARGET_DURATION, duration)
            trim_method = f"to {target_duration}s (0:00 - {target_duration // 60}:{target_duration % 60:02d})"
        elif FRAMES_TO_TRIM is not None and fps:
            # Use frame-based trimming
            time_to_trim = FRAMES_TO_TRIM / fps
            target_duration = duration - time_to_trim
            trim_method = f"last {FRAMES_TO_TRIM} frames ({time_to_trim:.2f}s)"
        else:
            print("⚠️  No trim method specified, keeping original")
            temp_file.rename(final_file)
            return
        
        if target_duration > 0 and target_duration < duration:
            print(f"\n✂️  Trimming video: {trim_method}")
            print(f"   Original duration: {duration:.2f}s ({int(duration // 60)}:{int(duration % 60):02d})")
            print(f"   Target duration: {target_duration:.2f}s ({int(target_duration // 60)}:{int(target_duration % 60):02d})")
            
            cmd = [
                "ffmpeg",
                "-i", str(temp_file),
                "-t", str(target_duration),  # Keep first N seconds
                "-c", "copy",  # Copy codecs (fast, no re-encoding)
                "-y",
                str(final_file)
            ]
            
            try:
                subprocess.run(cmd, check=True)
                temp_file.unlink()
                print(f"✅ Final video: {final_file.name}")
            except subprocess.CalledProcessError as e:
                print(f"⚠️  Trim failed: {e}")
                temp_file.rename(final_file)
        elif target_duration >= duration:
            print(f"⚠️  Target duration ({target_duration}s) >= original ({duration}s), keeping original")
            temp_file.rename(final_file)
        else:
            print(f"⚠️  Invalid target duration, keeping original")
            temp_file.rename(final_file)
    else:
        print("⚠️  Could not get video info, skipping trim")
        temp_file.rename(final_file)
    
    # Final info
    if final_file.exists():
        size_mb = final_file.stat().st_size / (1024 * 1024)
        print(f"\n✅ Success! File: {final_file.absolute()}")
        print(f"   Size: {size_mb:.2f} MB")
        print(f"\n💡 Upload to Firebase Storage and update 'backgroundVideoUrl' in Firestore")

if __name__ == "__main__":
    main()

