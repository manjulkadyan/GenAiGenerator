# Quick Test Setup - Google Sign-In & Subscriptions

## ✅ APK Built Successfully!

Your release APK is ready:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Quick Start Testing

### 1. Install APK on Device

```bash
# Connect your Android device via USB
adb devices  # Verify device is connected

# Install the APK
adb install app/build/outputs/apk/release/app-release.apk

# Or if you need to reinstall (replace existing)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 2. Test Google Sign-In

#### Prerequisites:
1. **Get SHA-1 Fingerprint:**
   ```bash
   # For debug keystore (if testing with debug build)
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   
   # For release keystore (if you created one)
   keytool -list -v -keystore ~/genai-video-keystore.jks -alias genai-video-key | grep SHA1
   ```

2. **Add SHA-1 to Firebase:**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Project: `genaivideogenerator`
   - Settings (gear icon) → Project settings → Your apps
   - Find your Android app
   - Click "Add fingerprint"
   - Paste SHA-1 from step 1
   - **Download new `google-services.json`**
   - **Replace** `app/google-services.json` with the new one
   - **Rebuild the app** (if you changed google-services.json)

3. **Enable Google Sign-In:**
   - Firebase Console → Authentication → Sign-in method
   - Enable **Google** provider
   - Add support email
   - Save

#### Test Steps:
1. Open the app
2. Go to **Profile** tab
3. You should see "Sign in with Google" card (if anonymous user)
4. Tap "Sign in with Google"
5. Select your Google account
6. ✅ Profile should update with your Google account info
7. ✅ "Logout" button should appear

---

### 3. Test Google Subscriptions

#### Prerequisites:
1. **Create Subscription Products in Play Console:**
   - Go to [Google Play Console](https://play.google.com/console)
   - Your app → **Monetize** → **Products** → **Subscriptions**
   - Create 3 subscriptions:
     - Product ID: `weekly_60_credits` - Price: $9.99/week
     - Product ID: `weekly_100_credits` - Price: $14.99/week
     - Product ID: `weekly_150_credits` - Price: $19.99/week
   - All set to **Weekly** billing period

2. **Upload App to Internal Testing:**
   - Play Console → **Internal testing** track
   - Create new release
   - Upload your APK: `app/build/outputs/apk/release/app-release.apk`
   - Or upload AAB: `app/build/outputs/bundle/release/app-release.aab` (recommended)
   - Add release notes
   - Save → Review → Start rollout

3. **Add License Tester:**
   - Play Console → **Settings** → **License testing**
   - Add your Google account email
   - Test purchases will be free for this account

#### Test Steps:
1. **Install app from Internal testing track** (or use the APK you built)
2. Open the app
3. Tap **"Buy Credits"** button (or go to Profile → Buy Credits)
4. You should see 3 subscription plans
5. Select a plan (one should be marked "Popular")
6. Tap **"Continue"**
7. ✅ Google Play billing dialog should appear
8. ✅ Complete the purchase (will be free if you're a license tester)
9. ✅ Success message: "Subscription purchased successfully!"
10. ✅ Credits should be added to your account

---

## 🔍 Debugging

### View Logs
```bash
# Filter for billing and auth logs
adb logcat | grep -E "BillingRepository|LandingPageViewModel|ProfileScreen|AuthManager|GoogleSignIn"

# Or view all logs
adb logcat
```

### Common Issues

#### Google Sign-In Not Working:
- ❌ SHA-1 not added to Firebase
- ❌ `google-services.json` not updated
- ❌ Google Sign-In not enabled in Firebase Console
- ✅ **Fix**: Add SHA-1, download new google-services.json, rebuild

#### Subscriptions Not Working:
- ❌ Products not created in Play Console
- ❌ Product IDs don't match (case-sensitive!)
- ❌ App not uploaded to any track
- ❌ Billing not initialized
- ✅ **Fix**: Create products with exact IDs, upload app to Internal testing

#### "Product details not available":
- Check product IDs match exactly: `weekly_60_credits`, `weekly_100_credits`, `weekly_150_credits`
- Verify products exist in Play Console
- Check logs for billing initialization

---

## 📱 Quick Commands

### Build APK
```bash
cd genai-android
./gradlew assembleRelease
```

### Build AAB (for Play Store)
```bash
cd genai-android
./gradlew bundleRelease
```

### Install APK
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### View Logs
```bash
adb logcat | grep -E "Billing|Auth|GoogleSignIn"
```

### Get SHA-1
```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Release keystore (if created)
keytool -list -v -keystore ~/genai-video-keystore.jks -alias genai-video-key | grep SHA1
```

---

## ✅ Testing Checklist

### Google Sign-In
- [ ] SHA-1 added to Firebase Console
- [ ] `google-services.json` updated
- [ ] Google Sign-In enabled in Firebase
- [ ] App rebuilt (if google-services.json changed)
- [ ] Anonymous user can sign in with Google
- [ ] Profile shows Google account info
- [ ] Logout works
- [ ] Sign in again works

### Google Subscriptions
- [ ] Subscription products created in Play Console
- [ ] Product IDs match exactly
- [ ] App uploaded to Internal testing
- [ ] License tester account added
- [ ] Billing initializes (check logs)
- [ ] Product details load
- [ ] Plans display correctly
- [ ] "Continue" button launches billing
- [ ] Purchase completes
- [ ] Success message appears
- [ ] Credits added to account

---

## 🎯 Expected Behavior

### Google Sign-In Flow:
1. App opens → Anonymous account created automatically
2. Profile tab → Shows "Sign in with Google" card
3. Tap button → Google account picker appears
4. Select account → Account linked
5. Profile updates → Shows Google name, email, "Logout" button

### Subscription Flow:
1. Tap "Buy Credits" → BuyCreditsScreen opens
2. Select plan → Plan highlighted
3. Tap "Continue" → Button shows "Processing..."
4. Google Play dialog → Complete purchase
5. Success → "Subscription purchased successfully!"
6. Credits added → Can generate videos

---

**Your APK is ready!** Install it and start testing! 🚀












