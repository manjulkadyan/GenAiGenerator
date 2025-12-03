# Build and Test Guide

## ✅ Build Status

**Debug APK**: ✅ Built successfully
**Release APK**: ⚠️ Needs keystore for signing (optional for testing)

---

## 📱 Quick Build Commands

### Build Debug APK (For Testing - No Keystore Needed)
```bash
cd genai-android
./gradlew assembleDebug
```

**APK Location:**
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK (For Production - Needs Keystore)
```bash
cd genai-android
./gradlew assembleRelease
```

**APK Location:**
```
app/build/outputs/apk/release/app-release.apk
```

**Note:** Release APK will be unsigned if you don't have `keystore.properties`. That's fine for testing, but you'll need a keystore for Play Store.

### Build AAB (For Play Store Upload - Recommended)
```bash
cd genai-android
./gradlew bundleRelease
```

**AAB Location:**
```
app/build/outputs/bundle/release/app-release.aab
```

---

## 🧪 Testing Google Sign-In

### Step 1: Get SHA-1 Fingerprint

**For Debug Build (testing):**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

**For Release Build:**
```bash
# If you created a keystore
keytool -list -v -keystore ~/genai-video-keystore.jks -alias genai-video-key | grep SHA1
```

### Step 2: Add SHA-1 to Firebase

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Project: `genaivideogenerator`
3. **Settings** (gear icon) → **Project settings**
4. Scroll to **Your apps** → Find your Android app
5. Click **"Add fingerprint"**
6. Paste SHA-1 from Step 1
7. **Download new `google-services.json`**
8. **Replace** `app/google-services.json` with the new one
9. **Rebuild the app**

### Step 3: Enable Google Sign-In

1. Firebase Console → **Authentication** → **Sign-in method**
2. Enable **Google** provider
3. Add support email
4. Save

### Step 4: Test in App

1. Install APK on device
2. Open app → Go to **Profile** tab
3. If anonymous user, you'll see **"Sign in with Google"** card
4. Tap it → Select Google account
5. ✅ Profile should update with your Google account info

---

## 💳 Testing Google Subscriptions

### Step 1: Create Subscription Products

1. Go to [Google Play Console](https://play.google.com/console)
2. Your app → **Monetize** → **Products** → **Subscriptions**
3. Create 3 subscriptions:

   **Subscription 1:**
   - Product ID: `weekly_60_credits`
   - Name: "60 Credits Weekly"
   - Billing period: **Weekly**
   - Price: $9.99

   **Subscription 2:**
   - Product ID: `weekly_100_credits`
   - Name: "100 Credits Weekly"
   - Billing period: **Weekly**
   - Price: $14.99
   - Mark as **Popular** (optional)

   **Subscription 3:**
   - Product ID: `weekly_150_credits`
   - Name: "150 Credits Weekly"
   - Billing period: **Weekly**
   - Price: $19.99

### Step 2: Upload App to Internal Testing

1. Play Console → **Internal testing** track
2. **Create new release**
3. Upload your APK or AAB:
   - APK: `app/build/outputs/apk/release/app-release.apk`
   - AAB: `app/build/outputs/bundle/release/app-release.aab` (recommended)
4. Add release notes
5. **Save** → **Review release** → **Start rollout**

### Step 3: Add License Tester

1. Play Console → **Settings** → **License testing**
2. Add your Google account email
3. Test purchases will be **free** for this account

### Step 4: Test in App

1. **Install app from Internal testing** (or use your built APK)
2. Open app → Tap **"Buy Credits"**
3. You should see 3 subscription plans
4. Select a plan → Tap **"Continue"**
5. ✅ Google Play billing dialog appears
6. ✅ Complete purchase (free if license tester)
7. ✅ Success message: "Subscription purchased successfully!"
8. ✅ Credits added to account

---

## 🔍 Debugging

### View Logs
```bash
# Filter for billing and auth
adb logcat | grep -E "BillingRepository|LandingPageViewModel|ProfileScreen|AuthManager|GoogleSignIn"

# All logs
adb logcat
```

### Common Issues

#### Google Sign-In Not Working
- ❌ SHA-1 not added → Add to Firebase Console
- ❌ `google-services.json` not updated → Download new one
- ❌ Google Sign-In not enabled → Enable in Firebase Console
- ✅ **Fix**: Add SHA-1, download new google-services.json, rebuild

#### Subscriptions Not Working
- ❌ Products not created → Create in Play Console
- ❌ Product IDs don't match → Must match exactly (case-sensitive)
- ❌ App not uploaded → Upload to Internal testing
- ❌ Billing not initialized → Check logs
- ✅ **Fix**: Create products, upload app, check logs

---

## 📋 Quick Checklist

### Before Testing Google Sign-In:
- [ ] SHA-1 fingerprint added to Firebase
- [ ] `google-services.json` updated
- [ ] Google Sign-In enabled in Firebase
- [ ] App rebuilt

### Before Testing Subscriptions:
- [ ] Subscription products created in Play Console
- [ ] Product IDs match exactly: `weekly_60_credits`, `weekly_100_credits`, `weekly_150_credits`
- [ ] App uploaded to Internal testing track
- [ ] License tester account added

---

## 🚀 Quick Commands

```bash
# Build debug APK (for testing)
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build AAB (for Play Store)
./gradlew bundleRelease

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "Billing|Auth|GoogleSignIn"

# Get SHA-1 (debug)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

---

**Ready to test!** Build the APK, install it, and follow the testing steps above! 🎯







