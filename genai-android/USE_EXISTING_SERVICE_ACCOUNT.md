# Using Your Existing Service Account

## ✅ Good News!

You already have a service account JSON file:
- **Location**: `genai-android/functions/service-account-key.json`
- **Project**: `genaivideogenerator`
- **Email**: `firebase-adminsdk-fbsvc@genaivideogenerator.iam.gserviceaccount.com`

## 🔒 Security Update

I've updated `.gitignore` to prevent accidentally committing this file. **Never commit service account keys to git!**

## 🚀 How to Use It

### Option 1: Manual Deployment (Simplest - No GitHub)

You don't need the service account for manual deployment. Just use:

```bash
cd genai-android
firebase login
firebase deploy --only hosting
```

The service account is only needed for:
- GitHub Actions (automatic deployment)
- Server-side operations
- CI/CD pipelines

### Option 2: Use with GitHub Actions (If You Want Auto-Deploy)

If you want to set up GitHub Actions for automatic deployment:

1. **Copy the service account JSON content:**
   ```bash
   cat genai-android/functions/service-account-key.json
   ```

2. **Add to GitHub Secrets:**
   - Go to your GitHub repo: `GenAiGenerator`
   - Settings → Secrets and variables → Actions
   - New repository secret
   - Name: `FIREBASE_SERVICE_ACCOUNT`
   - Value: Paste the entire JSON content
   - Add secret

3. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Add account deletion website"
   git push origin main
   ```

   The GitHub Actions workflow will automatically deploy!

## ⚠️ Important Security Notes

1. **Never commit this file to git** - It's now in `.gitignore`
2. **Don't share this file** - It has full access to your Firebase project
3. **If exposed, regenerate it** - Go to Firebase Console → Project Settings → Service Accounts → Delete and create new

## ✅ For Simple Static Website

Since you just want a static website, you **don't need** the service account at all. Just run:

```bash
cd genai-android
firebase deploy --only hosting
```

That's it! The service account is only needed if you want GitHub Actions to automatically deploy.

## 📋 Summary

- ✅ You have the service account file
- ✅ It's now protected in `.gitignore`
- ✅ For simple deployment: Just use `firebase deploy --only hosting`
- ⚠️ Service account only needed for GitHub Actions (optional)

---

**Recommendation**: For a simple static website, just use manual deployment. No need for GitHub Actions or the service account!







