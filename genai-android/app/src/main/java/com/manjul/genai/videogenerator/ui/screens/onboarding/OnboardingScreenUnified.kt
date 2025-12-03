package com.manjul.genai.videogenerator.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.manjul.genai.videogenerator.ui.components.onboarding.*

/**
 * SINGLE reusable onboarding screen component
 * Used for ALL onboarding pages - just pass different data!
 * 
 * CUSTOMIZATION GUIDE:
 * ====================
 * This ONE screen handles ALL onboarding pages.
 * Data comes from onboardingConfig.json or Firebase Firestore.
 * 
 * TO ADD YOUR OWN SCREENSHOTS:
 * 1. Upload screenshots to Firebase Storage: /onboarding/ folder
 * 2. Update URLs in onboardingConfig.json:
 *    - Line 9: premium.jpg
 *    - Line 19: create.jpg
 *    - Line 29: library.jpg
 * 3. OR update Firestore: config/onboarding collection
 * 
 * Recommended screenshot size: 390x844px (PNG or JPG)
 */
@Composable
fun OnboardingPageScreen(
    imageUrl: String? = null,
    title: String,
    description: String,
    isFirstPage: Boolean = false,
    isLastPage: Boolean = false,
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingLayout(
        currentPage = currentPage,
        totalPages = totalPages,
        mockupContent = {
            ScreenshotImage(
                imageUrl = imageUrl,
                contentDescription = title
            )
        },
        title = title,
        description = description,
        buttons = {
            NavigationButtons(
                onNext = if (!isLastPage) onNext else null,
                onSkip = if (!isLastPage) onSkip else null,
                onGetStarted = if (isLastPage) onNext else null
            )
        }
    )
}

/**
 * Previews for all 3 screens
 */
@Preview(name = "Page 1 - With Logo", showSystemUi = true)
@Composable
private fun OnboardingPage1Preview() {
    OnboardingPageScreen(
        imageUrl = null,
        title = "Upgrade to Premium",
        description = "Get more features and capabilities",
        isFirstPage = true,
        isLastPage = false,
        currentPage = 0,
        onNext = {},
        onSkip = {},
        totalPages = 1
    )
}

@Preview(name = "Page 2 - Middle", showSystemUi = true)
@Composable
private fun OnboardingPage2Preview() {
    OnboardingPageScreen(
        imageUrl = null,
        title = "Create Everything!",
        description = "Turn your imagination into stunning videos",
        isFirstPage = false,
        isLastPage = false,
        currentPage = 1,
        onNext = {},
        onSkip = {},
        totalPages = 1
    )
}

@Preview(name = "Page 3 - Last Page", showSystemUi = true)
@Composable
private fun OnboardingPage3Preview() {
    OnboardingPageScreen(
        imageUrl = null,
        title = "Manage Your Creations!",
        description = "Organize and share your masterpieces",
        isFirstPage = false,
        isLastPage = true,
        currentPage = 2,
        onNext = {},
        onSkip = {},
        totalPages = 1
    )
}

