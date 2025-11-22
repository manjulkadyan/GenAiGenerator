package com.manjul.genai.videogenerator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge

/**
 * Subscription plan card component for landing page.
 * Displays plan details with "Popular" badge if applicable.
 */
@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Exact colors from design - selected cards should look like popular card
    val backgroundColor = when {
        plan.isPopular || isSelected -> Color(0xFFE8E8E8) // Light grey/white for popular/selected (exact from image)
        else -> Color(0xFF1F1F1F) // Dark grey for unselected
    }
    
    val borderColor = when {
        plan.isPopular || isSelected -> Color(0xFFE8E8E8) // White border for popular/selected
        else -> Color.White.copy(alpha = 0.5f) // White border with opacity for unselected
    }
    
    val textColor = if (plan.isPopular || isSelected) Color.Black else Color.White
    val secondaryTextColor = if (plan.isPopular || isSelected) Color(0xFF6B7280) else Color(0xFF9CA3AF)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Popular badge - exact match from design (purple badge at top)
            if (plan.isPopular) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    CustomStatusBadge(
                        text = "Popular",
                        backgroundColor = Color(0xFF4B3FFF), // Exact purple from design
                        textColor = Color.White
                    )
                }
            }
            
            // Period (Weekly)
            Text(
                text = plan.period,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            
            // Credits
            Text(
                text = "${plan.credits} Credits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 16.sp
            )
            
            // Price
            Text(
                text = plan.price,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

