package com.manjul.genai.videogenerator

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.manjul.genai.videogenerator.ui.components.AuthGate
import com.manjul.genai.videogenerator.ui.screens.GenAiRoot
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.data.repository.BillingRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val billingRepository: BillingRepository by lazy {
        BillingRepository(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize billing connection once
        lifecycleScope.launch {
            billingRepository.initialize().collect { /* no-op */ }
        }
        setContent {
            GenAiVideoTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    AuthGate {
                        GenAiRoot()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val processed = billingRepository.reprocessExistingPurchases()
                processed.onSuccess {
                    if (it > 0) {
                        Log.d(
                            "MainActivity",
                            "Reprocessed $it purchase(s) on resume",
                        )
                    }
                }.onFailure { error ->
                    android.util.Log.e(
                        "MainActivity",
                        "Failed to reprocess purchases on resume",
                        error,
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error reprocessing purchases", e)
            }
        }
    }
}
