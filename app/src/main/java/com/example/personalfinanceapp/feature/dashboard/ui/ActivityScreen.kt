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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    uiState: DashboardUiState,
    currentNavDestination: NavDestination,
    onNavDestinationSelected: (NavDestination) -> Unit,
    onSelectTransaction: (String) -> Unit,
    onRecordExpenseClicked: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Expenses") }
    var selectedStateTab by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryDarkGreen, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💳", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rupee Pulse",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkGreen,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF2F4F5), CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔍", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryDarkGreen, CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRecordExpenseClicked,
                containerColor = PrimaryDarkGreen,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(AccentLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record Expense",
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite,
                        fontSize = 14.sp
                    )
                }
            }
        },
        bottomBar = {
            AppBottomNavigation(
                currentDestination = currentNavDestination,
                onDestinationSelected = onNavDestinationSelected
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header & Synced Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "March 2025 ▾",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryPaleGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PrimaryDarkGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Synced 09:15 AM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkGreen
                            )
                        }
                    }
                }
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem("All", selectedFilter == "All") { selectedFilter = "All" }
                    FilterChipItem("✔ Expenses", selectedFilter == "Expenses") { selectedFilter = "Expenses" }
                    FilterChipItem("↓ Income", selectedFilter == "Income") { selectedFilter = "Income" }
                    FilterChipItem("Category ▾", selectedFilter == "Category") { selectedFilter = "Category" }
                }
            }

            // Outflow Summary Card
            item {
                OutflowSummaryCard(uiState)
            }

            // Grouped Transactions
            item {
                Text(
                    text = "Today, 29 Mar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
            }

            item {
                ActivityTransactionCard(
                    title = "Dialog Axiata Bill",
                    subtitle = "Utilities • Auto-debit",
                    amountText = "-LKR 4,850.00",
                    timeText = "08:14 AM",
                    iconSymbol = "📠",
                    onClick = { onSelectTransaction("txn-1") }
                )
            }

            item {
                Text(
                    text = "Yesterday, 28 Mar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
            }

            item {
                ActivityTransactionCard(
                    title = "Cargills Food City",
                    subtitle = "Groceries • Debit Card",
                    amountText = "-LKR 12,200.00",
                    timeText = "06:40 PM",
                    iconSymbol = "🛒",
                    onClick = { onSelectTransaction("txn-2") }
                )
            }

            item {
                ActivityTransactionCard(
                    title = "Tuition Class Fee",
                    subtitle = "Education • Transfer",
                    amountText = "-LKR 15,000.00",
                    timeText = "02:30 PM",
                    iconSymbol = "🎓",
                    onClick = { onSelectTransaction("txn-3") }
                )
            }

            item {
                ActivityTransactionCard(
                    title = "Digital Ocean",
                    subtitle = "Hosting • Online",
                    amountText = "-LKR 48,250.00",
                    timeText = "11:10 AM",
                    iconSymbol = "☁",
                    onClick = { onSelectTransaction("txn-4") }
                )
            }

            // State Switcher Footer Tabs
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF2F5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StateTabItem("All", selectedStateTab == "All") { selectedStateTab = "All" }
                        StateTabItem("Empty", selectedStateTab == "Empty") { selectedStateTab = "Empty" }
                        StateTabItem("Loading", selectedStateTab == "Loading") { selectedStateTab = "Loading" }
                    }
                }
            }

            if (selectedStateTab == "Loading") {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryDarkGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) PrimaryDarkGreen else Color(0xFFEFF2F5),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SurfaceWhite else TextSecondaryMuted
        )
    }
}

@Composable
private fun StateTabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) SurfaceWhite else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimaryDark else TextSecondaryMuted
        )
    }
}

@Composable
private fun OutflowSummaryCard(uiState: DashboardUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL OUTFLOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted,
                    letterSpacing = 1.sp
                )
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF2F5))
                ) {
                    Text(
                        text = "${uiState.expenseCount} items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalExp = uiState.totalExpenseLkrMinor.toDouble() / 100.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓", color = MutedCoral, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "-LKR ${String.format(Locale.US, "%,.2f", totalExp)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedCoral
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Committed vs Discretionary Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(PrimaryDarkGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val comLkr = uiState.committedExpenseLkrMinor.toDouble() / 100.0
                    Text(
                        text = "Committed: LKR ${String.format(Locale.US, "%.1fk", comLkr / 1000)} (${uiState.committedPercent}%)",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AccentLime, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val discLkr = uiState.discretionaryExpenseLkrMinor.toDouble() / 100.0
                    Text(
                        text = "Discretionary: LKR ${String.format(Locale.US, "%.1fk", discLkr / 1000)} (${uiState.discretionaryPercent}%)",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Progress Bar (Committed dark green / Discretionary lime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uiState.committedPercent / 100f)
                        .height(8.dp)
                        .background(PrimaryDarkGreen, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(AccentLime, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                )
            }
        }
    }
}

@Composable
private fun ActivityTransactionCard(
    title: String,
    subtitle: String,
    amountText: String,
    timeText: String,
    iconSymbol: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SecondaryPaleGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconSymbol, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedCoral
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityScreenPreview() {
    PersonalFinanceAppTheme {
        ActivityScreen(
            uiState = DashboardUiState(),
            currentNavDestination = NavDestination.ACTIVITY,
            onNavDestinationSelected = {},
            onSelectTransaction = {},
            onRecordExpenseClicked = {}
        )
    }
}
