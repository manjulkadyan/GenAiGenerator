#!/usr/bin/env python3
"""
Complete solution: Download m3u8 videos in all resolutions, trim them,
upload to Firebase Storage, and generate m3u8 playlists for adaptive streaming.

Usage:
  python3 download_upload_m3u8_complete.py          # Download, trim, and upload
  python3 download_upload_m3u8_complete.py --skip-download  # Skip download, just upload existing files
"""

import subprocess
import os
import sys
import json
import argparse
from pathlib import Path
from urllib.parse import urlparse, quote

# Configuration
M3U8_URL = "https://video.twimg.com/amplify_video/1858525650694635520/pl/M1N2AhZP1we_u-at.m3u8?variant_version=1&tag=14"
OUTPUT_DIR = Path("downloaded_videos")
TARGET_DURATION = 63  # 1 minute 3 seconds
FIREBASE_STORAGE_PATH = "videos/landing"  # Path in Firebase Storage
FIREBASE_PROJECT_ID = "genaivideogenerator"  # Auto-detected from google-services.json

# Available resolutions from the m3u8 file
RESOLUTIONS = [
    {"name": "1280x720", "bandwidth": 2372879, "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/1280x720/mMcIfeVfjw-Z1J_D.m3u8"},
    {"name": "640x360", "bandwidth": 889250, "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/640x360/cbYJT2bxdEuam1A5.m3u8"},
    {"name": "480x270", "bandwidth": 308531, "url": "https://video.twimg.com/amplify_video/1858525650694635520/pl/avc1/480x270/P0RHyJWr6wy0B68H.m3u8"},
]

