package com.manjul.genai.videogenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.manjul.genai.videogenerator.ui.designsystem.colors.AppColors
import com.manjul.genai.videogenerator.ui.theme.GenAiVideoTheme

@Composable
fun GlassmorphicNavigationBar(
    items: List<NavigationItem>,
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .zIndex(10f)
    ) {
        // Glassmorphic background - Dark theme glass effect matching app theme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 0.5.dp,
                    color = AppColors.BorderDefault.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.SurfaceElevated.copy(alpha = 0.95f),
                            AppColors.SurfaceDark.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            // Inner highlight for depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 32.dp,
                            topEnd = 32.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.PrimaryPurple.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Navigation items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                GlassmorphicNavigationItem(
                    item = item,
                    isSelected = item == selectedItem,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun GlassmorphicNavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.PrimaryPurple.copy(alpha = 0.3f),
                        AppColors.PrimaryPurpleDark.copy(alpha = 0.25f)
                    )
                )
            )
    } else {
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
    }
    
    Box(
        modifier = backgroundModifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
            Box {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) {
                        AppColors.OnPrimaryPurple // White for selected (as user mentioned it's okay)
                    } else {
                        AppColors.TextSecondary // Use theme text color for unselected
                    }
                )
                // Badge indicator
                if (item.badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AppColors.StatusError) // Use theme error color
                            .padding(2.dp)
                    ) {
                        Text(
                            text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 8.sp
                        )
                    }
                }
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    AppColors.OnPrimaryPurple // White for selected (as user mentioned it's okay)
                } else {
                    AppColors.TextSecondary // Use theme text color for unselected
                }
            )
        }
    }
}

data class NavigationItem(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0 // Badge count for notifications
)

// ==================== Preview ====================

@Preview(
    name = "Glassmorphic Navigation Bar",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true
)
@Composable
private fun GlassmorphicNavigationBarPreview() {
    GenAiVideoTheme {
        var selectedItem by remember {
            mutableStateOf(
                NavigationItem(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "Generate",
                    badgeCount = 0
                )
            )
        }

        val navigationItems = listOf(
            NavigationItem(
                icon = Icons.Outlined.AutoAwesome,
                label = "Generate",
                badgeCount = 0
            ),
            NavigationItem(
                icon = Icons.Outlined.ViewInAr,
                label = "Models",
                badgeCount = 0
            ),
            NavigationItem(
                icon = Icons.Outlined.History,
                label = "History",
                badgeCount = 3 // Show badge example
            ),
            NavigationItem(
                icon = Icons.Outlined.Face,
                label = "Profile",
                badgeCount = 0
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.BackgroundDark)
                .padding(vertical = 200.dp)
        ) {
            GlassmorphicNavigationBar(
                items = navigationItems,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }
    }
}

