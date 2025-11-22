# Seed Landing Page Data to Firebase

This guide shows you how to upload the landing page configuration to Firebase using the functions script.

## Prerequisites

1. **Service Account Key**: Ensure `functions/service-account-key.json` exists
2. **Landing Page Config**: Ensure `landingPageConfig.json` exists in the `genai-android` directory
3. **Node.js**: Version 22 (as specified in package.json)

## Quick Start

### Step 1: Update the Config File

Edit `landingPageConfig.json` in the `genai-android` directory:

1. **Update the video URL**: Replace `"https://your-video-url.com/video.mp4"` with your actual video URL
2. **Update product IDs**: Ensure the `productId` values match your Google Play Console product IDs:
   - `"weekly_60_credits"`
   - `"weekly_100_credits"`
   - `"weekly_150_credits"`
3. **Customize features**: Edit the features array if needed
4. **Customize pricing**: Update prices if needed

### Step 2: Run the Script

From the `functions` directory, run:

```bash
cd functions
npm run seed:landing-page
```

This will:
1. Build the TypeScript code
2. Read `landingPageConfig.json`
3. Connect to Firebase using `service-account-key.json`
4. Upload the data to `app/landingPage` document
5. Verify the upload was successful

## Expected Output

```
📖 Reading landing page config from: /path/to/landingPageConfig.json

✅ Config loaded successfully:
   - Background video: https://your-video-url.com/video.mp4
   - Features: 6
   - Subscription plans: 3
   - Testimonials: 0

🔐 Initializing Firebase Admin...
✅ Firebase Admin initialized

📤 Uploading landing page config to Firestore...
   Collection: app
   Document: landingPage

✅ Successfully uploaded landing page config!

✅ Verification successful:
   - Document exists: ✅
   - Features count: 6
   - Plans count: 3

🎉 Landing page is ready! Your app will now use this configuration.

✅ Done!
```

## Troubleshooting

### Error: Service account key not found
- Ensure `functions/service-account-key.json` exists
- Download it from Firebase Console → Project Settings → Service Accounts

### Error: Landing page config not found
- Ensure `landingPageConfig.json` exists in the `genai-android` directory (parent of `functions`)
- The script looks for it at: `genai-android/landingPageConfig.json`

### Error: Invalid config
- Check that `features` and `subscriptionPlans` are arrays
- Ensure all required fields are present

### Error: Firebase initialization failed
- Check that `service-account-key.json` is valid
- Ensure the service account has Firestore write permissions

## Manual Alternative

If you prefer to upload manually:

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Navigate to Firestore Database
3. Create collection `app` (if it doesn't exist)
4. Create document with ID `landingPage`
5. Copy the JSON from `landingPageConfig.json` and paste it into the document

## Updating the Config

To update the landing page:

1. Edit `landingPageConfig.json`
2. Run `npm run seed:landing-page` again
3. The app will automatically pick up the changes (it observes the document in real-time)

## Notes

- The script uses `set()` with `merge: false`, so it will replace the entire document
- The app observes the `app/landingPage` document, so changes appear in real-time
- No app restart is needed after updating the config

