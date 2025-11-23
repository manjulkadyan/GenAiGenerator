# Testing M3U8 Video in BuyCreditsScreen

## ✅ Setup Complete

1. **Videos uploaded** to Firebase Storage
2. **Master playlist URL** added to `landingPageConfig.json`
3. **BackgroundVideoPlayer** enhanced with logging

## 🚀 Deploy to Firebase

### Option 1: Using Seed Script (Recommended)

```bash
cd genai-android/functions
npm run seed:landing-page
```

This will upload the updated config to Firebase Firestore.

### Option 2: Manual Upload

1. Go to [Firebase Console](https://console.firebase.google.com/project/genaivideogenerator/firestore)
2. Navigate to: **Firestore Database** → Collection: `app` → Document: `landingPage`
3. Update the `backgroundVideoUrl` field with:
   ```
   https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media
   ```
4. Save the document

## 📱 Testing in App

### 1. Run the App

```bash
cd genai-android
./gradlew installDebug
```

Or use Android Studio to run the app.

### 2. Navigate to BuyCreditsScreen

The video should automatically load when you open the Buy Credits screen.

### 3. Check Logs

Watch for these log messages in Logcat:

**Success indicators:**
```
BackgroundVideoPlayer: Loading video: https://...
BackgroundVideoPlayer: Video player initialized successfully
BackgroundVideoPlayer: Playback state: READY
BackgroundVideoPlayer: Is playing: true
```

**Error indicators:**
```
BackgroundVideoPlayer: Playback error: ...
BackgroundVideoPlayer: Failed to initialize player
```

### 4. What to Test

✅ **Video loads and plays** (should loop automatically)  
✅ **Adaptive streaming works** (check network tab - should select quality based on connection)  
✅ **Video is muted** (background video should be silent)  
✅ **Video loops** (should restart when it ends)  
✅ **Overlay is visible** (dark overlay for content readability)  
✅ **Video pauses/resumes** with app lifecycle  

## 🔍 Troubleshooting

### Video Not Loading

**Check:**
1. Firebase Storage rules allow public read for `videos/landing/`
2. Master playlist URL is correct in Firestore
3. Network connectivity
4. Logcat for error messages

**Fix Storage Rules:**
```javascript
match /videos/landing/{allPaths=**} {
  allow read: if true;  // Public read
}
```

Then deploy:
```bash
cd genai-android
firebase deploy --only storage
```

### Playback Errors

**Check Logcat for:**
- Network errors
- Codec errors
- URL format issues

**Common fixes:**
- Ensure all files are uploaded (MP4s + m3u8s)
- Verify URLs in master.m3u8 are correct
- Check ExoPlayer version supports HLS

### Video Plays But Wrong Quality

**Expected behavior:**
- ExoPlayer automatically selects quality based on network
- Fast connection → 1280x720
- Medium connection → 640x360
- Slow connection → 480x270

This is **adaptive streaming** working correctly!

### Video Not Looping

**Check:**
- `repeatMode = Player.REPEAT_MODE_ONE` is set (already in code)
- Video reaches end (check duration)
- No errors in logcat

## 🎯 Expected Behavior

1. **On Screen Open:**
   - Video starts loading immediately
   - Shows buffering state briefly
   - Starts playing when ready
   - Loops continuously

2. **During Playback:**
   - Video quality adapts to network
   - Smooth playback (no stuttering)
   - Dark overlay visible over video
   - Content readable on top

3. **On App Lifecycle:**
   - Pauses when app goes to background
   - Resumes when app comes to foreground
   - Releases resources on destroy

## 📊 Debugging Commands

### Check Firebase Storage Files

```bash
# List files in storage
gsutil ls gs://genaivideogenerator.firebasestorage.app/videos/landing/

# Check file permissions
gsutil ls -L gs://genaivideogenerator.firebasestorage.app/videos/landing/landing_video_master.m3u8
```

### Test Master Playlist URL

Open in browser:
```
https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media
```

Should download the m3u8 file.

### Test Individual Playlist

Open in browser:
```
https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_1280x720.m3u8?alt=media
```

Should download the individual playlist.

## ✅ Success Checklist

- [ ] Config updated in `landingPageConfig.json`
- [ ] Config seeded to Firebase Firestore
- [ ] All files uploaded to Firebase Storage
- [ ] Storage rules allow public read
- [ ] App runs without crashes
- [ ] Video loads in BuyCreditsScreen
- [ ] Video plays and loops
- [ ] Adaptive streaming works (check network)
- [ ] Overlay visible and content readable
- [ ] Logs show successful playback

## 🎉 You're Done!

Once all checks pass, your landing page video is working with adaptive streaming!

