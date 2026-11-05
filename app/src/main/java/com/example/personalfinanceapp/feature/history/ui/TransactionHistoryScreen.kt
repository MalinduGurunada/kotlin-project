package com.example.personalfinanceapp.feature.history.ui

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
import androidx.compose.material3.IconButton
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
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    uiState: TransactionHistoryUiState,
    onFilterChanged: (String) -> Unit,
    onRepeatClicked: (String) -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transaction History",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterTab("ALL", "All Transactions", uiState.selectedFilterType == "ALL") { onFilterChanged("ALL") }
                HistoryFilterTab("EXPENSE", "Expenses Only", uiState.selectedFilterType == "EXPENSE") { onFilterChanged("EXPENSE") }
                HistoryFilterTab("INCOME", "Income Only", uiState.selectedFilterType == "INCOME") { onFilterChanged("INCOME") }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryDarkGreen)
                }
            } else if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found for this filter.", style = MaterialTheme.typography.bodyLarge, color = TextSecondaryMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { txn ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(if (txn.isExpense) Color(0xFFFFEBEE) else SecondaryPaleGreen, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (txn.isExpense) "↓" else "↑", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (txn.isExpense) MutedCoral else PrimaryDarkGreen)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(txn.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${txn.paymentChannelOrKind} • ${txn.occurredDate}", fontSize = 12.sp, color = TextSecondaryMuted)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(txn.amountDecimalText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (txn.isExpense) MutedCoral else TextPrimaryDark)
                                    if (txn.isExpense) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🔁 Repeat",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDarkGreen,
                                            modifier = Modifier.clickable { onRepeatClicked(txn.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterTab(typeKey: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
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
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SurfaceWhite else TextSecondaryMuted
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionHistoryScreenPreview() {
    PersonalFinanceAppTheme {
        TransactionHistoryScreen(
            uiState = TransactionHistoryUiState(
                transactions = listOf(
                    UnifiedTransactionItem("1", "Dialog Axiata Bill", "COMMITTED", "-LKR 4,850.00", 4850_00L, true, LocalDate.now(), "CASH")
                )
            ),
            onFilterChanged = {},
            onRepeatClicked = {},
            onBackClicked = {}
        )
    }
}
