# Fix Play Console Subscription Products

## 🔴 Problem

You created **ONE product** with **multiple base plans**, but the app expects **THREE separate products**.

**Current Setup (Wrong):**
- Product ID: `weekly_60_credits`
- Base plans: `base60`, `base100`, `base150` ❌

**Required Setup (Correct):**
- Product 1: `weekly_60_credits` (with base plan)
- Product 2: `weekly_100_credits` (with base plan)
- Product 3: `weekly_150_credits` (with base plan) ✅

---

## ✅ Solution: Create 3 Separate Products

### Step 1: Delete Current Product (Optional)

If you want to start fresh:
1. Play Console → **Monetize** → **Products** → **Subscriptions**
2. Find `weekly_60_credits`
3. Delete it (or keep it and create 2 more)

### Step 2: Create Product 1 - `weekly_60_credits`

1. Play Console → **Monetize** → **Products** → **Subscriptions**
2. Click **Create subscription**
3. **Product ID**: `weekly_60_credits` (exact match!)
4. **Name**: "60 Credits Weekly"
5. **Description**: "Get 60 credits every week"
6. Click **Continue**
7. **Base plan**: Create new base plan
   - **Base plan ID**: `base60` (or any name you want)
   - **Billing period**: **Weekly**
   - **Price**: $9.99
8. Click **Save**

### Step 3: Create Product 2 - `weekly_100_credits`

1. Click **Create subscription** again
2. **Product ID**: `weekly_100_credits` (exact match!)
3. **Name**: "100 Credits Weekly"
4. **Description**: "Get 100 credits every week"
5. Click **Continue**
6. **Base plan**: Create new base plan
   - **Base plan ID**: `base100` (or any name)
   - **Billing period**: **Weekly**
   - **Price**: $14.99
7. Click **Save**

### Step 4: Create Product 3 - `weekly_150_credits`

1. Click **Create subscription** again
2. **Product ID**: `weekly_150_credits` (exact match!)
3. **Name**: "150 Credits Weekly"
4. **Description**: "Get 150 credits every week"
5. Click **Continue**
6. **Base plan**: Create new base plan
   - **Base plan ID**: `base150` (or any name)
   - **Billing period**: **Weekly**
   - **Price**: $19.99
7. Click **Save**

---

## 📋 Verify Setup

After creating all 3 products, you should see:

**In Play Console → Subscriptions:**
- ✅ `weekly_60_credits` (Product 1)
- ✅ `weekly_100_credits` (Product 2)
- ✅ `weekly_150_credits` (Product 3)

Each product should have:
- Its own Product ID
- One base plan (weekly billing)
- Appropriate price

---

## 🔧 Update Firestore (If Needed)

Make sure your Firestore `app/landingPage` document has:

```json
{
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
  ]
}
```

**Important:** The `productId` values must match the Product IDs in Play Console exactly!

---

## 🧪 Test After Fixing

1. **Wait 5-10 minutes** after creating products (they need to propagate)
2. **Rebuild app:**
   ```bash
   cd genai-android
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Open app** → Buy Credits screen
4. **Check logs:**
   ```bash
   adb logcat | grep BillingRepository
   ```
   Should see: "Loaded 3 product details"

---

## ⚠️ Important Notes

1. **Product IDs are case-sensitive** - Must match exactly
2. **Base plan names don't matter** - Only Product IDs matter
3. **Each product needs its own Product ID** - Can't reuse the same ID
4. **Wait for propagation** - Products take a few minutes to be available

---

## 🆘 Still Not Working?

### Check Product IDs Match:
- Firestore `productId`: `weekly_60_credits`
- Play Console Product ID: `weekly_60_credits`
- ✅ Must be identical!

### Check App is Uploaded:
- App must be on **Internal testing** track (or higher)
- Products won't work if app isn't uploaded

### Check Logs:
```bash
adb logcat | grep -E "BillingRepository|productId|Product details"
```

Look for:
- "Loading product details for: weekly_60_credits, weekly_100_credits, weekly_150_credits"
- "Loaded 3 product details" (success)
- Or error messages showing which products failed

---

**After creating 3 separate products, rebuild and test!** 🚀








