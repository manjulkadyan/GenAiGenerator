# Google Play Data Safety Section - Complete Guide

## ⚠️ IMPORTANT: This is REQUIRED

You **MUST** complete the Data Safety section before your app can be published on Google Play. This is mandatory for all apps.

---

## Step 1: Data Collection and Security

### Question 1: Does your app collect or share any of the required user data types?

**Answer: YES**

Your app collects:
- User account information (email, display name from Google Sign-In)
- App activity (video generation history, credits usage)
- Device or other IDs (Firebase Auth UID)
- Purchase history (Google Play Billing subscriptions)

### Question 2: Is all of the user data collected by your app encrypted in transit?

**Answer: YES**

✅ All data is encrypted in transit because:
- Firebase uses HTTPS/TLS for all connections
- Google Play Billing uses encrypted connections
- All API calls to Replicate use HTTPS

### Question 3: Which methods of account creation does your app support?

**Select:**
- ✅ **OAuth** (Google Sign-In)
- ✅ **My app does not allow users to create an account** (for anonymous users)

**Note:** You support both:
- Anonymous authentication (no account creation)
- Google Sign-In (OAuth)

---

## Step 2: Delete Account URL

### Question: Add a link that users can use to request account deletion

**Answer: YES** (You should provide this)

**Delete Account URL:** You need to create a page or use one of these options:

**Option 1: Create a simple web page**
Create a page on your website or use a service like:
- Your website: `https://yourwebsite.com/delete-account`
- Firebase Hosting: Create a simple HTML page
- Use the same flycricket.io hosting if possible

**Option 2: Use Firebase Functions**
Create a Firebase Function that handles account deletion requests:

```typescript
// functions/src/deleteAccount.ts
export const requestAccountDeletion = onCall(async (request) => {
  const { userId, email } = request.data;
  
  // Store deletion request in Firestore
  await firestore.collection("deletion_requests").add({
    userId,
    email,
    requestedAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "pending"
  });
  
  return { success: true, message: "Deletion request received" };
});
```

**Option 3: Use email**
If you don't have a website, you can use:
- URL: `mailto:your-email@example.com?subject=Account%20Deletion%20Request&body=Please%20delete%20my%20account%20with%20user%20ID:%20[USER_ID]`

**Recommended URL format:**
```
https://yourwebsite.com/delete-account
```

**What the page should include:**
1. Your app/developer name
2. Clear steps to request account deletion
3. What data will be deleted:
   - User account (Firebase Auth)
   - User credits
   - Video generation history
   - All associated data in Firestore
4. What data might be retained (if any):
   - Analytics data (anonymized)
   - Purchase records (required by law for tax purposes)

**Example page content:**
```html
<h1>Request Account Deletion - GenAI Video Generator</h1>
<p>To request deletion of your account and associated data:</p>
<ol>
  <li>Open the app and go to Profile screen</li>
  <li>Copy your User ID</li>
  <li>Email us at support@yourapp.com with your User ID</li>
  <li>We will delete your account within 30 days</li>
</ol>
<p><strong>Data that will be deleted:</strong></p>
<ul>
  <li>Your user account</li>
  <li>All video generation history</li>
  <li>Your credits balance</li>
  <li>All personal data stored in our database</li>
</ul>
<p><strong>Data that may be retained:</strong></p>
<ul>
  <li>Anonymized analytics data</li>
  <li>Purchase records (required for tax compliance)</li>
</ul>
```

### Question: Do you provide a way for users to request partial data deletion?

**Answer: NO** (unless you implement this feature)

For now, users can only delete their entire account. If you want to add partial deletion later, you can update this.

---

## Step 3: Data Types (What You Collect)

You need to declare each type of data your app collects. Here's what to declare:

### 1. Personal Info

#### Email address
- **Collected:** ✅ Yes
- **Shared:** ❌ No
- **Purpose:** Account management, user communication
- **Required or optional:** Optional (only for Google Sign-In users)
- **Ephemeral processing:** No

#### Name
- **Collected:** ✅ Yes (display name from Google Sign-In)
- **Shared:** ❌ No
- **Purpose:** Account personalization
- **Required or optional:** Optional
- **Ephemeral processing:** No

### 2. Financial Info

#### Purchase history
- **Collected:** ✅ Yes (via Google Play Billing)
- **Shared:** ❌ No (Google Play handles this)
- **Purpose:** Subscription management, purchase verification
- **Required or optional:** Required for subscription features
- **Ephemeral processing:** No

### 3. App Activity

#### App interactions
- **Collected:** ✅ Yes (video generation, model selection)
- **Shared:** ❌ No
- **Purpose:** App functionality, history tracking
- **Required or optional:** Required for core functionality
- **Ephemeral processing:** No

