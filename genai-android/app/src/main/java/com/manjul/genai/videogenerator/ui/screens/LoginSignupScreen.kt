package com.manjul.genai.videogenerator.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.manjul.genai.videogenerator.data.auth.AuthManager
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppPrimaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppSecondaryButton
import com.manjul.genai.videogenerator.ui.designsystem.components.buttons.AppTextButton
import com.manjul.genai.videogenerator.ui.designsystem.components.inputs.AppTextField
import com.manjul.genai.videogenerator.utils.AnalyticsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSignupScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Handle system back button - navigate back instead of closing app
    BackHandler(enabled = true) {
        Log.d("LoginSignupScreen", "Back button pressed - navigating back")
        onBackClick()
    }
    
    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsManager.trackScreenView("LoginSignup")
    }
    
    // Form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    isLoading = true
                    errorMessage = null
                    
                    val googleAccountName = account.displayName ?: ""
                    val googleAccountEmail = account.email ?: ""
                    
                    val authResult = if (AuthManager.isAnonymousUser()) {
                        AuthManager.linkWithGoogle(idToken, googleAccountName, googleAccountEmail)
                    } else {
                        AuthManager.signInWithGoogle(idToken, googleAccountName, googleAccountEmail)
                    }
                    
                    authResult.fold(
                        onSuccess = {
                            isLoading = false
                            onLoginSuccess()
                        },
                        onFailure = { error ->
                            isLoading = false
                            errorMessage = error.message ?: "Google sign-in failed"
                        }
                    )
                }
            } catch (e: ApiException) {
                isLoading = false
                if (e.statusCode != 12500) { // Not cancelled
                    errorMessage = "Google sign-in failed"
                }
            }
        }
    }
    
    // Start Google Sign-In
    val startGoogleSignIn: () -> Unit = {
        scope.launch {
            try {
                val webClientId = "407437371864-9dkicne9lg7l8l816jbut5dup9qs7sus.apps.googleusercontent.com"
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                
                // Sign out from Google Sign-In client first to ensure fresh account picker
                // This prevents the "already signed in" issue when user previously signed in with Google
                try {
                    googleSignInClient.signOut().await()
                    Log.d("LoginSignupScreen", "Signed out from Google Sign-In client before new sign-in")
                } catch (e: Exception) {
                    Log.w("LoginSignupScreen", "Failed to sign out from Google Sign-In client", e)
                }
                
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } catch (e: Exception) {
                errorMessage = "Failed to start Google Sign-In"
            }
        }
    }
    
    // Email sign-in/sign-up
    val signInWithEmail: () -> Unit = {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password"
        } else {
            scope.launch {
                isLoading = true
                errorMessage = null
                
                val result = if (isSignUpMode) {
                    AuthManager.createAccountWithEmail(email, password)
                } else {
                    AuthManager.signInWithEmail(email, password)
                }
                
                result.fold(
                    onSuccess = {
                        isLoading = false
                        onLoginSuccess()
                    },
                    onFailure = { error ->
                        isLoading = false
                        errorMessage = when {
                            error.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                                "Invalid email or password"
                            error.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                                "An account already exists with this email"
                            error.message?.contains("weak-password", ignoreCase = true) == true ->
                                "Password should be at least 6 characters"
                            error.message?.contains("invalid-email", ignoreCase = true) == true ->
                                "Please enter a valid email address"
                            else -> error.message ?: "Sign in failed"
                        }
                    }
                )
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSignUpMode) "Create Account" else "Sign In",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.PrimaryPurple
                )
            )
        },
        containerColor = Color(0xFF0F0720)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = if (isSignUpMode) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Text(
                text = "Save your progress and access your videos across devices",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email field
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "Enter your email",
                isError = errorMessage != null && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = AppColors.TextSecondary) },
                placeholder = { Text("Enter your password", color = AppColors.TextSecondary.copy(alpha = 0.5f)) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = AppColors.TextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.BorderSelected,
                    unfocusedBorderColor = AppColors.BorderLight,
                    focusedContainerColor = AppColors.SurfaceDark.copy(alpha = 0.3f),
                    unfocusedContainerColor = AppColors.SurfaceDark.copy(alpha = 0.2f),
                    cursorColor = AppColors.BorderSelected,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.TextPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null && password.isNotBlank()
            )
            
            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.StatusError
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sign In / Sign Up button
            AppPrimaryButton(
                text = when {
                    isLoading -> if (isSignUpMode) "Creating account..." else "Signing in..."
                    isSignUpMode -> "Create Account"
                    else -> "Sign In"
                },
                onClick = signInWithEmail,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                isLoading = isLoading
            )
            
            // Toggle between Sign In and Sign Up
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                AppTextButton(
                    text = if (isSignUpMode) "Sign In" else "Sign Up",
                    onClick = {
                        isSignUpMode = !isSignUpMode
                        errorMessage = null
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AppColors.BorderLight)
                )
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AppColors.BorderLight)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Google Sign-In button
            AppSecondaryButton(
                text = if (isLoading) "Signing in..." else "Continue with Google",
                onClick = startGoogleSignIn,
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
