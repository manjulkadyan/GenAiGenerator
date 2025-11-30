# Google Play Subscription Setup Guide

## ⚠️ IMPORTANT: This is REQUIRED

**You MUST create subscription products in Google Play Console** - there is no way to use Google Play Billing without this. Google Play validates all purchases against products defined in Play Console.

## Your Product IDs

Based on your Firebase config, you need to create these 3 subscription products:

1. `weekly_60_credits` - $9.99/week
2. `weekly_100_credits` - $14.99/week (Popular)
3. `weekly_150_credits` - $19.99/week

## Step-by-Step Setup

### Prerequisites
- Your app must be created in Google Play Console (even if not published yet)
- You need a Google Play Developer account ($25 one-time fee)

### Step 1: Navigate to Subscriptions

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to **Monetize** → **Products** → **Subscriptions**
4. Click **Create subscription**

### Step 2: Create First Subscription (`weekly_60_credits`)

1. **Product ID**: Enter exactly `weekly_60_credits`
   - ⚠️ Must match exactly with your Firebase config
   - Cannot be changed after creation

2. **Name**: Enter a display name (e.g., "60 Credits Weekly")

3. **Description**: Enter a description (e.g., "Get 60 credits every week")

4. **Billing period**: Select **Weekly**

5. **Price**: Set to **$9.99** (or your local currency equivalent)

6. **Free trial** (Optional): Set if you want to offer a free trial

7. **Grace period** (Optional): Set if you want a grace period for failed payments

8. Click **Save**

### Step 3: Create Second Subscription (`weekly_100_credits`)

Repeat Step 2 with:
- **Product ID**: `weekly_100_credits`
- **Name**: "100 Credits Weekly"
- **Price**: $14.99
- **Billing period**: Weekly

### Step 4: Create Third Subscription (`weekly_150_credits`)

Repeat Step 2 with:
- **Product ID**: `weekly_150_credits`
- **Name**: "150 Credits Weekly"
- **Price**: $19.99
- **Billing period**: Weekly

## Testing Before Publishing

### Option 1: Internal Testing Track (Recommended)

1. Upload your app to **Internal testing** track
2. Add testers to your internal testing group
3. Test purchases will work with real Google accounts (but won't charge)

### Option 2: License Testing

1. Go to **Settings** → **License testing**
2. Add your Google account email as a test account
3. Test purchases will be free for test accounts

### Option 3: Test Products (Temporary)

You can create test products with different IDs (e.g., `test_weekly_60_credits`) for testing, but you'll need to:
- Update your Firebase config with test product IDs
- Switch back to real product IDs before production

## Important Notes

### Product ID Rules
- Must be lowercase
- Can contain letters, numbers, and underscores
- Cannot be changed after creation
- Must be unique across your app

### Pricing
- Prices in Play Console should match your Firebase config (for display consistency)
- Google Play will show the actual price from Play Console (not Firebase)
- You can change prices later, but existing subscribers keep their price

### Subscription Status
- Subscriptions are active immediately after purchase
- Google handles renewals automatically
- You need to check subscription status in your app/backend

## Verification Checklist

After setup, verify:

- [ ] All 3 product IDs created in Play Console
- [ ] Product IDs match exactly with Firebase config (`weekly_60_credits`, `weekly_100_credits`, `weekly_150_credits`)
- [ ] All subscriptions set to **Weekly** billing period
- [ ] Prices match your Firebase config
- [ ] App uploaded to at least Internal testing track
- [ ] Test account added for license testing

## Testing the Integration

1. Build and install your app (from Internal testing or locally)
2. Navigate to Buy Credits screen
3. Select a subscription plan
4. Click "Continue"
5. Google Play billing dialog should appear
6. Complete test purchase (won't charge if using test account)

## Troubleshooting

### "Product details not available"
- Product ID doesn't exist in Play Console
- Product ID doesn't match exactly (case-sensitive)
- App not uploaded to any track yet

### "Billing service unavailable"
- Google Play Services not installed/updated
- Device not connected to internet
- Testing on emulator without Google Play Services

### "Item already owned"
- Test account already has an active subscription
- Cancel the subscription in Play Console or wait for it to expire

## Next Steps After Setup

1. **Handle subscription status in your backend**
   - Verify purchases server-side using Google Play Developer API
   - Grant credits based on subscription tier
   - Handle subscription renewals/cancellations

2. **Update user credits**
   - When purchase succeeds, add credits to user account
   - Check subscription status on app launch
   - Handle subscription expiration

3. **Monitor subscriptions**
   - Use Google Play Console to monitor active subscriptions
   - Set up server-side verification for security

## Alternative: One-Time Purchases (In-App Products)

If you want to avoid subscriptions, you could use **In-App Products** instead:
- One-time purchases (not recurring)
- Simpler setup
- But users would need to manually purchase credits each time

However, subscriptions are better for recurring revenue and user experience.

---

**Remember**: You cannot use Google Play Billing without creating products in Play Console. This is a Google requirement, not something we can work around.






