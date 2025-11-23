# Quick Upload Guide - Use Existing Files

Since videos are already downloaded and trimmed, use the upload-only script:

## Option 1: Dedicated Upload Script (Recommended)

```bash
python3 upload_existing_videos.py
```

This script:
- ✅ Uses existing files in `downloaded_videos/`
- ✅ Uploads all MP4 and m3u8 files
- ✅ Supports both gsutil and Firebase CLI
- ✅ Shows upload progress
- ✅ Provides master playlist URL at the end

## Option 2: Main Script with Skip Flag

```bash
python3 download_upload_m3u8_complete.py --skip-download
```

Same functionality, but uses the main script with a flag.

## What Gets Uploaded

From `downloaded_videos/`:
- `landing_video_1280x720.mp4` (9.1 MB)
- `landing_video_640x360.mp4` (3.0 MB)
- `landing_video_480x270.mp4` (1.0 MB)
- `landing_video_1280x720.m3u8`
- `landing_video_640x360.m3u8`
- `landing_video_480x270.m3u8`
- `landing_video_master.m3u8` ⭐

## After Upload

The script will output the master playlist URL. Use it in:

**Firebase Firestore:**
- Collection: `app`
- Document: `landingPage`
- Field: `backgroundVideoUrl`
- Value: (The master playlist URL from script output)

## Prerequisites

Install one of:
- **gsutil**: `pip install gsutil`
- **Firebase CLI**: `npm install -g firebase-tools` (then `firebase login`)

If neither is installed, upload manually via Firebase Console.

