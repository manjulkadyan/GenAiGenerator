# Fix: "This version of the application is not configured for billing through Google Play"

## 🔴 Error Message
**"This version of the application is not configured for billing through Google Play. Check the help center for more information."**

## ✅ Solution: Upload App to Google Play Console

Google Play Billing **only works** with apps that are uploaded to Google Play Console. You cannot test billing with a locally installed APK unless it's also uploaded to at least the **Internal Testing** track.

### Step 1: Build Release APK or AAB

#### Option A: Build Release APK (Easier for testing)
```bash
cd genai-android
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release.apk`

#### Option B: Build Release AAB (Recommended for Play Console)
```bash
cd genai-android
./gradlew bundleRelease
```

The AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

**Note:** Make sure you have `keystore.properties` configured for release builds. If not, see `KEYSTORE_SETUP_GUIDE.md`.

### Step 2: Create App in Google Play Console (If Not Already Created)

1. Go to [Google Play Console](https://play.google.com/console)
2. Click **Create app**
3. Fill in:
   - **App name**: Your app name
   - **Default language**: English (or your language)
   - **App or game**: App
   - **Free or paid**: Free
   - **Declarations**: Complete required sections
4. Click **Create app**

### Step 3: Upload to Internal Testing Track

1. In Play Console, go to your app
2. Navigate to **Testing** → **Internal testing**
3. Click **Create new release**
4. Upload your APK or AAB:
   - **APK**: `app/build/outputs/apk/release/app-release.apk`
   - **AAB**: `app/build/outputs/bundle/release/app-release.aab` (recommended)
5. Add **Release notes** (e.g., "Initial release for billing testing")
6. Click **Save**
7. Click **Review release**
8. Click **Start rollout to Internal testing**

### Step 4: Create Subscription Products (If Not Already Created)

1. In Play Console, go to **Monetize** → **Products** → **Subscriptions**
2. Click **Create subscription**

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

### Step 5: Add License Tester (For Free Test Purchases)

1. Go to **Settings** → **License testing**
2. Click **Add license testers**
3. Add your Google account email address
4. Click **Save**

**Note:** Test purchases will be **free** for license testers.

### Step 6: Install App from Internal Testing (Recommended)

1. In Play Console, go to **Testing** → **Internal testing**
2. Click **Testers** tab
3. Add testers (your Google account email)
4. Share the **Internal testing link** with yourself
5. Open the link on your device and install the app

**OR** install your locally built APK (but it must match the uploaded version):

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

**Important:** The version code and package name must match what's uploaded to Play Console.

### Step 7: Verify Package Name and Version

Check your `app/build.gradle.kts`:
- **Package name**: `com.manjul.genai.videogenerator` (must match Play Console)
- **Version code**: Currently `2` (must match uploaded version)
- **Version name**: Currently `0.0.2`

### Step 8: Test Billing

1. Open the app
2. Navigate to **Buy Credits** screen
3. Select a subscription plan
4. Tap **Continue**
5. ✅ Google Play billing dialog should appear
6. ✅ Complete the purchase (free if you're a license tester)
7. ✅ Success message should appear

---

## 🔍 Troubleshooting

### Still Getting "Not configured for billing" Error?

1. **Check version code matches:**
   - Play Console → **Internal testing** → Check version code
   - Your `build.gradle.kts` → Check `versionCode`
   - They must match exactly

2. **Check package name matches:**
   - Play Console → **App bundle explorer** → Check package name
   - Your `build.gradle.kts` → Check `applicationId`
   - They must match exactly

3. **Wait a few minutes:**
   - After uploading, it can take 5-10 minutes for Play Console to process
   - Try again after waiting

4. **Check you're using the uploaded version:**
   - Uninstall the app completely
   - Install from Internal testing link (recommended)
   - OR install the exact APK that was uploaded

5. **Check signing:**
   - If using release build, make sure `keystore.properties` is configured
   - The signing key must match what's in Play Console

### Common Mistakes

❌ **Installing debug APK instead of release:**
- Debug builds won't work with billing
- Use `assembleRelease` or `bundleRelease`

❌ **Version code mismatch:**
- If you uploaded version code 2, your local build must also be version code 2
- Increment version code only when uploading a new version

❌ **Package name mismatch:**
- Package name in Play Console must exactly match `applicationId` in `build.gradle.kts`

❌ **Not waiting for processing:**
- Play Console needs time to process uploads
- Wait 5-10 minutes after uploading

---

## ✅ Verification Checklist

Before testing, verify:

- [ ] App created in Google Play Console
- [ ] App uploaded to **Internal testing** track
- [ ] Release is **active** (not draft)
- [ ] All 3 subscription products created
- [ ] Product IDs match exactly: `weekly_60_credits`, `weekly_100_credits`, `weekly_150_credits`
- [ ] License tester account added
- [ ] Version code matches between Play Console and local build
- [ ] Package name matches between Play Console and local build
- [ ] App installed from Internal testing OR matching APK version

---

## 📝 Quick Commands

```bash
# Build release APK
cd genai-android
./gradlew assembleRelease

# Build release AAB (recommended)
./gradlew bundleRelease

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Check logs for billing
adb logcat | grep -E "BillingRepository|LandingPageViewModel|BuyCreditsScreen"
```

---

## 🆘 Still Not Working?

1. **Check logs:**
   ```bash
   adb logcat | grep -E "BillingRepository|LandingPageViewModel"
   ```

2. **Verify in Play Console:**
   - Go to **Monetize** → **Products** → **Subscriptions**
   - Make sure all 3 products are **Active** (not Draft)

3. **Try creating a new release:**
   - Increment `versionCode` in `build.gradle.kts` (e.g., from 2 to 3)
   - Rebuild and upload new version
   - Install new version

4. **Check Google Play Services:**
   - Make sure Google Play Services is up to date on your device
   - Settings → Apps → Google Play Services → Update

---

## 📚 Related Documentation

- `GOOGLE_PLAY_SUBSCRIPTION_SETUP.md` - Complete subscription setup guide
- `KEYSTORE_SETUP_GUIDE.md` - How to set up signing for release builds
- `TESTING_GUIDE.md` - Complete testing guide
- `FIX_ERRORS_GUIDE.md` - Other common errors and fixes