#### Other user-generated content
- **Collected:** ✅ Yes (video generation prompts, generated videos)
- **Shared:** ❌ No
- **Purpose:** Video generation service
- **Required or optional:** Required for core functionality
- **Ephemeral processing:** No

### 4. Device or Other IDs

#### User ID
- **Collected:** ✅ Yes (Firebase Auth UID)
- **Shared:** ❌ No
- **Purpose:** User identification, account management
- **Required or optional:** Required
- **Ephemeral processing:** No

#### Device ID
- **Collected:** ✅ Yes (for Firebase Analytics, if enabled)
- **Shared:** ❌ No
- **Purpose:** Analytics, crash reporting
- **Required or optional:** Optional
- **Ephemeral processing:** No

---

## Step 4: Data Usage and Handling

For each data type, specify:

### Data Collection
- **Why:** App functionality, account management, analytics
- **How:** Collected directly from user (email, name), automatically (device ID), or through third-party services (Google Play Billing)

### Data Sharing
- **With whom:** Generally NO, except:
  - Google Play (for purchase verification - automatic)
  - Firebase/Google (for authentication and storage - automatic)
  - Replicate API (for video generation - automatic, but no personal data shared)

### Data Security
- **Encryption in transit:** ✅ Yes (HTTPS/TLS)
- **Encryption at rest:** ✅ Yes (Firebase encrypts data at rest)

### User Controls
- **Can users request deletion?** ✅ Yes (via delete account URL)
- **Can users export data?** ❌ No (unless you implement this)

---

## Step 5: Additional Badges (Optional)

### Independent Security Review
- **Answer:** NO (unless you've had a security audit)

### UPI Payments Verified
- **Answer:** NO (only for finance apps in India using UPI)

---

## Quick Checklist

Before submitting, make sure you have:

- [ ] Answered "Yes" to data collection
- [ ] Answered "Yes" to encryption in transit
- [ ] Selected account creation methods (OAuth + No account creation)
- [ ] Created and added delete account URL
- [ ] Declared all data types you collect:
  - [ ] Email address
  - [ ] Name
  - [ ] Purchase history
  - [ ] App interactions
  - [ ] User-generated content
  - [ ] User ID
  - [ ] Device ID (if using Analytics)
- [ ] Specified data is NOT shared (except with Google/Firebase automatically)
- [ ] Specified encryption in transit: YES
- [ ] Previewed your data safety section

---

## Example Delete Account Implementation

### Option 1: Simple Firebase Function

```typescript
// functions/src/index.ts
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const deleteUserAccount = functions.https.onCall(async (data, context) => {
  // Verify user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  }

  const userId = context.auth.uid;

  try {
    // Delete user data from Firestore
    const userRef = admin.firestore().collection("users").doc(userId);
    const jobsRef = userRef.collection("jobs");
    
    // Delete all jobs
    const jobsSnapshot = await jobsRef.get();
    const batch = admin.firestore().batch();
    jobsSnapshot.docs.forEach((doc) => {
      batch.delete(doc.ref);
    });
    await batch.commit();
    
    // Delete user document
    await userRef.delete();
    
    // Delete Firebase Auth user
    await admin.auth().deleteUser(userId);
    
    return { success: true, message: "Account deleted successfully" };
  } catch (error) {
    console.error("Error deleting account:", error);
    throw new functions.https.HttpsError("internal", "Failed to delete account");
  }
});
```

### Option 2: Simple HTML Page

Create `public/delete-account.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Delete Account - GenAI Video Generator</title>
</head>
<body>
    <h1>Request Account Deletion</h1>
    <p>To delete your account and all associated data:</p>
    <ol>
        <li>Open the GenAI Video Generator app</li>
        <li>Go to Profile screen</li>
        <li>Copy your User ID</li>
        <li>Email us at: <a href="mailto:support@yourapp.com">support@yourapp.com</a></li>
        <li>Include your User ID in the email</li>
        <li>We will process your request within 30 days</li>
    </ol>
    
    <h2>What will be deleted:</h2>
    <ul>
        <li>Your user account</li>
        <li>All video generation history</li>
        <li>Your credits balance</li>
        <li>All personal data</li>
    </ul>
    
    <h2>What may be retained:</h2>
    <ul>
        <li>Anonymized analytics data</li>
        <li>Purchase records (for tax compliance)</li>
    </ul>
</body>
</html>
```

Then host it on Firebase Hosting or your website.

---

## Important Notes

1. **Be accurate:** Only declare data you actually collect
2. **Update when needed:** If you add new data collection, update this section
3. **Delete account URL is required:** You must provide a way for users to delete their accounts
4. **Review before publishing:** Preview your data safety section before submitting

---

## Need Help?

- [Google Play Data Safety Help](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Data Safety Form Guide](https://support.google.com/googleplay/android-developer/answer/10787469)

---

**Remember:** This is a legal requirement. Make sure all information is accurate and up-to-date!











