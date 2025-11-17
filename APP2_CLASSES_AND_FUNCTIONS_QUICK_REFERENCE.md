# App 2: Quick Reference - Classes and Functions

## Core Application Classes

### SoliText2VideoApplication
**Package:** `ai.video.generator.text.video`

**Key Methods:**
- `onCreate()` - Initializes app, Firebase, SDKs, remote config
- `a()` - Loads remote configuration from server

**Initializes:**
- SharedPreferences
- Firebase (Analytics, Crashlytics)
- Adjust SDK
- Qonversion SDK
- Ad networks
- Remote config

---

## UI Activities

### MainActivity
**Package:** `ai.video.generator.text.video.ui.activity`

**Purpose:** Main navigation hub with bottom navigation

**Key Methods:**
- `onCreate(Bundle)` - Sets up UI, fragments, navigation
- `onResume()` - Shows rating dialog, tracks usage
- `onHomeReload(HomeReload)` - Handles home reload events
- `m()` - Creates view binding

**Components:**
- BottomNavigationView
- Fragment container
- Ad containers

---

### SplashActivity
**Purpose:** App launch screen

**Features:**
- Splash animation
- Initial data loading
- Onboarding check
- Navigation routing

---

### LaunchingActivity
**Purpose:** Video generation screen

**Key Properties:**
- `f12333o: String` - Prompt
- `f12334p: String` - Aspect ratio
- `f12335q: String` - AI sound option
- `f12337s: String` - Additional option

**Features:**
- Prompt input
- Aspect ratio selection
- AI sound toggle
- Generate button
- Progress display

---

### DetailActivity
**Purpose:** Video playback and details

**Key Methods:**
- `A()` - Video playback
- `B()` - Download/share
- `C()` - Rating dialog
- `z()` - Inspiration handling

**Features:**
- ExoPlayer video playback
- Download functionality
- Share option
- Related videos
- Ad displays

---

### AirArt Activities (AI Art Generation)

#### AirArtDollActivity
**Purpose:** AI doll generation

#### AirArtFigureActivity
**Purpose:** AI figure generation

#### AirArtBarBieActivity
**Purpose:** Barbie-style generation

#### AiArtFigurineActivity
**Purpose:** Figurine generation

#### AirArtMoreActivity
**Purpose:** More AI art options

#### AirArtResultActivity
**Purpose:** AI art result display

---

### Other Activities

- **IntroActivity** - Onboarding/tutorial
- **LanguageActivity** - Language selection
- **FeedbackActivity** - User feedback
- **PWActivity** - Paywall (with offer)
- **PWActivityNoOffer** - Paywall (no offer)
- **WebviewActivity** - In-app browser

---

## Data Models

### CreateVideoRequest
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `prompt: String` - Video description
- `versionCode: int` - App version
- `deviceID: String` - Device identifier
- `isPremium: int` - Premium status (0/1)
- `ctry_target: String` - Target country
- `used: List<String>` - Used prompt keys
- `aspect_ratio: String` - Video aspect ratio
- `ai_sound: int` - AI sound option (0/1)

---

### VideoCreateResponse
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `code: int` - Response code
- `datas: ArrayList<VideoCreate>` - Video results

---

### VideoCreate
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `key: String` - Video key/ID
- `url: String` - Video URL
- `safe: boolean` - NSFW check result

---

### Video (Entity)
**Package:** `ai.video.generator.text.video.data.local.entities`

**Properties:**
- `id: int` - Database ID
- `prompt: String` - Video prompt
- `style_code: int` - Style identifier
- `code: String` - Server video code
- `localPath: String` - Local file path
- `status: int` - Status (0=success, 1=failed, 2=running, 3=nsfw, 4=server_nsfw)
- `time: Date` - Creation time
- `prompt_code: String` - Prompt identifier
- `imageUri: String` - Preview image

**Status Constants:**
- `STATUS_SUCCESS = 0`
- `STATUS_FAILED = 1`
- `STATUS_RUNNING = 2`
- `STATUS_NSFW = 3`
- `STATUS_SERVER_NSFW = 4`

---

### Inspiration
**Package:** `ai.video.generator.text.video.data.local.entities`

**Properties:**
- `id: int` - Inspiration ID
- `style_code: int` - Style code
- `url: String` - Video URL
- `thumb: String` - Thumbnail URL
- `prompt: String` - Associated prompt

**Methods:**
- `getVideoPath()` - URL with cache-busting
- `getThumbPath()` - Thumbnail with cache-busting

---

### Home
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `prompt: List<String>` - Available prompts
- `inspirations: List<Inspiration>` - Inspiration videos
- `imagePrompts: List<String>` - Image prompt suggestions

---

### CheckNSFWResponse
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `code: int` - Response code
- `success: boolean` - Check success
- `data: ArrayList<NSFWResponse>` - NSFW results
- `message: String` - Response message

---

