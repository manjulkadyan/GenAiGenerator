# Quick Start: M3U8 Download & Upload

## One Command Solution

```bash
python3 download_upload_m3u8_complete.py
```

This will:
1. ✅ Download all 3 resolutions (1280x720, 640x360, 480x270)
2. ✅ Trim each to 1 minute 3 seconds (63 seconds)
3. ✅ Upload to Firebase Storage: `videos/landing/`
4. ✅ Generate m3u8 playlists for adaptive streaming
5. ✅ Create master playlist URL for your landing page

## What You Get

After running, you'll have:

**Files in Firebase Storage:**
- `videos/landing/landing_video_1280x720.mp4` (High quality)
- `videos/landing/landing_video_640x360.mp4` (Medium quality)
- `videos/landing/landing_video_480x270.mp4` (Low quality)
- `videos/landing/landing_video_master.m3u8` ⭐ **Use this URL!**

**Master Playlist URL:**
```
https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.appspot.com/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media
```

## Update Your Landing Page

**Firebase Firestore:**
- Collection: `app`
- Document: `landingPage`
- Field: `backgroundVideoUrl`
- Value: (The master playlist URL from above)

## Prerequisites

1. **ffmpeg** (required)
   ```bash
   brew install ffmpeg  # macOS
   ```

2. **gsutil** (optional, for auto-upload)
   ```bash
   pip install gsutil
   ```
   
   If not installed, script will download/trim only. Upload manually via Firebase Console.

## Manual Upload (If Needed)

If gsutil is not available:

1. Go to Firebase Console → Storage
2. Create folder: `videos/landing/`
3. Upload all files from `downloaded_videos/` folder
4. Make files publicly readable
5. Copy the master.m3u8 URL

## Storage Rules

Already updated! The `storage.rules` file now includes:
```javascript
match /videos/landing/{allPaths=**} {
  allow read: if true;  // Public read
  allow write: if request.auth != null;
}
```

Deploy rules:
```bash
cd genai-android
firebase deploy --only storage
```

## That's It!

Your landing page will now use adaptive streaming:
- Fast connection → 1280x720 (HD)
- Medium connection → 640x360
- Slow connection → 480x270

ExoPlayer handles everything automatically! 🎉

