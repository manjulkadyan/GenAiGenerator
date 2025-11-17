# Firebase Authentication Setup for Seeding Scripts

## Problem
The seeding script fails with:
```
Could not load the default credentials. Browse to https://cloud.google.com/docs/authentication/getting-started
```

## Solution Options

### Option 1: Use Service Account (Recommended for Scripts)

1. **Download Service Account Key:**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select your project
   - Go to **Project Settings** → **Service Accounts**
   - Click **"Generate new private key"**
   - Save the JSON file (e.g., `service-account.json`)

2. **Set Environment Variable:**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ```

3. **Run Script:**
   ```bash
   cd genai-android/functions
   export REPLICATE_API_TOKEN=r8_your_token
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   npm run seed:all
   ```

### Option 2: Use gcloud CLI

1. **Install gcloud CLI** (if not installed):
   ```bash
   # macOS
   brew install google-cloud-sdk
   ```

2. **Authenticate:**
   ```bash
   gcloud auth application-default login
   ```

3. **Run Script:**
   ```bash
   cd genai-android/functions
   export REPLICATE_API_TOKEN=r8_your_token
   npm run seed:all
   ```

### Option 3: Use Firebase Emulator (Local Testing)

1. **Start Emulator:**
   ```bash
   cd genai-android
   firebase emulators:start --only firestore
   ```

2. **Set Emulator Environment:**
   ```bash
   export FIRESTORE_EMULATOR_HOST=localhost:8080
   ```

3. **Run Script:**
   ```bash
   cd genai-android/functions
   export REPLICATE_API_TOKEN=r8_your_token
   npm run seed:all
   ```

---

## Quick Fix (Easiest)

**For local development, use gcloud:**

```bash
# One-time setup
gcloud auth application-default login

# Then run script
cd genai-android/functions
export REPLICATE_API_TOKEN=r8_your_token
npm run seed:all
```

---

## Why This Happens

Firebase Admin SDK needs credentials to access Firestore. When running locally (not on GCP), you need to provide credentials explicitly.

The script now has better error messages to guide you! 🎉

