package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.feature.auth.ui.ProfileAvatar
import com.example.personalfinanceapp.feature.goal.ui.AddContributionDialog
import com.example.personalfinanceapp.feature.savings.SavingsCoachResult
import com.example.personalfinanceapp.feature.savings.SavingsCoachUnavailableReason
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SpecialDarkGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: DashboardUiState,
    savingsCoachActionState: SavingsCoachActionState = SavingsCoachActionState(),
    currentNavDestination: NavDestination,
    onNavDestinationSelected: (NavDestination) -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToIncome: () -> Unit = {},
    onConfirmSavingsContribution: (goalId: String, amountLkr: Long) -> Unit = { _, _ -> },
    onClearSavingsCoachActionMessage: () -> Unit = {}
) {
    var showSavingsContributionDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(PrimaryDarkGreen, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💳", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FinClarity",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkGreen,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    ProfileAvatar(
                        name = uiState.userName,
                        size = 38.dp,
                        containerColor = PrimaryDarkGreen,
                        contentColor = SurfaceWhite,
                        onClick = { onNavDestinationSelected(NavDestination.MORE) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentDestination = currentNavDestination,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryDarkGreen)
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.errorMessage, color = MutedCoral)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F9FA)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance & Stat Card Hero Header
                item {
                    OverviewBalanceCard(
                        uiState = uiState,
                        onNavigateToIncome = onNavigateToIncome,
                        onNavigateToActivity = onNavigateToActivity,
                        onNavigateToProfile = { onNavDestinationSelected(NavDestination.MORE) }
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToIncome() }
                    ) {
                        val monthlyCryptoLkr = uiState.monthlyCryptoResult.netLkrMinor.toDouble() / 100.0
                        val ytdCryptoLkr = uiState.yearToDateCryptoResult.netLkrMinor.toDouble() / 100.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Realised crypto · Month: LKR ${String.format(Locale.US, "%+,.2f", monthlyCryptoLkr)} · YTD: LKR ${String.format(Locale.US, "%+,.2f", ytdCryptoLkr)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                            Text("›", fontSize = 16.sp, color = TextSecondaryMuted)
                        }
                    }
                }

                // Active Savings Goal Card
                item {
                    uiState.activeGoal?.let { goal ->
                        ActiveGoalCard(
                            goal = goal,
                            onClick = { onNavDestinationSelected(NavDestination.GOALS) }
                        )
                    } ?: GoalEmptyCard(
                        onClick = { onNavDestinationSelected(NavDestination.GOALS) }
                    )
                }

                item {
                    SmartSavingsCard(
                        result = uiState.smartSavings,
                        actionState = savingsCoachActionState,
                        onReviewContribution = {
                            onClearSavingsCoachActionMessage()
                            showSavingsContributionDialog = true
                        },
                        onClick = { onNavDestinationSelected(NavDestination.GOALS) }
                    )
                }

                // Recent Activity Header & Items
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkGreen,
                            modifier = Modifier.clickable { onNavigateToActivity() }
                        )
                    }
                }

                if (uiState.recentActivity.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent transactions this month",
                                    fontSize = 14.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentActivity, key = { it.id }) { item ->
                        RecentActivityCard(item)
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEDEFEF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ⓘ Real transaction data",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                }
            }

        }
    }

    val coach = uiState.smartSavings
    val suggestedLkr = coach?.suggestedContributionMinor
        ?.divide(java.math.BigInteger.valueOf(100L))
        ?.toLongIfInRange()
    if (showSavingsContributionDialog && coach?.goal != null && suggestedLkr != null) {
        AddContributionDialog(
            goalName = coach.goal.goalName,
            initialAmountLkr = suggestedLkr,
            supportingText = "This records a goal allocation only. It does not transfer money. Check your bank balance before confirming.",
            errorMessage = savingsCoachActionState.errorMessage,
            onDismiss = {
                showSavingsContributionDialog = false
                onClearSavingsCoachActionMessage()
            },
            onConfirm = { amountLkr, _ ->
                onConfirmSavingsContribution(coach.goal.goalId, amountLkr)
                showSavingsContributionDialog = false
            }
        )
    }
}

