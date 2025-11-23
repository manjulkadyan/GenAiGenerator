# Video Playback Fixes & Debugging Guide

## ✅ Fixes Applied

### 1. **Proper HLS/m3u8 Support**
- Added explicit `HlsMediaSource` for m3u8 streams
- ExoPlayer now properly detects and handles HLS streams
- Added `setAllowChunklessPreparation(true)` for faster HLS startup

### 2. **Enhanced Error Handling**
- Comprehensive error logging with error codes and types
- Error state tracking (`hasError`, `errorMessage`)
- Detailed exception logging with stack traces

### 3. **Better DataSource Configuration**
- Using `DefaultDataSource.Factory` for proper network handling
- Supports both HTTP/HTTPS and HLS streams
- Proper MediaSource creation for different stream types

### 4. **Improved Logging**
- Logs when HLS stream is detected
- Logs playback state changes (IDLE, BUFFERING, READY, ENDED)
- Logs track information for debugging
- Logs all errors with full details

### 5. **Error State UI**
- Shows black background on error (instead of crashing)
- Maintains overlay for content readability
- Logs error messages for debugging

## 🔍 How to Debug

### Check Logcat

Filter by tag: `BackgroundVideoPlayer`

**Success indicators:**
```
BackgroundVideoPlayer: Loading video: https://...
BackgroundVideoPlayer: Is HLS stream: true
BackgroundVideoPlayer: Creating HLS MediaSource
BackgroundVideoPlayer: Video player initialized successfully
BackgroundVideoPlayer: Playback state: BUFFERING
BackgroundVideoPlayer: Playback state: READY
BackgroundVideoPlayer: Is playing: true
BackgroundVideoPlayer: Tracks changed. Groups: 2
```

**Error indicators:**
```
BackgroundVideoPlayer: Playback error: ... (errorCode: ...)
BackgroundVideoPlayer: Error type: ...
BackgroundVideoPlayer: Error cause: ...
```

### Common Error Codes

- **ERROR_CODE_IO_NETWORK_CONNECTION_FAILED (2002)**: Network issue
- **ERROR_CODE_IO_BAD_HTTP_STATUS (2003)**: HTTP error (404, 403, etc.)
- **ERROR_CODE_PARSING_CONTAINER_MALFORMED (3001)**: Invalid m3u8 format
- **ERROR_CODE_PARSING_MANIFEST_MALFORMED (3002)**: Malformed playlist

### Test the URL

1. **Check URL in browser:**
   ```
   https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_master.m3u8?alt=media
   ```
   Should download the m3u8 file

2. **Check individual playlist:**
   ```
   https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_1280x720.m3u8?alt=media
   ```
   Should download the individual playlist

3. **Check MP4 file:**
   ```
   https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/videos%2Flanding%2Flanding_video_1280x720.mp4?alt=media
   ```
   Should start downloading the video

## 🐛 Troubleshooting

### Video Not Loading

**Check:**
1. ✅ URL is correct in Firestore
2. ✅ Files are uploaded to Firebase Storage
3. ✅ Storage rules allow public read
4. ✅ Network connectivity
5. ✅ Logcat for specific error messages

**Fix Storage Rules:**
```javascript
match /videos/landing/{allPaths=**} {
  allow read: if true;  // Public read
}
```

### HLS Stream Not Detected

**Symptoms:**
- Log shows "Is HLS stream: false" for m3u8 URL
- Video doesn't play

**Fix:**
- The code automatically detects `.m3u8` in URL
- Ensure URL contains "m3u8" (case-insensitive)

### Network Errors

**Symptoms:**
- ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
- ERROR_CODE_IO_BAD_HTTP_STATUS

**Fixes:**
1. Check internet connection
2. Verify Firebase Storage URLs are accessible
3. Check if files are publicly readable
4. Verify CORS settings (if applicable)

### Playback Errors

**Symptoms:**
- ERROR_CODE_PARSING_CONTAINER_MALFORMED
- ERROR_CODE_PARSING_MANIFEST_MALFORMED

**Fixes:**
1. Verify m3u8 files are valid
2. Check URLs in master playlist are correct
3. Ensure all referenced files exist
4. Re-generate playlists if needed

## 📊 Expected Behavior

### Successful Playback Flow

1. **Initialization:**
   ```
   Loading video → Is HLS stream: true → Creating HLS MediaSource → Initialized
   ```

2. **Loading:**
   ```
   Playback state: IDLE → BUFFERING → READY
   ```

3. **Playing:**
   ```
   Is playing: true → Tracks changed → Video visible
   ```

4. **Looping:**
   ```
   Playback state: ENDED → (automatically restarts) → READY
   ```

## 🧪 Testing Checklist

- [ ] Video URL is set in Firestore
- [ ] All files uploaded to Firebase Storage
- [ ] Storage rules allow public read
- [ ] App runs without crashes
- [ ] Logcat shows "Loading video"
- [ ] Logcat shows "Is HLS stream: true"
- [ ] Logcat shows "Playback state: READY"
- [ ] Logcat shows "Is playing: true"
- [ ] Video is visible on screen
- [ ] Video loops continuously
- [ ] No error messages in logcat

## 🔧 Code Changes Summary

1. **Added HLS support:** Explicit `HlsMediaSource` for m3u8 streams
2. **Enhanced error handling:** Comprehensive logging and error states
3. **Better DataSource:** Using `DefaultDataSource.Factory`
4. **Improved logging:** Detailed debug information
5. **Error UI:** Graceful error handling with black background

## 📝 Next Steps

1. **Run the app** and check Logcat
2. **Navigate to BuyCreditsScreen**
3. **Look for log messages** starting with "BackgroundVideoPlayer"
4. **Check for errors** - they'll be clearly logged
5. **Verify video plays** - should see "Is playing: true"

If errors persist, check the specific error code and message in Logcat for targeted fixes.

