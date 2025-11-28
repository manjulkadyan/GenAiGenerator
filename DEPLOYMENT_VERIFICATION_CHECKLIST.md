# Deployment & Configuration Verification Checklist

This checklist provides step-by-step verification commands to check the deployment status and configuration of the subscription system.

---

## Firebase Project Information

**Project ID**: `genaivideogenerator`  
**Firestore Location**: `asia-south1`  
**Package Name**: `com.manjul.genai.videogenerator`

---

## 1. Firebase Functions Deployment Verification

### Check Deployed Functions

```bash
cd genai-android
firebase functions:list
```

**Expected Functions**:
- ✅ `callReplicateVeoAPIV2`
- ✅ `testCallReplicateVeoAPIV2`
- ✅ `addTestCredits`
- ✅ `handleSubscriptionPurchase`
- ✅ `checkUserSubscriptionRenewal`
- ✅ `handlePlayRtdn`
- ✅ `addSubscriptionCredits`
- ✅ `replicateWebhook`
- ✅ `generateVideoEffect`
- ✅ `requestAccountDeletion`
- ✅ `processAccountDeletion`

### Check Function URLs

```bash
firebase functions:list | grep handlePlayRtdn
```

**Get RTDN Webhook URL** (needed for Play Console):
```
https://REGION-genaivideogenerator.cloudfunctions.net/handlePlayRtdn
```

---

## 2. Environment Variables & Secrets

### Check Firebase Config

```bash
firebase functions:config:get
```

**Expected Variables**:
- `play.package_name` = "com.manjul.genai.videogenerator"
- `play.service_account_key` (should be moved to secrets)

### Check Firebase Secrets

```bash
firebase functions:secrets:list
```

**Expected Secrets**:
- ✅ `REPLICATE_API_TOKEN`
- ⚠️ `PLAY_SERVICE_ACCOUNT_KEY` (if migrated from config)

### Set Missing Secrets

If `PLAY_SERVICE_ACCOUNT_KEY` is not set as a secret:

```bash
cd genai-android
firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_KEY
# Paste the JSON content from service-account-key.json
```

---

## 3. Google Cloud Pub/Sub Configuration

### List Existing Topics

```bash
gcloud pubsub topics list --project=genaivideogenerator
```

**Expected Output**:
```
name: projects/genaivideogenerator/topics/play-rtdn-notifications
```

### Create Topic if Missing

```bash
gcloud pubsub topics create play-rtdn-notifications \
  --project=genaivideogenerator
```

### Verify Topic Details

```bash
gcloud pubsub topics describe play-rtdn-notifications \
  --project=genaivideogenerator
```

### Grant Pub/Sub Permissions to Service Account

```bash
# Get the service account email from your service-account-key.json
SERVICE_ACCOUNT="YOUR_SERVICE_ACCOUNT@genaivideogenerator.iam.gserviceaccount.com"

gcloud pubsub topics add-iam-policy-binding play-rtdn-notifications \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/pubsub.subscriber" \
  --project=genaivideogenerator
```

### Create Push Subscription to Cloud Function

```bash
# Get the Cloud Function URL for handlePlayRtdn
FUNCTION_URL="https://REGION-genaivideogenerator.cloudfunctions.net/handlePlayRtdn"

gcloud pubsub subscriptions create play-rtdn-subscription \
  --topic=play-rtdn-notifications \
  --push-endpoint=$FUNCTION_URL \
  --project=genaivideogenerator
```

---

## 4. Google Play Console Configuration

### Navigate to Play Console

1. Go to: https://play.google.com/console
2. Select your app: **GenAI Video Generator**
3. Navigate to: **Monetize > Monetization setup**

### Configure Real-time Developer Notifications

**Path**: Play Console → Setup → API access → Real-time developer notifications

**Settings**:
- **Topic name**: `play-rtdn-notifications`
- **Project ID**: `genaivideogenerator`
- **Enable**: ✅ Enabled

### Test RTDN Configuration

In Play Console:
1. Go to: **Setup → API access → Real-time developer notifications**
2. Click **Send test notification**
3. Check Cloud Function logs:

```bash
firebase functions:log --only handlePlayRtdn --limit 10
```

**Expected Log**:
```
📨 RTDN received: type=X, token=test_token_...
```

---

## 5. Service Account Permissions

### Verify Service Account in Play Console

**Path**: Play Console → Setup → API access → Service accounts

**Required Permissions** for the service account used in `PLAY_SERVICE_ACCOUNT_KEY`:
- ✅ **View financial data** (to read subscription purchases)
- ✅ **Manage orders and subscriptions** (to acknowledge purchases)

### Grant Permissions

1. In Play Console → Setup → API access
2. Find your service account
3. Click **Grant access**
4. Select permissions:
   - ✅ View app information and download bulk reports (read only)
   - ✅ View financial data, orders, and cancellation survey responses
   - ✅ Manage orders and subscriptions
5. Click **Invite user**

---

## 6. Google Play Subscription Products Configuration

### Verify Subscription Products Exist

1. Go to: Play Console → Monetize → Subscriptions
2. Verify products match your app configuration:
   - `starter_plan` or similar
   - `pro_plan` or similar
   - `premium_plan` or similar

### Check Product Details

For each subscription:
- ✅ **Status**: Active
- ✅ **Base plan**: Configured
- ✅ **Offer**: Default offer or promotional offers
- ✅ **Pricing**: Set for all countries
- ✅ **Free trial**: Optional, configured if needed

---

## 7. Firestore Security Rules

### Check Current Rules

```bash
cd genai-android
cat firestore.rules
```

### Verify Rules Allow Subscription Operations

