package com.manjul.genai.videogenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.manjul.genai.videogenerator.ui.components.AuthGate
import com.manjul.genai.videogenerator.ui.screens.GenAiRoot
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenAiVideoTheme {
                Surface {
                    AuthGate {
                        GenAiRoot()
                    }
                }
            }
        }
    }
}
