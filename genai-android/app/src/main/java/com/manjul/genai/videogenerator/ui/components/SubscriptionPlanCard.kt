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
import com.android.billingclient.api.ProductDetails
import com.manjul.genai.videogenerator.data.model.SubscriptionPlan
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Subscription plan card component for landing page.
 * Matches the exact design: Period/Type -> Large Number -> Credits -> Price -> Per-credit cost
 * All cards have the same height for consistent layout.
 * Can be used for both subscriptions and one-time purchases.
 * 
 * @param productDetails Optional ProductDetails from Google Play - if provided, uses formatted price with currency
 * @param showTrialInfo If true, shows trial/intro pricing information if available
 */
@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    periodText: String? = null, // Override period text (e.g., "One Time" instead of "Weekly")
    showPerCreditCost: Boolean = true, // Always show per-credit cost by default
    productDetails: ProductDetails? = null, // Google Play product details for accurate pricing
    showTrialInfo: Boolean = false // Show trial/intro pricing information
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
            // Use ProductDetails formatted price if available (for currency consistency)
            val displayPrice = if (productDetails != null) {
                // Check for one-time purchase first (INAPP products)
                if (productDetails.oneTimePurchaseOfferDetails != null) {
                    // For one-time purchases
                    productDetails.oneTimePurchaseOfferDetails!!.formattedPrice
                } else {
                    // For subscriptions, get price from subscription offer details
                    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                    if (!subscriptionOfferDetails.isNullOrEmpty()) {
                        val offer = subscriptionOfferDetails.first()
                        val pricingPhases = offer.pricingPhases.pricingPhaseList
                        if (pricingPhases.isNotEmpty()) {
                            // Get the recurring price (after trial/intro if any)
                            val recurringPhase = pricingPhases.lastOrNull()
                            recurringPhase?.formattedPrice ?: plan.price
                        } else {
                            plan.price
                        }
                    } else {
                        plan.price
                    }
                }
            } else {
                // Fallback to config price
                plan.price
            }
            
            Text(
                text = displayPrice,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            
            // Per-credit cost - always shown for consistent height
            // Get currency code and price amount from ProductDetails for accurate calculation
            val (priceAmountMicros, currencyCode) = if (productDetails != null) {
                if (productDetails.oneTimePurchaseOfferDetails != null) {
                    // One-time purchase
                    val offer = productDetails.oneTimePurchaseOfferDetails!!
                    Pair(offer.priceAmountMicros, offer.priceCurrencyCode)
                } else {
                    // Subscription - get from recurring phase
                    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                    if (!subscriptionOfferDetails.isNullOrEmpty()) {
                        val offer = subscriptionOfferDetails.first()
                        val pricingPhases = offer.pricingPhases.pricingPhaseList
                        if (pricingPhases.isNotEmpty()) {
                            val recurringPhase = pricingPhases.lastOrNull()
                            Pair(
                                recurringPhase?.priceAmountMicros ?: 0L,
                                recurringPhase?.priceCurrencyCode ?: "USD"
                            )
                        } else {
                            Pair(0L, "USD")
                        }
                    } else {
                        Pair(0L, "USD")
                    }
                }
            } else {
                // Fallback: try to parse from displayPrice
                Pair(0L, "USD")
            }
            
            // Calculate per-credit cost
            val perCredit = if (priceAmountMicros > 0) {
                priceAmountMicros / 1_000_000.0 / plan.credits
            } else {
                // Fallback: parse from displayPrice string
                try {
                    displayPrice.replace(Regex("[^0-9.,]"), "")
                        .replace(",", "")
                        .replace(".", "")
                        .toDoubleOrNull()?.div(100.0)?.div(plan.credits)
                        ?: displayPrice.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.div(plan.credits)
                } catch (e: Exception) {
                    null
                }
            }
            
            // Format per-credit cost with proper currency
            val formattedPerCredit = if (perCredit != null && priceAmountMicros > 0) {
                formatPerCreditCost(perCredit, currencyCode)
            } else if (perCredit != null) {
                // Fallback to USD if we don't have currency info
                formatPerCreditCost(perCredit, "USD")
            } else {
                null
            }
            
            // Show trial/intro pricing info if available and requested
            if (showTrialInfo && productDetails != null) {
                val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                if (!subscriptionOfferDetails.isNullOrEmpty()) {
                    val offer = subscriptionOfferDetails.first()
                    val pricingPhases = offer.pricingPhases.pricingPhaseList
                    if (pricingPhases.size > 1) {
                        // Has trial or intro pricing
                        val trialPhase = pricingPhases.first()
                        val recurringPhase = pricingPhases.last()

                        // Parse trial duration from billing period (ISO 8601 format: P1W, P3D, P7D, etc.)
                        val trialDuration = parseTrialDuration(trialPhase.billingPeriod)

                        if (trialPhase.priceAmountMicros == 0L) {
                            // Free trial with duration
                            Text(
                                text = "$trialDuration free trial",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "then ${recurringPhase.formattedPrice}/${parseRecurringPeriod(recurringPhase.billingPeriod)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Normal,
                                fontSize = 9.sp
                            )
                        } else if (trialPhase.priceAmountMicros < recurringPhase.priceAmountMicros) {
                            // Intro pricing with duration
                            Text(
                                text = "${trialPhase.formattedPrice} for $trialDuration",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "then ${recurringPhase.formattedPrice}/${parseRecurringPeriod(recurringPhase.billingPeriod)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Normal,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
            
            // Always render this to maintain consistent card height
            Text(
                text = if (showPerCreditCost && formattedPerCredit != null) {
                    "$formattedPerCredit/credit"
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

/**
 * Data class to hold trial/intro pricing information for a subscription
 */
data class SubscriptionOfferInfo(
    val hasFreeTrial: Boolean = false,
    val hasIntroPricing: Boolean = false,
    val trialDuration: String? = null,           // e.g., "7-day", "3-day"
    val trialPrice: String? = null,              // e.g., "$0.99" for intro pricing
    val recurringPrice: String? = null,          // e.g., "$9.99"
    val recurringPeriod: String? = null,         // e.g., "week", "month"
    val billingPeriodIso: String? = null         // e.g., "P1W", "P1M"
)

/**
 * Extracts trial/intro pricing information from ProductDetails.
 * Returns null if no subscription offer details are available.
 *
 * How it works:
 * - Google Play Console lets you configure offers (free trial, intro pricing) per subscription
 * - When user queries products, Google returns ONLY offers they're eligible for
 * - If user already used trial, that offer won't be in the list
 *
 * @param productDetails The ProductDetails from Google Play Billing
 * @return SubscriptionOfferInfo with trial details, or null if not a subscription
 */
fun getSubscriptionOfferInfo(productDetails: ProductDetails?): SubscriptionOfferInfo? {
    if (productDetails == null) return null

    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
    if (subscriptionOfferDetails.isNullOrEmpty()) return null

    val offer = subscriptionOfferDetails.first()
    val pricingPhases = offer.pricingPhases.pricingPhaseList

    if (pricingPhases.isEmpty()) return null

    // Single phase = no trial/intro
    if (pricingPhases.size == 1) {
        val phase = pricingPhases.first()
        return SubscriptionOfferInfo(
            hasFreeTrial = false,
            hasIntroPricing = false,
            recurringPrice = phase.formattedPrice,
            recurringPeriod = parseRecurringPeriod(phase.billingPeriod),
            billingPeriodIso = phase.billingPeriod
        )
    }

    // Multiple phases = has trial or intro pricing
    val firstPhase = pricingPhases.first()
    val recurringPhase = pricingPhases.last()

    val isFreeTrial = firstPhase.priceAmountMicros == 0L
    val isIntroPricing = !isFreeTrial && firstPhase.priceAmountMicros < recurringPhase.priceAmountMicros

    return SubscriptionOfferInfo(
        hasFreeTrial = isFreeTrial,
        hasIntroPricing = isIntroPricing,
        trialDuration = parseTrialDuration(firstPhase.billingPeriod),
        trialPrice = if (isIntroPricing) firstPhase.formattedPrice else null,
        recurringPrice = recurringPhase.formattedPrice,
        recurringPeriod = parseRecurringPeriod(recurringPhase.billingPeriod),
        billingPeriodIso = recurringPhase.billingPeriod
    )
}

/**
 * Quick check if a product has a free trial offer.
 * Note: This only returns true if the USER is eligible for the trial.
 * If they've already used it, Google Play won't return the trial offer.
 */
fun hasFreeTrial(productDetails: ProductDetails?): Boolean {
    return getSubscriptionOfferInfo(productDetails)?.hasFreeTrial == true
}

/**
 * Quick check if a product has intro pricing offer.
 */
fun hasIntroPricing(productDetails: ProductDetails?): Boolean {
    return getSubscriptionOfferInfo(productDetails)?.hasIntroPricing == true
}

/**
 * Parse ISO 8601 billing period to human-readable trial duration
 * Examples: P3D -> "3-day", P1W -> "7-day", P7D -> "7-day", P1M -> "1-month"
 */
private fun parseTrialDuration(billingPeriod: String): String {
    return try {
        val regex = Regex("P(\\d+)([DWMY])")
        val match = regex.find(billingPeriod) ?: return "Free"
        val (count, unit) = match.destructured
        val countInt = count.toIntOrNull() ?: 1
        when (unit) {
            "D" -> "$countInt-day"
            "W" -> "${countInt * 7}-day"
            "M" -> if (countInt == 1) "1-month" else "$countInt-month"
            "Y" -> if (countInt == 1) "1-year" else "$countInt-year"
            else -> "Free"
        }
    } catch (e: Exception) {
        "Free"
    }
}

/**
 * Parse ISO 8601 billing period to short recurring period label
 * Examples: P1W -> "week", P1M -> "month"
 */
private fun parseRecurringPeriod(billingPeriod: String): String {
    return try {
        val regex = Regex("P(\\d+)([DWMY])")
        val match = regex.find(billingPeriod) ?: return "period"
        val (count, unit) = match.destructured
        val countInt = count.toIntOrNull() ?: 1
        when (unit) {
            "D" -> if (countInt == 1) "day" else "$countInt days"
            "W" -> if (countInt == 1) "week" else "$countInt weeks"
            "M" -> if (countInt == 1) "month" else "$countInt months"
            "Y" -> if (countInt == 1) "year" else "$countInt years"
            else -> "period"
        }
    } catch (e: Exception) {
        "period"
    }
}

/**
 * Format per-credit cost with proper currency symbol
 */
private fun formatPerCreditCost(perCredit: Double, currencyCode: String): String {
    return try {
        val locale = when (currencyCode.uppercase()) {
            "USD" -> Locale.US
            "EUR" -> Locale("en", "EU")
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "CNY" -> Locale.CHINA
            "INR" -> Locale("en", "IN")
            "AUD" -> Locale("en", "AU")
            "CAD" -> Locale.CANADA
            "BRL" -> Locale("pt", "BR")
            "MXN" -> Locale("es", "MX")
            "KRW" -> Locale.KOREA
            "RUB" -> Locale("ru", "RU")
            else -> {
                // Try to find locale by currency code
                Locale.getAvailableLocales().firstOrNull { locale ->
                    try {
                        val currency = Currency.getInstance(locale)
                        currency.currencyCode == currencyCode.uppercase()
                    } catch (e: Exception) {
                        false
                    }
                } ?: Locale.getDefault()
            }
        }
        
        val currency = try {
            Currency.getInstance(currencyCode.uppercase())
        } catch (e: Exception) {
            Currency.getInstance("USD") // Fallback to USD
        }
        
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        formatter.minimumFractionDigits = 3 // Show 3 decimal places for per-credit cost
        formatter.maximumFractionDigits = 3
        formatter.format(perCredit)
    } catch (e: Exception) {
        // Fallback to simple format if currency formatting fails
        "$currencyCode ${String.format(Locale.US, "%.3f", perCredit)}"
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