def check_ffmpeg():
    """Check if ffmpeg is installed."""
    try:
        subprocess.run(["ffmpeg", "-version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("ERROR: ffmpeg is not installed")
        print("Install: brew install ffmpeg (macOS) or sudo apt-get install ffmpeg (Linux)")
        return False

def check_gsutil():
    """Check if gsutil is installed."""
    try:
        subprocess.run(["gsutil", "version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("WARNING: gsutil is not installed")
        print("Install: pip install gsutil or use Firebase CLI")
        print("Alternative: Use Firebase Console to upload manually")
        return False

def get_firebase_config():
    """Get Firebase project ID and storage bucket from google-services.json."""
    global FIREBASE_PROJECT_ID
    google_services_path = Path("genai-android/app/google-services.json")
    if google_services_path.exists():
        try:
            with open(google_services_path) as f:
                data = json.load(f)
                project_info = data.get("project_info", {})
                project_id = project_info.get("project_id")
                storage_bucket = project_info.get("storage_bucket")
                if project_id:
                    FIREBASE_PROJECT_ID = project_id
                return project_id, storage_bucket
        except Exception as e:
            print(f"Warning: Could not read google-services.json: {e}")
    
    return None, None

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

def get_video_info(video_path):
    """Get video resolution and codec info."""
    try:
        cmd = [
            "ffprobe",
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,codec_name",
            "-of", "json",
            str(video_path)
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        data = json.loads(result.stdout)
        stream = data.get("streams", [{}])[0]
        return {
            "width": stream.get("width"),
            "height": stream.get("height"),
            "codec": stream.get("codec_name", "h264")
        }
    except Exception as e:
        print(f"Warning: Could not get video info: {e}")
        return None

def download_m3u8(m3u8_url, output_path):
    """Download m3u8 video and convert to MP4."""
    print(f"\n📥 Downloading: {output_path.name}")
    
    cmd = [
        "ffmpeg",
        "-i", m3u8_url,
        "-c", "copy",
        "-bsf:a", "aac_adtstoasc",
        "-y",
        str(output_path)
    ]
    
    try:
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✅ Downloaded: {output_path.name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Error downloading {output_path.name}")
        return False

def trim_video(input_path, output_path, target_duration):
    """Trim video to specific duration."""
    duration = get_video_duration(input_path)
    
    if duration is None:
        print("⚠️  Could not determine video duration. Skipping trim.")
        return False
    
    target = min(target_duration, duration)
    
    if target >= duration:
        print(f"⚠️  Target duration ({target}s) >= original ({duration}s). Skipping trim.")
        return False
    
    print(f"   Trimming to {target}s (0:00 - {int(target // 60)}:{int(target % 60):02d})")
    
    cmd = [
        "ffmpeg",
        "-i", str(input_path),
        "-t", str(target),
        "-c", "copy",
        "-y",
        str(output_path)
    ]
    
    try:
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✅ Trimmed: {output_path.name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Error trimming {output_path.name}")
        return False

def upload_to_firebase_storage(file_path, storage_path, project_id, storage_bucket=None):
    """Upload file to Firebase Storage using gsutil."""
    # Use storage_bucket if provided, otherwise fall back to default format
    if storage_bucket:
        bucket = f"gs://{storage_bucket}"
    else:
        bucket = f"gs://{project_id}.appspot.com"
    gs_path = f"{bucket}/{storage_path}/{file_path.name}"
    
    print(f"\n📤 Uploading to Firebase Storage...")
    print(f"   Local: {file_path.name}")
    print(f"   Remote: {storage_path}/{file_path.name}")
    
    cmd = ["gsutil", "cp", str(file_path), gs_path]
    
    try:
        subprocess.run(cmd, check=True)
        
        # Make file publicly readable
        cmd_acl = ["gsutil", "acl", "ch", "-u", "AllUsers:R", gs_path]
        subprocess.run(cmd_acl, check=True, capture_output=True)
        
        # Get public URL
        bucket_name = storage_bucket if storage_bucket else f"{project_id}.appspot.com"
        public_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket_name}/o/{quote(storage_path, safe='')}%2F{quote(file_path.name, safe='')}?alt=media"
        
        print(f"✅ Uploaded: {file_path.name}")
        return public_url
    except subprocess.CalledProcessError as e:
        print(f"❌ Upload failed: {e}")
        return None

def generate_m3u8_playlist(resolutions_data, output_path, base_url):
    """Generate master m3u8 playlist for adaptive streaming."""
    print(f"\n📝 Generating m3u8 playlist...")
    
    lines = [
        "#EXTM3U",
        "#EXT-X-VERSION:6",
        "#EXT-X-INDEPENDENT-SEGMENTS",
        ""
    ]
    
    # Add audio tracks (if you have separate audio, otherwise skip)
    # For now, we'll assume audio is embedded in video
    
    # Add video streams
    for res in sorted(resolutions_data, key=lambda x: x['bandwidth'], reverse=True):
        name = res['name']
        width, height = name.split('x')
        bandwidth = res['bandwidth']
        url = res['url']
        
        # Generate individual m3u8 for this resolution
        individual_m3u8 = f"{name}.m3u8"
        individual_url = f"{base_url}/{individual_m3u8}"
        
        lines.append(f'#EXT-X-STREAM-INF:BANDWIDTH={bandwidth},RESOLUTION={name},CODECS="mp4a.40.2,avc1.640020"')
        lines.append(individual_url)
        lines.append("")
    
    content = "\n".join(lines)
    
    with open(output_path, "w") as f:
        f.write(content)
    
    print(f"✅ Generated: {output_path.name}")
    return content

def generate_individual_m3u8(video_url, output_path, duration):
    """Generate individual m3u8 playlist for a single resolution."""
    # For MP4 files, we create a simple m3u8 that points to the MP4
    # This works with ExoPlayer and most HLS players
    lines = [
        "#EXTM3U",
        "#EXT-X-VERSION:6",
        f"#EXTINF:{duration:.6f},",
        video_url,
        "#EXT-X-ENDLIST"
    ]
    
    content = "\n".join(lines)
    
    with open(output_path, "w") as f:
        f.write(content)
    
    return content

def upload_existing_files(output_dir, storage_path, project_id, storage_bucket, has_gsutil):
    """Upload existing files without downloading."""
    files_to_upload = [
        "landing_video_1280x720.mp4",
        "landing_video_640x360.mp4",
        "landing_video_480x270.mp4",
        "landing_video_1280x720.m3u8",
        "landing_video_640x360.m3u8",
        "landing_video_480x270.m3u8",
        "landing_video_master.m3u8",
    ]
    
    existing_files = []
    for filename in files_to_upload:
        file_path = output_dir / filename
        if file_path.exists():
            existing_files.append(file_path)
        else:
            print(f"⚠️  Missing: {filename}")
    
    if not existing_files:
        print("\n❌ No files found to upload!")
        return
    
    print(f"\n📦 Found {len(existing_files)} files to upload")
    
    uploaded_urls = {}
    for file_path in existing_files:
        print(f"\n📤 Uploading: {file_path.name}")
        url = upload_to_firebase_storage(file_path, storage_path, project_id, storage_bucket)
        if url:
            uploaded_urls[file_path.name] = url
    
    # Find master playlist URL
    master_url = uploaded_urls.get("landing_video_master.m3u8")
    
    print(f"\n{'='*70}")
    print("📊 Upload Summary")
    print(f"{'='*70}")
    print(f"\n✅ Uploaded: {len(uploaded_urls)}/{len(existing_files)} files")
    
    if master_url:
        print(f"\n✅ Master Playlist URL:")
        print(f"   {master_url}")
        print(f"\n📝 Update your landing page config:")
        print(f"   Firebase Firestore → app/landingPage")
        print(f"   Set backgroundVideoUrl to the URL above")
    else:
        print(f"\n⚠️  Master playlist not found in uploaded files")

def main():
    """Main function."""
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Download, trim, and upload m3u8 videos to Firebase Storage')
    parser.add_argument('--skip-download', action='store_true', 
                       help='Skip download/trim, just upload existing files')
    args = parser.parse_args()
    
    print("=" * 70)
    if args.skip_download:
        print("Upload Existing Videos to Firebase Storage")
    else:
        print("M3U8 Video Download, Trim & Upload to Firebase Storage")
    print("=" * 70)
    
    # Check dependencies (only if not skipping download)
    if not args.skip_download:
        if not check_ffmpeg():
            sys.exit(1)
    
    has_gsutil = check_gsutil()
    project_id, storage_bucket = get_firebase_config()
    
    if not has_gsutil:
        print("\n⚠️  gsutil not found. You'll need to upload manually.")
        print("   Option 1: Install gsutil: pip install gsutil")
        print("   Option 2: Use Firebase Console to upload files")
        print("   Option 3: Use Firebase CLI: firebase storage:upload")
        response = input("\nContinue with download/trim only? (y/n): ")
        if response.lower() != 'y':
            sys.exit(0)
    
    if has_gsutil and not project_id:
        print("\n⚠️  Could not detect Firebase project ID.")
        project_id = input("Enter your Firebase project ID (or press Enter to skip upload): ").strip()
        if not project_id:
            has_gsutil = False
        else:
            storage_bucket = input(f"Enter storage bucket (or press Enter for {project_id}.appspot.com): ").strip()
            if not storage_bucket:
                storage_bucket = None

    
    # Create output directory
    OUTPUT_DIR.mkdir(exist_ok=True)
    print(f"\n📁 Output directory: {OUTPUT_DIR.absolute()}")
    
    # If skipping download, just upload existing files
    if args.skip_download:
        upload_existing_files(OUTPUT_DIR, FIREBASE_STORAGE_PATH, project_id, storage_bucket, has_gsutil)
        return
    
    results = []
    
    # Process each resolution
    for resolution in RESOLUTIONS:
        name = resolution["name"]
        url = resolution["url"]
        bandwidth = resolution["bandwidth"]
        
        print(f"\n{'='*70}")
        print(f"Processing: {name}")
        print(f"{'='*70}")
        
        # File paths
        temp_file = OUTPUT_DIR / f"temp_{name}.mp4"
        final_file = OUTPUT_DIR / f"landing_video_{name}.mp4"
        
        # Step 1: Download
        if not download_m3u8(url, temp_file):
            print(f"⚠️  Skipping {name} due to download error")
            continue
        
        # Step 2: Trim
        if not trim_video(temp_file, final_file, TARGET_DURATION):
            print(f"⚠️  Trim failed for {name}, keeping original")
            if temp_file.exists():
                temp_file.rename(final_file)
        
        # Clean up temp file
        if temp_file.exists() and final_file.exists():
            temp_file.unlink()
        
        # Step 3: Get video info
        video_info = get_video_info(final_file)
        duration = get_video_duration(final_file)
        
        # Step 4: Upload to Firebase Storage
        public_url = None
        if has_gsutil and project_id:
            public_url = upload_to_firebase_storage(
                final_file,
                FIREBASE_STORAGE_PATH,
                project_id,
                storage_bucket
            )
        else:
            print(f"\n⏭️  Skipping upload (manual upload required)")
            print(f"   Upload this file: {final_file.absolute()}")
            print(f"   To: {FIREBASE_STORAGE_PATH}/{final_file.name}")
        
        results.append({
            "resolution": name,
            "bandwidth": bandwidth,
            "file": str(final_file),
            "url": public_url or f"MANUAL_UPLOAD_REQUIRED/{final_file.name}",
            "width": video_info.get("width") if video_info else None,
            "height": video_info.get("height") if video_info else None,
            "codec": video_info.get("codec") if video_info else None,
            "duration": duration
        })
    
    # Generate m3u8 playlists
    print(f"\n{'='*70}")
    print("Generating M3U8 Playlists")
    print(f"{'='*70}")
    
    # Determine base URL
    if project_id:
        # Use storage_bucket if available, otherwise use project_id
        bucket_name = storage_bucket if storage_bucket else f"{project_id}.appspot.com"
        base_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket_name}/o/{quote(FIREBASE_STORAGE_PATH, safe='')}"
    else:
        base_url = "YOUR_CDN_BASE_URL"  # User needs to replace this
    
    # Generate individual m3u8 files for each resolution
    individual_playlists = []
    for result in results:
        if result["url"].startswith("https://"):
            # Generate individual m3u8
            m3u8_name = f"landing_video_{result['resolution']}.m3u8"
            m3u8_path = OUTPUT_DIR / m3u8_name
            
            # URL for the MP4 file
            video_url = result["url"]
            
            generate_individual_m3u8(
                video_url,
                m3u8_path,
                result.get("duration", TARGET_DURATION)
            )
            
            # Upload m3u8 to Firebase Storage
            if has_gsutil and project_id:
                m3u8_public_url = upload_to_firebase_storage(
                    m3u8_path,
                    FIREBASE_STORAGE_PATH,
                    project_id
                )
                individual_playlists.append({
                    "resolution": result["resolution"],
                    "url": m3u8_public_url
                })
            else:
                individual_playlists.append({
                    "resolution": result["resolution"],
                    "url": f"MANUAL_UPLOAD_REQUIRED/{m3u8_name}"
                })
    
    # Generate master m3u8 playlist
    master_m3u8_path = OUTPUT_DIR / "landing_video_master.m3u8"
    
    # Update results with m3u8 URLs for master playlist
    master_playlist_data = []
    for result, playlist in zip(results, individual_playlists):
        if playlist["url"].startswith("https://"):
            master_playlist_data.append({
                "name": result["resolution"],
                "bandwidth": result["bandwidth"],
                "url": playlist["url"]
            })
    
    if master_playlist_data:
        generate_m3u8_playlist(
            master_playlist_data,
            master_m3u8_path,
            base_url
        )
        
        # Upload master m3u8
        if has_gsutil and project_id:
            master_url = upload_to_firebase_storage(
                master_m3u8_path,
                FIREBASE_STORAGE_PATH,
                project_id
            )
        else:
            master_url = f"MANUAL_UPLOAD_REQUIRED/{master_m3u8_path.name}"
    
    # Summary
    print(f"\n{'='*70}")
    print("📊 Summary")
    print(f"{'='*70}")
    
    for result in results:
        file_path = Path(result["file"])
        if file_path.exists():
            size_mb = file_path.stat().st_size / (1024 * 1024)
            print(f"\n✅ {result['resolution']}:")
            print(f"   File: {file_path.name} ({size_mb:.2f} MB)")
            print(f"   URL: {result['url']}")
            if result.get("duration"):
                print(f"   Duration: {result['duration']:.2f}s")
    
    # Save results to JSON
    json_output = OUTPUT_DIR / "upload_results.json"
    output_data = {
        "master_playlist_url": master_url if 'master_url' in locals() else None,
        "resolutions": results,
        "individual_playlists": individual_playlists,
        "firebase_storage_path": FIREBASE_STORAGE_PATH,
        "project_id": project_id,
        "storage_bucket": storage_bucket if 'storage_bucket' in locals() else None
    }
    
    with open(json_output, "w") as f:
        json.dump(output_data, f, indent=2)
    
    print(f"\n📄 Results saved to: {json_output}")
    
    print(f"\n{'='*70}")
    print("💡 Next Steps")
    print(f"{'='*70}")
    
    if has_gsutil and project_id:
        print(f"\n✅ All files uploaded to Firebase Storage!")
        print(f"   Master Playlist URL: {master_url if 'master_url' in locals() else 'N/A'}")
        print(f"\n📝 Update your landing page config:")
        print(f"   Firebase Firestore → app/landingPage")
        print(f"   Set backgroundVideoUrl to: {master_url if 'master_url' in locals() else 'YOUR_MASTER_M3U8_URL'}")
    else:
        print(f"\n📤 Manual Upload Required:")
        print(f"   1. Upload all files from: {OUTPUT_DIR.absolute()}")
        print(f"   2. Upload to: {FIREBASE_STORAGE_PATH}/")
        print(f"   3. Make files publicly readable")
        print(f"   4. Update URLs in upload_results.json")
        print(f"   5. Use master playlist URL in landing page config")

if __name__ == "__main__":
    main()

