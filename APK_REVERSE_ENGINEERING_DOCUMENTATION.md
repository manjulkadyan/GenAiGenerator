# Complete APK Reverse Engineering Documentation
## com.pneurs.soraai (SoraAI - AI Video Generation App)

**Version:** 1.2 (Version Code: 13)  
**Package:** com.pneurs.soraai  
**Min SDK:** 24  
**Target SDK:** 36

---

## Table of Contents
1. [Application Overview](#application-overview)
2. [Core Application Classes](#core-application-classes)
3. [Data Models](#data-models)
4. [Database & Repository Layer](#database--repository-layer)
5. [ViewModels](#viewmodels)
6. [UI Components & Screens](#ui-components--screens)
7. [Services](#services)
8. [Utilities](#utilities)
9. [License Verification](#license-verification)
10. [Third-Party Integrations](#third-party-integrations)

---

## Application Overview

This is an AI-powered video generation application that allows users to:
- Generate videos from text prompts using AI models (Replicate Veo API)
- Apply video effects to images
- Manage credits/subscriptions via Superwall
- Track generation history via Firestore

**Key Technologies:**
- Kotlin/Java (Android)
- Jetpack Compose (UI)
- Firebase (Auth, Firestore, Storage, Functions, Messaging)
- Superwall SDK (Paywall/Subscription management)
- Replicate API (AI video generation)

---

## Core Application Classes

### 1. SoraApplication
**Package:** `com.decent.soraai`  
**File:** `SoraApplication.java`

**Purpose:** Main Application class that initializes Firebase and Superwall SDK.

**Key Constants:**
- `SUPERWALL_API_KEY = "pk_uCLNKCm_EcvXzXCnRRG5a"` - Superwall API key for paywall management

**Methods:**
- `onCreate()` - Initializes:
  - Firebase App
  - Firebase App Check (Play Integrity)
  - Firebase Analytics
  - Superwall SDK with configuration

**How it works:**
1. Initializes Firebase when app starts
2. Sets up Play Integrity for app verification
3. Enables analytics collection
4. Configures Superwall for subscription management

---

### 2. MainActivity
**Package:** `com.decent.soraai`  
**File:** `MainActivity.java`

**Purpose:** Main entry point activity that handles authentication, deep linking, and UI composition.

**Implements:** `SuperwallDelegate` - Handles paywall events

**Key Properties:**
- `TAG = "MainActivity"`
- State management for onboarding, authentication, and paywall type

**Methods:**

#### `onCreate(Bundle savedInstanceState)`
- Sets portrait orientation
- Enables edge-to-edge display
- Sets up Jetpack Compose UI
- Registers Superwall delegate
- Handles deep links

#### `onNewIntent(Intent intent)`
- Handles new intents (deep links)
- Processes Superwall deep links

#### `handleDeepLink(Intent intent)`
- Processes deep link intents
- Extracts URI and passes to Superwall for subscription redemption

#### `authenticateWithFirebase(Context context, Continuation<String> continuation)`
**Purpose:** Authenticates user with Firebase or creates anonymous user
**Returns:** User ID (String)

**Flow:**
1. Checks if user ID exists in SharedPreferences
2. If exists, returns cached user ID
3. If not, checks Firebase Auth for current user
4. If no user, creates anonymous Firebase user (with 10s timeout)
5. Saves user ID to SharedPreferences
6. On error/timeout, generates UUID and saves it

#### `fetchAppConfig(Context context, String currentUserId, Continuation<AppConfigResult> continuation)`
**Purpose:** Fetches app configuration from Firestore
**Returns:** `AppConfigResult` containing userId and paywallType

**Flow:**
1. Connects to Firestore
2. Reads document from `app/config` collection
3. Extracts `paywall_type` field (NORMAL, MODERATE, or HARD)
4. Checks if app version matches `test_version` field
5. If version matches and `review_id` exists, uses that as userId (for testing)
6. Returns configuration result

#### `saveUserIdToPrefs(Context context, String userId)`
- Saves user ID to SharedPreferences (`veo_prefs`)
- Key: `"user_id"`

#### `getUserIdFromPrefs(Context context)`
- Retrieves user ID from SharedPreferences
- Returns null if not found

#### SuperwallDelegate Methods:
- `willRedeemLink()` - Shows "Redeeming subscription..." toast
- `didRedeemLink(RedemptionResult result)` - Handles redemption results:
  - Success: Shows success toast
  - Error: Shows error toast
  - Expired: Shows expiration toast
  - Invalid: Shows invalid toast

---

### 3. AppConfigResult
**Package:** `com.decent.soraai`  
**File:** `AppConfigResult.java`

**Purpose:** Data class to hold app configuration results from Firestore.

**Properties:**
- `userId: String` - User identifier
- `paywallType: PaywallType` - Type of paywall to show (NORMAL, MODERATE, HARD)

**Methods:**
- Standard data class methods (copy, equals, hashCode, toString)

---

## Data Models

### 1. AIModel
**Package:** `com.decent.soraai.data`  
**File:** `AIModel.java`

**Purpose:** Represents an AI model configuration for video generation.

**Properties:**
- `id: String` - Unique model identifier
- `index: int` - Display index
- `name: String` - Model name
- `url: String` - Model URL/endpoint
- `trending: boolean` - Whether model is trending
- `duration: int` - Default duration in seconds
- `aspectRatio: List<String>` - Supported aspect ratios (e.g., ["16:9", "9:16"])
- `durationOption: List<Integer>` - Available duration options
- `firstFrame: Boolean?` - Whether first frame is required
- `lastFrame: Boolean?` - Whether last frame is required
- `pricePerSec: int` - Price per second of video
- `replicateName: String` - Replicate API model name
- `videoUrl: String?` - Preview video URL
- `description: String` - Model description

**Methods:**
- Getters/setters for mutable properties
- `writeToParcel()` / `createFromParcel()` - Parcelable implementation

---

### 2. VideoEffect
**Package:** `com.decent.soraai.data`  
**File:** `VideoEffect.java`

**Purpose:** Represents a video effect that can be applied to images.

**Properties:**
- `id: String` - Unique effect identifier
- `index: Integer?` - Display index
- `name: String` - Effect name
- `prompt: String` - Effect prompt/description
- `credits: int` - Credits required to use effect
- `aspectRatio: String` - Supported aspect ratio
- `previewUrl: String` - Preview image URL
- `endpoint: String` - API endpoint for effect
- `identifier: String` - Effect identifier

**Methods:**
- Getters/setters
- Parcelable implementation

---

### 3. ResultVideo
**Package:** `com.decent.soraai.data`  
**File:** `ResultVideo.java`

**Purpose:** Represents a generated video result stored in Firestore.

**Properties:**
- `id: String` - Unique video ID
- `previewImage: String?` - Preview image URL
- `storageUrl: String?` - Final video storage URL
- `status: Status` - Processing status (PROCESSED, INPROGRESS, ERROR)
- `requestCredits: Integer?` - Credits used for generation
- `errorMessage: String?` - Error message if failed
- `contentType: ContentType?` - Content type (veo3)
- `createdAt: Date` - Creation timestamp

**Firestore Field Mappings:**
- `preview_image` → `previewImage`
- `storage_url` → `storageUrl`
- `request_credits` → `requestCredits`
- `error_message` → `errorMessage`
- `created_at` → `createdAt`

---

### 4. User
**Package:** `com.decent.soraai.data`  
**File:** `User.java`

**Purpose:** Represents user data.

**Properties:**
- `credits: Integer?` - User's credit balance

---

### 5. Status (Enum)
**Package:** `com.decent.soraai.data`  
**File:** `Status.java`

**Values:**
- `PROCESSED` - Video generation completed
- `INPROGRESS` - Video generation in progress
- `ERROR` - Video generation failed

**Firestore Mappings:**
- `"processed"` → `PROCESSED`
- `"inprogress"` → `INPROGRESS`
- `"error"` → `ERROR`

---

### 6. PaywallType (Enum)
**Package:** `com.decent.soraai.data`  
**File:** `PaywallType.java`

**Values:**
- `NORMAL` - Standard paywall
- `MODERATE` - Moderate paywall frequency
- `HARD` - Aggressive paywall frequency

**Methods:**
- `Companion.fromRaw(String value)` - Converts string to PaywallType (case-insensitive)

---

### 7. ImageAspectRatio (Enum)
**Package:** `com.decent.soraai.data`  
**File:** `ImageAspectRatio.java`

**Values:**
- `SQUARE` - 1:1 (value: 1.0f)
- `WIDESCREEN` - 16:9 (value: 1.7777778f)
- `PORTRAIT` - 9:16 (value: 0.5625f)
- `LANDSCAPE` - 21:9 (value: 2.3333333f)
- `PORTRAIT_ULTRA` - 9:21 (value: 0.42857143f)
- `THREE_FOUR` - 3:4 (value: 0.75f)
- `FOUR_THREE` - 4:3 (value: 1.3333334f)

**Methods:**
- `getRatio()` - Returns ratio string (e.g., "16:9")
- `getValue()` - Returns float value
- `Companion.fromString(String ratio)` - Converts string to enum

---

### 8. ContentType (Enum)
**Package:** `com.decent.soraai.data`  
**File:** `ContentType.java`

**Values:**
- `veo3` - Veo3 content type

---

## Database & Repository Layer

### 1. VideoGenerateRepository
**Package:** `com.decent.soraai.data.db`  
**File:** `VideoGenerateRepository.java`

**Implements:** `IVideoGenerateRepository`

**Purpose:** Handles video generation API calls and image uploads.

**Dependencies:**
- Firebase Storage (for image uploads)
- Firebase Functions (for API calls)

**Methods:**

#### `uploadImage(String uid, Uri uri, Continuation<String> continuation)`
**Purpose:** Uploads image to Firebase Storage
**Returns:** Download URL (String)

**Flow:**
1. Creates storage reference: `users/{uid}/inputs/{uuid}.jpeg`
2. Uploads file to Firebase Storage
3. Waits for upload completion
4. Gets download URL
5. Returns URL string

#### `callReplicateVeoAPI(Map<String, Object> request, Continuation<Object> continuation)`
**Purpose:** Calls Firebase Function `callReplicateVeoAPIV2` to generate video
**Returns:** Function call result (Object)

**Flow:**
1. Gets Firebase Functions instance
2. Calls `callReplicateVeoAPIV2` HTTPS callable function
3. Passes request map
4. Returns result

---

### 2. EffectRepository
**Package:** `com.decent.soraai.data.db`  
**File:** `EffectRepository.java`

**Implements:** `IEffectRepository`

**Purpose:** Handles video effect generation API calls and image uploads.

**Methods:**

#### `uploadEffectImage(String uid, Uri uri, Continuation<String> continuation)`
**Purpose:** Uploads effect image to Firebase Storage
**Returns:** Download URL (String)

**Flow:**
1. Creates storage reference: `users/{uid}/effects/{uuid}.jpeg`
2. Uploads file
3. Returns download URL

#### `callVideoEffectAPI(Map<String, Object> request, Continuation<Object> continuation)`
**Purpose:** Calls Firebase Function `generateVideoEffect` to apply effect
**Returns:** Function call result

**Flow:**
1. Calls `generateVideoEffect` HTTPS callable function
2. Passes request map
3. Returns result

---

### 3. HistoryProvider Interfaces
**Package:** `com.decent.soraai.data.db`

**Interfaces:**
- `HistoryProvider` - Interface for fetching video generation history
- `RealHistoryProvider` - Real Firestore implementation
- `FakeHistoryProvider` - Mock implementation for testing
- `EmptyHistoryProvider` - Empty implementation

**Similar pattern for:**
- `EffectProvider` / `RealEffectProvider` / `FakeVideoFeatureProvider`
- `VideoFeatureProvider` / `RealVideoFeatureProvider`

---

## ViewModels

### 1. VideoGenerateViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `VideoGenerateViewModel.java`

**Purpose:** Manages state and logic for video generation screen.

**Properties:**
- `model: AIModel` - Selected AI model
- `userId: String` - Current user ID
- `repository: IVideoGenerateRepository` - Repository for API calls
- `state: State<VideoGenerateState>` - Observable state

**State Properties (VideoGenerateState):**
- `prompt: String?` - User's text prompt
- `firstFrameUri: Uri?` - First frame image URI
- `lastFrameUri: Uri?` - Last frame image URI
- `aspectRatio: String?` - Selected aspect ratio
- `duration: int` - Selected duration
- `durationOptions: List<Integer>` - Available durations
- `promptOptimizer: boolean` - Whether prompt optimizer is enabled
- `isGenerating: boolean` - Generation in progress
- `showErrorDialog: boolean` - Show error dialog
- `errorMessage: String?` - Error message
- `estimatedCost: int` - Estimated credits cost
- `isGenerateDisabled: boolean` - Whether generate button is disabled
- `showSuccessDialog: boolean` - Show success dialog
- `uploadStatus: String?` - Upload status message

**Methods:**

#### `setPrompt(String value)`
- Updates prompt in state
- Recalculates state (cost, validation)

#### `setFirstFrameUri(Uri uri)`
- Sets first frame image
- Recalculates validation

#### `setLastFrameUri(Uri uri)`
- Sets last frame image
- Recalculates validation

#### `setAspectRatio(String ratio)`
- Updates aspect ratio selection

#### `setDuration(int value)`
- Updates duration
- Recalculates cost

#### `setPromptOptimizer(boolean value)`
- Toggles prompt optimizer

#### `dismissError()`
- Hides error dialog

#### `dismissSuccess()`
- Hides success dialog

#### `onGenerate(int credits, Function0<Unit> onInsufficientCredits)`
**Purpose:** Initiates video generation
**Flow:**
1. Checks if user has sufficient credits
2. If not, calls `onInsufficientCredits` callback
3. Sets `isGenerating = true`
4. Uploads first frame (if required)
5. Uploads last frame (if required)
6. Calls `callReplicateVeoAPI` with:
   - Image URLs
   - Prompt
   - Aspect ratio
   - Duration
   - Model configuration
7. Updates state with result
8. Handles errors

**Private Methods:**
- `recalculateState(...)` - Recalculates state based on current inputs:
  - Calculates cost: `pricePerSec * duration`
  - Validates inputs (prompt not empty, required frames present)
  - Updates `isGenerateDisabled`

---

### 2. EffectDetailViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `EffectDetailViewModel.java`

**Purpose:** Manages state for video effect generation screen.

**Properties:**
- `repository: IEffectRepository` - Repository for API calls
- `selectedImage: Uri?` - Selected image for effect
- `isImageLoading: boolean` - Image upload in progress
- `isGenerating: boolean` - Effect generation in progress
- `errorMessage: String?` - Error message
- `uploadStatus: String` - Upload status text

**Methods:**

#### `updateSelectedImage(Uri uri)`
- Sets selected image

#### `generateEffect(String uid, VideoEffect effect, String appVersion, Continuation<Result<Unit>> continuation)`
**Purpose:** Generates video effect
**Returns:** `Result<Unit>`

**Flow:**
1. Validates image is selected
2. Uploads image to Firebase Storage
3. Sets `isImageLoading = true`
4. Calls `callVideoEffectAPI` with:
   - Image URL
   - Effect configuration
   - User ID
   - App version
5. Sets `isGenerating = true`
6. Updates status messages
7. Handles errors
8. Returns result

---

### 3. CreditsViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `CreditsViewModel.java`

**Purpose:** Manages user credits state.

**Methods:**
- `listenToUserCredits(String userId)` - Listens to Firestore for credit updates

---

### 4. EffectsViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `EffectsViewModel.java`

**Purpose:** Manages list of available video effects.

---

### 5. ResultsViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `ResultsViewModel.java`

**Purpose:** Manages generated video results/history.

---

### 6. AIModelsViewModel
**Package:** `com.decent.soraai.ui.screens.viewmodels`  
**File:** `AIModelsViewModel.java`

**Purpose:** Manages available AI models.

---

## UI Components & Screens

### Main Screens

#### 1. Main Screen
**Package:** `com.decent.soraai.ui.screens`  
**File:** `MainKt.java`

**Purpose:** Main navigation screen with tabs.

**Features:**
- Tab navigation
- Shows different screens based on selected tab
- Integrates with ViewModels

---

#### 2. EffectsListScreen
**Package:** `com.decent.soraai.ui.screens`  
**File:** `EffectsListScreenKt.java`

**Purpose:** Displays grid of available video effects.

---

#### 3. EffectDetailScreen
**Package:** `com.decent.soraai.ui.screens`  
**File:** `EffectDetailScreenKt.java`

**Purpose:** Shows effect details and allows image selection/generation.

**Features:**
- Image picker
- Effect preview
- Generate button
- Status indicators

---

#### 4. GenerationView
**Package:** `com.decent.soraai.ui.screens`  
**File:** `GenerationViewKt.java`

**Purpose:** Video generation interface.

**Features:**
- Prompt input
- Aspect ratio selector
- Duration selector
- First/Last frame image pickers
- Cost display
- Generate button

---

#### 5. ResultsView
**Package:** `com.decent.soraai.ui.screens.views`  
**File:** `ResultsViewKt.java`

**Purpose:** Displays generated video results.

**Features:**
- Video grid/list
- Status indicators
- Playback controls

---

#### 6. Onboarding
**Package:** `com.decent.soraai.ui.screens.views`  
**File:** `OnboardingKt.java`

**Purpose:** First-time user onboarding flow.

**Features:**
- Multi-step tutorial
- Completion tracking

---

#### 7. ProfileView
**Package:** `com.decent.soraai.ui.screens.views`  
**File:** `ProfileViewKt.java`

**Purpose:** User profile and settings.

**Features:**
- User ID display
- Credits display
- Settings

---

#### 8. AIModelsView
**Package:** `com.decent.soraai.ui.screens.views`  
**File:** `AIModelsViewKt.java`

**Purpose:** Displays available AI models for selection.

---

#### 9. PlayerView
**Package:** `com.decent.soraai.ui.screens.views`  
**File:** `PlayerViewKt.java`

**Purpose:** Video player for generated videos.

**Features:**
- Playback controls
- Fullscreen support
- Status display

---

### UI Components

#### 1. EffectCard
**Package:** `com.decent.soraai.ui.components`  
**File:** `EffectCardKt.java`

**Purpose:** Card component for displaying effect preview.

---

#### 2. StaggeredEffectsGrid
**Package:** `com.decent.soraai.ui.components`  
**File:** `StaggeredEffectsGridKt.java`

**Purpose:** Staggered grid layout for effects.

---

#### 3. CreditsBanner
**Package:** `com.decent.soraai.ui.components`  
**File:** `CreditsBannerKt.java`

**Purpose:** Displays user's credit balance.

---

#### 4. GenerateActionButton
**Package:** `com.decent.soraai.ui.components`  
**File:** `GenerateActionButtonKt.java`

**Purpose:** Generate button with loading states.

---

#### 5. CostInfoRow
**Package:** `com.decent.soraai.ui.components`  
**File:** `CostInfoRowKt.java`

**Purpose:** Displays cost information.

---

#### 6. GradientComponents
**Package:** `com.decent.soraai.ui.components`  
**File:** `GradientComponentsKt.java`

**Purpose:** Gradient background components.

---

## Services

### 1. SoraFirebaseMessagingService
**Package:** `com.decent.soraai.services`  
**File:** `SoraFirebaseMessagingService.java`

**Extends:** `FirebaseMessagingService`

**Purpose:** Handles Firebase Cloud Messaging (push notifications).

**Methods:**

#### `onNewToken(String token)`
- Called when FCM token is refreshed
- Logs token
- Sends token to server via `NotificationHelper.sendRegistrationToServer()`

#### `onMessageReceived(RemoteMessage remoteMessage)`
- Handles incoming push notifications
- Checks if message has data payload
- Checks if message has notification payload
- Routes to appropriate handler

#### `handleDataMessage(RemoteMessage remoteMessage)`
- Processes data-only messages
- Extracts title and message from data
- Shows notification via `NotificationHelper`

#### `handleNotificationMessage(RemoteMessage remoteMessage)`
- Processes notification messages
- Extracts title and body
- Shows notification

---

## Utilities

### 1. InappHelper
**Package:** `com.decent.soraai.utils`  
**File:** `InappHelper.java`

**Purpose:** Helper for in-app purchases and subscriptions.

**Methods:**

#### `isUserSubscribed()`
**Returns:** `boolean` - Whether user has active subscription

**Flow:**
1. Gets Superwall subscription status
2. Returns true if status is `SubscriptionStatus.Active`
3. Returns false otherwise
4. Handles exceptions gracefully

#### `safeRegister(String event, int credits)`
**Purpose:** Registers Superwall event with credits data
**Parameters:**
- `event` - Event name
- `credits` - Credits value to track

**Flow:**
1. Registers event with Superwall
2. Passes credits as event parameter

---

### 2. NotificationHelper
**Package:** `com.decent.soraai.utils`  
**File:** `NotificationHelper.java`

**Purpose:** Helper for displaying notifications.

**Methods:**
- `showNotification(Context, String title, String message, ...)` - Shows notification
- `sendRegistrationToServer(Context, String token)` - Sends FCM token to server

---

### 3. OnboardingPreferences
**Package:** `com.decent.soraai.utils`  
**File:** `OnboardingPreferences.java`

**Purpose:** Manages onboarding completion state.

**Methods:**
- `isOnboardingCompleted(Context)` - Checks if onboarding is complete
- `setOnboardingCompleted(Context, boolean)` - Sets completion state

---

## License Verification

### 1. LicenseClient
**Package:** `com.pairip.licensecheck`  
**File:** `LicenseClient.java`

**Purpose:** Verifies app license with Google Play Licensing Service.

**Key Constants:**
- `licensePubKey` - RSA public key for license verification
- `packageName = "com.pneurs.soraai"`
- `SERVICE_PACKAGE = "com.android.vending"`

**License Check States:**
- `CHECK_REQUIRED` - Initial state
- `FULL_CHECK_OK` - Full license check passed
- `LOCAL_CHECK_OK` - Local installer check passed
- `LOCAL_CHECK_REPORTED` - Local check reported to service

**Methods:**

#### `initializeLicenseCheck()`
**Purpose:** Starts license verification process

**Flow:**
1. If `localCheckEnabled`, performs local installer check first
2. If local check passes, sets `LOCAL_CHECK_OK`
3. Connects to Google Play Licensing Service
4. If already checked, validates cached response

#### `performLocalInstallerCheck()`
**Purpose:** Checks if app was installed from Google Play

**Flow:**
1. Checks Android SDK version (requires 30+)
2. Gets package info
3. Checks if app is system app (flags 1 or 128)
4. Gets install source info
5. Verifies installer is `com.android.vending`
6. Returns true if valid

#### `connectToLicensingService()`
**Purpose:** Binds to Google Play Licensing Service

**Flow:**
1. Creates intent for licensing service
2. Binds service
3. Retries on failure (max 3 retries)

#### `onServiceConnected(ComponentName, IBinder)`
**Purpose:** Called when service connection established

**Flow:**
1. If `CHECK_REQUIRED`, calls `checkLicenseInternal()`
2. If `LOCAL_CHECK_OK`, reports successful check

#### `checkLicenseInternal(IBinder)`
**Purpose:** Performs license check via IPC

**Flow:**
1. Creates Parcel with:
   - Package name
   - Result listener binder
   - Flags
2. Transacts with service (TRANSACTION_CHECK_LICENSE_V2 = 2)
3. Receives response code and payload

#### `processResponse(int responseCode, Bundle responsePayload)`
**Purpose:** Processes license check response

**Response Codes:**
- `0` (LICENSED) - License valid
- `2` (NOT_LICENSED) - Not licensed, shows paywall
- `3` (ERROR_INVALID_PACKAGE_NAME) - Invalid package

**Flow:**
1. Validates response code
2. If licensed, validates response signature
3. Sets state to `FULL_CHECK_OK`
4. If not licensed, shows paywall activity
5. Handles errors

#### `reportSuccessfulLicenseCheck(IBinder)`
**Purpose:** Reports successful local check to service

**Flow:**
1. Transacts with service (TRANSACTION_REPORT_SUCCESSFUL_LICENSE_CHECK = 3)
2. Sets state to `LOCAL_CHECK_REPORTED`

#### `retryOrThrow(LicenseCheckException, boolean)`
**Purpose:** Retries license check or handles final failure

**Flow:**
1. If retries < 3, schedules retry after 1 second
2. If max retries reached:
   - If `ignoreErrorOnFinalFailure`, logs error
   - Otherwise, calls `handleError()`

#### `handleError(LicenseCheckException)`
**Purpose:** Handles license check errors

**Flow:**
1. Logs error
2. If already fully checked, ignores
3. Otherwise, shows error dialog activity

#### `startPaywallActivity(PendingIntent)`
**Purpose:** Shows paywall for unlicensed users

**Flow:**
1. Creates intent for `LicenseActivity`
2. Adds paywall intent and type
3. Starts activity

#### `startErrorDialogActivity()`
**Purpose:** Shows error dialog

**Flow:**
1. Creates intent for `LicenseActivity`
2. Sets activity type to ERROR_DIALOG
3. Starts activity

---

### 2. LicenseActivity
**Package:** `com.pairip.licensecheck`  
**File:** `LicenseActivity.java`

**Purpose:** Activity for displaying paywall or error dialog.

**Activity Types:**
- `PAYWALL` - Shows paywall
- `ERROR_DIALOG` - Shows error message

---

### 3. ResponseValidator
**Package:** `com.pairip.licensecheck`  
**File:** `ResponseValidator.java`

**Purpose:** Validates license response signature.

**Methods:**
- `validateResponse(Bundle, String packageName)` - Validates response using RSA signature

---

### 4. LicenseContentProvider
**Package:** `com.pairip.licensecheck`  
**File:** `LicenseContentProvider.java`

**Purpose:** Content provider for license data.

---

## Third-Party Integrations

### 1. Superwall SDK
**Purpose:** Paywall and subscription management

**Configuration:**
- API Key: `pk_uCLNKCm_EcvXzXCnRRG5a`
- Passes identifiers to Play Store
- Handles subscription status changes
- Manages paywall presentation

**Events:**
- Paywall presentation/dismissal
- Subscription status changes
- Deep link handling
- Custom events with credits tracking

---

### 2. Firebase Services

#### Firebase Auth
- Anonymous authentication
- User ID management

#### Firestore
- Collections:
  - `app/config` - App configuration
  - `users/{userId}` - User data
  - `users/{userId}/videos` - Generated videos
- Real-time listeners for credits

#### Firebase Storage
- Paths:
  - `users/{userId}/inputs/{uuid}.jpeg` - Input images
  - `users/{userId}/effects/{uuid}.jpeg` - Effect images

#### Firebase Functions
- `callReplicateVeoAPIV2` - Video generation
- `generateVideoEffect` - Effect generation

#### Firebase Messaging
- Push notifications
- Token management

#### Firebase Analytics
- Event tracking
- User properties

#### Firebase App Check
- Play Integrity verification

---

### 3. Replicate API
**Purpose:** AI video generation backend

**Integration:**
- Called via Firebase Functions
- Model: Configured via `AIModel.replicateName`
- Parameters: Prompt, images, aspect ratio, duration

---

## Key Workflows

### Video Generation Workflow

1. **User Input:**
   - Selects AI model
   - Enters prompt
   - Selects aspect ratio
   - Selects duration
   - Optionally uploads first/last frame

2. **Validation:**
   - Checks prompt is not empty
   - Validates required frames (if model requires)
   - Calculates cost: `pricePerSec * duration`
   - Checks user has sufficient credits

3. **Generation:**
   - Uploads images to Firebase Storage (if provided)
   - Calls Firebase Function `callReplicateVeoAPIV2`
   - Function calls Replicate API
   - Returns job ID

4. **Result:**
   - Video status tracked in Firestore
   - User notified when complete
   - Video URL stored in `storageUrl` field

---

### Effect Generation Workflow

1. **User Input:**
   - Selects effect
   - Uploads image

2. **Generation:**
   - Uploads image to Firebase Storage
   - Calls Firebase Function `generateVideoEffect`
   - Function processes effect
   - Returns result

3. **Result:**
   - Effect applied video available
   - Status updated in UI

---

### Authentication Workflow

1. **App Launch:**
   - Checks SharedPreferences for user ID
   - If found, uses cached ID
   - If not, checks Firebase Auth

2. **Firebase Auth:**
   - If user exists, uses Firebase UID
   - If not, creates anonymous user (10s timeout)
   - On timeout/error, generates UUID

3. **Configuration:**
   - Fetches app config from Firestore
   - Determines paywall type
   - Sets user ID (may override with test ID)

---

### License Verification Workflow

1. **App Launch:**
   - Performs local installer check (if enabled)
   - Connects to Google Play Licensing Service

2. **License Check:**
   - Sends package name and listener
   - Receives response code

3. **Response Handling:**
   - If licensed: Validates signature, continues
   - If not licensed: Shows paywall
   - If error: Shows error dialog

4. **Reporting:**
   - If local check passed, reports to service

---

## Security Features

1. **License Verification:**
   - Google Play Licensing Service
   - RSA signature validation
   - Local installer verification

2. **App Check:**
   - Play Integrity verification
   - Prevents unauthorized access

3. **Firebase Security:**
   - Firestore security rules (not visible in APK)
   - Storage security rules
   - Function authentication

---

## Permissions

**Required Permissions:**
- `INTERNET` - Network access
- `ACCESS_NETWORK_STATE` - Check connectivity
- `ACCESS_WIFI_STATE` - Network info
- `BILLING` - In-app purchases
- `POST_NOTIFICATIONS` - Push notifications
- `VIBRATE` - Haptic feedback
- `WAKE_LOCK` - Keep device awake
- `FOREGROUND_SERVICE` - Background services
- `RECEIVE_BOOT_COMPLETED` - Auto-start
- `AD_ID` - Advertising ID
- `CHECK_LICENSE` - License verification

---

## Configuration

### App Configuration (Firestore: `app/config`)

**Fields:**
- `paywall_type` - String: "NORMAL", "MODERATE", or "HARD"
- `test_version` - Long: Version code for testing
- `review_id` - String: User ID override for testing

### SharedPreferences

**File:** `veo_prefs`
- `user_id` - String: Cached user ID

---

## Notes

1. **Code Obfuscation:** Some methods show "Method not decompiled" - likely due to obfuscation or complex coroutine code.

2. **Firebase Functions:** Actual API implementation is in Firebase Functions (not in APK).

3. **Replicate API:** Video generation happens server-side via Replicate API.

4. **Superwall:** Paywall configuration is managed server-side via Superwall dashboard.

5. **License Check:** Uses PairIP library for license verification.

---

## Summary

This APK implements an AI video generation app with:
- Firebase backend integration
- Superwall subscription management
- Google Play license verification
- Jetpack Compose UI
- Real-time credit tracking
- Video generation via Replicate API
- Effect application system

The app follows MVVM architecture with clear separation between UI, ViewModels, and data layer.

