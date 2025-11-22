package com.manjul.genai.videogenerator.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.data.model.VideoJobStatus
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.VideoGenerateViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
class GeneratingActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            GenAiVideoTheme {
                val generateViewModel: VideoGenerateViewModel = viewModel(factory = VideoGenerateViewModel.Factory)
                val generateState by generateViewModel.state.collectAsState()
                
                val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
                val jobs by historyViewModel.jobs.collectAsState()
                
                // Monitor job completion
                LaunchedEffect(jobs) {
                    // Wait a bit for the job to appear in Firestore
                    delay(1500)
                    
                    // Get the latest job
                    val latestJob = jobs.firstOrNull()
                    
                    if (latestJob != null && latestJob.status == VideoJobStatus.COMPLETE) {
                        // Job completed - launch ResultsActivity
                        val intent = Intent(this@GeneratingActivity, ResultsActivity::class.java).apply {
                            putExtra(ResultsActivity.EXTRA_JOB_ID, latestJob.id)
                        }
                        startActivity(intent)
                        finish() // Close GeneratingActivity
                    } else if (latestJob != null && latestJob.status == VideoJobStatus.FAILED) {
                        // Job failed - show error (error will be shown in GeneratingScreen)
                        // Don't navigate away, let user see the error and retry
                    }
                }
                
                GeneratingScreen(
                    modifier = Modifier.fillMaxSize(),
                    statusMessage = generateState.uploadMessage,
                    errorMessage = generateState.errorMessage,
                    onCancel = {
                        finish()
                    },
                    onRetry = if (generateState.errorMessage != null) {
                        {
                            generateViewModel.dismissMessage()
                            generateViewModel.generate()
                        }
                    } else null
                )
            }
        }
    }
}

