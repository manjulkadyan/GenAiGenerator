package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Custom shape for the purple gradient section with downward curve at bottom
 * Matches the WavyTopShape to create seamless transition
 */
class WavyBottomShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Start from top-left corner
            moveTo(0f, 0f)
            
            // Top edge
            lineTo(size.width, 0f)
            
            // Right edge going down
            lineTo(size.width, size.height)
            
            // Create downward curve at bottom (dips down in the middle)
            cubicTo(
                x1 = size.width * 0.75f,    // First control point X (coming from right)
                y1 = size.height + (size.height * 0.1f),  // Control point Y (extends DOWN)
                x2 = size.width * 0.25f,    // Second control point X
                y2 = size.height + (size.height * 0.1f),  // Control point Y (extends DOWN)
                x3 = 0f,                     // End point X (left side)
                y3 = size.height             // End point Y (back to bottom)
            )
            
            // Close path back to start
            close()
        }
        return Outline.Generic(path)
    }
}

