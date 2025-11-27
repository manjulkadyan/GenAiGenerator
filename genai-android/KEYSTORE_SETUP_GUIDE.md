# Android App Signing Setup Guide

## Why You Need a Keystore

Google Play Console requires all apps to be signed with a keystore before uploading. This ensures:
- App authenticity
- Security
- Ability to update your app later (must use the same keystore)

⚠️ **IMPORTANT**: Keep your keystore file and password safe! If you lose it, you cannot update your app on Google Play.

## Step 1: Generate a Keystore

### Option A: Using Android Studio (Recommended)

1. Open Android Studio
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **Android App Bundle** (recommended) or **APK**
4. Click **Create new...** to create a new keystore
5. Fill in the form:
   - **Key store path**: Choose a location (e.g., `~/genai-video-keystore.jks`)
   - **Password**: Create a strong password (save it!)
   - **Key alias**: `genai-video-key` (or any name you prefer)
   - **Key password**: Can be same as keystore password or different
   - **Validity**: 25 years (recommended)
   - **Certificate information**: Fill in your details
6. Click **OK**
7. **Save the keystore file and passwords in a secure location!**

### Option B: Using Command Line

Open Terminal and run:

```bash
cd ~/Desktop/GenAiVideo/genai-android

keytool -genkey -v -keystore genai-video-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias genai-video-key
```

You'll be prompted for:
- Keystore password (create a strong one)
- Key password (can be same as keystore password)
- Your name, organization, city, state, country code

**Example:**
```
Enter keystore password: [YourPassword123!]
Re-enter new password: [YourPassword123!]
What is your first and last name?
  [Unknown]: Manjul Kadyan
What is the name of your organizational unit?
  [Unknown]: Development
What is the name of your organization?
  [Unknown]: Your Company Name
What is the name of your City or Locality?
  [Unknown]: Your City
What is the name of your State or Province?
  [Unknown]: Your State
What is the two-letter country code for this unit?
  [Unknown]: US
```

## Step 2: Create keystore.properties File

Create a file `keystore.properties` in the `genai-android` folder (same level as `build.gradle.kts`):

```properties
storeFile=../genai-video-keystore.jks
storePassword=YourKeystorePassword
keyAlias=genai-video-key
keyPassword=YourKeyPassword
```

**Important:**
- Replace `YourKeystorePassword` and `YourKeyPassword` with your actual passwords
- Adjust `storeFile` path if your keystore is in a different location
- This file will be automatically ignored by git (already configured)

## Step 3: Configure build.gradle.kts

The `build.gradle.kts` file has been updated to automatically load the keystore configuration. It will:
- Read `keystore.properties` if it exists
- Use the keystore for signing release builds
- Keep debug builds unsigned (for development)

## Step 4: Generate Signed APK/AAB

### Using Android Studio

1. **Build** → **Generate Signed Bundle / APK**
2. Select **Android App Bundle** (recommended for Play Store) or **APK**
3. Select your keystore file
4. Enter passwords
5. Select **release** build variant
6. Click **Finish**
7. The signed file will be in `app/release/`

### Using Command Line

```bash
cd ~/Desktop/GenAiVideo/genai-android

# Generate signed AAB (recommended for Play Store)
./gradlew bundleRelease

# Or generate signed APK
./gradlew assembleRelease
```

The signed files will be in:
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- APK: `app/build/outputs/apk/release/app-release.apk`

## Step 5: Upload to Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to **Production** (or **Internal testing** for testing)
4. Click **Create new release**
5. Upload your `.aab` file (or `.apk` if using APK)
6. Fill in release notes
7. Click **Save** → **Review release** → **Start rollout**

## Security Best Practices

### ✅ DO:
- Store keystore file in a secure location (password manager, encrypted drive)
- Backup keystore file to multiple secure locations
- Use strong passwords (at least 16 characters, mix of letters, numbers, symbols)
- Keep `keystore.properties` out of version control (already in .gitignore)
- Document keystore location and passwords securely

### ❌ DON'T:
- Commit keystore file to git
- Commit `keystore.properties` to git
- Share keystore file or passwords
- Lose the keystore file (you can't update your app without it!)

## Troubleshooting

### "Keystore file not found"
- Check the path in `keystore.properties`
- Use absolute path or relative path from project root
- Make sure the file exists

### "Keystore was tampered with, or password was incorrect"
- Double-check your passwords in `keystore.properties`
- Make sure there are no extra spaces or special characters
- Try regenerating the keystore if you're sure passwords are correct

### "Key alias not found"
- Check the `keyAlias` in `keystore.properties` matches the alias you created
- List aliases: `keytool -list -v -keystore genai-video-keystore.jks`

### "APK/AAB not signed"
- Make sure you're building the `release` variant
- Check that `keystore.properties` exists and is readable
- Verify signing config in `build.gradle.kts`

## Verify Your Signed APK/AAB

Check if your APK is properly signed:

```bash
# For APK
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# For AAB (extract first, then verify)
unzip app/build/outputs/bundle/release/app-release.aab -d temp/
jarsigner -verify -verbose -certs temp/BASE/dex
```

You should see: `jar verified.`

## Alternative: Google Play App Signing (Recommended)

Google Play offers **App Signing by Google Play**:
- Google manages your app signing key
- You upload an upload key (different from app signing key)
- More secure and convenient
- Can recover if you lose your upload key

To enable:
1. Upload your first release with your keystore
2. Google Play will offer to enroll in App Signing
3. You'll create an upload key (can be different from your current keystore)
4. Google manages the app signing key

This is recommended for production apps!

## Quick Reference

**Keystore file location**: `~/genai-video-keystore.jks` (or wherever you saved it)

**Properties file**: `genai-android/keystore.properties`

**Build command**: `./gradlew bundleRelease` (for AAB) or `./gradlew assembleRelease` (for APK)

**Output location**: 
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- APK: `app/build/outputs/apk/release/app-release.apk`

---

**Remember**: Keep your keystore and passwords safe! You'll need them for every app update.



