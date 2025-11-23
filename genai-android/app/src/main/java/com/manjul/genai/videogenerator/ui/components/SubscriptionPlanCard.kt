package com.manjul.genai.videogenerator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge

/**
 * Subscription plan card component for landing page.
 * Matches the exact design: Weekly -> Large Number -> Credits -> Price
 * All cards have the same height for consistent layout.
 */
@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Exact colors from design
    val backgroundColor = when {
        isSelected || plan.isPopular -> Color(0xFFE8E8E8) // Light grey for popular/selected
        else -> Color(0xFF1F1F1F) // Black/dark grey for unselected
    }
    
    val borderColor = when {
        isSelected || plan.isPopular -> Color(0xFFD1D1D1) // Slightly darker grey border for selected
        else -> Color.White.copy(alpha = 1.0f) // White border for unselected
    }
    
    val textColor = if (isSelected || plan.isPopular) Color.Black else Color.White
    val secondaryTextColor = if (isSelected || plan.isPopular) Color(0xFF6B7280) else Color(0xFF9CA3AF)

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
    ) {
        // Popular badge - positioned at top center, overlapping the card edge
        if (plan.isPopular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp) // Overlap the top edge
            ) {
                CustomStatusBadge(
                    text = "Popular",
                    backgroundColor = Color(0xFF4B3FFF), // Exact purple from design
                    textColor = Color.White
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Period (Weekly) - top
            Text(
                text = plan.period,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            
            // Large Credits Number - center (much larger and bold)
            Text(
                text = "${plan.credits}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 36.sp, // Very large, bold number
                lineHeight = 44.sp
            )
            
            // "Credits" text - below number
            Text(
                text = "Credits",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
            
            // Price - bottom
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SubscriptionPlanCardPreview() {
    Column(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Standard card (60 credits)
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "weekly_60_credits",
                    credits = 60,
                    price = "$9.99",
                    period = "Weekly",
                    isPopular = false
                ),
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            
            // Popular card (100 credits)
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "weekly_100_credits",
                    credits = 100,
                    price = "$14.99",
                    period = "Weekly",
                    isPopular = true
                ),
                isSelected = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            
            // Standard card (150 credits)
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "weekly_150_credits",
                    credits = 150,
                    price = "$19.99",
                    period = "Weekly",
                    isPopular = false
                ),
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

