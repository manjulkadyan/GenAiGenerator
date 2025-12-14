# Testing Guide - Google Subscriptions & Google Sign-In

## 🧪 Testing Setup

### Prerequisites
1. **Release APK built** (we'll build it now)
2. **Test Google Account** (for subscriptions)
3. **SHA-1 Fingerprint** added to Firebase Console
4. **Google Sign-In enabled** in Firebase Console

---

## Part 1: Build Release APK

### Step 1: Update Gradle Version
✅ Already done - Updated to Gradle 8.7

### Step 2: Build APK
```bash
cd genai-android
./gradlew assembleRelease
```

The APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

### Step 3: Install on Device
```bash
# Connect your Android device via USB
adb install app/build/outputs/apk/release/app-release.apk

# Or transfer the APK to your device and install manually
```

---

## Part 2: Test Google Sign-In

### Prerequisites for Google Sign-In

1. **Enable Google Sign-In in Firebase:**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select project: `genaivideogenerator`
   - Go to **Authentication** → **Sign-in method**
   - Enable **Google** sign-in provider
   - Add support email
   - Save

2. **Get SHA-1 Fingerprint:**
   ```bash
   # For debug keystore (testing)
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For release keystore (if you created one)
   keytool -list -v -keystore ~/genai-video-keystore.jks -alias genai-video-key
   ```

3. **Add SHA-1 to Firebase:**
   - Firebase Console → Project Settings → Your apps
   - Find your Android app
   - Click "Add fingerprint"
   - Paste SHA-1 from step 2
   - Download new `google-services.json` and replace the old one

4. **Get Web Client ID:**
   - Firebase Console → Project Settings → General
   - Scroll to "Your apps"
   - Find "Web client ID" (OAuth 2.0 Client ID)
   - Copy it (should be: `407437371864-9dkicne9lg7l8l816jbut5dup9qs7sus.apps.googleusercontent.com`)

### Testing Google Sign-In

1. **Open the app** on your device
2. **Go to Profile tab**
3. **If you're anonymous user**, you'll see "Sign in with Google" card
4. **Tap "Sign in with Google"**
5. **Select your Google account**
6. **Verify:**
   - ✅ Profile shows your Google account name and email
   - ✅ User ID is displayed
   - ✅ "Logout" button appears (replaces "Sign in with Google")
   - ✅ No more anonymous user status

### Troubleshooting Google Sign-In

**Error: "Configuration error. Please ensure SHA-1 fingerprint is added"**
- ✅ Add SHA-1 fingerprint to Firebase Console
- ✅ Download new `google-services.json`
- ✅ Rebuild the app

**Error: "Sign-in was cancelled"**
- User cancelled - this is normal

**Error: "Network error"**
- Check internet connection
- Verify Firebase project is active

**Error: "Account linking failed"**
- Try signing out first, then sign in fresh
- Check Firebase Console → Authentication for errors

---

## Part 3: Test Google Subscriptions

### Prerequisites for Subscriptions

1. **Create Subscription Products in Play Console:**
   - Go to [Google Play Console](https://play.google.com/console)
   - Select your app
   - Go to **Monetize** → **Products** → **Subscriptions**
   - Create subscriptions with IDs:
     - `weekly_60_credits`
     - `weekly_100_credits`
     - `weekly_150_credits`
   - Set all to **Weekly** billing
   - Set prices: $9.99, $14.99, $19.99

2. **Upload App to Internal Testing:**
   - Go to Play Console → **Internal testing** track
   - Upload your APK/AAB
   - Add testers (your Google account)

3. **Add License Testers:**
   - Play Console → **Settings** → **License testing**
   - Add your Google account email as a test account
   - Test purchases will be free for test accounts

### Testing Subscriptions

1. **Open the app** on your device
2. **Navigate to Buy Credits screen:**
   - Tap "Buy Credits" button
   - Or go to Profile → "Buy Credits"
3. **Select a subscription plan:**
   - You should see 3 plans (60, 100, 150 credits)
   - One should be marked as "Popular"
4. **Tap "Continue"**
5. **Google Play billing dialog should appear:**
   - If you're a license tester, it will say "Test purchase"
   - Complete the purchase flow
6. **Verify:**
   - ✅ Success message appears
   - ✅ Subscription is active
   - ✅ Credits are added to your account
   - ✅ Purchase appears in Play Console

### Troubleshooting Subscriptions

**Error: "Product details not available"**
- ❌ Product IDs don't exist in Play Console
- ❌ Product IDs don't match exactly (case-sensitive)
- ✅ Solution: Create products in Play Console with exact IDs

**Error: "Billing service unavailable"**
- ❌ Google Play Services not installed/updated
- ❌ Device not connected to internet
- ❌ Testing on emulator without Google Play Services
- ✅ Solution: Use a real device with Google Play Services

**Error: "Item already owned"**
- ✅ You already have an active subscription
- ✅ Solution: Cancel the test subscription in Play Console or wait for it to expire

**No billing dialog appears:**
- ❌ Billing not initialized
- ❌ Product details not loaded
- ✅ Check logs: Look for "BillingRepository" or "LandingPageViewModel" logs
- ✅ Verify billing is initialized: Check `uiState.billingInitialized` is true

---

## Part 4: Complete Testing Checklist

### Google Sign-In Testing
- [ ] SHA-1 fingerprint added to Firebase
- [ ] Google Sign-In enabled in Firebase Console
- [ ] `google-services.json` updated
- [ ] App rebuilt with new config
- [ ] Anonymous user can sign in with Google
- [ ] Profile shows Google account info
- [ ] Logout works correctly
- [ ] Signing in again works

### Google Subscriptions Testing
- [ ] Subscription products created in Play Console
- [ ] Product IDs match exactly (case-sensitive)
- [ ] App uploaded to Internal testing track
- [ ] License tester account added
- [ ] Billing client initializes successfully
- [ ] Product details load correctly
- [ ] Subscription plans display correctly
- [ ] "Continue" button launches billing flow
- [ ] Purchase completes successfully
- [ ] Success message appears
- [ ] Credits are added to account
- [ ] Purchase appears in Play Console

---

## Quick Test Commands

### Build Release APK
```bash
cd genai-android
./gradlew assembleRelease
```

### Install APK
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Check Logs (for debugging)
```bash
adb logcat | grep -E "BillingRepository|LandingPageViewModel|ProfileScreen|AuthManager"
```

### Get SHA-1 Fingerprint
```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Release keystore (if you created one)
keytool -list -v -keystore ~/genai-video-keystore.jks -alias genai-video-key | grep SHA1
```

---

## Expected Behavior

### Google Sign-In Flow
1. User opens app → Anonymous account created
2. User goes to Profile → Sees "Sign in with Google" card
3. User taps "Sign in with Google" → Google account picker appears
4. User selects account → Account linked/signed in
5. Profile updates → Shows Google account info, "Logout" button appears

### Subscription Flow
1. User taps "Buy Credits" → BuyCreditsScreen opens
2. User selects a plan → Plan highlighted
3. User taps "Continue" → Button shows "Processing..."
4. Google Play billing dialog → User completes purchase
5. Success message → "Subscription purchased successfully!"
6. Credits added → User can now generate videos

---

## Need Help?

- **Google Sign-In Issues**: Check Firebase Console → Authentication → Users
- **Subscription Issues**: Check Play Console → Monetize → Products → Subscriptions
- **Logs**: Use `adb logcat` to see detailed error messages
- **Firebase Console**: Check for any error messages or warnings

---

**Ready to test!** Build the APK and follow the steps above. 🚀












