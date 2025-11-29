# Credit Check Navigation Fix

## Summary
Modified the GenerateScreen to check for insufficient credits **before** attempting video generation and show BuyCreditsScreen as an **overlay** on top of any screen with a popup dialog explaining the issue.

## Key Architecture Change

**BuyCreditsScreen is now shown as an overlay**, not a navigation route:
- Can appear on top of **GenerateScreen**, **ProfileScreen**, **HistoryScreen**, or any other screen
- **No route change** when showing BuyCreditsScreen
- Acts like a full-screen dialog/modal
- Shows insufficients credits dialog when triggered from Generate button

## Changes Made

### 1. GenerateScreen.kt

#### Added `onBuyCreditsClick` callback parameter with credits parameter
- Added new callback parameter to `GenerateScreen` composable function: `onBuyCreditsClick: (requiredCredits: Int) -> Unit`
- This callback is invoked when user tries to generate without sufficient credits
- Passes the required credits amount to display in the dialog

#### Modified Generate Button Click Logic (Line 236-249)
**Before:**
```kotlin
onGenerateClick = {
    if (!isSubmitting && state.canGenerate && !state.isGenerating && state.uploadMessage == null) {
        isSubmitting = true
        viewModel.dismissMessage()
        viewModel.generate()
    }
}
```

**After:**
```kotlin
onGenerateClick = {
    if (!isSubmitting && state.canGenerate && !state.isGenerating && state.uploadMessage == null) {
        // Check if user has enough credits
        val estimatedCost = state.estimatedCost
        if (creditsState.credits < estimatedCost) {
            // Navigate to BuyCreditsScreen if insufficient credits
            onBuyCreditsClick(estimatedCost) // Pass required credits
        } else {
            isSubmitting = true
            viewModel.dismissMessage()
            viewModel.generate()
        }
    }
}
```

#### Modified Error Dialog Logic (Line 258-293)
**Before:**
```kotlin
if (!state.isGenerating && state.errorMessage != null) {
    AppDialog(
        onDismissRequest = viewModel::dismissMessage,
        title = "Error"
    ) {
        Text(text = state.errorMessage ?: "Unknown error", ...)
        // ... OK button
    }
}
```

**After:**
```kotlin
if (!state.isGenerating && state.errorMessage != null) {
    // Only show error dialog for non-credit related errors
    // Credit-related errors are handled by navigating to BuyCreditsScreen
    val errorMessage = state.errorMessage ?: ""
    if (!errorMessage.contains("Insufficient credits", ignoreCase = true) && 
        !errorMessage.contains("not enough credits", ignoreCase = true)) {
        AppDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = "Error"
        ) {
            Text(text = errorMessage.ifBlank { "Unknown error" }, ...)
            // ... OK button
        }
    } else {
        // For credit errors, dismiss the message and navigate to BuyCreditsScreen
        LaunchedEffect(errorMessage) {
            viewModel.dismissMessage()
            onBuyCreditsClick(state.estimatedCost)
        }
    }
}
```

### 2. BuyCreditsScreen.kt

#### Added Parameters for Insufficient Credits Dialog
```kotlin
fun BuyCreditsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
    showInsufficientCreditsDialog: Boolean = false, // NEW: Show dialog when coming from insufficient credits
    requiredCredits: Int = 0, // NEW: Credits needed for the generation
    viewModel: LandingPageViewModel = ...
)
```

#### Added Insufficient Credits Dialog (Line 502-560)
Shows a dialog when user is brought to BuyCreditsScreen due to insufficient credits:
- Yellow star icon to indicate credits issue
- Message explaining they need X credits
- "View Plans" button to dismiss and browse subscription plans

### 3. GenAiRoot.kt - Major Architecture Change

#### Added State Variables (Line 106-108)
```kotlin
var showInsufficientCreditsDialog by rememberSaveable { mutableStateOf(false) }
var requiredCredits by rememberSaveable { mutableStateOf(0) }
```

#### BuyCreditsScreen as Overlay (Line 276-362)
**Key Change**: Wrapped navigation content in a `Box` and show BuyCreditsScreen conditionally on top:

```kotlin
Scaffold(...) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
        // Main navigation content (Generate, Profile, History, Models)
        when (currentRoute) {
            AppDestination.Generate -> GenerateScreen(...)
            AppDestination.Profile -> ProfileScreen(...)
            AppDestination.History -> HistoryScreen(...)
            AppDestination.Models -> ModelsScreen(...)
        }
        
        // BuyCreditsScreen as overlay - shown on top of any screen
        if (showBuyCreditsScreen) {
            BuyCreditsScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { 
                    showBuyCreditsScreen = false
                    showInsufficientCreditsDialog = false
                    requiredCredits = 0
                },
                onPurchaseSuccess = {
                    showBuyCreditsScreen = false
                    showInsufficientCreditsDialog = false
                    requiredCredits = 0
                    currentRoute = AppDestination.Generate
                },
                showInsufficientCreditsDialog = showInsufficientCreditsDialog,
                requiredCredits = requiredCredits
            )
        }
    }
}
```

