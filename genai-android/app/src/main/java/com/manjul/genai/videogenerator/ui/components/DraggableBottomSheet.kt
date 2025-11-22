package com.manjul.genai.videogenerator.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Persistent draggable bottom sheet that is always visible and cannot be dismissed.
 * Starts at partial height (default 30%) and can be dragged up to full screen.
 * This is NOT a modal - it's a permanent part of the layout.
 * 
 * @param modifier Modifier to be applied to the sheet container
 * @param initialHeightPercent Initial height as percentage of screen (default 0.3 = 30%)
 * @param content The content to display inside the scrollable sheet
 */
@Composable
fun DraggableBottomSheet(
    modifier: Modifier = Modifier,
    initialHeightPercent: Float = 0.3f, // Start at 30% of screen - always visible
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightDp = configuration.screenHeightDp
    
    // Calculate heights - can expand to full screen (100%)
    val minHeightDp = screenHeightDp * initialHeightPercent
    val maxHeightDp = screenHeightDp.toFloat() // Full screen
    
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Current height can go from min to max (full screen)
    val currentHeightDp = (minHeightDp + dragOffset).coerceIn(minHeightDp, maxHeightDp)
    val animatedHeight by animateDpAsState(
        targetValue = currentHeightDp.dp,
        animationSpec = if (isDragging) tween(0) else tween(300),
        label = "sheetHeight"
    )
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Sheet positioned from bottom, can expand to full screen
        // Entire sheet is draggable, not just the handle
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(animatedHeight)
                .background(
                    color = Color(0xFF1F1F1F),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            isDragging = false
                            // Snap to nearest position: initial, middle, or full screen
                            // Allow staying at full screen if dragged close to top
                            val currentPercent = currentHeightDp / maxHeightDp
                            dragOffset = when {
                                currentPercent > 0.85f -> maxHeightDp - minHeightDp // Full screen - stay there
                                currentPercent < 0.4f -> 0f // Back to initial
                                else -> dragOffset // Keep current position - don't force snap
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset -= dragAmount.y / density.density
                            dragOffset = dragOffset.coerceIn(0f, maxHeightDp - minHeightDp)
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                // Content - NOT scrollable, sheet itself is draggable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

