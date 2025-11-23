# Upload Instructions - Landing Page Videos

## ✅ What's Ready

All videos are downloaded, trimmed, and m3u8 playlists are generated!

**Files ready to upload:**
- `landing_video_1280x720.mp4` (9.1 MB) - High quality
- `landing_video_640x360.mp4` (3.0 MB) - Medium quality  
- `landing_video_480x270.mp4` (1.0 MB) - Low quality
- `landing_video_1280x720.m3u8` - Playlist for 720p
- `landing_video_640x360.m3u8` - Playlist for 360p
- `landing_video_480x270.m3u8` - Playlist for 270p
- `landing_video_master.m3u8` ⭐ **Use this URL in your app!**

## 📤 Upload Methods

### Method 1: Firebase Console (Easiest)

1. **Go to Firebase Console**
   - https://console.firebase.google.com/project/genaivideogenerator/storage

2. **Create folder** (if it doesn't exist):
   - Click "Get started" or navigate to Storage
   - Create folder: `videos/landing/`

3. **Upload all files:**
   - Click "Upload file"
   - Upload all 7 files from `downloaded_videos/` folder:
     - All 3 MP4 files
     - All 4 M3U8 files

4. **Make files public:**
   - Right-click each file → "Get download URL"
   - Or: Click file → "File location" tab → Copy URL

5. **Get Master Playlist URL:**
   - Right-click `landing_video_master.m3u8`
   - Copy the download URL
   - It should look like:
     ```
     https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media
     ```

### Method 2: Firebase CLI

```bash
# Make sure you're logged in
firebase login

# Navigate to project
cd genai-android

# Upload all files
firebase storage:upload ../downloaded_videos/landing_video_1280x720.mp4 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_640x360.mp4 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_480x270.mp4 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_1280x720.m3u8 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_640x360.m3u8 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_480x270.m3u8 videos/landing/ --project genaivideogenerator
firebase storage:upload ../downloaded_videos/landing_video_master.m3u8 videos/landing/ --project genaivideogenerator
```

Or use the provided script:
```bash
./upload_to_firebase.sh
```

### Method 3: Fixed gsutil (After fixing bucket name)

The script has been fixed. You can re-run:
```bash
python3 download_upload_m3u8_complete.py
```

But since files are already downloaded, just use Method 1 or 2.

## 🔧 Update Storage Rules

Make sure your storage rules allow public read access:

**File:** `genai-android/storage.rules`

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

## 📝 Update Landing Page Config

After uploading, update your Firebase Firestore:

**Collection:** `app`  
**Document:** `landingPage`  
**Field:** `backgroundVideoUrl`

**Value:** (The master playlist URL from Firebase Console)

Example:
```json
{
  "backgroundVideoUrl": "https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media",
  ...
}
```

## ✅ Verify Upload

1. Check all files are in Firebase Storage: `videos/landing/`
2. Test the master playlist URL in a browser (should download the m3u8 file)
3. Test in your app - ExoPlayer will handle adaptive streaming automatically!

## 🎉 That's It!

Your landing page will now use adaptive streaming:
- Fast connection → 1280x720 (HD)
- Medium connection → 640x360
- Slow connection → 480x270

ExoPlayer handles everything automatically!

