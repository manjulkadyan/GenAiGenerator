# How to Add Credits to a User

## Method 1: Firebase Console (Manual)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Firestore Database**
4. Navigate to collection: `users`
5. Find or create document with user's UID (from Firebase Auth)
6. Add field:
   - **Field name:** `credits`
   - **Type:** `number`
   - **Value:** `1000`

**Example Document:**
```
Collection: users
Document ID: {user_uid_from_firebase_auth}
Fields:
  credits: 1000 (number)
```

## Method 2: Get User UID from Android App

1. Run the app
2. Check Logcat for:
   ```
   AuthManager: Anonymous sign-in success: {uid}
   ```
3. Copy the UID
4. Use it in Firebase Console as document ID

## Method 3: Firebase CLI

```bash
# Set user ID
USER_ID="your-user-uid-here"

# Add 1000 credits
firebase firestore:set users/$USER_ID '{"credits": 1000}'
```

## Method 4: Firebase Function (Programmatic)

You can create a Firebase Function to add credits:

```typescript
export const addCredits = onCall(async ({data, auth}) => {
  if (!auth) {
    throw new Error("Unauthorized");
  }
  
  const userId = data.userId || auth.uid;
  const credits = data.credits || 0;
  
  const userRef = firestore.collection("users").doc(userId);
  await userRef.set({
    credits: admin.firestore.FieldValue.increment(credits),
  }, {merge: true});
  
  return {success: true, creditsAdded: credits};
});
```

## User Document Structure

```json
{
  "credits": 1000,
  "created_at": "2025-01-17T12:00:00Z"
}
```

## Automatic User Document Creation

The app now automatically creates user documents when:
1. User signs in anonymously
2. Credits are first accessed
3. First video generation is attempted

The Firebase Function also creates the document if it doesn't exist when called.

