package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

enum class NavDestination(val label: String, val icon: String) {
    OVERVIEW("Overview", "⊞"),
    ACTIVITY("Activity", "📋"),
    GOALS("Goals", "🐷"),
    MORE("More", "•••")
}

@Composable
fun AppBottomNavigation(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) SecondaryPaleGreen else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onDestinationSelected(destination) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = destination.icon,
                            fontSize = 18.sp,
                            color = if (isSelected) PrimaryDarkGreen else TextSecondaryMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destination.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryDarkGreen else TextSecondaryMuted
                        )
                    }
                }
            }
        }
    }
}