### VideoCreateRequest
**Package:** `ai.video.generator.text.video.model`

**Properties:**
- `keys: List<String>` - Video keys to check

---

## API & Network

### HeaderInterceptor
**Package:** `ai.video.generator.text.video.apiservice`

**Purpose:** Adds authentication headers to API requests

**Method:**
- `intercept(InterfaceC0571w chain)` - Adds headers:
  - `Authorization: {key_api}`
  - `sign: {signature}`
  - `pt: {platform_token}`
  - `v: 71`
  - `deviceID: {device_id}`

---

## Database

### SoliDatabase
**Package:** `ai.video.generator.text.video.data.local`

**Purpose:** Room database for local storage

**Method:**
- `u()` - Returns DAO interface

**Entities:**
- Video
- Inspiration
- Prompts
- Style
- HomeImagePrompt
- Surprise
- HistoryPrompt
- HistoryFeedback

---

## Services & Workers

### CheckVideoWorker
**Package:** `ai.video.generator.text.video.service`

**Extends:** `CoroutineWorker`

**Purpose:** Background worker that checks video generation status

**Key Methods:**

#### `doWork(Continuation)`
**Purpose:** Main work execution
- Gets running videos
- Checks status via API
- Downloads when complete
- Updates database
- Shows notifications

#### `f(List<Video>, Continuation)`
**Purpose:** Checks multiple videos
- Iterates through running videos
- Calls API: `GET /checkVideo/{code}`
- Updates status
- Downloads videos
- Updates notifications

#### `h(Continuation)`
**Purpose:** Checks single/batch videos
- Queries database
- Calls API for each
- Handles responses
- Updates database

#### `g(String, String)`
**Purpose:** Creates local file path
**Returns:** `File` object
**Path:** `{cacheDir}/Soli_{timestamp}{extension}.mp4`

#### `c()`
**Purpose:** Creates notification
**Returns:** `ForegroundInfo`
**Channel:** "AI_generating_status"

#### `e(Continuation)`
**Purpose:** Main checking logic
- Gets running videos
- Checks each one
- Updates status

#### `b(P8.d, Q9.e, CheckVideoWorker)`
**Purpose:** Helper for video checking

#### `i()`
**Purpose:** Generates random string (3 chars)

---

### FCMService
**Package:** `ai.video.generator.text.video.service`

**Extends:** `FirebaseMessagingService`

**Purpose:** Handles Firebase Cloud Messaging

**Methods:**
- `c(RemoteMessage)` - Handles incoming messages
- `d(String)` - Handles new FCM token
- `onCreate()` - Initializes FCM

---

## Initializers

### AdjustInitializer
**Package:** `ai.video.generator.text.video.initializer`

**Purpose:** Initializes Adjust SDK

---

### QonversionInitializer
**Package:** `ai.video.generator.text.video.initializer`

**Purpose:** Initializes Qonversion SDK

---

## Utils

### HeaderInterceptor
**Package:** `ai.video.generator.text.video.apiservice`

**Implements:** `x` (OkHttp Interceptor)

**Method:**
- `intercept(InterfaceC0571w chain)` - Adds headers to requests

---

### LoggingCDNInterceptor
**Package:** `ai.video.generator.text.video.utils.okhttp`

**Purpose:** Logs CDN requests

---

### LoggingWorkerApiInterceptor
**Package:** `ai.video.generator.text.video.utils.okhttp`

**Purpose:** Logs worker API requests

---

### RoundCornerLayout
**Package:** `ai.video.generator.text.video.utils`

**Purpose:** Custom view with rounded corners

---

### BlurImageView
**Package:** `ai.video.generator.text.video.ui.component`

**Purpose:** Image view with blur effect

---

## Data Entities

### HistoryFeedback
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Stores user feedback history

---

### HistoryPrompt
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Stores prompt history

---

### HomeImagePrompt
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Home screen image prompts

---

### Prompts
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Saved prompts

---

### Style
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Video styles

---

### Surprise
**Package:** `ai.video.generator.text.video.data.local.entities`

**Purpose:** Surprise/featured content

---

## Model Classes

### HomeReload
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Event for home screen reload

**Properties:**
- `tabSelect: int` - Tab to select
- `isReloadPrompt: boolean` - Reload prompts flag

---

### CollectionReload
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Event for collection reload

---

### ClearInputData
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Clears input data

---

### HomeResponse
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Home screen API response

---

### ImageBaseResponse
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Base response for image APIs

---

### PromptResponse
**Package:** `ai.video.generator.text.video.model`

**Purpose:** Prompt API response

---

### NSFWResponse
**Package:** `ai.video.generator.text.video.model`

**Purpose:** NSFW check result

---

## Key Functions Summary

### Video Generation Flow

1. **User Input** → `LaunchingActivity`
   - Prompt entry
   - Aspect ratio selection
   - AI sound toggle

