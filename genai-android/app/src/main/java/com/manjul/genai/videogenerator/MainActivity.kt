package com.manjul.genai.videogenerator

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import com.manjul.genai.videogenerator.ui.components.AuthGate
import com.manjul.genai.videogenerator.ui.screens.GenAiRoot
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}
