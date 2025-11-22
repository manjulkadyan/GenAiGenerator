# Landing Page Firebase Setup Guide

This guide shows you how to set up the Firebase document to control your landing page remotely.

## Firebase Document Location

**Collection:** `app`  
**Document ID:** `landingPage`

## Document Structure

Create a document at `app/landingPage` with the following structure:

```json
{
  "backgroundVideoUrl": "https://your-video-url.com/video.mp4",
  "features": [
    {
      "title": "Powered by 22 Models",
      "description": "Studio-quality results from text or images",
      "icon": "gear"
    },
    {
      "title": "Scale Content Creation",
      "description": "Ship more videos; grow organic reach 2.1x",
      "icon": "flame"
    },
    {
      "title": "Native Audio & 1080p Exports",
      "description": "Best-in-class, advanced AI video creation.",
      "icon": "sound"
    },
    {
      "title": "Ad Factory Templates",
      "description": "Scale content production",
      "icon": "house"
    },
    {
      "title": "No-Watermark 9:16 & 16:9",
      "description": "Clean exports in vertical or horizontal",
      "icon": "screen"
    },
    {
      "title": "Premium Prompt Assistance",
      "description": "Expert help crafting high-performing prompts.",
      "icon": "quote"
    }
  ],
  "subscriptionPlans": [
    {
      "credits": 60,
      "price": "$9.99",
      "isPopular": false,
      "productId": "weekly_60_credits",
      "period": "Weekly"
    },
    {
      "credits": 100,
      "price": "$14.99",
      "isPopular": true,
      "productId": "weekly_100_credits",
      "period": "Weekly"
    },
    {
      "credits": 150,
      "price": "$19.99",
      "isPopular": false,
      "productId": "weekly_150_credits",
      "period": "Weekly"
    }
  ],
  "testimonials": []
}
```

## Field Descriptions

### `backgroundVideoUrl` (String)
- URL to the background video (can be a compilation/reels video)
- Should be a direct video URL (MP4, etc.)
- If empty, a black background will be shown

### `features` (Array of Objects)
Each feature object contains:
- **`title`** (String): Feature title
- **`description`** (String): Feature description
- **`icon`** (String): Icon identifier. Supported values:
  - `"gear"` or `"settings"` - Settings icon
  - `"flame"` or `"fire"` - Fire icon
  - `"sound"`, `"audio"`, or `"wave"` - Sound wave icon
  - `"house"`, `"home"`, or `"building"` - House icon
  - `"screen"` or `"display"` - Screen icon
  - `"quote"` or `"prompt"` - Quote icon

### `subscriptionPlans` (Array of Objects)
Each plan object contains:
- **`credits`** (Number): Number of credits in the plan
- **`price`** (String): Display price (e.g., "$9.99")
- **`isPopular`** (Boolean): Whether to show "Popular" badge
- **`productId`** (String): Google Play product ID (must match your Play Console setup)
- **`period`** (String): Subscription period (e.g., "Weekly")

### `testimonials` (Array of Objects, Optional)
Each testimonial object contains:
- **`username`** (String): Reviewer username
- **`rating`** (Number): Star rating (1-5)
- **`text`** (String): Review text

## Example Complete Document

Here's a complete example matching your design:

```json
{
  "backgroundVideoUrl": "https://storage.googleapis.com/your-bucket/landing-video.mp4",
  "features": [
    {
      "title": "Powered by 22 Models",
      "description": "Studio-quality results from text or images",
      "icon": "gear"
    },
    {
      "title": "Scale Content Creation",
      "description": "Ship more videos; grow organic reach 2.1x",
      "icon": "flame"
    },
    {
      "title": "Native Audio & 1080p Exports",
      "description": "Best-in-class, advanced AI video creation.",
      "icon": "sound"
    },
    {
      "title": "Ad Factory Templates",
      "description": "Scale content production",
      "icon": "house"
    },
    {
      "title": "No-Watermark 9:16 & 16:9",
      "description": "Clean exports in vertical or horizontal",
      "icon": "screen"
    },
    {
      "title": "Premium Prompt Assistance",
      "description": "Expert help crafting high-performing prompts.",
      "icon": "quote"
    }
  ],
  "subscriptionPlans": [
    {
      "credits": 60,
      "price": "$9.99",
      "isPopular": false,
      "productId": "weekly_60_credits",
      "period": "Weekly"
    },
    {
      "credits": 100,
      "price": "$14.99",
      "isPopular": true,
      "productId": "weekly_100_credits",
      "period": "Weekly"
    },
    {
      "credits": 150,
      "price": "$19.99",
      "isPopular": false,
      "productId": "weekly_150_credits",
      "period": "Weekly"
    }
  ],
  "testimonials": []
}
```

## How to Set Up in Firebase Console

1. Go to Firebase Console → Firestore Database
2. Navigate to the `app` collection (create it if it doesn't exist)
3. Create a new document with ID `landingPage`
4. Add the fields as shown above
5. For arrays, use the "Add field" → "Array" option in Firebase Console

## Google Play Product IDs

**Important:** The `productId` values in your Firebase document must match the product IDs you create in Google Play Console:

1. Go to Google Play Console → Your App → Monetize → Products → Subscriptions
2. Create subscription products with IDs:
   - `weekly_60_credits`
   - `weekly_100_credits`
   - `weekly_150_credits`
3. Configure pricing and other subscription details in Play Console

## Default Values

If the Firebase document doesn't exist or has missing fields, the app will use default values:
- Default features: The 6 features shown above
- Default plans: 60, 100, 150 credits with the prices shown
- Default video: Black background if no video URL

## Testing

1. Create the document in Firebase
2. Run the app
3. Navigate to the Buy Credits screen
4. The landing page should load with your configured data
5. Changes to the Firebase document will be reflected in real-time (the app observes the document)

## Notes

- The app observes the `app/landingPage` document in real-time, so changes will update automatically
- Video URL should be a direct link to an MP4 file (or other ExoPlayer-supported format)
- Make sure your video is hosted on a CORS-enabled server or Firebase Storage
- Product IDs must match exactly between Firebase and Google Play Console