2. **Create Request** → `CreateVideoRequest`
   - Builds request object
   - Includes all parameters

3. **API Call** → HTTP POST
   - Headers from `HeaderInterceptor`
   - Endpoint: `{api_url}/createVideo`

4. **Response** → `VideoCreateResponse`
   - Contains `key` and `url`
   - NSFW check result

5. **Store Locally** → Room Database
   - Creates `Video` entity
   - Status: `STATUS_RUNNING (2)`

6. **Background Check** → `CheckVideoWorker`
   - Polls API: `GET /checkVideo/{key}`
   - Updates status
   - Downloads when ready

7. **Update Database** → Room
   - Status: `STATUS_SUCCESS (0)`
   - Local file path

8. **Notification** → System notification
   - Video ready
   - User can open

9. **Playback** → `DetailActivity`
   - ExoPlayer
   - Download/share options

---

### AI Art Generation Flow

1. **User Input** → `AirArt*Activity`
   - Prompt or style selection

2. **Check Limits** → SharedPreferences
   - Free/paid daily limits

3. **API Call** → HTTP POST
   - AI art endpoint

4. **Display** → `AirArtResultActivity`
   - Shows generated image

5. **Download/Share** → User options

---

## Configuration Keys (SharedPreferences)

### API Configuration
- `api_url` - Backend API base URL
- `key_api` - API authentication key
- `device_id` - Android device ID
- `pt_` - Platform token

### Rate Limits
- `max_video_free_in_hour` - Free tier hourly limit
- `max_video_premium_in_hour` - Premium tier hourly limit
- `max_image_free_figurine` - Free AI art limit
- `max_image_paid_figurine` - Paid AI art limit
- `max_current_generate` - Concurrent limit
- `ai_art_free_in_day` - Daily free AI art
- `max_gen_image_to_video_per_day` - I2V daily limit

### NSFW Settings
- `nsfw_threshold` - General threshold
- `nsfw_threshold_hentai` - Hentai threshold
- `nsfw_threshold_porn` - Porn threshold
- `nsfw_threshold_sexy` - Suggestive threshold

### Feature Flags
- `i2v_feature` - Image-to-video enabled
- `is_figurine_3d` - 3D figurine enabled
- `is_realistic_figurine` - Realistic figurine enabled
- `is_giant_figurine` - Giant figurine enabled

### Paywall
- `pw_type` - Paywall type
- `pw_after_splash` - Show after splash
- `pw_no_offer` - No offer mode
- `verify_subs_token` - Subscription token

### Ad Configuration
- `app_resume_ads` - Show on resume
- `splash_inters` - Interstitial on splash
- `home_banner_collapse_ads` - Collapsible banner
- `detail_banner_collapse_ads` - Detail banner
- `launching_banner_collapse_ads` - Launching banner
- `inters_inspirations` - Interstitial on inspirations
- `inters_collection` - Interstitial on collection
- `inters_pw` - Interstitial before paywall
- `reward_detail` - Reward ad on detail
- `reward_ai_art` - Reward ad for AI art
- `native_ads_input` - Native ads in input
- `native_ads_list` - Native ads in list
- `native_ads_result` - Native ads in results

### Other
- `version_data` - Data version
- `reload_banner` - Banner reload interval
- `time_change_prompt` - Prompt change interval
- `time_close_iap` - IAP close time
- `ctry_target` - Target country
- `native_full_interval` - Native ad interval
- `date_open_app` - Last app open date
- `count_rating` - Rating dialog count

---

## API Endpoints (Inferred)

### Video Generation
- `POST {api_url}/createVideo` - Create video
- `GET {api_url}/checkVideo/{key}` - Check status
- `GET {api_url}/video/{key}` - Get video details

### Home Data
- `GET {api_url}/home` - Get home screen data

### NSFW Check
- `POST {api_url}/checkNSFW` - Check content

### AI Art
- `POST {api_url}/createImage` - Create AI art
- `POST {api_url}/imageToVideo` - Convert image to video

---

## Status Codes

### Video Status
- `0` - STATUS_SUCCESS
- `1` - STATUS_FAILED
- `2` - STATUS_RUNNING
- `3` - STATUS_NSFW
- `4` - STATUS_SERVER_NSFW

### API Response Codes
- `200` - Success
- Other codes - Error (specific codes unknown)

---

## Key Differences from App 1

1. **Architecture:** Traditional Activities vs Compose
2. **Storage:** Room (local) vs Firestore (cloud)
3. **API:** Direct HTTP vs Firebase Functions
4. **Updates:** Polling vs Real-time listeners
5. **Features:** More features (AI art, I2V) vs Focused (video effects)
6. **Security:** API key in SharedPreferences vs Server-side
7. **Monetization:** Ads + Qonversion vs Credits + Superwall

---

This quick reference covers all major classes and functions in App 2. For detailed analysis, see `APP2_COMPLETE_REVERSE_ENGINEERING.md`.

