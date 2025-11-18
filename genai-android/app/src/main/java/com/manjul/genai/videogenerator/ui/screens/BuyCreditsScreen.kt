package com.manjul.genai.videogenerator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import com.manjul.genai.videogenerator.ui.components.AppToolbar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manjul.genai.videogenerator.ui.viewmodel.CreditsViewModel
import com.manjul.genai.videogenerator.ui.viewmodel.HistoryViewModel

data class CreditPackage(
    val name: String,
    val credits: Int,
    val bonus: Int? = null,
    val price: String,
    val isPopular: Boolean = false
)

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF6FF) // Blue-50
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (packageInfo.isPopular) {
                Color(0xFFF5F3FF) // Purple-50
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (packageInfo.isPopular) 2.dp else 1.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (packageInfo.isPopular) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color(0xFF4B3FFF), // Purple-600
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Popular Badge
                if (packageInfo.isPopular) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(
                                Color(0xFF4B3FFF),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Popular",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = packageInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${packageInfo.credits}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B3FFF) // Purple-600
                )
                
                packageInfo.bonus?.let { bonus ->
                    Text(
                        text = "+$bonus bonus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00A63E) // Green-600
                    )
                }
                
                Text(
                    text = packageInfo.price,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun UsageGraph(
    modifier: Modifier = Modifier
) {
    // Sample data for the graph (7 days)
    val dataPoints = listOf(160, 140, 120, 100, 80, 60, 40)
    val dates = listOf("Nov 12", "Nov 14", "Nov 16", "Nov 18")
    val yAxisLabels = listOf(0, 40, 80, 120, 160)
    
    Box(
        modifier = modifier
            .background(Color.Transparent)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yAxisLabels.reversed().forEach { value ->
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280) // Gray-500
                )
            }
        }
        
        // Graph area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 8.dp, bottom = 24.dp)
        ) {
            // Simple line representation
            // In a real implementation, you'd use a charting library
            // For now, we'll create a simple visual representation
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val maxValue = 160f
                
                // Draw grid lines
                yAxisLabels.forEachIndexed { index, value ->
                    val y = height - (value / maxValue * height)
                    drawLine(
                        color = Color(0xFFE5E7EB), // Gray-200
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw line chart
                val pointWidth = width / (dataPoints.size - 1)
                val path = Path().apply {
                    dataPoints.forEachIndexed { index, value ->
                        val x = index * pointWidth
                        val y = height - (value / maxValue * height)
                        if (index == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }
                
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
                    val x = index * pointWidth
                    val y = height - (value / maxValue * height)
                    drawCircle(
                        color = Color(0xFF4B3FFF),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            
            // X-axis labels (dates)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dates.forEach { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280) // Gray-500
                    )
                }
            }
        }
    }
}

