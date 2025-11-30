# Firebase One-Time Products Configuration

## Overview
One-time product configurations are now fetched from Firebase (just like subscription plans), allowing you to update prices, credits, and badges without redeploying the app.

## Firebase Document Structure

### Location
**Firestore Database → app → landingPage**

### Complete Document Structure

```json
{
  "backgroundVideoUrl": "your_video_url_here",
  "features": [ ... existing features ... ],
  "subscriptionPlans": [ ... existing plans ... ],
  "oneTimeProducts": [
    {
      "productId": "credits_100",
      "credits": 100,
      "price": "$9.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_200",
      "credits": 200,
      "price": "$17.99",
      "isPopular": false,
      "isBestValue": true
    },
    {
      "productId": "credits_300",
      "credits": 300,
      "price": "$24.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_500",
      "credits": 500,
      "price": "$39.99",
      "isPopular": true,
      "isBestValue": false
    },
    {
      "productId": "credits_1000",
      "credits": 1000,
      "price": "$69.99",
      "isPopular": false,
      "isBestValue": false
    }
  ],
  "testimonials": [ ... existing testimonials ... ]
}
```

## How to Update in Firebase Console

### Step 1: Open Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database**
4. Navigate to: **app → landingPage**

### Step 2: Add One-Time Products Array

Click "Edit document" and add the **oneTimeProducts** array with the structure shown above.

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | string | ✅ Yes | Must match Google Play Console INAPP product ID (e.g., "credits_100") |
| `credits` | number | ✅ Yes | Number of credits this product gives |
| `price` | string | ✅ Yes | Display price (e.g., "$9.99") |
| `isPopular` | boolean | No | Shows green "POPULAR" badge above card |
| `isBestValue` | boolean | No | Shows yellow/orange "BEST VALUE" badge above card |

## Important Notes

### Product IDs
- Must start with `credits_` prefix (e.g., `credits_100`, `credits_500`)
- Must exactly match the Product ID in Google Play Console
- Used to identify products in billing flow

### Badges
- **POPULAR** badge: Green badge, use for most commonly purchased tier
- **BEST VALUE** badge: Yellow/orange badge, use for best price-per-credit ratio
- Only one badge will show if both are true (isBestValue takes priority)
- Badge positioned above the card (like subscription "Popular" badge)

### Auto-Selection
- The product marked as `isPopular: true` will be auto-selected when user switches to "Top-Up Credits" tab
- Recommended: Mark the middle tier as popular (e.g., 500 credits)

## Example Pricing Strategy

Based on your current configuration (6 tiers: 50, 100, 150, 250, 500, 1000):

```json
{
  "oneTimeProducts": [
    {
      "productId": "credits_50",
      "credits": 50,
      "price": "$9.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_100",
      "credits": 100,
      "price": "$17.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_150",
      "credits": 150,
      "price": "$24.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_250",
      "credits": 250,
      "price": "$39.99",
      "isPopular": true,
      "isBestValue": true
    },
    {
      "productId": "credits_500",
      "credits": 500,
      "price": "$69.99",
      "isPopular": false,
      "isBestValue": false
    },
    {
      "productId": "credits_1000",
      "credits": 1000,
      "price": "$129.99",
      "isPopular": false,
      "isBestValue": false
    }
  ]
}
```

## Benefits of Firebase Configuration

✅ **No app update needed** - Change prices anytime  
✅ **A/B testing** - Test different price points  
✅ **Regional pricing** - Adjust for different markets  
✅ **Promotional periods** - Temporarily adjust prices  
✅ **Add/remove tiers** - Scale up or down based on demand  
✅ **Badge updates** - Highlight different products  

## Testing

After updating Firebase:
1. **App will automatically refresh** the configuration (real-time listener)
2. **No app restart needed** - changes appear immediately
3. **Verify in app** - Switch to "Top-Up Credits" tab to see new products

## Google Play Console Products

Remember to create matching INAPP products in Play Console for each product ID:
- Navigate to: **Monetize → Products → In-app products**
- Create product with matching **Product ID** (e.g., `credits_100`)
- Set price in Play Console
- Status must be **Active**

The price in Firebase is just for display - the actual charge comes from Google Play Console pricing.