#### Updated Navigation Callbacks (No Route Change!)
```kotlin
// From GenerateScreen - clicking credits badge (no dialog)
onCreditsClick = {
    showInsufficientCreditsDialog = false
    requiredCredits = 0
    showBuyCreditsScreen = true
    // NO currentRoute change!
}

// From GenerateScreen - insufficient credits (with dialog)
onBuyCreditsClick = { credits ->
    showInsufficientCreditsDialog = true
    requiredCredits = credits
    showBuyCreditsScreen = true
    // NO currentRoute change!
}

// From ProfileScreen - clicking Buy Credits card (no dialog)
onBuyCreditsClick = { 
    showInsufficientCreditsDialog = false
    requiredCredits = 0
    showBuyCreditsScreen = true
    // NO currentRoute change!
}
```

## Behavior

### User Flow
1. **User clicks "Generate AI Video" button**
2. **Credit Check** (happens in GenerateScreen before calling ViewModel):
   - If `userCredits >= estimatedCost`: Proceed with generation
   - If `userCredits < estimatedCost`: Show BuyCreditsScreen overlay with dialog
3. **BuyCreditsScreen appears ON TOP** of GenerateScreen with a popup dialog explaining:
   - "You need X credits to generate this video"
   - "Please purchase a plan below to continue"
   - Yellow star icon indicating credits issue
4. **User dismisses dialog** and sees subscription plans
5. User can purchase credits and return to GenerateScreen
6. **Current screen stays the same** - BuyCreditsScreen is just an overlay

### Navigation Architecture
- **Overlay, NOT Route**: BuyCreditsScreen is shown as an overlay using `Box` and conditional rendering
- **No Route Change**: `currentRoute` is NOT changed when showing BuyCreditsScreen
- **Works Everywhere**: Can appear on top of GenerateScreen, ProfileScreen, HistoryScreen, or ModelsScreen
- **Back Button**: Pressing back closes the overlay and returns to the underlying screen

### Scenarios

#### Scenario 1: Insufficient Credits from Generate
1. User on GenerateScreen
2. Clicks "Generate AI Video" with insufficient credits
3. BuyCreditsScreen overlays on top with dialog
4. Back button returns to GenerateScreen (same state)

#### Scenario 2: Clicking Credits Badge
1. User on GenerateScreen
2. Clicks credits badge in header
3. BuyCreditsScreen overlays on top WITHOUT dialog
4. Back button returns to GenerateScreen

#### Scenario 3: From ProfileScreen
1. User on ProfileScreen
2. Clicks "Buy Credits" card
3. BuyCreditsScreen overlays on top WITHOUT dialog
4. Back button returns to ProfileScreen

### Fallback Handling
- If the ViewModel detects insufficient credits (backend validation), the error dialog check will also show BuyCreditsScreen overlay with dialog
- This provides double protection: frontend and backend validation

## Benefits

1. ✅ **Clear Communication**: Popup explains WHY user was brought to BuyCreditsScreen
2. ✅ **Shows Required Amount**: Dialog displays exact credits needed
3. ✅ **Better UX**: Smooth overlay transition without changing screens
4. ✅ **No Navigation Confusion**: Current screen remains underneath
5. ✅ **Works Everywhere**: Can appear from any screen in the app
6. ✅ **Cleaner Architecture**: BuyCreditsScreen is independent of navigation routes
7. ✅ **Double Protection**: Credit check happens both in UI and backend
8. ✅ **Maintains Other Errors**: Non-credit errors still show in dialog

## Testing Checklist

- [ ] Test with sufficient credits → Generation should proceed normally
- [ ] Test with insufficient credits from Generate → Should show BuyCreditsScreen overlay with dialog
- [ ] Test dialog shows correct required credits amount
- [ ] Test dialog dismissal shows subscription plans
- [ ] Test clicking credits badge from Generate → Should show BuyCreditsScreen WITHOUT dialog
- [ ] Test clicking Buy Credits from Profile → Should show BuyCreditsScreen WITHOUT dialog
- [ ] Test back button → Should return to underlying screen (Generate/Profile)
- [ ] Test backend validation → If backend returns credit error, should show BuyCreditsScreen with dialog
- [ ] Test other errors → Non-credit errors should still show error dialog
- [ ] Test purchase flow → After purchase, user should be redirected to GenerateScreen
- [ ] Test overlay appears correctly on all screens (Generate, Profile, History, Models)



