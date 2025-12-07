# Account Deletion Website - Setup Summary

## ✅ What Was Created

I've created a complete account deletion system for your app:

### 1. Website Files
- **`public/index.html`** - Landing page that redirects to deletion form
- **`public/delete-account.html`** - Beautiful, responsive account deletion form

### 2. Backend Function
- **`functions/src/index.ts`** - Added two functions:
  - `requestAccountDeletion` - Handles form submissions, saves to Firestore
  - `processAccountDeletion` - Admin function to actually delete accounts

### 3. Configuration
- **`firebase.json`** - Updated with Firebase Hosting configuration
- **`firestore.rules`** - Updated to allow deletion requests

### 4. Documentation
- **`DEPLOY_ACCOUNT_DELETION_GUIDE.md`** - Complete deployment guide
- **`QUICK_DEPLOY.sh`** - One-command deployment script

## 🚀 Quick Start

### Option 1: Use the Quick Deploy Script (Easiest)

```bash
cd genai-android
./QUICK_DEPLOY.sh
```

This will:
1. Deploy Firestore rules
2. Deploy the deletion function
3. Deploy the website

### Option 2: Manual Deployment

```bash
cd genai-android

# Deploy Firestore rules
firebase deploy --only firestore:rules

# Deploy the function
firebase deploy --only functions:requestAccountDeletion

# Deploy the website
firebase deploy --only hosting
```

## 📍 Your Website URLs

After deployment, your website will be available at:

- **Main page**: `https://genaivideogenerator.web.app`
- **Delete account**: `https://genaivideogenerator.web.app/delete-account.html`
- **Alternative**: `https://genaivideogenerator.firebaseapp.com/delete-account.html`

## 📝 Add to Google Play Console

1. Go to Google Play Console → Your App → Data Safety
2. In "Delete account URL", enter:
   ```
   https://genaivideogenerator.web.app/delete-account.html
   ```

## 📊 Viewing Deletion Requests

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: `genaivideogenerator`
3. Go to **Firestore Database**
4. Look for the `deletion_requests` collection
5. Each request contains:
   - `userId` - User's ID from the app
   - `email` - User's email
   - `reason` - Optional reason for deletion
   - `status` - "pending" (until you process it)
   - `requestedAt` - Timestamp

## 🔧 Processing Deletion Requests

### Manual Processing (Recommended for now)

1. Go to Firestore → `deletion_requests`
2. Find a pending request
3. Copy the `userId`
4. Delete the user data:
   - **Firestore**: Delete `users/{userId}` and `users/{userId}/jobs/*`
   - **Firebase Auth**: Go to Authentication → Delete user
5. Update the request status to "completed"

### Automated Processing (Future)

You can use the `processAccountDeletion` function, but it requires:
- Admin authentication
- Setting up an admin key

See `DEPLOY_ACCOUNT_DELETION_GUIDE.md` for details.

## ✨ Features

### Website Features
- ✅ Beautiful, responsive design
- ✅ Mobile-friendly
- ✅ Form validation
- ✅ Success/error messages
- ✅ Loading states
- ✅ Clear instructions for users

### Backend Features
- ✅ Saves requests to Firestore
- ✅ Validates input (email format, required fields)
- ✅ CORS enabled for web access
- ✅ Secure (only allows POST requests)
- ✅ Logs all requests

## 🔒 Security

- ✅ Firestore rules allow public creation of deletion requests
- ✅ Only admins can read/update requests (via Functions)
- ✅ Function validates all input
- ✅ CORS properly configured

## 📋 Checklist

Before going live:

- [ ] Deploy the website and function
- [ ] Test the form submission
- [ ] Verify requests appear in Firestore
- [ ] Add URL to Google Play Console
- [ ] Set up a process to regularly check and process requests
- [ ] (Optional) Add email notifications for new requests

## 🆘 Troubleshooting

### Function Not Found
- Make sure you deployed: `firebase deploy --only functions:requestAccountDeletion`
- Check the function URL in the HTML matches your project

### CORS Error
- The function has CORS enabled, but verify it's deployed correctly
- Check browser console for specific error messages

### Permission Denied
- Deploy Firestore rules: `firebase deploy --only firestore:rules`
- Make sure rules allow creating documents in `deletion_requests`

## 📚 More Information

See `DEPLOY_ACCOUNT_DELETION_GUIDE.md` for:
- Detailed deployment instructions
- Troubleshooting guide
- Security best practices
- Processing deletion requests

---

**Ready to deploy?** Run `./QUICK_DEPLOY.sh` and you're done! 🚀










