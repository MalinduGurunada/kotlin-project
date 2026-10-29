package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    uiState: DashboardUiState,
    onNavDestinationSelected: (NavDestination) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Goals",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkGreen
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentDestination = NavDestination.GOALS,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(color = PrimaryDarkGreen)
                uiState.errorMessage != null -> Text(uiState.errorMessage, color = MutedCoral)
                uiState.activeGoal == null -> GoalEmptySummary()
                else -> GoalSummary(uiState.activeGoal)
            }
        }
    }
}

@Composable
private fun GoalEmptySummary() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No active savings goal", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Active goals will appear here.",
                fontSize = 13.sp,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun GoalSummary(goal: ActiveSavingsGoalUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
            Text(
                text = goal.currentSavedLkrMinor?.let { saved ->
                    "${formatGoalLkr(saved)} of ${formatGoalLkr(goal.targetAmountLkrMinor)}"
                } ?: "Saved balance unavailable",
                color = TextSecondaryMuted
            )
            goal.progressPercent?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AccentLime,
                    trackColor = Color(0xFFE0E0E0)
                )
                Text("$progress%", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
            }
            Text("Target: ${goal.targetDateText}", fontSize = 12.sp, color = TextSecondaryMuted)
        }
    }
}

private fun formatGoalLkr(amountMinor: Long): String =
    "LKR ${String.format(Locale.US, "%,.2f", BigDecimal.valueOf(amountMinor, 2))}"
