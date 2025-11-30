# Deploy Account Deletion Website - Complete Guide

This guide will help you deploy the account deletion website to Firebase Hosting and set up the backend function.

## Prerequisites

- Firebase CLI installed: `npm install -g firebase-tools`
- Firebase project created
- You're logged into Firebase: `firebase login`

## Step 1: Update Project ID

### Update Firebase Config

1. Open `genai-android/firebase.json`
2. Replace `YOUR_FIREBASE_PROJECT_ID` with your actual Firebase project ID

### Update HTML File

1. Open `genai-android/public/delete-account.html`
2. Find the `getProjectId()` function (around line 280)
3. Replace `'YOUR_PROJECT_ID'` with your actual Firebase project ID

**Example:**
```javascript
function getProjectId() {
    // ... existing code ...
    // Replace this line:
    return 'YOUR_PROJECT_ID';
    // With your actual project ID:
    return 'genaivideogenerator'; // or whatever your project ID is
}
```

## Step 2: Deploy Firebase Functions

The account deletion function is already added to `functions/src/index.ts`. Deploy it:

```bash
cd genai-android
firebase deploy --only functions:requestAccountDeletion
```

This will deploy the function that handles deletion requests.

## Step 3: Deploy Firebase Hosting

Deploy the website:

```bash
cd genai-android
firebase deploy --only hosting
```

This will deploy your website to Firebase Hosting.

## Step 4: Get Your Website URL

After deployment, Firebase will give you a URL like:
- `https://YOUR_PROJECT_ID.web.app`
- `https://YOUR_PROJECT_ID.firebaseapp.com`

Your account deletion page will be at:
- `https://YOUR_PROJECT_ID.web.app/delete-account.html`
- Or just `https://YOUR_PROJECT_ID.web.app` (redirects to delete-account.html)

## Step 5: Update Google Play Console

1. Go to Google Play Console → Your App → Data Safety
2. In the "Delete account URL" field, enter:
   ```
   https://YOUR_PROJECT_ID.web.app/delete-account.html
   ```
   (Replace `YOUR_PROJECT_ID` with your actual project ID)

## Step 6: Test the Website

1. Visit your deployed URL
2. Fill out the form with test data
3. Submit the form
4. Check Firestore → `deletion_requests` collection to see if the request was saved

## Viewing Deletion Requests

### In Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Firestore Database**
4. Look for the `deletion_requests` collection
5. You'll see all deletion requests with:
   - `userId`: User's ID from the app
   - `email`: User's email
   - `reason`: Reason for deletion (optional)
   - `status`: "pending"
   - `requestedAt`: Timestamp

### Processing Deletion Requests

To actually delete accounts, you can:

**Option 1: Manual Processing (Recommended for now)**

1. Go to Firestore → `deletion_requests`
2. Find pending requests
3. Copy the `userId`
4. Use Firebase Console or a script to delete:
   - User document: `users/{userId}`
   - User's jobs: `users/{userId}/jobs/*`
   - Firebase Auth user (via Firebase Console → Authentication)

**Option 2: Use the Admin Function**

The `processAccountDeletion` function is available but requires an admin key. You can call it via:

```bash
# Set admin secret (optional, for security)
firebase functions:config:set admin.secret_key="your-secret-key"

# Then call the function (you'll need to create a script or use Firebase Console)
```

## Firestore Rules

The Firestore rules have been updated to allow:
- ✅ Anyone can create deletion requests (for the web form)
- ❌ Only admins can read/update deletion requests (via Functions)

## Troubleshooting

### Function Not Found Error

If you get a "Function not found" error:

1. Make sure you deployed the function:
   ```bash
   firebase deploy --only functions:requestAccountDeletion
   ```

2. Check the function URL matches your project ID:
   ```
   https://asia-south1-YOUR_PROJECT_ID.cloudfunctions.net/requestAccountDeletion
   ```

3. Make sure CORS is enabled (it is in the function code)

### CORS Error

The function already has `cors: true` enabled. If you still get CORS errors:

1. Check that the function is deployed correctly
2. Verify the function URL in the HTML matches your deployment

### Project ID Not Found

If the website can't find your project ID:

1. Update the `getProjectId()` function in `delete-account.html`
2. Hardcode your project ID:
   ```javascript
   function getProjectId() {
       return 'your-actual-project-id';
   }
   ```

### Firestore Permission Denied

If you get permission errors:

1. Make sure Firestore rules are deployed:
   ```bash
   firebase deploy --only firestore:rules
   ```

2. Check that the rules allow creating documents in `deletion_requests`

## Security Notes

1. **Admin Function**: The `processAccountDeletion` function requires an admin key. Keep this secure.

2. **Rate Limiting**: Consider adding rate limiting to prevent abuse (not implemented yet).

3. **Email Verification**: You might want to add email verification before processing deletions (optional).

4. **Data Retention**: Remember that some data (like purchase records) may need to be retained for legal/tax purposes.

## Next Steps

1. ✅ Deploy the website
2. ✅ Add URL to Google Play Console
3. ✅ Test the form
4. ⏳ Set up a process to regularly check and process deletion requests
5. ⏳ (Optional) Add email notifications when requests are received
6. ⏳ (Optional) Add automated account deletion after X days

## Quick Deploy Commands

```bash
# Deploy everything at once
cd genai-android
firebase deploy

# Or deploy separately
firebase deploy --only functions:requestAccountDeletion
firebase deploy --only hosting
firebase deploy --only firestore:rules
```

## Your Website URLs

After deployment, you'll have:

- **Main page**: `https://YOUR_PROJECT_ID.web.app`
- **Delete account**: `https://YOUR_PROJECT_ID.web.app/delete-account.html`
- **Alternative**: `https://YOUR_PROJECT_ID.firebaseapp.com/delete-account.html`

Both URLs work - use whichever you prefer for Google Play Console!

---

**Remember**: Replace `YOUR_PROJECT_ID` with your actual Firebase project ID throughout this guide!






