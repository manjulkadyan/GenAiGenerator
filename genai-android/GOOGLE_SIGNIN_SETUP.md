# Google Sign-In Setup Guide

## Fixing Error Code 10 (DEVELOPER_ERROR)

Error code 10 typically occurs when the SHA-1 fingerprint is not configured in Firebase Console. Follow these steps:

### Step 1: Get Your SHA-1 Fingerprint

#### For Debug Build:
```bash
cd genai-android
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### For Release Build:
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

Look for the **SHA-1** value in the output (it looks like: `AA:BB:CC:DD:EE:FF:...`)

### Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: **genaivideogenerator**
3. Go to **Project Settings** (gear icon) → **Your apps**
4. Find your Android app: `com.manjul.genai.videogenerator`
5. Click **Add fingerprint**
6. Paste your SHA-1 fingerprint
7. Click **Save**

### Step 3: Get the Web Client ID

1. In Firebase Console, go to **Project Settings** → **General**
2. Scroll down to **Your apps** section
3. Find the **Web app** (or create one if it doesn't exist)
4. Copy the **Web client ID** (OAuth 2.0 Client ID)
5. Update the `webClientId` in `ProfileScreen.kt` if different from the current one

### Step 4: Enable Google Sign-In

1. Go to **Authentication** → **Sign-in method**
2. Click on **Google**
3. Enable it and save
4. Make sure the **Web client ID** matches the one you're using in the app

### Step 5: Verify Configuration

The current web client ID in the code is:
```
407437371864-9dkicne9lg7l8l816jbut5dup9qs7sus.apps.googleusercontent.com
```

If this doesn't match your Firebase Console, update it in:
- File: `app/src/main/java/com/manjul/genai/videogenerator/ui/screens/ProfileScreen.kt`
- Line: ~396 (in `startGoogleSignIn` function)

### Common Issues

1. **Error 10 (DEVELOPER_ERROR)**: SHA-1 not added or wrong client ID
2. **Error 7 (NETWORK_ERROR)**: Check internet connection
3. **Error 8 (INTERNAL_ERROR)**: Try again later
4. **Error 12500**: User cancelled sign-in

### Testing

After adding SHA-1:
1. Wait a few minutes for Firebase to propagate changes
2. Rebuild the app
3. Try Google Sign-In again

### Notes

- Debug and Release builds need separate SHA-1 fingerprints
- SHA-1 must match the keystore used to sign the APK
- Changes in Firebase Console may take a few minutes to propagate

