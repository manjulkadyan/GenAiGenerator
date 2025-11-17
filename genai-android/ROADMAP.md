# GenAI Video – Development Roadmap (Hours Not Weeks)

This roadmap captures the sprint-style plan we discussed. Each block is meant to be achievable within roughly an hour, so we can iterate fast and mark progress as we go.

---

## Hour 0–1: Scaffold & Tooling ✅
- [x] Create Compose-based Android project (`genai-android/`) with Firebase BoM + Navigation deps.
- [x] Add `GenAiApp` initializing Firebase + Play Integrity App Check.
- [x] Build placeholder bottom-nav shell (Models / Generate / History / Profile) using Compose.
- [x] Add `google-services.json` and run the Google Services Gradle plugin.

## Hour 1–2: Firebase & Auth Setup
- [x] Create Firebase project, enable Auth (anonymous), Firestore, Storage, Functions, Analytics.
- [x] Configure App Check + download `google-services.json`.
- [x] Add lightweight Auth manager that signs in anonymously on launch and caches the UID.
- [x] Confirm analytics/logging events fire on first launch.

## Hour 2–3: Data Layer + MVVM Skeleton
- [x] Define core data classes (`AIModel`, `VideoJob`, `UserCredits`, etc.).
- [x] Implement repositories (`VideoFeatureRepository`, `VideoGenerateRepository`, `HistoryRepository`, `CreditsRepository`).
- [x] Create ViewModels (`AIModelsViewModel`, `VideoGenerateViewModel`, `HistoryViewModel`, `CreditsViewModel`) with stubbed data.
- [x] Wire placeholder screens to these ViewModels so we can see mock data flowing end-to-end.

## Hour 3–4: Firestore Integration
- [x] Seed Firestore collections (`video_features`, `users/{uid}`, `users/{uid}/jobs`).
- [x] Connect repositories to Firestore reads/writes.
- [x] Listen to credits (`users/{uid}.credits`) and job history updates in real time.
- [x] Harden offline/cache behavior (e.g., `Source.SERVER` fallbacks).

## Hour 4–5: Storage + Callable Functions
- [x] Implement `VideoGenerateRepository.uploadReferenceFrame` to Firebase Storage paths (`users/{uid}/inputs/{uuid}.jpeg`).
- [x] Stub callable function request payloads for `callReplicateVeoAPIV2` (+ effect variant).
- [x] Add client coroutine flow: upload frames → call function → show progress states.
- [x] Log upload/generation failures with actionable error strings in the UI.

## Hour 5–6: Cloud Functions (Server)
- [x] Set up Firebase Functions project (TypeScript).
- [x] Implement `callReplicateVeoAPIV2`: validate credits, call Replicate with `replicateName`, write job doc, return job ID.
- [x] Implement `generateVideoEffect` (optional) with similar structure.
- [x] Unit-test callable functions locally and deploy to Firebase.

## Hour 6–7: Credits, Paywall, Monetization
- [ ] Implement credit deduction inside Functions and surface the new balance to Firestore.
- [ ] Integrate Superwall (or Play Billing) for paywall + subscription events.
- [ ] Update client to react to `paywall_type` from `app/config`.
- [ ] Add “insufficient credits” UI flow and callbacks to trigger paywall.

## Hour 7–8: UX Polish & Notifications
- [ ] Replace placeholders with production Compose components (prompt editors, ratio/duration chips, history cards).
- [ ] Add success/error dialogs, upload status chips, and blocked states for missing frames.
- [ ] Integrate FCM to notify when jobs finish; deep-link to the History tab.
- [ ] Run end-to-end tests: prompt → function → Firestore updates → UI state → credits change.

## Hour 8+: Hardening & Extras
- [ ] App Check enforcement in Functions + Storage rules.
- [ ] License/attestation placeholder (if we mimic PairIP-like checks).
- [ ] Analytics instrumentation for major events (prompt submitted, job success/failure, paywall shown).
- [ ] Prepare release assets (icons, theming, versioning) and document deployment steps.

---

We’ll tick each item as we complete it. Let me know when you’re ready to move to the next hour block.***
