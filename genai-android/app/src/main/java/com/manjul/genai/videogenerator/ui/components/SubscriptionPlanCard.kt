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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan

/**
 * Subscription plan card component for landing page.
 * Matches the exact design: Period/Type -> Large Number -> Credits -> Price -> Per-credit cost
 * All cards have the same height for consistent layout.
 * Can be used for both subscriptions and one-time purchases.
 */
@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    periodText: String? = null, // Override period text (e.g., "One Time" instead of "Weekly")
    showPerCreditCost: Boolean = true // Always show per-credit cost by default
) {
    // All cards have the same styling - only difference is the badge for popular
    val backgroundColor = if (isSelected) {
        Color(0xFFE8E8E8) // Light grey for selected
    } else {
        Color(0xFF1F1F1F) // Black/dark grey for unselected
    }
    
    val borderColor = if (isSelected) {
        Color(0xFFD1D1D1) // Grey border for selected
    } else {
        Color.White.copy(alpha = 1.0f) // White border for unselected
    }
    
    val textColor = if (isSelected) Color.Black else Color.White
    val secondaryTextColor = if (isSelected) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Card content - all cards look the same
        Box(
            modifier = Modifier
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
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Period (Weekly/One Time) - top
            Text(
                text = periodText ?: plan.period,
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
                fontSize = 32.sp, // Very large, bold number
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
            
            // Per-credit cost - always shown for consistent height
            val priceValue = plan.price.replace("$", "").replace(",", "").toDoubleOrNull()
            val perCredit = priceValue?.div(plan.credits)
            
            // Always render this to maintain consistent card height
            Text(
                text = if (showPerCreditCost && perCredit != null) {
                    "$${String.format("%.3f", perCredit)}/credit"
                } else {
                    " " // Empty space to maintain height
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    Color(0xFF10B981) // Green for selected (shows savings)
                } else {
                    Color(0xFF9CA3AF) // Grey for unselected
                },
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                modifier = Modifier.height(16.dp) // Fixed height for consistency
            )
            }
        }

        // Popular badge - positioned ABOVE the card (rendered last, appears on top)
        if (plan.isPopular || plan.isBestValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp) // Position above card, showing border behind it
            ) {
                // Gradient badge with horizontal purple gradient (darker left to lighter right)
                val bgColor1= if (plan.isPopular) Color(0xFF2C20D9) else Color(0xFF009F6B)
                val bgColor2= if (plan.isPopular) Color(0xFF9089F6) else Color(0xFFACE1AF)

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    bgColor1, // Darker purple on the left
                                    bgColor2  // Lighter purple on the right
                                )
                            ),
                            shape = RoundedCornerShape(50.dp) // Pill shape (fully rounded)
                        )
                ) {
                    val text = if (!plan.isPopular) "Best Value" else "Popular"
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
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
        // Weekly subscriptions (with per-credit cost)
        Text(
            text = "Weekly Subscriptions",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
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
        
        // One-time purchases (with per-credit cost)
        Text(
            text = "One-Time Top-Ups",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 200 credits
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "credits_200",
                    credits = 200,
                    price = "$17.99",
                    period = "One Time",
                    isPopular = false
                ),
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f),
                periodText = "One Time"
            )
            
            // 500 credits - Popular
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "credits_500",
                    credits = 500,
                    price = "$39.99",
                    period = "One Time",
                    isPopular = true
                ),
                isSelected = true,
                onClick = {},
                modifier = Modifier.weight(1f),
                periodText = "One Time"
            )
            
            // 1000 credits
            SubscriptionPlanCard(
                plan = SubscriptionPlan(
                    productId = "credits_1000",
                    credits = 1000,
                    price = "$69.99",
                    period = "One Time",
                    isPopular = false,
                    isBestValue = true
                ),
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f),
                periodText = "One Time"
            )
        }
    }
}

