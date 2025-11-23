#!/bin/bash
# Quick script to upload videos to Firebase Storage using Firebase CLI
# Make sure you're logged in: firebase login

cd "$(dirname "$0")"

echo "📤 Uploading videos to Firebase Storage..."
echo ""

# Upload MP4 files
echo "Uploading MP4 files..."
firebase storage:upload downloaded_videos/landing_video_1280x720.mp4 videos/landing/ --project genaivideogenerator
firebase storage:upload downloaded_videos/landing_video_640x360.mp4 videos/landing/ --project genaivideogenerator
firebase storage:upload downloaded_videos/landing_video_480x270.mp4 videos/landing/ --project genaivideogenerator

# Upload m3u8 files (if they exist)
if [ -f "downloaded_videos/landing_video_1280x720.m3u8" ]; then
    echo ""
    echo "Uploading m3u8 playlists..."
    firebase storage:upload downloaded_videos/landing_video_1280x720.m3u8 videos/landing/ --project genaivideogenerator
    firebase storage:upload downloaded_videos/landing_video_640x360.m3u8 videos/landing/ --project genaivideogenerator
    firebase storage:upload downloaded_videos/landing_video_480x270.m3u8 videos/landing/ --project genaivideogenerator
    firebase storage:upload downloaded_videos/landing_video_master.m3u8 videos/landing/ --project genaivideogenerator
fi

echo ""
echo "✅ Upload complete!"
echo ""
echo "💡 Get the master playlist URL from Firebase Console:"
echo "   Storage → videos/landing/landing_video_master.m3u8 → Get download URL"

