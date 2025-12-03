# Quick Fix for Both Errors

## 🔴 Error 1: "Product details not available"

**Problem:** Subscription products don't exist in Google Play Console.

**Quick Fix:**
1. Go to [Google Play Console](https://play.google.com/console) → Your app
2. **Monetize** → **Products** → **Subscriptions** → **Create subscription**
3. Create 3 subscriptions with **exact** IDs:
   - `weekly_60_credits` ($9.99/week)
   - `weekly_100_credits` ($14.99/week) 
   - `weekly_150_credits` ($19.99/week)
4. Upload app to **Internal testing** track
5. Rebuild and test

**Note:** Products MUST exist in Play Console before the app can use them!

---

## 🔴 Error 2: "Configuration error. Please ensure SHA-1 fingerprint is added"

**Problem:** SHA-1 fingerprint not in Firebase Console.

**Quick Fix:**

### Step 1: Get SHA-1
```bash
cd genai-android
./get-sha1.sh
# Or manually:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

### Step 2: Add to Firebase
1. [Firebase Console](https://console.firebase.google.com) → Project: `genaivideogenerator`
2. **Settings** → **Project settings** → **Your apps**
3. Find Android app → **Add fingerprint**
4. Paste SHA-1 → **Save**

### Step 3: Download New Config
1. Still in Firebase Console → **Your apps**
2. Click **"google-services.json"** → Download
3. Replace `app/google-services.json` with new file

### Step 4: Enable Google Sign-In
1. Firebase Console → **Authentication** → **Sign-in method**
2. Enable **Google** → Add support email → **Save**

### Step 5: Rebuild
```bash
cd genai-android
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ After Fixing

### Test Subscriptions:
1. Open app → Buy Credits
2. Plans should load (no error)
3. Tap Continue → Billing dialog appears

### Test Google Sign-In:
1. Open app → Profile tab
2. Tap "Sign in with Google"
3. No SHA-1 error
4. Google account picker appears

---

## 🚀 Quick Commands

```bash
# Get SHA-1
./get-sha1.sh

# Rebuild app
./gradlew clean assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "Billing|Auth|GoogleSignIn"
```

---

**See `FIX_ERRORS_GUIDE.md` for detailed instructions!**








