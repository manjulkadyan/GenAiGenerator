# Quick Reference: All Classes and Functions

## Core Application Classes

### SoraApplication
- `onCreate()` - Initializes Firebase and Superwall

### MainActivity
- `onCreate(Bundle)` - Sets up UI and handles deep links
- `onNewIntent(Intent)` - Handles new intents
- `handleDeepLink(Intent)` - Processes deep links
- `authenticateWithFirebase(Context, Continuation<String>)` - Authenticates user
- `fetchAppConfig(Context, String, Continuation<AppConfigResult>)` - Fetches config from Firestore
- `saveUserIdToPrefs(Context, String)` - Saves user ID
- `getUserIdFromPrefs(Context)` - Gets user ID
- `willRedeemLink()` - Superwall delegate method
- `didRedeemLink(RedemptionResult)` - Handles subscription redemption

### AppConfigResult
- `getUserId()` - Returns user ID
- `getPaywallType()` - Returns paywall type

---

## Data Models

### AIModel
- `getId()` - Model ID
- `getName()` - Model name
- `getUrl()` - Model URL
- `getTrending()` - Is trending
- `getDuration()` - Default duration
- `getAspectRatio()` - Supported aspect ratios
- `getDurationOption()` - Duration options
- `getFirstFrame()` - Requires first frame
- `getLastFrame()` - Requires last frame
- `getPricePerSec()` - Price per second
- `getReplicateName()` - Replicate API name
- `getVideoUrl()` - Preview video URL
- `getDescription()` - Model description

### VideoEffect
- `getId()` - Effect ID
- `getIndex()` - Display index
- `getName()` - Effect name
- `getPrompt()` - Effect prompt
- `getCredits()` - Required credits
- `getAspectRatio()` - Aspect ratio
- `getPreviewUrl()` - Preview URL
- `getEndpoint()` - API endpoint
- `getIdentifier()` - Effect identifier

### ResultVideo
- `getId()` - Video ID
- `getPreviewImage()` - Preview image URL
- `getStorageUrl()` - Video URL
- `getStatus()` - Processing status
- `getRequestCredits()` - Credits used
- `getErrorMessage()` - Error message
- `getContentType()` - Content type
- `getCreatedAt()` - Creation date

### User
- `getCredits()` - Credit balance

### Status (Enum)
- `PROCESSED` - Completed
- `INPROGRESS` - In progress
- `ERROR` - Failed

### PaywallType (Enum)
- `NORMAL` - Standard
- `MODERATE` - Moderate frequency
- `HARD` - Aggressive frequency
- `Companion.fromRaw(String)` - Parse from string

### ImageAspectRatio (Enum)
- `SQUARE` - 1:1
- `WIDESCREEN` - 16:9
- `PORTRAIT` - 9:16
- `LANDSCAPE` - 21:9
- `PORTRAIT_ULTRA` - 9:21
- `THREE_FOUR` - 3:4
- `FOUR_THREE` - 4:3
- `getRatio()` - Ratio string
- `getValue()` - Float value
- `Companion.fromString(String)` - Parse from string

### ContentType (Enum)
- `veo3` - Veo3 content type

---

## Repository Layer

### VideoGenerateRepository
- `uploadImage(String uid, Uri uri, Continuation<String>)` - Uploads image, returns URL
- `callReplicateVeoAPI(Map<String, Object>, Continuation<Object>)` - Calls video generation API

### EffectRepository
- `uploadEffectImage(String uid, Uri uri, Continuation<String>)` - Uploads effect image
- `callVideoEffectAPI(Map<String, Object>, Continuation<Object>)` - Calls effect API

### HistoryProvider (Interface)
- Methods for fetching video history

### EffectProvider (Interface)
- Methods for fetching effects

### VideoFeatureProvider (Interface)
- Methods for video features

---

## ViewModels

### VideoGenerateViewModel
- `getState()` - Returns observable state
- `setPrompt(String)` - Sets prompt
- `setFirstFrameUri(Uri)` - Sets first frame
- `setLastFrameUri(Uri)` - Sets last frame
- `setAspectRatio(String)` - Sets aspect ratio
- `setDuration(int)` - Sets duration
- `setPromptOptimizer(boolean)` - Toggles optimizer
- `dismissError()` - Hides error dialog
- `dismissSuccess()` - Hides success dialog
- `onGenerate(int credits, Function0<Unit> onInsufficientCredits)` - Starts generation
- `recalculateState(...)` - Private: Recalculates state

### EffectDetailViewModel
- `getSelectedImage()` - Selected image URI
- `updateSelectedImage(Uri)` - Sets selected image
- `isImageLoading()` - Upload in progress
- `isGenerating()` - Generation in progress
- `getErrorMessage()` - Error message
- `getUploadStatus()` - Status text
- `generateEffect(String uid, VideoEffect, String appVersion, Continuation<Result<Unit>>)` - Generates effect

### CreditsViewModel
- `listenToUserCredits(String userId)` - Listens to credit updates

### EffectsViewModel
- Methods for managing effects list

### ResultsViewModel
- Methods for managing results

### AIModelsViewModel
- Methods for managing AI models

