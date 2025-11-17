# AI Video Generator App - Development Roadmap

## Project Goal
Build an AI video generation app using:
- **Android (Kotlin + Jetpack Compose)**
- **Firebase Functions** (backend)
- **Firebase Firestore** (database)
- **Firebase Storage** (file storage)
- **Replicate API** (AI model execution)

## Timeline: Hours (Not Weeks!)

---

## Phase 1: Project Setup & Foundation (1-2 hours)

### Step 1.1: Android Project Setup ✅
- [x] Create Android project with Jetpack Compose
- [x] Configure build.gradle dependencies (Compose, Navigation, Firebase BoM, etc.)
- [x] Set up initial project structure (Application class, placeholder nav shell)
- [ ] Configure Firebase project
- [ ] Add Firebase to Android app (`google-services.json` + Gradle plugin)

### Step 1.2: Firebase Setup
- [ ] Create Firebase project
- [ ] Enable Firestore
- [ ] Enable Firebase Storage
- [ ] Enable Firebase Functions
- [ ] Enable Firebase Authentication
- [ ] Enable Firebase Analytics
- [ ] Download google-services.json

### Step 1.3: Project Structure
- [ ] Create package structure:
  - `data/models` - Data classes
  - `data/repository` - Repository pattern
  - `ui/screens` - Compose screens
  - `ui/viewmodel` - ViewModels
  - `ui/components` - Reusable components
  - `utils` - Utilities
- [ ] Set up dependency injection (Hilt)
- [ ] Configure navigation

---

## Phase 2: Core Features (2-3 hours)

### Step 2.1: Authentication
- [ ] Firebase Anonymous Auth
- [ ] User ID management
- [ ] User session handling

### Step 2.2: Data Models
- [ ] `User` model
- [ ] `Video` model
- [ ] `AIModel` model
- [ ] `VideoRequest` model
- [ ] `VideoResponse` model

### Step 2.3: Repository Layer
- [ ] `VideoRepository` interface
- [ ] `VideoRepositoryImpl` (Firebase)
- [ ] Image upload to Firebase Storage
- [ ] Video generation API call

### Step 2.4: Firebase Functions Setup
- [ ] Create `callReplicateVeoAPI` function
- [ ] Configure Replicate API integration
- [ ] Set up environment variables
- [ ] Test function locally

---

## Phase 3: UI Implementation (2-3 hours)

### Step 3.1: Main Screen
- [ ] Home screen with prompt input
- [ ] Aspect ratio selector
- [ ] Duration selector
- [ ] Generate button
- [ ] Loading states

### Step 3.2: Video Generation Screen
- [ ] Prompt input field
- [ ] Settings (aspect ratio, duration)
- [ ] Preview area
- [ ] Progress indicator
- [ ] Status display

### Step 3.3: Results Screen
- [ ] Video player
- [ ] Download button
- [ ] Share button
- [ ] Regenerate option

### Step 3.4: Navigation
- [ ] Set up Navigation Compose
- [ ] Define routes
- [ ] Implement navigation flow

---

## Phase 4: Firebase Functions Implementation (1-2 hours)

### Step 4.1: Replicate API Integration
- [ ] Install Replicate SDK in Functions
- [ ] Create video generation function
- [ ] Handle API responses
- [ ] Error handling

### Step 4.2: Firestore Integration
- [ ] Create video documents
- [ ] Update status in real-time
- [ ] Listen for status changes

### Step 4.3: Storage Integration
- [ ] Upload input images
- [ ] Store video URLs
- [ ] Generate download URLs

---

## Phase 5: Real-time Updates (1 hour)

### Step 5.1: Firestore Listeners
- [ ] Listen to video status changes
- [ ] Update UI in real-time
- [ ] Handle status updates

### Step 5.2: Notifications
- [ ] Firebase Cloud Messaging setup
- [ ] Send notification when video ready
- [ ] Handle notification clicks

---

## Phase 6: Polish & Testing (1 hour)

### Step 6.1: Error Handling
- [ ] Network error handling
- [ ] API error handling
- [ ] User-friendly error messages

### Step 6.2: UI Polish
- [ ] Loading animations
- [ ] Error states
- [ ] Empty states
- [ ] Success states

### Step 6.3: Testing
- [ ] Test video generation flow
- [ ] Test error scenarios
- [ ] Test real-time updates

---

## Technology Stack

### Android
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Coroutines:** Kotlin Coroutines
- **State:** StateFlow / Compose State

### Firebase
- **Functions:** Node.js (TypeScript)
- **Database:** Firestore
- **Storage:** Firebase Storage
- **Auth:** Firebase Auth (Anonymous)
- **Analytics:** Firebase Analytics
- **Messaging:** FCM

### Third-Party
- **AI Model:** Replicate API (Veo)
- **Image Loading:** Coil
- **Video Player:** ExoPlayer

---

## Project Structure

```
app/
├── src/main/java/com/yourpackage/
│   ├── data/
│   │   ├── models/
│   │   │   ├── User.kt
│   │   │   ├── Video.kt
│   │   │   └── AIModel.kt
│   │   ├── repository/
│   │   │   ├── VideoRepository.kt
│   │   │   └── VideoRepositoryImpl.kt
│   │   └── local/
│   │       └── AppDatabase.kt (Room - optional)
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── GenerateScreen.kt
│   │   │   └── ResultScreen.kt
│   │   ├── viewmodel/
│   │   │   ├── HomeViewModel.kt
│   │   │   └── GenerateViewModel.kt
│   │   └── components/
│   │       ├── PromptInput.kt
│   │       └── VideoPlayer.kt
│   ├── utils/
│   │   └── Constants.kt
│   └── MainActivity.kt
│
functions/
├── src/
│   ├── index.ts
│   ├── callReplicateVeoAPI.ts
│   └── utils/
│       └── replicate.ts
├── package.json
└── tsconfig.json
```

---

## Next Steps

1. ✅ Create roadmap (DONE)
2. ⏳ Set up Android project
3. ⏳ Configure Firebase
4. ⏳ Implement core features
5. ⏳ Deploy Firebase Functions
6. ⏳ Test end-to-end

---

## Progress Tracking

- [x] Roadmap created
- [ ] Phase 1: Project Setup
- [ ] Phase 2: Core Features
- [ ] Phase 3: UI Implementation
- [ ] Phase 4: Firebase Functions
- [ ] Phase 5: Real-time Updates
- [ ] Phase 6: Polish & Testing

---

Let's start with **Step 1.1: Android Project Setup**!
