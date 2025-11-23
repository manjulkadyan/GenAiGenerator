#!/usr/bin/env python3
"""
Generate m3u8 playlists for already downloaded videos.
Use this after videos are downloaded and trimmed.
"""

import json
from pathlib import Path
from urllib.parse import quote

OUTPUT_DIR = Path("downloaded_videos")
FIREBASE_STORAGE_PATH = "videos/landing"
PROJECT_ID = "genaivideogenerator"
STORAGE_BUCKET = "genaivideogenerator.firebasestorage.app"

# Resolution data
RESOLUTIONS = [
    {"name": "1280x720", "bandwidth": 2372879},
    {"name": "640x360", "bandwidth": 889250},
    {"name": "480x270", "bandwidth": 308531},
]

def get_video_duration(video_path):
    """Get video duration using ffprobe."""
    import subprocess
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
    except:
        return 63.0  # Default

def generate_individual_m3u8(video_url, output_path, duration):
    """Generate individual m3u8 playlist."""
    lines = [
        "#EXTM3U",
        "#EXT-X-VERSION:6",
        f"#EXTINF:{duration:.6f},",
        video_url,
        "#EXT-X-ENDLIST"
    ]
    
    with open(output_path, "w") as f:
        f.write("\n".join(lines))
    
    print(f"✅ Generated: {output_path.name}")

def generate_master_playlist(resolutions_data, output_path, base_url_prefix):
    """Generate master m3u8 playlist pointing directly to MP4 files."""
    lines = [
        "#EXTM3U",
        "#EXT-X-VERSION:6",
        "#EXT-X-INDEPENDENT-SEGMENTS",
        ""
    ]
    
    for res in sorted(resolutions_data, key=lambda x: x['bandwidth'], reverse=True):
        name = res['name']
        bandwidth = res['bandwidth']
        # Point directly to MP4 files (not individual m3u8 files)
        # HLS can work with regular MP4 files when pointed to directly
        mp4_url = res['mp4_url']
        
        lines.append(f'#EXT-X-STREAM-INF:BANDWIDTH={bandwidth},RESOLUTION={name},CODECS="mp4a.40.2,avc1.640020"')
        lines.append(mp4_url)
        lines.append("")
    
    with open(output_path, "w") as f:
        f.write("\n".join(lines))
    
    print(f"✅ Generated: {output_path.name}")

def main():
    print("=" * 70)
    print("Generating M3U8 Playlists")
    print("=" * 70)
    
    base_url_prefix = f"https://firebasestorage.googleapis.com/v0/b/{STORAGE_BUCKET}/o/"
    
    resolutions_data = []
    
    # Generate individual playlists
    for res in RESOLUTIONS:
        name = res["name"]
        mp4_file = OUTPUT_DIR / f"landing_video_{name}.mp4"
        m3u8_file = OUTPUT_DIR / f"landing_video_{name}.m3u8"
        
        if not mp4_file.exists():
            print(f"⚠️  Skipping {name}: MP4 file not found")
            continue
        
        # Get duration
        duration = get_video_duration(mp4_file)
        
        # Generate MP4 URL - encode the full path including the slash
        mp4_path = f"{FIREBASE_STORAGE_PATH}/landing_video_{name}.mp4"
        mp4_url = f"{base_url_prefix}{quote(mp4_path, safe='')}?alt=media"
        
        # Generate individual m3u8
        generate_individual_m3u8(mp4_url, m3u8_file, duration)
        
        # Generate m3u8 URL - encode the full path including the slash
        m3u8_path = f"{FIREBASE_STORAGE_PATH}/landing_video_{name}.m3u8"
        m3u8_url = f"{base_url_prefix}{quote(m3u8_path, safe='')}?alt=media"
        
        resolutions_data.append({
            "name": name,
            "bandwidth": res["bandwidth"],
            "mp4_url": mp4_url,
            "m3u8_url": m3u8_url
        })
    
    # Generate master playlist
    if resolutions_data:
        master_file = OUTPUT_DIR / "landing_video_master.m3u8"
        generate_master_playlist(resolutions_data, master_file, base_url_prefix)
        
        master_path = f"{FIREBASE_STORAGE_PATH}/landing_video_master.m3u8"
        master_url = f"{base_url_prefix}{quote(master_path, safe='')}?alt=media"
        
        print(f"\n{'='*70}")
        print("✅ All playlists generated!")
        print(f"{'='*70}")
        print(f"\n📝 Master Playlist URL:")
        print(f"   {master_url}")
        print(f"\n💡 Next steps:")
        print(f"   1. Upload all files from {OUTPUT_DIR} to Firebase Storage")
        print(f"   2. Path: {FIREBASE_STORAGE_PATH}/")
        print(f"   3. Use the master playlist URL in your landing page config")
    else:
        print("\n❌ No videos found to generate playlists for")

if __name__ == "__main__":
    main()

