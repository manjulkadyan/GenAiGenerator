# How to Add Your Own Screenshots to Onboarding

This guide explains how to replace the onboarding screenshots with your own app screenshots.

## Quick Overview

The app has **3 onboarding screens**:
1. **Screen 1**: Premium Features (with app logo)
2. **Screen 2**: Create AI Videos
3. **Screen 3**: Video Library (final screen with "Let's Get Started")

## Screenshot Requirements

### Optimal Dimensions
- **Width**: 390px
- **Height**: 844px
- **Aspect Ratio**: iPhone 14/15 Pro dimensions
- **Format**: PNG (recommended) or JPG

### What to Screenshot
1. **Screen 1**: Your app's premium/upgrade/subscription screen
2. **Screen 2**: Your app's main create/generation screen
3. **Screen 3**: Your app's video library/gallery screen

## Step-by-Step Instructions

### Method 1: Using Firebase Storage (Recommended)

1. **Take Screenshots**
   - Open your app on a device or emulator
   - Navigate to each screen and take screenshots
   - Crop/resize to 390x844px if needed

2. **Upload to Firebase Storage**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select your project: `genaivideogenerator`
   - Navigate to **Storage** in the left menu
   - Create/navigate to the `onboarding` folder
   - Upload your 3 screenshots with these names:
     - `premium.jpg` (or `premium.png`)
     - `create.jpg` (or `create.png`)
     - `library.jpg` (or `library.png`)

3. **Get the URLs**
   - Click on each uploaded file
   - Click "Get download URL" or copy the URL
   - The URL will look like: `https://firebasestorage.googleapis.com/v0/b/genaivideogenerator.firebasestorage.app/o/onboarding%2Fpremium.jpg?alt=media`

4. **Update Configuration**
   
   **Option A: Update Local Config** (for testing)
   - Open `genai-android/onboardingConfig.json`
   - Replace the `imageUrl` values with your new URLs:
   ```json
   {
     "pages": [
       {
         "id": "premium",
         "imageUrl": "YOUR_PREMIUM_SCREENSHOT_URL",
         ...
       },
       {
         "id": "create",
         "imageUrl": "YOUR_CREATE_SCREENSHOT_URL",
         ...
       },
       {
         "id": "library",
         "imageUrl": "YOUR_LIBRARY_SCREENSHOT_URL",
         ...
       }
     ]
   }
   ```

   **Option B: Update Firebase Firestore** (for production)
   - Go to Firebase Console > Firestore Database
   - Navigate to collection: `config` → document: `onboarding`
   - Update the `pages` array with your new image URLs

5. **Test**
   - Run your app
   - Clear app data to see onboarding again
   - Verify all 3 screenshots load correctly

### Method 2: Using Local Resources (Alternative)

If you prefer to bundle screenshots in the app:

1. Add your screenshots to `app/src/main/res/drawable/`
   - Name them: `onboarding_1.png`, `onboarding_2.png`, `onboarding_3.png`

2. Modify the code to load from resources instead of URLs
   - This requires code changes in `ScreenshotImage.kt`

## UI Changes Made

### 1. App Logo on First Screen ✅
- The first onboarding screen now shows your **ic_launcher** icon
- Displays in a circular white background with shadow
- Replace the placeholder in `AppLogo.kt` line 48 with your actual launcher icon resource

### 2. Standardized Buttons ✅
- **Skip** and **Continue** buttons now have the **same styling**
- Both buttons have equal width and purple background
- Last screen shows single "Let's Get Started" button

### 3. No Random Icons ✅
- Removed the star icon from placeholders
- Clean, simple placeholder text when screenshots aren't loaded

## File Locations

### Component Files
- **AppLogo**: `app/src/main/java/com/manjul/genai/videogenerator/ui/components/onboarding/AppLogo.kt`
- **NavigationButtons**: `app/src/main/java/com/manjul/genai/videogenerator/ui/components/onboarding/NavigationButtons.kt`
- **ScreenshotImage**: `app/src/main/java/com/manjul/genai/videogenerator/ui/components/onboarding/ScreenshotImage.kt`

### Screen Files
- **Screen 1**: `app/src/main/java/com/manjul/genai/videogenerator/ui/screens/onboarding/OnboardingScreen1.kt`
- **Screen 2**: `app/src/main/java/com/manjul/genai/videogenerator/ui/screens/onboarding/OnboardingScreen2.kt`
- **Screen 3**: `app/src/main/java/com/manjul/genai/videogenerator/ui/screens/onboarding/OnboardingScreen3.kt`

### Configuration Files
- **Local Config**: `genai-android/onboardingConfig.json`
- **Remote Config**: Firebase Firestore → `config/onboarding`

## Testing Previews

All components now have Compose previews! View them in Android Studio:

1. Open any component file (e.g., `NavigationButtons.kt`)
2. Look for the `@Preview` annotated functions at the bottom
3. Click the "Split" or "Design" button in Android Studio to see previews
4. Available previews:
   - **AppLogo**: Shows logo on gradient background
   - **NavigationButtons**: Shows all button states (Skip+Continue, Get Started)
   - **ScreenshotImage**: Shows loading, placeholder, and error states
   - **OnboardingScreens 1-3**: Shows complete screen layouts

## Troubleshooting

### Screenshots Not Loading
- Verify the Firebase Storage URLs are publicly accessible
- Check Firebase Storage rules allow read access
- Clear app cache and retry

### Wrong Dimensions
- Screenshots should be 390x844px for best results
- The IPhoneMockup component will scale images, but native size is best

### App Logo Not Showing
- Update line 48 in `AppLogo.kt` to use your actual ic_launcher resource
- Default uses `android.R.mipmap.sym_def_app_icon` as placeholder

## Need Help?

Check the inline documentation in each Kotlin file - every screen has detailed comments explaining how to customize it!

