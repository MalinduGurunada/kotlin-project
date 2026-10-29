package com.example.personalfinanceapp.feature.dashboard.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.feature.dashboard.analytics.CategoryBreakdownItem
import com.example.personalfinanceapp.feature.dashboard.analytics.MonthlyAnalyticsBreakdown
import com.example.personalfinanceapp.feature.dashboard.analytics.SourceBreakdownItem
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSpendingBreakdownScreen(
    analytics: MonthlyAnalyticsBreakdown,
    onBackClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Income & Spending Analytics",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crypto YTD Results Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CRYPTO YTD RESULTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SurfaceWhite.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(AccentLime, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = analytics.cryptoYtdResult.pnlLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val netCryptoPnlLkr = analytics.cryptoYtdResult.netPnlLkrMinor.toDouble() / 100.0
                    Text(
                        text = "LKR ${String.format(Locale.US, "%,.2f", netCryptoPnlLkr)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val gainLkr = analytics.cryptoYtdResult.totalGainLkrMinor.toDouble() / 100.0
                    val lossLkr = analytics.cryptoYtdResult.totalLossLkrMinor.toDouble() / 100.0
                    Text(
                        text = "Gains: LKR ${String.format(Locale.US, "%,.2f", gainLkr)} • Losses: LKR ${String.format(Locale.US, "%,.2f", lossLkr)}",
                        fontSize = 12.sp,
                        color = SurfaceWhite.copy(alpha = 0.8f)
                    )
                }
            }

            // Income Sources Breakdown Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Income Sources Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (analytics.incomeSourceBreakdown.isEmpty()) {
                        Text("No income records available for breakdown.", color = TextSecondaryMuted, fontSize = 13.sp)
                    } else {
                        analytics.incomeSourceBreakdown.forEach { item ->
                            SourceBreakdownRow(item)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Expense Categories Breakdown Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Expense Categories Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (analytics.expenseCategoryBreakdown.isEmpty()) {
                        Text("No expense records available for breakdown.", color = TextSecondaryMuted, fontSize = 13.sp)
                    } else {
                        analytics.expenseCategoryBreakdown.forEach { item ->
                            CategoryBreakdownRow(item)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Committed vs Discretionary Ratio & Month Comparison Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Ratios & Month Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Committed Share:", fontSize = 13.sp, color = TextSecondaryMuted)
                        Text("${analytics.committedPercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkGreen)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discretionary Share:", fontSize = 13.sp, color = TextSecondaryMuted)
                        Text("${analytics.discretionaryPercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedCoral)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MoM Outflow Change:", fontSize = 13.sp, color = TextSecondaryMuted)
                        val momText = String.format(Locale.US, "%+.1f%%", analytics.monthOverMonthOutflowChangePercent)
                        Text(
                            text = momText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analytics.monthOverMonthOutflowChangePercent <= 0) PrimaryDarkGreen else MutedCoral
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBreakdownRow(item: SourceBreakdownItem) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.sourceName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
            val lkr = item.totalLkrMinor.toDouble() / 100.0
            Text("LKR ${String.format(Locale.US, "%,.2f", lkr)} (${item.percentage}%)", fontSize = 13.sp, color = TextSecondaryMuted)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = PrimaryDarkGreen,
            trackColor = SecondaryPaleGreen
        )
    }
}

@Composable
private fun CategoryBreakdownRow(item: CategoryBreakdownItem) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.categoryName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
            val lkr = item.totalLkrMinor.toDouble() / 100.0
            Text("LKR ${String.format(Locale.US, "%,.2f", lkr)} (${item.percentage}%)", fontSize = 13.sp, color = TextSecondaryMuted)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MutedCoral,
            trackColor = Color(0xFFFFEBEE)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IncomeSpendingBreakdownScreenPreview() {
    PersonalFinanceAppTheme {
        IncomeSpendingBreakdownScreen(
            analytics = MonthlyAnalyticsBreakdown(
                incomeSourceBreakdown = listOf(
                    SourceBreakdownItem("SALARY", 300_000_00L, 73),
                    SourceBreakdownItem("FREELANCE", 110_000_00L, 27)
                ),
                expenseCategoryBreakdown = listOf(
                    CategoryBreakdownItem("bills", "Bills & Utilities", 84_200_00L, 67),
                    CategoryBreakdownItem("food", "Food & Dining", 41_300_00L, 33)
                ),
                committedPercent = 67,
                discretionaryPercent = 33,
                monthOverMonthOutflowChangePercent = -5.4
            ),
            onBackClicked = {}
        )
    }
}
