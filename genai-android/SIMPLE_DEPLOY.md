# Simple Static Website Deployment - No GitHub Needed

## ✅ What You Need

Just deploy the static HTML files directly to Firebase Hosting. No GitHub, no complex setup!

## 🚀 Quick Deploy (3 Commands)

```bash
cd genai-android
firebase login
firebase deploy --only hosting
```

That's it! Your website will be live.

## 📍 Your Website URL

After deployment:
```
https://genaivideogenerator.web.app/delete-account.html
```

## 📝 Step-by-Step

### 1. Make sure you're in the right directory
```bash
cd genai-android
```

### 2. Login to Firebase (if not already)
```bash
firebase login
```
This will open a browser - just login with your Google account.

### 3. Set your project (if needed)
```bash
firebase use genaivideogenerator
```

### 4. Deploy!
```bash
firebase deploy --only hosting
```

## ✅ Done!

Your website is now live at:
- `https://genaivideogenerator.web.app`
- `https://genaivideogenerator.web.app/delete-account.html`

## 🔄 Update the Website Later

Whenever you want to update the website, just run:
```bash
cd genai-android
firebase deploy --only hosting
```

## 📋 Add to Google Play Console

1. Go to Google Play Console → Your App → Data Safety
2. In "Delete account URL", enter:
   ```
   https://genaivideogenerator.web.app/delete-account.html
   ```

## 🎯 That's All!

No GitHub, no complex setup, no CI/CD - just simple static file hosting!

---

**Note:** The GitHub Actions workflow I created earlier is optional. You can delete it if you don't want it. The static files will work perfectly with just `firebase deploy --only hosting`.

