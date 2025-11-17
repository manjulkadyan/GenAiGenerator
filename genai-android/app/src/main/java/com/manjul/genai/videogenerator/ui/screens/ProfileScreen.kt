package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.ui.viewmodel.CreditsViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: CreditsViewModel = viewModel(factory = CreditsViewModel.Factory)
) {
    val credits by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Credits", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "${credits.credits}",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = { /* TODO: hook up paywall */ }) {
            Text("Add Credits")
        }
    }
}
