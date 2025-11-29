# 🧪 Testing Upload Message Display

## Current Issue
User reports: "No upload message is seen now"

## What SHOULD Happen

### Scenario 1: Text-to-Video (No frames)
```
1. User clicks "Generate AI Video"
   → Button shows: "Submitting..." immediately
   → No upload messages (no frames to upload)
   → Transitions to GeneratingScreen
```

### Scenario 2: Image-to-Video (With frames)
```
1. User selects first frame
2. User selects last frame  
3. User clicks "Generate AI Video"
   
   → Button IMMEDIATELY changes to: "📤 Uploading first frame..."
   → Button is DISABLED
   → Upload happens (1-3 seconds)
   
   → Button changes to: "📤 Uploading last frame..."
   → Button stays DISABLED
   → Upload happens (1-3 seconds)
   
   → Button changes to: "✅ Frames uploaded • Submitting request..."
   → Transitions to GeneratingScreen
```

## Current Code Logic

### Button Text Determination
```kotlin
val isUploading = state.uploadMessage != null
val buttonText = when {
    isUploading -> state.uploadMessage ?: "Uploading..."
    state.isGenerating || isSubmitting -> "Submitting..."
    else -> "Generate AI Video"
}
```

### Upload Flow in ViewModel
```kotlin
// Step 1: Upload first frame
_state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
uploadReferenceFrame(uri, "first frame")

// Step 2: Upload last frame
_state.update { it.copy(uploadMessage = "📤 Uploading last frame...") }
uploadReferenceFrame(uri, "last frame")

// Step 3: Submit request
_state.update { it.copy(uploadMessage = "✅ Frames uploaded • Submitting request...") }
```

## Possible Reasons for Not Seeing Messages

### 1. ✅ App Not Rebuilt
**Solution:** Rebuild and reinstall the app
```bash
cd genai-android
./gradlew clean
./gradlew installDebug
```

### 2. ❓ Upload Happens Too Fast
If images are cached or network is very fast, uploads complete in <100ms and user might miss the message.

**Verification:** Add a small delay to test:
```kotlin
// TEMPORARY TEST CODE - Remove after testing
_state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
delay(1000) // Force 1 second visibility
val url = uploadReferenceFrame(uri, "first frame")
```

### 3. ❓ State Not Being Observed
The button is inside a composable that should recompose when `state` changes.

**Check:** Does the button text change to "Submitting..." at all?
- If YES → state is being observed, uploadMessage just not set
- If NO → larger composable issue

### 4. ❓ Navigation Happens Too Quick
GeneratingScreen shows immediately and user doesn't see the button change.

**Solution:** Keep button visible longer OR show status in GeneratingScreen (which we already do!)

## Debug Steps

### Step 1: Add Logging
Add to `VideoGenerateViewModel.kt` after each uploadMessage update:

```kotlin
_state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
android.util.Log.d("VideoGenVM", "📤 Set uploadMessage: Uploading first frame")
```

### Step 2: Check Logcat
Run the app and check logcat:
```bash
adb logcat | grep "VideoGenVM"
```

You should see:
```
📤 Set uploadMessage: Uploading first frame
📤 Set uploadMessage: Uploading last frame  
✅ Set uploadMessage: Frames uploaded • Submitting
```

### Step 3: Check Button State
Add logging to GenerateScreen.kt:

```kotlin
val buttonText = when {
    isUploading -> state.uploadMessage ?: "Uploading..."
    state.isGenerating || isSubmitting -> "Submitting..."
    else -> "Generate AI Video"
}.also { 
    android.util.Log.d("GenerateScreen", "Button text: $it, uploadMessage: ${state.uploadMessage}")
}
```

### Step 4: Verify Recomposition
```kotlin
val isUploading = state.uploadMessage != null
LaunchedEffect(state.uploadMessage) {
    android.util.Log.d("GenerateScreen", "Upload message changed: ${state.uploadMessage}")
}
```

## Quick Fix Options

### Option A: Force Message Visibility (Debugging)
In `VideoGenerateViewModel.kt`, add delays:

```kotlin
val firstUrl = snapshot.firstFrameUri?.let { uri ->
    _state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
    delay(500) // Make message visible
    val url = uploadReferenceFrame(uri, "first frame")
    delay(500) // Keep showing after upload
    url ?: return@launch
}
```

### Option B: Show Toast Messages (Alternative)
If button updates aren't visible, show toasts:

```kotlin
_state.update { it.copy(uploadMessage = "📤 Uploading first frame...") }
// Also show toast for immediate feedback
```

### Option C: Keep Button Visible Longer
Delay navigation to GeneratingScreen by 500ms after uploads complete.

## Testing Checklist

- [ ] Clean and rebuild app
- [ ] Install fresh APK
- [ ] Select first frame image
- [ ] Select last frame image
- [ ] Click "Generate AI Video"
- [ ] Observe button text changes
- [ ] Check logcat for upload messages
- [ ] Verify GeneratingScreen shows status

## Expected Behavior Video Flow

```
┌─────────────────────────┐
│ Generate Screen         │
│                         │
│ [Generate AI Video]     │ ← Initial
└─────────────────────────┘
            ↓ Click
┌─────────────────────────┐
│ Generate Screen         │
│                         │
│ [📤 Uploading first...] │ ← Should see this!
└─────────────────────────┘
            ↓ 1-2s
┌─────────────────────────┐
│ Generate Screen         │
│                         │
│ [📤 Uploading last...]  │ ← Should see this!
└─────────────────────────┘
            ↓ 1-2s
┌─────────────────────────┐
│ Generate Screen         │
│                         │
│ [✅ Frames uploaded...] │ ← Should see this!
└─────────────────────────┘
            ↓ Transition
┌─────────────────────────┐
│ Generating Screen       │
│      ✨ ✨ ✨          │
│ "Generating Video..."   │
└─────────────────────────┘
```

---

**Next Step:** Please rebuild the app and test with the scenarios above. Let me know what you observe!
