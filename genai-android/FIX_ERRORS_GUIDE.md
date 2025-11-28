# Fix Errors Guide - Subscriptions & Google Sign-In

## 🔴 Error 1: "Product details not available"

### Cause
The subscription products don't exist in Google Play Console, or the app isn't uploaded to any track.

### Solution

#### Step 1: Create Subscription Products in Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to **Monetize** → **Products** → **Subscriptions**
4. Click **Create subscription**

**Create 3 subscriptions:**

**Subscription 1:**
- Product ID: `weekly_60_credits` (must match exactly!)
- Name: "60 Credits Weekly"
- Billing period: **Weekly**
- Price: $9.99

**Subscription 2:**
- Product ID: `weekly_100_credits` (must match exactly!)
- Name: "100 Credits Weekly"  
- Billing period: **Weekly**
- Price: $14.99
- Mark as **Popular** (optional)

**Subscription 3:**
- Product ID: `weekly_150_credits` (must match exactly!)
- Name: "150 Credits Weekly"
- Billing period: **Weekly**
- Price: $19.99

#### Step 2: Upload App to Internal Testing

1. Play Console → **Internal testing** track
2. **Create new release**
3. Upload your APK or AAB:
   - APK: `app/build/outputs/apk/debug/app-debug.apk` (for testing)
   - Or AAB: `app/build/outputs/bundle/release/app-release.aab` (recommended)
4. Add release notes
5. **Save** → **Review release** → **Start rollout**

#### Step 3: Add License Tester (Optional - for free test purchases)

1. Play Console → **Settings** → **License testing**
2. Add your Google account email
3. Test purchases will be free

#### Step 4: Rebuild and Test

```bash
cd genai-android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Important:** Products must be created BEFORE the app can query them. The app queries products from Play Console, so they must exist there first.

---

## 🔴 Error 2: "Configuration error. Please ensure SHA-1 fingerprint is added"

### Cause
SHA-1 fingerprint not added to Firebase Console, so Google Sign-In can't verify your app.

### Solution

#### Step 1: Get SHA-1 Fingerprint

**For Debug Build (what you're using now):**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

**Copy the SHA-1 value** (it looks like: `AA:BB:CC:DD:EE:FF:...`)

#### Step 2: Add SHA-1 to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project: `genaivideogenerator`
3. Click **Settings** (gear icon) → **Project settings**
4. Scroll down to **Your apps** section
5. Find your Android app (package: `com.manjul.genai.videogenerator`)
6. Click **"Add fingerprint"** button
7. Paste your SHA-1 fingerprint
8. Click **Save**

#### Step 3: Download New google-services.json

1. Still in Firebase Console → Project settings
2. In **Your apps** section, find your Android app
3. Click **"google-services.json"** button (or download icon)
4. **Download the file**
5. **Replace** `genai-android/app/google-services.json` with the new one

#### Step 4: Rebuild the App

```bash
cd genai-android
./gradlew clean
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Step 5: Enable Google Sign-In in Firebase

1. Firebase Console → **Authentication** → **Sign-in method**
2. Find **Google** in the list
3. Click on it → **Enable**
4. Add a support email
5. Click **Save**

---

## ✅ Quick Fix Commands

### Get SHA-1 (Debug)
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

### Rebuild App
```bash
cd genai-android
./gradlew clean assembleDebug
```

### Install APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### View Logs (to debug)
```bash
adb logcat | grep -E "BillingRepository|LandingPageViewModel|ProfileScreen|AuthManager|GoogleSignIn"
```

---

## 📋 Checklist

### For Subscriptions:
- [ ] Subscription products created in Play Console
- [ ] Product IDs match exactly: `weekly_60_credits`, `weekly_100_credits`, `weekly_150_credits`
- [ ] All set to **Weekly** billing period
- [ ] App uploaded to **Internal testing** track
- [ ] License tester added (optional)
- [ ] App rebuilt and installed

### For Google Sign-In:
- [ ] SHA-1 fingerprint obtained
- [ ] SHA-1 added to Firebase Console
- [ ] New `google-services.json` downloaded
- [ ] `google-services.json` replaced in project
- [ ] Google Sign-In enabled in Firebase Console
- [ ] App rebuilt and installed

---

## 🔍 Verify Setup

### Check Subscriptions:
1. Open app → Buy Credits screen
2. Check logs: `adb logcat | grep BillingRepository`
3. Should see: "Loaded X product details"
4. Plans should be clickable

### Check Google Sign-In:
1. Open app → Profile tab
2. Check logs: `adb logcat | grep GoogleSignIn`
3. Should NOT see SHA-1 error
4. "Sign in with Google" button should work

---

## 🆘 Still Having Issues?

### Subscriptions Still Not Working:
- ✅ Verify products exist: Play Console → Products → Subscriptions
- ✅ Check product IDs match exactly (case-sensitive!)
- ✅ Make sure app is on Internal testing track
- ✅ Wait a few minutes after creating products (they need to propagate)
- ✅ Check logs for specific error messages

### Google Sign-In Still Not Working:
- ✅ Verify SHA-1 is correct (copy entire value)
- ✅ Make sure new google-services.json is in the project
- ✅ Rebuild the app after replacing google-services.json
- ✅ Check Firebase Console → Authentication → Sign-in method → Google is enabled
- ✅ Try uninstalling and reinstalling the app

---

**After fixing both issues, rebuild and test again!** 🚀