---

## Services

### SoraFirebaseMessagingService
- `onNewToken(String)` - Handles token refresh
- `onMessageReceived(RemoteMessage)` - Handles incoming messages
- `handleDataMessage(RemoteMessage)` - Processes data messages
- `handleNotificationMessage(RemoteMessage)` - Processes notifications

---

## Utilities

### InappHelper
- `isUserSubscribed()` - Checks subscription status
- `safeRegister(String event, int credits)` - Registers Superwall event

### NotificationHelper
- `showNotification(Context, String, String, ...)` - Shows notification
- `sendRegistrationToServer(Context, String)` - Sends FCM token

### OnboardingPreferences
- `isOnboardingCompleted(Context)` - Checks completion
- `setOnboardingCompleted(Context, boolean)` - Sets completion

---

## License Verification

### LicenseClient
- `initializeLicenseCheck()` - Starts license check
- `performLocalInstallerCheck()` - Checks installer
- `connectToLicensingService()` - Connects to service
- `onServiceConnected(ComponentName, IBinder)` - Service connected
- `onServiceDisconnected(ComponentName)` - Service disconnected
- `checkLicenseInternal(IBinder)` - Performs license check
- `reportSuccessfulLicenseCheck(IBinder)` - Reports success
- `processResponse(int, Bundle)` - Processes response
- `retryOrThrow(LicenseCheckException, boolean)` - Retries or handles error
- `handleError(LicenseCheckException)` - Handles errors
- `startPaywallActivity(PendingIntent)` - Shows paywall
- `startErrorDialogActivity()` - Shows error dialog

### LicenseActivity
- Activity for paywall/error display

### ResponseValidator
- `validateResponse(Bundle, String)` - Validates signature

### LicenseContentProvider
- Content provider for license data

---

## UI Screens (Compose Functions)

### Main Screen
- `Main(...)` - Main navigation screen

### EffectsListScreen
- `EffectsListScreen(...)` - Effects grid

### EffectDetailScreen
- `EffectDetailScreen(...)` - Effect details

### GenerationView
- `GenerationView(...)` - Video generation UI

### ResultsView
- `ResultsView(...)` - Results display

### Onboarding
- `Onboarding(...)` - Onboarding flow

### ProfileView
- `ProfileView(...)` - User profile

### AIModelsView
- `AIModelsView(...)` - AI models selection

### PlayerView
- `PlayerView(...)` - Video player

---

## UI Components (Compose Functions)

### EffectCard
- `EffectCard(...)` - Effect card component

### StaggeredEffectsGrid
- `StaggeredEffectsGrid(...)` - Staggered grid layout

### CreditsBanner
- `CreditsBanner(...)` - Credits display

### GenerateActionButton
- `GenerateActionButton(...)` - Generate button

### CostInfoRow
- `CostInfoRow(...)` - Cost information

### GradientComponents
- `Background(...)` - Gradient background

---

## Key Workflows Summary

### Video Generation
1. User inputs prompt, selects model/aspect ratio/duration
2. Optionally uploads first/last frame images
3. System validates inputs and calculates cost
4. Checks user credits
5. Uploads images to Firebase Storage
6. Calls Firebase Function → Replicate API
7. Tracks status in Firestore
8. Notifies user when complete

### Effect Generation
1. User selects effect and uploads image
2. Uploads image to Firebase Storage
3. Calls Firebase Function for effect processing
4. Returns processed video

### Authentication
1. Checks cached user ID
2. If not found, checks Firebase Auth
3. If no user, creates anonymous user
4. Fetches app config from Firestore
5. May override with test user ID

### License Verification
1. Performs local installer check
2. Connects to Google Play Licensing Service
3. Sends license check request
4. Validates response signature
5. If not licensed, shows paywall
6. Reports successful local checks

---

## Firebase Collections/Documents

- `app/config` - App configuration
- `users/{userId}` - User document
- `users/{userId}/videos/{videoId}` - Generated videos
- `users/{userId}/inputs/{uuid}.jpeg` - Input images (Storage)
- `users/{userId}/effects/{uuid}.jpeg` - Effect images (Storage)

---

## Firebase Functions

- `callReplicateVeoAPIV2` - Video generation
- `generateVideoEffect` - Effect generation

---

## Constants

### Superwall
- API Key: `pk_uCLNKCm_EcvXzXCnRRG5a`

### License
- Package: `com.pneurs.soraai`
- Public Key: RSA key in `LicenseClient.licensePubKey`

### SharedPreferences
- File: `veo_prefs`
- Key: `user_id`

---

## Enums Summary

- **Status:** PROCESSED, INPROGRESS, ERROR
- **PaywallType:** NORMAL, MODERATE, HARD
- **ImageAspectRatio:** SQUARE, WIDESCREEN, PORTRAIT, LANDSCAPE, PORTRAIT_ULTRA, THREE_FOUR, FOUR_THREE
- **ContentType:** veo3
- **LicenseCheckState:** CHECK_REQUIRED, FULL_CHECK_OK, LOCAL_CHECK_OK, LOCAL_CHECK_REPORTED