@Composable
private fun OverviewBalanceCard(
    uiState: DashboardUiState,
    onNavigateToIncome: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateToProfile() }
                ) {
                    ProfileAvatar(
                        name = uiState.userName,
                        size = 44.dp,
                        containerColor = AccentLime,
                        contentColor = PrimaryDarkGreen,
                        fontSize = 16
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Welcome back",
                            fontSize = 12.sp,
                            color = SurfaceWhite.copy(alpha = 0.7f)
                        )
                        val firstName = uiState.userName.trim().split(" ").firstOrNull()?.ifBlank { "User" } ?: "User"
                        Text(
                            text = "Good morning, $firstName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balance
            Text(
                text = "THIS MONTH'S NET CASH FLOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            val balanceLkr = uiState.monthlyNetCashFlowLkrMinor.toDouble() / 100.0
            Text(
                text = "LKR ${String.format(Locale.US, "%,.2f", balanceLkr)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            val now = java.time.LocalDate.now()
            val monthShort = now.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)
            Text(
                text = "This month (1 $monthShort – Today) • Real data",
                fontSize = 12.sp,
                color = SurfaceWhite.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Income / Expense Dual Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToIncome)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(SpecialDarkGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("↑", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Income", fontSize = 12.sp, color = TextSecondaryMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val incLkr = uiState.totalIncomeLkrMinor.toDouble() / 100.0
                        Text(
                            text = "LKR ${String.format(Locale.US, "%,.2f", incLkr)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.incomeCount} deposits",
                            fontSize = 11.sp,
                            color = PrimaryDarkGreen
                        )
                    }
                }

                // Expenses Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToActivity)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MutedCoral, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("↓", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expenses", fontSize = 12.sp, color = TextSecondaryMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val expLkr = uiState.totalExpenseLkrMinor.toDouble() / 100.0
                        Text(
                            text = "LKR ${String.format(Locale.US, "%,.2f", expLkr)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedCoral
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.expenseCount} records",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveGoalCard(
    goal: ActiveSavingsGoalUiState,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE SAVINGS GOAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Target: ${goal.targetDateText}",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SecondaryPaleGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💻", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = goal.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        goal.progressPercent?.let { progressPercent ->
                            Box(
                                modifier = Modifier
                                    .background(SecondaryPaleGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$progressPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDarkGreen
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = goal.currentSavedLkrMinor?.let { savedMinor ->
                            "${formatLkrMinor(savedMinor)} of ${formatLkrMinor(goal.targetAmountLkrMinor)} target"
                        } ?: "Saved balance unavailable",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            goal.progressPercent?.let { progressPercent ->
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AccentLime,
                    trackColor = Color(0xFFE0E0E0),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = goal.remainingLkrMinor?.let { "${formatLkrMinor(it)} remaining" }
                        ?: "Progress unavailable",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}

@Composable
private fun GoalEmptyCard(onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No active savings goal",
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create or activate a goal to track progress here.",
                fontSize = 12.sp,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun SmartSavingsCard(
    result: SavingsCoachResult?,
    actionState: SavingsCoachActionState,
    onReviewContribution: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SMART SAVINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryMuted,
                letterSpacing = 1.sp
            )

            val goal = result?.goal
            if (goal != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(goal.goalName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text("${goal.progressPercent}%", fontWeight = FontWeight.Bold, color = PrimaryDarkGreen)
                }
                LinearProgressIndicator(
                    progress = { goal.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = AccentLime,
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    text = "${formatLkrMinor(goal.remainingAmountMinor)} remaining by ${goal.targetDate}",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            result?.estimatedCapacityAfterReserveMinor?.let { capacity ->
                Text(
                    text = "Estimated capacity after 20% reserve: ${formatLkrMinor(capacity)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
            }

            val suggestion = result?.suggestedContributionMinor
            if (suggestion != null) {
                Text(
                    text = "Suggested contribution: ${formatLkrMinor(suggestion)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkGreen
                )
            } else {
                Text(
                    text = savingsCoachUnavailableText(result?.unavailableReason),
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            result?.discretionaryReductionTipMinor?.let { reduction ->
                Text(
                    text = "💡 Tip: Cutting 10% discretionary spend retains ~${formatLkrMinor(reduction)}.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkGreen
                )
            }

            actionState.errorMessage?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            actionState.successMessage?.let {
                Text(it, fontSize = 12.sp, color = PrimaryDarkGreen)
            }

            if (suggestion != null) {
                Button(
                    onClick = onReviewContribution,
                    enabled = !actionState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen, contentColor = SurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        if (actionState.isSaving) "Recording allocation" else "Review contribution",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun savingsCoachUnavailableText(reason: SavingsCoachUnavailableReason?): String = when (reason) {
    SavingsCoachUnavailableReason.REQUIRED_DATA_UNAVAILABLE ->
        "No suggestion because required financial data is unavailable."
    SavingsCoachUnavailableReason.NO_ACTIVE_GOAL ->
        "Create an active savings goal to receive a suggestion."
    SavingsCoachUnavailableReason.EXPIRED_DEADLINE ->
        "Update the expired goal deadline before using a savings suggestion."
    SavingsCoachUnavailableReason.NO_QUALIFYING_RECEIPTS ->
        "No suggestion because no non-crypto receipts are recorded this month."
    SavingsCoachUnavailableReason.NO_POSITIVE_CAPACITY ->
        "No positive capacity remains after commitments and safety reserve."
    SavingsCoachUnavailableReason.BELOW_ONE_RUPEE ->
        "Calculated suggestion is below one rupee."
    null -> "A savings suggestion is not available yet."
}

private fun formatLkrMinor(amountMinor: Long): String =
    "LKR ${String.format(Locale.US, "%,.2f", BigDecimal.valueOf(amountMinor, 2))}"

private fun formatLkrMinor(amountMinor: java.math.BigInteger): String =
    "LKR ${String.format(Locale.US, "%,.2f", BigDecimal(amountMinor, 2))}"

@Composable
private fun RecentActivityCard(item: TimelineActivityItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (item.isIncome) SecondaryPaleGreen else Color(0xFFFFEBEE),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.isIncome) "↑" else "↓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isIncome) PrimaryDarkGreen else MutedCoral
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.amountText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isRedAmount) MutedCoral else PrimaryDarkGreen
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OverviewScreenPreview() {
    PersonalFinanceAppTheme {
        OverviewScreen(
            uiState = DashboardUiState(),
            currentNavDestination = NavDestination.OVERVIEW,
            onNavDestinationSelected = {},
            onNavigateToActivity = {}
        )
    }
}
