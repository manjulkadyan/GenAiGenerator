# M3U8 Video Download and Trim Guide

This guide explains how to download the m3u8 video from Twitter/X, trim the watermark frames, and use it in your landing page.

## Your Questions Answered

### 1. Should I download all resolutions?

**Answer: It depends on your needs:**

- **Option A: Download all resolutions** (use `download_and_trim_m3u8.py`)
  - Useful if you want to serve different qualities based on user's connection
  - Creates 3 files: 1280x720, 640x360, 480x270
  - Recommended for production apps with adaptive streaming

- **Option B: Download best quality only** (use `download_m3u8_simple.py`)
  - Simpler and faster
  - Downloads the highest quality (1280x720) automatically
  - Recommended for landing page background video

**For a landing page background video, Option B (single best quality) is usually sufficient.**

### 2. How to trim the video to a specific duration?

Both scripts support two trimming methods:

**Method 1: Exact Duration (Recommended)**
- Set `TARGET_DURATION = 63` (for 1 minute 3 seconds)
- Video will be trimmed from 0:00 to the target duration
- Currently configured to trim to **1 minute 3 seconds (63 seconds)**

**Method 2: Frame-based Trimming**
- Set `FRAMES_TO_TRIM = 15` and `TARGET_DURATION = None`
- Detects the video FPS automatically
- Calculates the duration to trim (15 frames ÷ FPS)
- Removes that duration from the end of the video

Both methods use `-c copy` for fast processing (no re-encoding).

## Quick Start

### Prerequisites

Install ffmpeg:
```bash
# macOS
brew install ffmpeg

# Linux
sudo apt-get install ffmpeg

# Windows
# Download from https://ffmpeg.org/download.html
```

### Option 1: Download Best Quality Only (Recommended)

```bash
python3 download_m3u8_simple.py
```

This will:
1. Download the best quality (1280x720) from the m3u8 playlist
2. Trim the last 15 frames
3. Save as `downloaded_videos/landing_video.mp4`

### Option 2: Download All Resolutions

```bash
python3 download_and_trim_m3u8.py
```

This will:
1. Download all 3 resolutions separately
2. Trim the last 15 frames from each
3. Save as:
   - `downloaded_videos/landing_video_1280x720.mp4`
   - `downloaded_videos/landing_video_640x360.mp4`
   - `downloaded_videos/landing_video_480x270.mp4`

## Configuration

You can adjust settings in the scripts:

```python
# Trim to exact duration (recommended)
TARGET_DURATION = 63  # 1 minute 3 seconds (in seconds)
FRAMES_TO_TRIM = None  # Set to None to use TARGET_DURATION

# Or trim by frames
TARGET_DURATION = None  # Set to None to use frame-based trimming
FRAMES_TO_TRIM = 15  # Number of frames to trim from end
```

**Current configuration:** Trims video to exactly **1 minute 3 seconds (63 seconds)** from the start.

## Upload to Firebase Storage

After downloading, upload the video to Firebase Storage:

### Using Firebase Console:
1. Go to Firebase Console → Storage
2. Create folder: `videos/landing/`
3. Upload your `landing_video.mp4`
4. Get the download URL

### Using Firebase CLI:
```bash
firebase storage:upload downloaded_videos/landing_video.mp4 videos/landing/
```

### Using gsutil:
```bash
gsutil cp downloaded_videos/landing_video.mp4 gs://your-bucket/videos/landing/
```

## Update Landing Page Config

Once uploaded, update your Firebase Firestore document:

**Collection:** `app`  
**Document:** `landingPage`

Update the `backgroundVideoUrl` field:
```json
{
  "backgroundVideoUrl": "https://firebasestorage.googleapis.com/v0/b/your-project.appspot.com/o/videos%2Flanding%2Flanding_video.mp4?alt=media",
  ...
}
```

Or if you're using the local config file:
```json
{
  "backgroundVideoUrl": "https://your-cdn.com/videos/landing_video.mp4",
  ...
}
```

## Video Format Support

Your `BackgroundVideoPlayer` component uses ExoPlayer, which supports:
- ✅ MP4 (recommended for landing page)
- ✅ M3U8/HLS (can use directly, but MP4 is more reliable)
- ✅ WebM
- ✅ Other formats supported by ExoPlayer

**Recommendation:** Convert to MP4 for better compatibility and caching.

## Troubleshooting

### ffmpeg not found
```bash
# Check if installed
ffmpeg -version

# Install if missing (macOS)
brew install ffmpeg
```

### Download fails
- Check internet connection
- Verify the m3u8 URL is still valid (Twitter URLs may expire)
- Try downloading individual resolution URLs from the m3u8 file

### Trim doesn't work
- The script will skip trimming if video is too short
- Check the output messages for warnings
- You can manually trim using:
  ```bash
  ffmpeg -i input.mp4 -t 10 -c copy output.mp4  # Keep first 10 seconds
  ```

### Video quality issues
- Use the 1280x720 resolution for best quality
- If file size is too large, you can re-encode:
  ```bash
  ffmpeg -i input.mp4 -crf 23 -preset medium output.mp4
  ```

## Advanced: Multiple Resolutions

If you want to serve different qualities based on connection:

1. Download all resolutions using `download_and_trim_m3u8.py`
2. Upload all to Firebase Storage
3. Create an adaptive streaming setup (requires backend logic)
4. Or simply use the best quality - modern devices handle it well

For a landing page background video, **one high-quality MP4 is usually sufficient**.

## Notes

- Twitter/X m3u8 URLs may expire after some time
- The watermark is typically in the last 10-15 frames
- Adjust `FRAMES_TO_TRIM` if you need to trim more/less
- The scripts use `-c copy` for fast processing (no re-encoding)
- Final MP4 files are ready for web/mobile playback

