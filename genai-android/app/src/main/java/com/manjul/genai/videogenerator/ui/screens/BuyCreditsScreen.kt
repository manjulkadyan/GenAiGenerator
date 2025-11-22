package com.manjul.genai.videogenerator.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import com.manjul.genai.videogenerator.ui.designsystem.components.badges.CustomStatusBadge
import com.manjul.genai.videogenerator.ui.designsystem.components.cards.AppCard
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme
import com.manjul.genai.videogenerator.ui.viewmodel.CreditsViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel

data class CreditPackage(
    val name: String,
    val credits: Int,
    val bonus: Int? = null,
    val price: String,
    val isPopular: Boolean = false
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BuyCreditsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    creditsViewModel: CreditsViewModel = viewModel(factory = CreditsViewModel.Factory),
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    onPackageSelected: (CreditPackage) -> Unit = {}
) {
    val credits by creditsViewModel.state.collectAsState()
    val jobs by historyViewModel.jobs.collectAsState()

    // Handle system back button
    BackHandler(onBack = onBackClick)

    // Calculate usage this week (simplified - using last 7 days)
    val usageThisWeek = try {
        jobs
            .filter {
                val weekAgo = java.time.Instant.now().minusSeconds(7 * 24 * 60 * 60)
                it.createdAt.isAfter(weekAgo)
            }
            .sumOf { it.cost }
    } catch (e: Exception) {
        0
    }

    val packages = listOf(
        CreditPackage("Starter", 100, null, "$9.99"),
        CreditPackage("Creator", 500, 50, "$39.99", isPopular = true),
        CreditPackage("Pro", 1000, 150, "$69.99"),
        CreditPackage("Studio", 5000, 1000, "$299.99")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        AppToolbar(
            title = "Credits",
            showBackButton = true,
            onBackClick = onBackClick,
            showBorder = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Current Balance Card
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4A5565) // Gray-600
                    )
                    Text(
                        text = "${credits.credits}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B3FFF) // Purple-600
                    )
                    Text(
                        text = "credits available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6A7282) // Gray-500
                    )
                }
            }

            // Usage This Week Card
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Usage This Week",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Usage",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFE7000B) // Red
                            )
                            Text(
                                text = "-$usageThisWeek",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE7000B) // Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simple usage graph representation
                    UsageGraph(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // Buy Credits Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Buy Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Package Grid (2x2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PackageCard(
                            packageInfo = packages[0],
                            modifier = Modifier.weight(1f),
                            onClick = { onPackageSelected(packages[0]) }
                        )
                        PackageCard(
                            packageInfo = packages[1],
                            modifier = Modifier.weight(1f),
                            onClick = { onPackageSelected(packages[1]) }
                        )
                    }

                    // Second Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PackageCard(
                            packageInfo = packages[2],
                            modifier = Modifier.weight(1f),
                            onClick = { onPackageSelected(packages[2]) }
                        )
                        PackageCard(
                            packageInfo = packages[3],
                            modifier = Modifier.weight(1f),
                            onClick = { onPackageSelected(packages[3]) }
                        )
                    }
                }
            }

            // Tip Card
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "💡 Tip:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1447E6) // Blue-600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Credits never expire and can be used across all AI models. Larger packs offer better value with bonus credits!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF364153) // Gray-700
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageCard(
    packageInfo: CreditPackage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Popular Badge
            if (packageInfo.isPopular) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    CustomStatusBadge(
                        text = "Popular",
                        backgroundColor = Color(0xFF4B3FFF),
                        textColor = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = packageInfo.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${packageInfo.credits}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B3FFF) // Purple-600
            )

            packageInfo.bonus?.let { bonus ->
                Text(
                    text = "+$bonus bonus",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981) // Green-500
                )
            }

            Text(
                text = packageInfo.price,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsageGraph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()

        // Draw grid lines
        val gridColor = Color(0xFFE5E7EB).copy(alpha = 0.5f)
        val gridStroke = 1.dp.toPx()

        // Horizontal grid lines
        for (i in 0..4) {
            val y = padding + (height - 2 * padding) * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = gridStroke
            )
        }

        // Vertical grid lines
        val days = 7
        for (i in 0..days) {
            val x = padding + (width - 2 * padding) * (i / days.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(x, padding),
                end = Offset(x, height - padding),
                strokeWidth = gridStroke
            )
        }

        // Draw sample data line
        val dataPoints = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.9f)
        val path = Path()
        val graphWidth = width - 2 * padding
        val graphHeight = height - 2 * padding

        dataPoints.forEachIndexed { index, value ->
            val x = padding + (graphWidth / (dataPoints.size - 1)) * index
            val y = padding + graphHeight * (1 - value)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw line
        drawPath(
            path = path,
            color = Color(0xFF4B3FFF), // Purple-600
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw points
        dataPoints.forEachIndexed { index, value ->
            val x = padding + (graphWidth / (dataPoints.size - 1)) * index
            val y = padding + graphHeight * (1 - value)

            drawCircle(
                color = Color(0xFF4B3FFF),
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw labels
        val dates = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        dates.forEachIndexed { index, date ->
            val x = padding + (graphWidth / (dates.size - 1)) * index
            val y = height - padding + 8.dp.toPx()

            // Note: Text drawing in Canvas requires custom implementation
            // For preview purposes, we'll skip text labels
        }
    }
}

// ==================== Preview ====================

@RequiresApi(Build.VERSION_CODES.O)
@Preview(
    name = "Buy Credits Screen",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
private fun BuyCreditsScreenPreview() {
    GenAiVideoTheme {
        BuyCreditsScreen(
            onBackClick = {},
            onPackageSelected = {}
        )
    }
}