```javascript
match /users/{userId}/subscriptions/{subscriptionId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

match /users/{userId}/purchases/{purchaseToken} {
  allow read: if request.auth != null && request.auth.uid == userId;
  // Writes should be server-side only
}
```

### Deploy Rules if Modified

```bash
firebase deploy --only firestore:rules
```

---

## 8. Testing Checklist

### Test Purchase Flow

1. **Test Environment**: Internal testing track in Play Console
2. **Test Account**: Add test account in Play Console → Setup → License testing
3. **Test Purchase**:
   ```
   - Launch app
   - Navigate to subscription page
   - Select plan
   - Complete purchase
   - Verify credits added
   ```
4. **Check Logs**:
   ```bash
   firebase functions:log --only handleSubscriptionPurchase --limit 20
   ```

### Test RTDN Webhook

1. Send test notification from Play Console
2. Check logs:
   ```bash
   firebase functions:log --only handlePlayRtdn --limit 10
   ```
3. Verify subscription state updated in Firestore

### Test Renewal Check

1. Trigger renewal check manually:
   ```bash
   # Call checkUserSubscriptionRenewal via app or Firebase console
   ```
2. Check logs:
   ```bash
   firebase functions:log --only checkUserSubscriptionRenewal --limit 10
   ```

---

## 9. Monitoring & Alerts

### Set Up Cloud Monitoring

1. Go to: https://console.cloud.google.com/monitoring
2. Create alert policies for:
   - **Function errors** (handleSubscriptionPurchase, checkUserSubscriptionRenewal)
   - **High latency** (> 5 seconds)
   - **RTDN failures** (handlePlayRtdn errors)

### Set Up Log-Based Metrics

```bash
# Create metric for purchase failures
gcloud logging metrics create subscription-purchase-failures \
  --description="Failed subscription purchases" \
  --log-filter='resource.type="cloud_function"
    resource.labels.function_name="handleSubscriptionPurchase"
    severity="ERROR"'
```

### Set Up Email Alerts

1. Go to Cloud Console → Monitoring → Alerting
2. Create alert for `subscription-purchase-failures`
3. Set threshold: > 5 errors in 5 minutes
4. Add notification channel (email)

---

## 10. Production Readiness Checklist

### Security

- [ ] Remove or restrict `addTestCredits` function (production builds only)
- [ ] Set up rate limiting for callable functions
- [ ] Enable Cloud Armor for DDoS protection
- [ ] Review Firestore security rules
- [ ] Rotate service account keys regularly

### Configuration

- [ ] Verify `PLAY_SERVICE_ACCOUNT_KEY` secret is set
- [ ] Verify `REPLICATE_API_TOKEN` secret is set
- [ ] Verify `PLAY_PACKAGE_NAME` environment variable
- [ ] Verify Pub/Sub topic configured
- [ ] Verify RTDN enabled in Play Console

### Deployment

- [ ] Deploy all Cloud Functions
- [ ] Deploy Firestore rules
- [ ] Deploy Firestore indexes
- [ ] Verify function URLs
- [ ] Test all functions end-to-end

### Monitoring

- [ ] Set up error alerts
- [ ] Set up latency monitoring
- [ ] Set up purchase volume tracking
- [ ] Set up RTDN webhook monitoring
- [ ] Set up budget alerts for Cloud costs

### Testing

- [ ] Test new purchase flow
- [ ] Test purchase with 3D Secure
- [ ] Test renewal flow
- [ ] Test cancellation flow
- [ ] Test RTDN webhook with test notification
- [ ] Test duplicate purchase prevention
- [ ] Load test subscription functions
- [ ] Test with multiple test accounts

---

## 11. Troubleshooting Commands

### View Recent Function Logs

```bash
# All functions
firebase functions:log --limit 50

# Specific function
firebase functions:log --only handleSubscriptionPurchase --limit 20

# With timestamp
firebase functions:log --since 2h
```

### Check Function Execution Count

```bash
gcloud monitoring time-series list \
  --filter='metric.type="cloudfunctions.googleapis.com/function/execution_count"' \
  --project=genaivideogenerator
```

### Check Function Error Rate

```bash
gcloud monitoring time-series list \
  --filter='metric.type="cloudfunctions.googleapis.com/function/execution_count" AND metric.label.status!="ok"' \
  --project=genaivideogenerator
```

### Verify Firestore Data

```bash
# Check user subscriptions
firebase firestore:get users/USER_ID/subscriptions

# Check purchases
firebase firestore:get users/USER_ID/purchases
```

---

## 12. Rollback Plan

### If Functions Deployment Fails

```bash
# Rollback to previous version
gcloud functions describe FUNCTION_NAME --region=REGION --project=genaivideogenerator
# Note the revision number
gcloud functions deploy FUNCTION_NAME --revision=PREVIOUS_REVISION --project=genaivideogenerator
```

### If RTDN Breaks

```bash
# Disable RTDN in Play Console temporarily
# Rely on app-side renewal checks
```

### If Purchase Flow Breaks

1. Disable subscription page in app (server-side feature flag)
2. Investigate issue from logs
3. Hotfix and redeploy
4. Re-enable when verified

---

## Summary

This checklist ensures all components of the subscription system are properly configured and deployed:

1. ✅ Firebase Functions deployed
2. ✅ Environment variables and secrets configured
3. ✅ Pub/Sub topic created and configured
4. ✅ RTDN webhook linked to Play Console
5. ✅ Service account permissions granted
6. ✅ Subscription products configured in Play Console
7. ✅ Security rules deployed
8. ✅ Monitoring and alerts set up
9. ✅ All flows tested end-to-end
10. ✅ Production readiness checklist completed

**Next Steps**: Execute each section systematically and mark items as completed.

