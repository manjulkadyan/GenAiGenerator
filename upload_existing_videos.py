#!/usr/bin/env python3
"""
Upload existing downloaded videos and m3u8 playlists to Firebase Storage.
Skips download/trim - uses files that are already ready.
"""

import subprocess
import json
import sys
from pathlib import Path
from urllib.parse import quote

# Configuration
OUTPUT_DIR = Path("downloaded_videos")
FIREBASE_STORAGE_PATH = "videos/landing"
PROJECT_ID = "genaivideogenerator"
STORAGE_BUCKET = "genaivideogenerator.firebasestorage.app"

# Files to upload
FILES_TO_UPLOAD = [
    "landing_video_1280x720.mp4",
    "landing_video_640x360.mp4",
    "landing_video_480x270.mp4",
    "landing_video_1280x720.m3u8",
    "landing_video_640x360.m3u8",
    "landing_video_480x270.m3u8",
    "landing_video_master.m3u8",
]

def check_gsutil():
    """Check if gsutil is installed."""
    try:
        subprocess.run(["gsutil", "version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False

def check_firebase_cli():
    """Check if Firebase CLI is installed."""
    try:
        subprocess.run(["firebase", "--version"], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False

def upload_with_gsutil(file_path, storage_path, storage_bucket):
    """Upload file using gsutil."""
    bucket = f"gs://{storage_bucket}"
    gs_path = f"{bucket}/{storage_path}/{file_path.name}"
    
    print(f"   📤 Uploading: {file_path.name}")
    
    cmd = ["gsutil", "cp", str(file_path), gs_path]
    
    try:
        subprocess.run(cmd, check=True, capture_output=True)
        
        # Make file publicly readable
        cmd_acl = ["gsutil", "acl", "ch", "-u", "AllUsers:R", gs_path]
        subprocess.run(cmd_acl, check=True, capture_output=True)
        
        # Get public URL
        public_url = f"https://firebasestorage.googleapis.com/v0/b/{storage_bucket}/o/{quote(storage_path, safe='')}%2F{quote(file_path.name, safe='')}?alt=media"
        
        print(f"   ✅ Uploaded: {file_path.name}")
        return public_url
    except subprocess.CalledProcessError as e:
        print(f"   ❌ Upload failed: {e}")
        return None

def upload_with_firebase_cli(file_path, storage_path, project_id):
    """Upload file using Firebase CLI."""
    print(f"   📤 Uploading: {file_path.name}")
    
    # Use absolute path for Firebase CLI
    absolute_path = file_path.resolve()
    
    cmd = [
        "firebase",
        "storage:upload",
        str(absolute_path),
        f"{storage_path}/",
        "--project", project_id
    ]
    
    try:
        # Run from current directory (Firebase CLI handles paths)
        result = subprocess.run(
            cmd,
            check=True,
            capture_output=True,
            text=True
        )
        
        # Get public URL
        storage_bucket = f"{project_id}.firebasestorage.app"
        public_url = f"https://firebasestorage.googleapis.com/v0/b/{storage_bucket}/o/{quote(storage_path, safe='')}%2F{quote(file_path.name, safe='')}?alt=media"
        
        print(f"   ✅ Uploaded: {file_path.name}")
        return public_url
    except subprocess.CalledProcessError as e:
        error_msg = e.stderr if hasattr(e, 'stderr') and e.stderr else (e.stdout if hasattr(e, 'stdout') else str(e))
        print(f"   ❌ Upload failed: {error_msg}")
        return None

def main():
    print("=" * 70)
    print("Upload Existing Videos to Firebase Storage")
    print("=" * 70)
    
    # Check for upload tools
    has_gsutil = check_gsutil()
    has_firebase = check_firebase_cli()
    
    if not has_gsutil and not has_firebase:
        print("\n❌ No upload tool found!")
        print("   Install one of:")
        print("   - gsutil: pip install gsutil")
        print("   - Firebase CLI: npm install -g firebase-tools")
        print("\n   Or upload manually via Firebase Console:")
        print("   https://console.firebase.google.com/project/genaivideogenerator/storage")
        sys.exit(1)
    
    # Check if files exist
    missing_files = []
    existing_files = []
    
    for filename in FILES_TO_UPLOAD:
        file_path = OUTPUT_DIR / filename
        if file_path.exists():
            existing_files.append(file_path)
        else:
            missing_files.append(filename)
    
    if missing_files:
        print(f"\n⚠️  Missing files:")
        for f in missing_files:
            print(f"   - {f}")
        print(f"\n   Found {len(existing_files)}/{len(FILES_TO_UPLOAD)} files")
        response = input("\nContinue with existing files only? (y/n): ")
        if response.lower() != 'y':
            sys.exit(0)
    
    if not existing_files:
        print("\n❌ No files found to upload!")
        print(f"   Expected files in: {OUTPUT_DIR.absolute()}")
        sys.exit(1)
    
    print(f"\n📁 Found {len(existing_files)} files to upload")
    print(f"   Storage path: {FIREBASE_STORAGE_PATH}/")
    print(f"   Bucket: {STORAGE_BUCKET}")
    
    # Choose upload method
    upload_method = None
    if has_gsutil and has_firebase:
        print(f"\n🔧 Upload tools available:")
        print(f"   1. gsutil (recommended)")
        print(f"   2. Firebase CLI")
        choice = input("   Choose method (1/2): ").strip()
        upload_method = "gsutil" if choice == "1" else "firebase"
    elif has_gsutil:
        upload_method = "gsutil"
    else:
        upload_method = "firebase"
    
    print(f"\n🚀 Using: {upload_method}")
    print("=" * 70)
    
    # Upload files
    uploaded_files = []
    failed_files = []
    
    for file_path in existing_files:
        print(f"\n📦 {file_path.name}")
        
        if upload_method == "gsutil":
            url = upload_with_gsutil(file_path, FIREBASE_STORAGE_PATH, STORAGE_BUCKET)
        else:
            url = upload_with_firebase_cli(file_path, FIREBASE_STORAGE_PATH, PROJECT_ID)
        
        if url:
            uploaded_files.append({
                "file": str(file_path),
                "name": file_path.name,
                "url": url
            })
        else:
            failed_files.append(file_path.name)
    
    # Summary
    print("\n" + "=" * 70)
    print("📊 Upload Summary")
    print("=" * 70)
    
    print(f"\n✅ Successfully uploaded: {len(uploaded_files)}/{len(existing_files)}")
    for item in uploaded_files:
        print(f"   ✅ {item['name']}")
    
    if failed_files:
        print(f"\n❌ Failed: {len(failed_files)}")
        for name in failed_files:
            print(f"   ❌ {name}")
    
    # Find master playlist URL
    master_url = None
    for item in uploaded_files:
        if "master.m3u8" in item['name']:
            master_url = item['url']
            break
    
    # Save results
    results = {
        "master_playlist_url": master_url,
        "uploaded_files": uploaded_files,
        "failed_files": failed_files,
        "firebase_storage_path": FIREBASE_STORAGE_PATH,
        "project_id": PROJECT_ID,
        "storage_bucket": STORAGE_BUCKET
    }
    
    results_file = OUTPUT_DIR / "upload_results.json"
    with open(results_file, "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📄 Results saved to: {results_file}")
    
    # Next steps
    print("\n" + "=" * 70)
    print("💡 Next Steps")
    print("=" * 70)
    
    if master_url:
        print(f"\n✅ Master Playlist URL:")
        print(f"   {master_url}")
        print(f"\n📝 Update your landing page config:")
        print(f"   Firebase Firestore → app/landingPage")
        print(f"   Set backgroundVideoUrl to the URL above")
    else:
        print(f"\n⚠️  Master playlist not uploaded. Upload manually:")
        print(f"   {OUTPUT_DIR / 'landing_video_master.m3u8'}")
    
    if failed_files:
        print(f"\n⚠️  Some files failed to upload. Upload manually via Firebase Console:")
        print(f"   https://console.firebase.google.com/project/genaivideogenerator/storage")

if __name__ == "__main__":
    main()

