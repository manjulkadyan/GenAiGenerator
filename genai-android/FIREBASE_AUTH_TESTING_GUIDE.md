# Firebase Authentication Testing Guide - Debug & Release Builds

## 🔐 Overview

Firebase Authentication requires SHA-1 fingerprints to be added to Firebase Console for both **debug** and **release** builds. This guide shows you how to set up and test Firebase Auth on both build types.

---

## 📋 Prerequisites

- Firebase project created
- Google Sign-In enabled in Firebase Console
- Android app registered in Firebase Console
- Keystore file for release builds (if testing release)

---

## 🔑 Step 1: Get SHA-1 Fingerprints

### For Debug Build (Testing)

```bash
cd genai-android
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Or use the helper script:**
```bash
cd genai-android
./get-sha1.sh
# Choose option 1 for debug
```

Look for the **SHA-1** line in the output:
```
SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE
```

### For Release Build (Production)

```bash
cd genai-android
keytool -list -v -keystore /path/to/your/release.keystore -alias your-key-alias
```

**Or use the helper script:**
```bash
cd genai-android
./get-sha1.sh
# Choose option 2 for release
# Enter keystore path, alias, and password when prompted
```

**Note:** If you don't have a release keystore yet, see `KEYSTORE_SETUP_GUIDE.md` to create one.

---

## 🔧 Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: **genaivideogenerator**
3. Click the **gear icon** → **Project Settings**
4. Scroll down to **Your apps** section
5. Find your Android app: `com.manjul.genai.videogenerator`
6. Click **Add fingerprint** (you can add multiple)
7. Paste your **Debug SHA-1** fingerprint
8. Click **Save**
9. Repeat steps 6-8 for **Release SHA-1** (if you have one)

**Important:** You can add multiple SHA-1 fingerprints to the same app. Add both debug and release SHA-1s.

---

## 📥 Step 3: Download Updated google-services.json

After adding SHA-1 fingerprints:

1. In Firebase Console → **Project Settings** → **Your apps**
2. Find your Android app
3. Click **Download google-services.json**
4. Replace the file in your project:
   ```bash
   # Copy the downloaded file to:
   genai-android/app/google-services.json
   ```

---

## ✅ Step 4: Verify Web Client ID

1. In Firebase Console → **Project Settings** → **General**
2. Scroll to **Your apps** section
3. Find the **Web app** (or create one if it doesn't exist)
4. Copy the **Web client ID** (OAuth 2.0 Client ID)
5. Verify it matches in `ProfileScreen.kt`:

```kotlin
// File: app/src/main/java/com/manjul/genai/videogenerator/ui/screens/ProfileScreen.kt
// Line: ~425
val webClientId = "407437371864-9dkicne9lg7l8l816jbut5dup9qs7sus.apps.googleusercontent.com"
```

If different, update the `webClientId` in `ProfileScreen.kt`.

---

## 🧪 Step 5: Test Debug Build

### Build Debug APK

```bash
cd genai-android
./gradlew assembleDebug
```

### Install and Test

```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or install via Android Studio
# Run → Run 'app'
```

### Test Google Sign-In

1. Open the app
2. Go to **Profile** tab
3. If you're anonymous, you'll see **"Sign in with Google"** card
4. Tap **"Sign in with Google"**
5. Select your Google account
6. ✅ Should sign in successfully

### Check Logs

```bash
# Filter for authentication logs
adb logcat | grep -E "AuthManager|ProfileScreen|GoogleSignIn"
```

**Expected logs:**
```
AuthManager: Google sign-in success: {uid}
ProfileScreen: Google sign-in/linking successful
```

---

## 🚀 Step 6: Test Release Build

### Build Release APK/AAB

**Option A: Build Release APK**
```bash
cd genai-android
./gradlew assembleRelease
```

**Option B: Build Release AAB (Recommended)**
```bash
cd genai-android
./gradlew bundleRelease
```

**Note:** Make sure `keystore.properties` is configured. See `KEYSTORE_SETUP_GUIDE.md`.

### Install Release Build

```bash
# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Or upload AAB to Play Console Internal Testing
# Then install from Internal testing link
```

### Test Google Sign-In

1. Open the app
2. Go to **Profile** tab
3. If you're anonymous, you'll see **"Sign in with Google"** card
4. Tap **"Sign in with Google"**
5. Select your Google account
6. ✅ Should sign in successfully

---

## 🔍 Troubleshooting

### Error: "Configuration error. Please ensure SHA-1 fingerprint is added"

**Cause:** SHA-1 not added to Firebase Console or wrong SHA-1.

**Solution:**
1. Verify SHA-1 matches the keystore used to sign the APK
2. Check SHA-1 is added in Firebase Console
3. Wait 5-10 minutes after adding SHA-1 (Firebase needs time to propagate)
4. Rebuild the app after adding SHA-1
5. Download new `google-services.json` after adding SHA-1

### Error: "This Google account is already linked to another account"

**Cause:** Trying to sign in with a Google account that's already linked to a different Firebase user.

**Solution:**
- This is expected behavior - the account is already linked
- The app now handles this gracefully (shows success if already signed in)

### Error: "Account linking failed"

**Cause:** Network issue or Firebase Auth error.

**Solution:**
1. Check internet connection
2. Check Firebase Console → Authentication → Sign-in method → Google is enabled
3. Verify Web client ID matches Firebase Console
4. Check logs for detailed error: `adb logcat | grep AuthManager`

### Google Sign-In Dialog Doesn't Appear

**Cause:** SHA-1 not configured or wrong Web client ID.

**Solution:**
1. Verify SHA-1 is added to Firebase Console
2. Verify Web client ID in `ProfileScreen.kt` matches Firebase Console
3. Rebuild the app
4. Clear app data and try again

### Works in Debug but Not in Release

**Cause:** Release SHA-1 not added to Firebase Console.

**Solution:**
1. Get release SHA-1 from your release keystore
2. Add it to Firebase Console (can add multiple SHA-1s)
3. Download new `google-services.json`
4. Rebuild release APK
5. Test again

---

## 📝 Quick Reference

### Get SHA-1 Commands

**Debug:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

**Release:**
```bash
keytool -list -v -keystore /path/to/keystore.jks -alias your-alias | grep SHA1
```

### Check Current SHA-1s in Firebase

1. Firebase Console → Project Settings → Your apps
2. Find Android app → See "SHA certificate fingerprints" section

### Verify google-services.json

Check that `google-services.json` contains your package name:
```json
{
  "project_info": {
    "project_id": "genaivideogenerator"
  },
  "client": [
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.manjul.genai.videogenerator"
        }
      }
    }
  ]
}
```

---

## ✅ Testing Checklist

### Debug Build
- [ ] Debug SHA-1 added to Firebase Console
- [ ] `google-services.json` downloaded and updated
- [ ] Web client ID verified in code
- [ ] Google Sign-In enabled in Firebase Console
- [ ] Debug APK built and installed
- [ ] Google Sign-In works in debug build
- [ ] Logs show successful authentication

### Release Build
- [ ] Release SHA-1 added to Firebase Console
- [ ] `google-services.json` includes release SHA-1
- [ ] Release keystore configured
- [ ] Release APK/AAB built
- [ ] Release build installed (or from Play Console)
- [ ] Google Sign-In works in release build
- [ ] Logs show successful authentication

---

## 🔄 Common Workflow

1. **Add SHA-1 to Firebase Console**
2. **Wait 5-10 minutes** (for Firebase to propagate)
3. **Download new google-services.json**
4. **Replace** `app/google-services.json`
5. **Rebuild** the app
6. **Test** Google Sign-In
7. **Check logs** if issues occur

---

## 📚 Related Documentation

- `GOOGLE_SIGNIN_SETUP.md` - Initial Google Sign-In setup
- `KEYSTORE_SETUP_GUIDE.md` - How to create release keystore
- `TESTING_GUIDE.md` - Complete testing guide
- `FIX_ERRORS_GUIDE.md` - Common errors and fixes

---

## 🆘 Still Having Issues?

1. **Check logs:**
   ```bash
   adb logcat | grep -E "AuthManager|ProfileScreen|GoogleSignIn|FirebaseAuth"
   ```

2. **Verify in Firebase Console:**
   - Authentication → Sign-in method → Google is **Enabled**
   - Project Settings → Your apps → SHA-1 fingerprints are added
   - Project Settings → General → Web client ID matches code

3. **Clear app data:**
   ```bash
   adb shell pm clear com.manjul.genai.videogenerator
   ```

4. **Rebuild from scratch:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug  # or assembleRelease
   ```

---

## 💡 Pro Tips

1. **Add both SHA-1s at once** - You can add multiple SHA-1 fingerprints to the same app in Firebase Console
2. **Use helper script** - `./get-sha1.sh` makes it easier to get SHA-1
3. **Wait for propagation** - Firebase changes can take 5-10 minutes to propagate
4. **Check logs first** - Most issues are visible in logcat
5. **Test on real device** - Emulators may have issues with Google Sign-In





