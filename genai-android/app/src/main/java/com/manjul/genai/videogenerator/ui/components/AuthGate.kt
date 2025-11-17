package com.manjul.genai.videogenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manjul.genai.videogenerator.data.auth.AuthManager

private sealed interface AuthStatus {
    data object Loading : AuthStatus
    data object Authenticated : AuthStatus
    data class Error(val message: String) : AuthStatus
}

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    var retryKey by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<AuthStatus>(AuthStatus.Loading) }

    LaunchedEffect(retryKey) {
        val result = AuthManager.ensureAnonymousUser()
        status = result.fold(
            onSuccess = { AuthStatus.Authenticated },
            onFailure = { AuthStatus.Error(it.message ?: "Unable to sign in") }
        )
    }

    when (val state = status) {
        AuthStatus.Loading -> LoadingState()
        AuthStatus.Authenticated -> content()
        is AuthStatus.Error -> ErrorState(state.message) {
            status = AuthStatus.Loading
            retryKey++
        }
    }
}

@Composable
private fun LoadingState() {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = "Signing you in..."
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Authentication Failed", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
