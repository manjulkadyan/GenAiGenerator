# Complete Reverse Engineering: App 2
## ai.video.generator.text.video (SoliText2Video)

**Version:** 1.7.1 (Version Code: 71)  
**Package:** ai.video.generator.text.video  
**Min SDK:** 24  
**Target SDK:** 35

---

## Table of Contents
1. [Application Overview](#application-overview)
2. [Core Application Classes](#core-application-classes)
3. [Data Models](#data-models)
4. [API & Network Layer](#api--network-layer)
5. [Database Layer](#database-layer)
6. [UI Activities](#ui-activities)
7. [Services & Workers](#services--workers)
8. [Features](#features)
9. [Configuration & Settings](#configuration--settings)

---

## Application Overview

This is an AI video generation app with additional AI art features. It provides:
- Text-to-video generation
- AI art generation (figurines, dolls, Barbie-style, etc.)
- Image-to-video conversion
- Inspiration gallery
- Local video storage
- NSFW content filtering
- Premium/Free tier system

**Key Technologies:**
- Kotlin/Java (Android)
- Traditional Android Views (not Compose)
- Room Database (local storage)
- OkHttp/Retrofit (networking)
- WorkManager (background tasks)
- Firebase (Analytics, FCM)
- Qonversion (subscription management)
- Adjust (analytics)
- Multiple ad networks (AppLovin, Vungle, etc.)

---

## Core Application Classes

### 1. SoliText2VideoApplication
**Package:** `ai.video.generator.text.video`  
**File:** `SoliText2VideoApplication.java`

**Purpose:** Main Application class that initializes all SDKs and configurations.

**Key Initializations:**
- SharedPreferences setup (`SoliText2Video`, `AmenoApp`)
- Firebase Crashlytics
- Firebase Analytics
- Adjust SDK
- Qonversion SDK
- Ad networks configuration
- Remote config loading

**Key Methods:**

#### `onCreate()`
**Initializes:**
1. SharedPreferences for app settings
2. Firebase Crashlytics
3. Firebase Analytics
4. Device ID (Android ID)
5. Remote config (fetches from server)
6. Ad revenue tracking
7. Activity lifecycle callbacks for ads

#### `a()` (Static method - loads remote config)
**Loads Configuration:**
- Ad display settings (banner collapse modes, interstitial triggers)
- API configuration (`api_url`, `key_api`)
- Rate limiting (`max_video_free_in_hour`, `max_video_premium_in_hour`)
- NSFW thresholds (`nsfw_threshold`, `nsfw_threshold_hentai`, etc.)
- Feature flags (`i2v_feature`, `is_figurine_3d`, etc.)
- Paywall settings (`pw_type`, `pw_after_splash`)
- Subscription verification token

**Key Configuration Values:**
- `api_url` - Backend API base URL
- `key_api` - API authentication key
- `max_video_free_in_hour` - Free tier rate limit
- `max_video_premium_in_hour` - Premium tier rate limit
- `max_image_free_figurine` - Free AI art limit
- `max_image_paid_figurine` - Paid AI art limit
- `max_current_generate` - Concurrent generation limit
- `nsfw_threshold` - NSFW detection threshold
- `time_change_prompt` - Prompt change interval
- `ai_art_free_in_day` - Daily free AI art limit
- `max_gen_image_to_video_per_day` - Image-to-video daily limit

---

### 2. MainActivity
**Package:** `ai.video.generator.text.video.ui.activity`  
**File:** `MainActivity.java`

**Purpose:** Main activity with bottom navigation and fragment management.

**Key Components:**
- BottomNavigationView for tab navigation
- FrameLayout for fragment container
- Ad containers (banner ads)
- Fragment management

**Fragments:**
- Home fragment (video generation)
- Collection/History fragment
- Profile/Settings fragment

**Methods:**

#### `onCreate(Bundle)`
- Sets up bottom navigation
- Initializes fragments
- Configures ad displays
- Sets up event bus listeners

#### `onResume()`
- Shows rating dialog (after certain app opens)
- Tracks app usage
- Updates ad displays

#### `onHomeReload(HomeReload event)`
- Handles home screen reload events
- Switches tabs
- Reloads prompt list if needed

---

## Data Models

### 1. CreateVideoRequest
**Package:** `ai.video.generator.text.video.model`  
**File:** `CreateVideoRequest.java`

**Purpose:** Request model for video generation API call.

**Properties:**
- `prompt: String` - Text description for video
- `versionCode: int` - App version code
- `deviceID: String` - Device identifier
- `isPremium: int` - Premium status (0 or 1)
- `ctry_target: String` - Target country
- `used: List<String>` - List of used prompt keys
- `aspect_ratio: String` - Video aspect ratio
- `ai_sound: int` - AI sound option (0 or 1)

**Usage:**
Sent to backend API to create video generation request.

---

### 2. VideoCreateResponse
**Package:** `ai.video.generator.text.video.model`  
**File:** `VideoCreateResponse.java`

**Purpose:** Response model from video creation API.

**Properties:**
- `code: int` - Response code
- `datas: ArrayList<VideoCreate>` - List of video creation results

---

### 3. VideoCreate
**Package:** `ai.video.generator.text.video.model`  
**File:** `VideoCreate.java`

**Purpose:** Individual video creation result.

**Properties:**
- `key: String` - Video key/ID for tracking
- `url: String` - Video URL (when ready)
- `safe: boolean` - NSFW check result

---

### 4. Video (Entity)
**Package:** `ai.video.generator.text.video.data.local.entities`  
**File:** `Video.java`

**Purpose:** Local database entity for video storage.

**Properties:**
- `id: int` - Local database ID
- `prompt: String` - Video prompt
- `style_code: int` - Style identifier
- `code: String` - Server video code/key
- `localPath: String` - Local file path
- `status: int` - Status code:
  - `STATUS_SUCCESS = 0` - Completed
  - `STATUS_FAILED = 1` - Failed
  - `STATUS_RUNNING = 2` - In progress
  - `STATUS_NSFW = 3` - NSFW detected
  - `STATUS_SERVER_NSFW = 4` - Server NSFW check failed
- `time: Date` - Creation timestamp
- `prompt_code: String` - Prompt identifier
- `imageUri: String` - Preview image URI

---

### 5. Inspiration
**Package:** `ai.video.generator.text.video.data.local.entities`  
**File:** `Inspiration.java`

**Purpose:** Inspiration video/item from gallery.

**Properties:**
- `id: int` - Inspiration ID
- `style_code: int` - Style code
- `url: String` - Video URL
- `thumb: String` - Thumbnail URL
- `prompt: String` - Associated prompt

**Methods:**
- `getVideoPath()` - Returns URL with cache-busting parameter
- `getThumbPath()` - Returns thumbnail URL with cache-busting

---

### 6. Home
**Package:** `ai.video.generator.text.video.model`  
**File:** `Home.java`

**Purpose:** Home screen data model.

**Properties:**
- `prompt: List<String>` - Available prompts
- `inspirations: List<Inspiration>` - Inspiration videos
- `imagePrompts: List<String>` - Image prompt suggestions

---

### 7. CheckNSFWResponse
**Package:** `ai.video.generator.text.video.model`  
**File:** `CheckNSFWResponse.java`

**Purpose:** NSFW content check response.

**Properties:**
- `code: int` - Response code
- `success: boolean` - Check success
- `data: ArrayList<NSFWResponse>` - NSFW check results
- `message: String` - Response message

---

## API & Network Layer

### 1. HeaderInterceptor
**Package:** `ai.video.generator.text.video.apiservice`  
**File:** `HeaderInterceptor.java`

**Purpose:** OkHttp interceptor that adds authentication headers to API requests.

**Headers Added:**
1. **`Authorization`** - API key from SharedPreferences (`key_api`)
   - Default: `"eyJzdWIiwsdeOiIyMzQyZmczNHJ0MzR0weMzQiLCJuYW1lIjorwiSm9objMdf0NTM0NT"`
   - Stored in: `SharedPreferences.getString("key_api")`

2. **`sign`** - Signature from Glide
   - Value: `Glide.d.f19544g`

3. **`pt`** - Platform token
   - From: `SharedPreferences.getString("pt_", "")`

4. **`v`** - Version
   - Value: `"71"` (version code)

5. **`deviceID`** - Device identifier (if available)
   - From: `SharedPreferences.getString("device_id", "")`

**Implementation:**
```java
public O intercept(InterfaceC0571w chain) {
    // Get request builder
    E eA = chain.request().newBuilder();
    
    // Add headers
    eA.addHeader("Authorization", apiKey);
    eA.addHeader("sign", signature);
    eA.addHeader("pt", platformToken);
    eA.addHeader("v", "71");
    if (deviceID.length() > 0) {
        eA.addHeader("deviceID", deviceID);
    }
    
    // Proceed with modified request
    return chain.proceed(eA.build());
}
```

---

### 2. API Endpoints

**Base URL:** Stored in SharedPreferences as `api_url` (loaded from remote config)

**Likely Endpoints:**
- `POST /createVideo` - Create video generation request
- `GET /checkVideo/{key}` - Check video generation status
- `GET /home` - Get home screen data (prompts, inspirations)
- `POST /checkNSFW` - Check content for NSFW
- `GET /video/{key}` - Get video details
- `POST /createImage` - Create AI art image
- `POST /imageToVideo` - Convert image to video

**Request Format:**
All requests include headers from `HeaderInterceptor`.

**Response Format:**
```json
{
  "code": 200,
  "success": true,
  "data": [...],
  "message": "..."
}
```

---

## Database Layer

### 1. SoliDatabase
**Package:** `ai.video.generator.text.video.data.local`  
**File:** `SoliDatabase.java`

**Purpose:** Room database for local data storage.

**Entities:**
- `Video` - Generated videos
- `Inspiration` - Inspiration items
- `Prompts` - Saved prompts
- `Style` - Video styles
- `HomeImagePrompt` - Home screen image prompts
- `Surprise` - Surprise/featured content
- `HistoryPrompt` - Prompt history
- `HistoryFeedback` - User feedback

**DAO Methods:**
- Video CRUD operations
- Inspiration queries
- Prompt management
- History tracking

---

## UI Activities

### 1. SplashActivity
**Purpose:** App launch screen
- Shows splash animation
- Loads initial data
- Checks onboarding status
- Navigates to appropriate screen

### 2. IntroActivity
**Purpose:** Onboarding/tutorial screen
- First-time user introduction
- Feature explanations

### 3. MainActivity
**Purpose:** Main navigation hub
- Bottom navigation
- Fragment container
- Ad displays

### 4. LaunchingActivity
**Purpose:** Video generation screen
- Prompt input
- Aspect ratio selection
- AI sound toggle
- Generate button
- Generation status display

**Key Features:**
- Prompt input with suggestions
- Aspect ratio selector
- AI sound option
- Generation progress
- Notification when complete

### 5. DetailActivity
**Purpose:** Video detail/playback screen
- Video player
- Download option
- Share option
- Related videos
- Ad displays

**Features:**
- Media3 ExoPlayer for video playback
- Download to device
- Share functionality
- Rating prompts
- Ad rewards

### 6. LanguageActivity
**Purpose:** Language selection
- Multi-language support
- Language switching

### 7. FeedbackActivity
**Purpose:** User feedback form
- Rating submission
- Feedback collection

### 8. PWActivity / PWActivityNoOffer
**Purpose:** Paywall screens
- Subscription options
- Premium features display
- Purchase flow

### 9. AirArt Activities (AI Art Features)
- **AirArtDollActivity** - AI doll generation
- **AirArtFigureActivity** - AI figure generation
- **AirArtBarBieActivity** - Barbie-style generation
- **AiArtFigurineActivity** - Figurine generation
- **AirArtMoreActivity** - More AI art options
- **AirArtResultActivity** - AI art result display

### 10. WebviewActivity
**Purpose:** In-app web browser
- Terms of service
- Privacy policy
- External links

---

## Services & Workers

### 1. CheckVideoWorker
**Package:** `ai.video.generator.text.video.service`  
**File:** `CheckVideoWorker.java`

**Purpose:** Background worker that checks video generation status.

**Extends:** `CoroutineWorker` (WorkManager)

**Key Methods:**

#### `doWork(Continuation)`
**Purpose:** Main work execution
**Flow:**
1. Gets list of videos with `STATUS_RUNNING` status
2. For each video, calls API to check status
3. Updates local database with new status
4. Downloads video if complete
5. Shows notification when ready
6. Updates UI if app is open

#### `f(List<Video>, Continuation)`
**Purpose:** Checks multiple videos
**Flow:**
1. Iterates through running videos
2. Calls API endpoint: `GET /checkVideo/{code}`
3. Parses response
4. Updates status in database
5. Downloads video file if `status = SUCCESS`
6. Saves to local storage
7. Updates notification

#### `h(Continuation)`
**Purpose:** Checks single video or batch
**Flow:**
1. Queries database for running videos
2. Calls API for each
3. Handles responses
4. Updates database

#### `g(String, String)`
**Purpose:** Creates local file path for video
**Returns:** `File` object
**Path:** `{cacheDir}/Soli_{timestamp}{extension}.mp4`

#### `c()`
**Purpose:** Creates notification for video generation
**Returns:** `ForegroundInfo` for foreground service
**Channel:** "AI_generating_status"

**Notification Features:**
- Custom layout (`notify_view.xml`)
- Progress updates
- Status text
- Non-dismissible while generating

---

### 2. FCMService
**Package:** `ai.video.generator.text.video.service`  
**File:** `FCMService.java`

**Purpose:** Firebase Cloud Messaging service for push notifications.

**Extends:** `FirebaseMessagingService`

**Methods:**
- `onNewToken(String)` - Handles FCM token refresh
- `onMessageReceived(RemoteMessage)` - Handles incoming notifications

---

## Features

### 1. Video Generation

**Flow:**
1. User enters prompt in `LaunchingActivity`
2. Selects aspect ratio
3. Optionally enables AI sound
4. Clicks "Generate"
5. App creates `CreateVideoRequest`:
   ```json
   {
     "prompt": "user prompt",
     "versionCode": 71,
     "deviceID": "device_id",
     "isPremium": 0 or 1,
     "ctry_target": "country",
     "used": ["prompt_key1", "prompt_key2"],
     "aspect_ratio": "16:9",
     "ai_sound": 0 or 1
   }
   ```
6. Sends POST request to API
7. Receives `VideoCreateResponse` with `key` and `url`
8. Creates `Video` entity in database with `STATUS_RUNNING`
9. `CheckVideoWorker` polls API for status
10. When complete, downloads video
11. Updates database: `STATUS_SUCCESS`
12. Shows notification
13. User can view in `DetailActivity`

---

### 2. AI Art Generation

**Features:**
- **Doll Generation** (`AirArtDollActivity`)
- **Figure Generation** (`AirArtFigureActivity`)
- **Barbie-Style** (`AirArtBarBieActivity`)
- **Figurine** (`AiArtFigurineActivity`)
- **3D Figurine** (if `is_figurine_3d` enabled)
- **Realistic Figurine** (if `is_realistic_figurine` enabled)

**Rate Limits:**
- Free: `max_image_free_figurine` per day
- Paid: `max_image_paid_figurine` per day

---

### 3. Image-to-Video

**Feature Flag:** `i2v_feature`

**Daily Limit:** `max_gen_image_to_video_per_day`

**Flow:**
1. User selects image
2. App uploads image
3. Calls image-to-video API
4. Generates video from image
5. Similar to text-to-video flow

---

### 4. NSFW Content Filtering

**Purpose:** Detects and blocks inappropriate content

**Thresholds:**
- `nsfw_threshold` - General NSFW threshold
- `nsfw_threshold_hentai` - Hentai detection
- `nsfw_threshold_porn` - Porn detection
- `nsfw_threshold_sexy` - Suggestive content

**Implementation:**
- Client-side check before generation
- Server-side check after generation
- Videos marked as `STATUS_NSFW` or `STATUS_SERVER_NSFW` if detected

---

### 5. Inspiration Gallery

**Purpose:** Shows example videos to inspire users

**Features:**
- Browse inspiration videos
- Copy prompts from inspirations
- View video details
- Use as template for generation

---

### 6. Rate Limiting

**Free Tier:**
- `max_video_free_in_hour` - Max videos per hour
- `max_image_free_figurine` - Max AI art per day
- `ai_art_free_in_day` - Daily AI art limit

**Premium Tier:**
- `max_video_premium_in_hour` - Higher limit
- `max_image_paid_figurine` - Higher AI art limit
- `max_current_generate` - Concurrent generations

**Enforcement:**
- Tracked in SharedPreferences
- Checked before API calls
- Shows paywall if limit reached

---

### 7. Subscription Management

**SDK:** Qonversion

**Features:**
- Premium subscription
- Subscription verification (`verify_subs_token`)
- Paywall display (`PWActivity`)
- Subscription status tracking

**Paywall Types:**
- `pw_type` - Paywall configuration (from remote config)
- `pw_after_splash` - Show paywall after splash
- `pw_no_offer` - No offer paywall

---

## Configuration & Settings

### Remote Config Keys

Loaded from server via remote config:

**API Configuration:**
- `api_url` - Backend API base URL
- `key_api` - API authentication key

**Rate Limits:**
- `max_video_free_in_hour` - Free tier hourly limit
- `max_video_premium_in_hour` - Premium tier hourly limit
- `max_image_free_figurine` - Free AI art limit
- `max_image_paid_figurine` - Paid AI art limit
- `max_current_generate` - Concurrent generation limit
- `ai_art_free_in_day` - Daily free AI art
- `max_gen_image_to_video_per_day` - Image-to-video daily limit

**NSFW Settings:**
- `nsfw_threshold` - General threshold
- `nsfw_threshold_hentai` - Hentai threshold
- `nsfw_threshold_porn` - Porn threshold
- `nsfw_threshold_sexy` - Suggestive threshold

**Feature Flags:**
- `i2v_feature` - Image-to-video enabled
- `is_figurine_3d` - 3D figurine enabled
- `is_realistic_figurine` - Realistic figurine enabled
- `is_giant_figurine` - Giant figurine enabled

**Ad Configuration:**
- `app_resume_ads` - Show ads on app resume
- `splash_inters` - Interstitial on splash
- `home_banner_collapse_ads` - Collapsible banner on home
- `detail_banner_collapse_ads` - Collapsible banner on detail
- `launching_banner_collapse_ads` - Banner on launching screen
- `inters_inspirations` - Interstitial on inspirations
- `inters_collection` - Interstitial on collection
- `inters_pw` - Interstitial before paywall
- `reward_detail` - Reward ad on detail screen
- `reward_ai_art` - Reward ad for AI art
- `native_ads_input` - Native ads in input
- `native_ads_list` - Native ads in list
- `native_ads_result` - Native ads in results

**Paywall:**
- `pw_type` - Paywall type
- `pw_after_splash` - Show after splash
- `pw_no_offer` - No offer mode
- `verify_subs_token` - Subscription verification token

**Other:**
- `version_data` - Data version
- `reload_banner` - Banner reload interval
- `time_change_prompt` - Prompt change interval
- `time_close_iap` - IAP close time
- `ctry_target` - Target country
- `native_full_interval` - Native ad interval

---

## SharedPreferences Keys

**File:** `SoliText2Video`

**Key Values:**
- `device_id` - Android device ID
- `key_api` - API authentication key
- `api_url` - API base URL
- `date_open_app` - Last app open date
- `count_rating` - Rating dialog count
- `pt_` - Platform token
- `max_video_free_in_hour` - Free tier limit
- `max_video_premium_in_hour` - Premium limit
- `max_image_free_figurine` - Free AI art limit
- `max_image_paid_figurine` - Paid AI art limit
- `max_current_generate` - Concurrent limit
- `nsfw_threshold` - NSFW threshold
- `time_change_prompt` - Prompt change time
- `ai_art_free_in_day` - Daily AI art limit
- `max_gen_image_to_video_per_day` - I2V daily limit
- `i2v_feature` - Image-to-video enabled
- `pw_type` - Paywall type
- `verify_subs_token` - Subscription token
- All ad display flags (see above)

---

## Third-Party Integrations

### 1. Firebase
- **Analytics** - User tracking
- **Crashlytics** - Crash reporting
- **FCM** - Push notifications

### 2. Qonversion
- Subscription management
- Revenue tracking
- Subscription verification

### 3. Adjust
- Attribution tracking
- Analytics
- Campaign tracking

### 4. Ad Networks
- **AppLovin** - Ad mediation
- **Vungle** - Video ads
- **Google AdMob** - Banner/interstitial ads
- **Facebook Audience Network** - Native ads

### 5. Media3 (ExoPlayer)
- Video playback
- Media controls
- Streaming support

### 6. Room Database
- Local data storage
- Video history
- Prompt storage

### 7. WorkManager
- Background video checking
- Periodic status updates
- Download management

---

## Key Workflows

### Video Generation Workflow

```
1. User opens LaunchingActivity
   ↓
2. Enters prompt, selects aspect ratio, toggles AI sound
   ↓
3. Clicks "Generate"
   ↓
4. App checks rate limits (free/premium)
   ↓
5. If limit reached → Show paywall
   ↓
6. Creates CreateVideoRequest:
   {
     prompt, versionCode, deviceID, isPremium,
     ctry_target, used, aspect_ratio, ai_sound
   }
   ↓
7. Sends POST to API with headers:
   - Authorization: key_api
   - sign: signature
   - pt: platform token
   - v: 71
   - deviceID: device_id
   ↓
8. Receives VideoCreateResponse:
   {
     code: 200,
     datas: [{key, url, safe}]
   }
   ↓
9. Creates Video entity in Room database:
   - status: STATUS_RUNNING (2)
   - code: response.key
   - prompt: user prompt
   ↓
10. CheckVideoWorker starts polling:
    - Calls GET /checkVideo/{key}
    - Checks every X seconds
    ↓
11. When status = SUCCESS:
    - Downloads video from URL
    - Saves to local storage
    - Updates database: status = STATUS_SUCCESS (0)
    - Shows notification
    ↓
12. User opens DetailActivity:
    - Plays video from local path
    - Can download/share
```

---

### AI Art Generation Workflow

```
1. User opens AirArt activity (Doll/Figure/etc.)
   ↓
2. Enters prompt or selects style
   ↓
3. Checks daily limit (free/paid)
   ↓
4. Calls AI art API
   ↓
5. Receives image URL
   ↓
6. Displays in AirArtResultActivity
   ↓
7. User can download/share
```

---

### NSFW Check Workflow

```
1. Before generation:
   - Client-side NSFW check (optional)
   ↓
2. After generation:
   - Server checks content
   ↓
3. If NSFW detected:
   - Video marked as STATUS_NSFW (3) or STATUS_SERVER_NSFW (4)
   - Not shown to user
   - Error message displayed
```

---

## Permissions

**Required Permissions:**
- `INTERNET` - Network access
- `ACCESS_NETWORK_STATE` - Check connectivity
- `ACCESS_WIFI_STATE` - Network info
- `POST_NOTIFICATIONS` - Push notifications
- `FOREGROUND_SERVICE_DATA_SYNC` - Background service
- `WAKE_LOCK` - Keep device awake
- `READ_EXTERNAL_STORAGE` (SDK < 33) - Read files
- `WRITE_EXTERNAL_STORAGE` (SDK < 33) - Write files
- `BILLING` - In-app purchases
- `VIBRATE` - Haptic feedback
- `AD_ID` - Advertising ID
- `ACCESS_ADSERVICES_*` - Ad services

---

## Security Features

1. **API Key Authentication:**
   - Stored in SharedPreferences
   - Sent in Authorization header
   - Loaded from remote config

2. **Request Signing:**
   - `sign` header with signature
   - Prevents tampering

3. **Device ID Tracking:**
   - Android ID used for device identification
   - Sent in `deviceID` header

4. **NSFW Filtering:**
   - Client and server-side checks
   - Multiple threshold levels

5. **Rate Limiting:**
   - Prevents abuse
   - Enforced client and server-side

---

## Database Schema

### Video Table
```sql
CREATE TABLE Video (
    id INTEGER PRIMARY KEY,
    prompt TEXT,
    style_code INTEGER,
    code TEXT,
    localPath TEXT,
    status INTEGER,
    time DATE,
    prompt_code TEXT,
    imageUri TEXT
)
```

### Inspiration Table
```sql
CREATE TABLE Inspiration (
    id INTEGER PRIMARY KEY,
    style_code INTEGER,
    url TEXT,
    thumb TEXT,
    prompt TEXT
)
```

### Other Tables
- Prompts
- Style
- HomeImagePrompt
- Surprise
- HistoryPrompt
- HistoryFeedback

---

## API Request/Response Examples

### Create Video Request
```http
POST {api_url}/createVideo
Headers:
  Authorization: eyJzdWIiwsdeOiIyMzQyZmczNHJ0MzR0weMzQiLCJuYW1lIjorwiSm9objMdf0NTM0NT
  sign: {signature}
  pt: {platform_token}
  v: 71
  deviceID: {device_id}

Body:
{
  "prompt": "A cat walking on the beach",
  "versionCode": 71,
  "deviceID": "abc123",
  "isPremium": 0,
  "ctry_target": "US",
  "used": ["prompt1", "prompt2"],
  "aspect_ratio": "16:9",
  "ai_sound": 0
}
```

### Create Video Response
```json
{
  "code": 200,
  "datas": [
    {
      "key": "video_key_123",
      "url": "https://cdn.example.com/video.mp4",
      "safe": true
    }
  ]
}
```

### Check Video Status Request
```http
GET {api_url}/checkVideo/{key}
Headers:
  Authorization: {key_api}
  sign: {signature}
  v: 71
  deviceID: {device_id}
```

### Check Video Status Response
```json
{
  "code": 200,
  "status": "completed",
  "url": "https://cdn.example.com/video.mp4",
  "safe": true
}
```

---

## Summary

This app provides:
- ✅ Text-to-video generation
- ✅ AI art generation (multiple styles)
- ✅ Image-to-video conversion
- ✅ Local video storage
- ✅ Inspiration gallery
- ✅ NSFW filtering
- ✅ Free/Premium tiers
- ✅ Rate limiting
- ✅ Background video checking
- ✅ Push notifications
- ✅ Multiple ad networks
- ✅ Subscription management

**Architecture:**
- Traditional Android (Activities, Fragments)
- Room Database for local storage
- OkHttp/Retrofit for networking
- WorkManager for background tasks
- Remote config for dynamic settings

