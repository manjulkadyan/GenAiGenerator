# Complete M3U8 Download, Trim & Upload Guide

This guide explains how to download all resolutions, trim them, upload to Firebase Storage, and create adaptive streaming m3u8 playlists.

## What This Does

1. ✅ Downloads all 3 resolutions (1280x720, 640x360, 480x270)
2. ✅ Trims each video to exactly 1 minute 3 seconds (63 seconds)
3. ✅ Uploads all videos to Firebase Storage
4. ✅ Generates individual m3u8 playlists for each resolution
5. ✅ Creates a master m3u8 playlist for adaptive streaming
6. ✅ Makes all files publicly accessible

## Prerequisites

### 1. Install ffmpeg
```bash
# macOS
brew install ffmpeg

# Linux
sudo apt-get install ffmpeg
```

### 2. Install gsutil (for Firebase Storage upload)

**Option A: Using pip (Recommended)**
```bash
pip install gsutil
```

**Option B: Using Firebase CLI**
```bash
npm install -g firebase-tools
firebase login
```

**Option C: Manual Upload**
If you don't want to install gsutil, you can upload manually via Firebase Console.

## Quick Start

### Run the Complete Script

```bash
python3 download_upload_m3u8_complete.py
```

The script will:
1. Download all resolutions
2. Trim each to 63 seconds
3. Upload to Firebase Storage (if gsutil is available)
4. Generate m3u8 playlists
5. Save results to `upload_results.json`

## Firebase Storage Setup

### Update Storage Rules

Add public read access for landing page videos. Update `genai-android/storage.rules`:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Public read access for landing page videos
    match /videos/landing/{allPaths=**} {
      allow read: if true;  // Public read
      allow write: if request.auth != null;  // Authenticated write
    }
    
    // ... rest of your rules
  }
}
```

Then deploy:
```bash
cd genai-android
firebase deploy --only storage
```

## Manual Upload (If gsutil not available)

If the script skips upload, manually upload files:

1. **Go to Firebase Console** → Storage
2. **Create folder**: `videos/landing/`
3. **Upload all files** from `downloaded_videos/`:
   - `landing_video_1280x720.mp4`
   - `landing_video_640x360.mp4`
   - `landing_video_480x270.mp4`
   - `landing_video_1280x720.m3u8`
   - `landing_video_640x360.m3u8`
   - `landing_video_480x270.m3u8`
   - `landing_video_master.m3u8`
4. **Make files public**: Right-click each file → "Get download URL" or set permissions

## Using Firebase CLI (Alternative to gsutil)

```bash
cd genai-android

# Upload all files
firebase storage:upload ../downloaded_videos/landing_video_1280x720.mp4 videos/landing/
firebase storage:upload ../downloaded_videos/landing_video_640x360.mp4 videos/landing/
firebase storage:upload ../downloaded_videos/landing_video_480x270.mp4 videos/landing/
firebase storage:upload ../downloaded_videos/landing_video_master.m3u8 videos/landing/
```

## Update Landing Page Config

After upload, update your Firebase Firestore:

**Collection:** `app`  
**Document:** `landingPage`

Update `backgroundVideoUrl` to the master m3u8 URL:

```json
{
  "backgroundVideoUrl": "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.appspot.com/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media",
  ...
}
```

Or if using local config file (`landingPageConfig.json`):

```json
{
  "backgroundVideoUrl": "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.appspot.com/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media",
  ...
}
```

## How Adaptive Streaming Works

The master m3u8 playlist contains references to all resolutions:

```
#EXTM3U
#EXT-X-VERSION:6
#EXT-X-STREAM-INF:BANDWIDTH=2372879,RESOLUTION=1280x720
landing_video_1280x720.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=889250,RESOLUTION=640x360
landing_video_640x360.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=308531,RESOLUTION=480x270
landing_video_480x270.m3u8
```

ExoPlayer (used in your app) will:
1. Download the master playlist
2. Automatically select the best quality based on network conditions
3. Switch between qualities if network changes
4. Provide smooth playback experience

## File Structure in Firebase Storage

```
videos/
  landing/
    landing_video_1280x720.mp4    (High quality)
    landing_video_640x360.mp4      (Medium quality)
    landing_video_480x270.mp4      (Low quality)
    landing_video_1280x720.m3u8    (Playlist for 720p)
    landing_video_640x360.m3u8     (Playlist for 360p)
    landing_video_480x270.m3u8     (Playlist for 270p)
    landing_video_master.m3u8      (Master playlist - use this!)
```

## Troubleshooting

### gsutil not found
```bash
# Install gsutil
pip install gsutil

# Or use Firebase CLI instead
npm install -g firebase-tools
```

### Upload permission denied
- Check Firebase Storage rules allow public read for `videos/landing/`
- Ensure you're authenticated: `firebase login` or `gcloud auth login`

### m3u8 not playing
- Ensure all files are publicly accessible
- Check URLs in master.m3u8 are correct
- Verify ExoPlayer supports HLS (it does by default)

### Video quality issues
- Use 1280x720 for best quality
- Lower resolutions are for slower connections
- ExoPlayer will auto-select based on network

## Configuration

Edit `download_upload_m3u8_complete.py` to change:

```python
TARGET_DURATION = 63  # Change video length (in seconds)
FIREBASE_STORAGE_PATH = "videos/landing"  # Change storage path
```

## Benefits of Adaptive Streaming

✅ **Better User Experience**: Automatically adjusts to network speed  
✅ **Reduced Bandwidth**: Uses lower quality on slow connections  
✅ **Smoother Playback**: Less buffering, better performance  
✅ **Cost Efficient**: Users only download what they need  

## Next Steps

1. ✅ Run the script: `python3 download_upload_m3u8_complete.py`
2. ✅ Update Firebase Storage rules (if needed)
3. ✅ Update Firestore `backgroundVideoUrl` with master m3u8 URL
4. ✅ Test in your app - ExoPlayer will handle adaptive streaming automatically!

