# Connect GitHub to Firebase Hosting - Complete Guide

## ⚠️ Important: Firebase Hosting vs App Hosting

You're currently looking at **Firebase App Hosting** (for Node.js apps), but we created **static HTML files**. We need to use **Firebase Hosting** instead.

**Firebase Hosting** = Static files (HTML, CSS, JS) ✅ What we need
**Firebase App Hosting** = Node.js apps (Express, etc.) ❌ Not what we need

## Option 1: Use Firebase Hosting with GitHub Actions (Recommended)

This will automatically deploy your website whenever you push to GitHub.

### Step 1: Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: `genaivideogenerator`
3. Go to **Project Settings** (gear icon) → **Service accounts**
4. Click **Generate new private key**
5. Download the JSON file (keep it secure!)

### Step 2: Add Secret to GitHub

1. Go to your GitHub repository: `GenAiGenerator`
2. Go to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `FIREBASE_SERVICE_ACCOUNT`
5. Value: Paste the **entire contents** of the JSON file you downloaded
6. Click **Add secret**

### Step 3: Push to GitHub

The GitHub Actions workflow is already created (`.github/workflows/firebase-hosting.yml`). Just push your code:

```bash
cd genai-android
git add .
git commit -m "Add account deletion website"
git push origin main
```

The workflow will automatically:
- ✅ Deploy to Firebase Hosting
- ✅ Run on every push to `main` branch
- ✅ Only when files in `public/` folder change

### Step 4: Verify Deployment

After pushing, check:
1. GitHub Actions tab → You should see the workflow running
2. Firebase Console → Hosting → Your site should be live
3. Visit: `https://genaivideogenerator.web.app`

## Option 2: Manual Deployment (No GitHub Integration)

If you prefer to deploy manually:

```bash
cd genai-android
firebase deploy --only hosting
```

## Option 3: Use Firebase App Hosting (If You Want Node.js)

If you want to use Firebase App Hosting instead, we'd need to convert the static HTML to a Node.js Express app. This is more complex but gives you more features.

**Would you like to:**
- ✅ Use Firebase Hosting (static files) - **Recommended, easier**
- ⚠️ Convert to Node.js app for App Hosting - More complex

## Current Setup

Your current setup uses **Firebase Hosting** (static files):
- ✅ `public/delete-account.html` - Static HTML
- ✅ `public/index.html` - Static HTML
- ✅ `firebase.json` - Configured for hosting
- ✅ GitHub Actions workflow - Auto-deploy on push

## Quick Start (Recommended)

1. **Get Firebase Service Account Key** (see Step 1 above)
2. **Add to GitHub Secrets** (see Step 2 above)
3. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Add account deletion website"
   git push origin main
   ```
4. **Wait for deployment** (check GitHub Actions tab)
5. **Visit your site:** `https://genaivideogenerator.web.app`

## Troubleshooting

### GitHub Actions Fails

**Error: "FIREBASE_SERVICE_ACCOUNT secret not found"**
- Make sure you added the secret in GitHub Settings → Secrets
- Secret name must be exactly: `FIREBASE_SERVICE_ACCOUNT`

**Error: "Permission denied"**
- Make sure the service account JSON is valid
- Check that the service account has Hosting Admin permissions

### Deployment Not Triggering

- Make sure you're pushing to the `main` branch
- Check that files in `public/` folder are being changed
- Verify the workflow file is in `.github/workflows/`

### Website Not Updating

- Check GitHub Actions tab for deployment status
- Wait a few minutes for Firebase to propagate changes
- Clear browser cache and try again

## Manual Deployment (Alternative)

If GitHub Actions doesn't work, you can always deploy manually:

```bash
cd genai-android
firebase login
firebase use genaivideogenerator
firebase deploy --only hosting
```

## Your Website URLs

After deployment (either method):

- **Main page**: `https://genaivideogenerator.web.app`
- **Delete account**: `https://genaivideogenerator.web.app/delete-account.html`
- **Alternative**: `https://genaivideogenerator.firebaseapp.com/delete-account.html`

## Next Steps

1. ✅ Set up GitHub Actions (follow steps above)
2. ✅ Push to GitHub
3. ✅ Verify deployment
4. ✅ Add URL to Google Play Console:
   ```
   https://genaivideogenerator.web.app/delete-account.html
   ```

---

**Note:** Firebase App Hosting is different from Firebase Hosting. Since we have static HTML files, we use Firebase Hosting with GitHub Actions for automatic deployment.











