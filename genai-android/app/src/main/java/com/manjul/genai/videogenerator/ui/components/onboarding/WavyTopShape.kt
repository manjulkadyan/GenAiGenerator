package com.manjul.genai.videogenerator.ui.components.onboarding

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Custom shape for the white card with downward curve at top
 * Creates a smooth dip/curve downward like in the Imagify design
 */
class WavyTopShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Start from top-left corner
            moveTo(0f, 0f)
            
            // Create downward curve at top (dips down in the middle)
            cubicTo(
                x1 = size.width * 0.25f,    // First control point X
                y1 = size.height * 0.1f,   // First control point Y (dips DOWN)
                x2 = size.width * 0.75f,    // Second control point X
                y2 = size.height * 0.1f,   // Second control point Y (dips DOWN)
                x3 = size.width,             // End point X (right side)
                y3 = 0f                      // End point Y (back to top)
            )
            
            // Right edge going down
            lineTo(size.width, size.height)
            
            // Bottom edge
            lineTo(0f, size.height)
            
            // Close path back to start
            close()
        }
        return Outline.Generic(path)
    }
}

