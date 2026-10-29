package com.example.personalfinanceapp.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.expense.ui.ExpenseUiState
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

internal enum class ExpenseActivityFilter {
    ALL,
    COMMITTED,
    DISCRETIONARY,
    CATEGORY
}

internal data class ExpenseDayGroup(
    val date: LocalDate,
    val expenses: List<Expense>,
    val totalLkrMinor: Long
)

internal data class ExpenseFilterSummary(
    val totalLkrMinor: Long,
    val committedLkrMinor: Long,
    val discretionaryLkrMinor: Long,
    val committedPercentage: Int,
    val discretionaryPercentage: Int
)

internal fun filterExpenses(
    expenses: List<Expense>,
    filter: ExpenseActivityFilter,
    categoryId: String? = null
): List<Expense> = expenses.filter { expense ->
    when (filter) {
        ExpenseActivityFilter.ALL -> true
        ExpenseActivityFilter.COMMITTED -> expense.spendingClass == SpendingClass.COMMITTED
        ExpenseActivityFilter.DISCRETIONARY -> expense.spendingClass == SpendingClass.DISCRETIONARY
        ExpenseActivityFilter.CATEGORY -> categoryId != null && expense.categoryId == categoryId
    }
}

internal fun groupExpensesByDate(expenses: List<Expense>): List<ExpenseDayGroup> =
    expenses
        .sortedWith(compareByDescending<Expense> { it.occurredDate }.thenBy { it.id })
        .groupBy { it.occurredDate }
        .map { (date, dayExpenses) ->
            ExpenseDayGroup(
                date = date,
                expenses = dayExpenses,
                totalLkrMinor = dayExpenses.sumOf { it.money.baseAmountMinor }
            )
        }

internal fun summarizeExpenses(expenses: List<Expense>): ExpenseFilterSummary {
    val committed = expenses
        .filter { it.spendingClass == SpendingClass.COMMITTED }
        .sumOf { it.money.baseAmountMinor }
    val discretionary = expenses
        .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
        .sumOf { it.money.baseAmountMinor }
    val total = committed + discretionary
    val committedPercentage = if (total > 0L) {
        ((committed.toDouble() / total) * 100).toInt()
    } else {
        0
    }
    return ExpenseFilterSummary(
        totalLkrMinor = total,
        committedLkrMinor = committed,
        discretionaryLkrMinor = discretionary,
        committedPercentage = committedPercentage,
        discretionaryPercentage = if (total > 0L) 100 - committedPercentage else 0
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    expenseUiState: ExpenseUiState,
    currentNavDestination: NavDestination,
    onNavDestinationSelected: (NavDestination) -> Unit,
    onRecordExpenseClicked: () -> Unit,
    onExpenseClicked: (Expense) -> Unit = {},
    onNavigateToIncome: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(ExpenseActivityFilter.ALL) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

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
                            .background(PrimaryDarkGreen, CircleShape),
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
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(AccentLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
        if (expenseUiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryDarkGreen)
            }
        } else {
            val filteredExpenses = remember(
                expenseUiState.expenses,
                selectedFilter,
                selectedCategoryId
            ) {
                filterExpenses(expenseUiState.expenses, selectedFilter, selectedCategoryId)
            }
            val groupedExpenses = remember(filteredExpenses) {
                groupExpensesByDate(filteredExpenses)
            }
            val summary = remember(filteredExpenses) { summarizeExpenses(filteredExpenses) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF9FAFB)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    val now = LocalDate.now()
                    val monthLabel = "${now.month.getDisplayName(TextStyle.FULL, Locale.US)} ${now.year} ▾"
                    Column {
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryMuted
                            )
                    }
                }

                expenseUiState.errorMessage?.let { errorMessage ->
                    item {
                        Text(
                            text = errorMessage,
                            color = MutedCoral,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipItem("All", selectedFilter == ExpenseActivityFilter.ALL) {
                            selectedFilter = ExpenseActivityFilter.ALL
                        }
                        FilterChipItem("Committed", selectedFilter == ExpenseActivityFilter.COMMITTED) {
                            selectedFilter = ExpenseActivityFilter.COMMITTED
                        }
                        FilterChipItem("Flexible", selectedFilter == ExpenseActivityFilter.DISCRETIONARY) {
                            selectedFilter = ExpenseActivityFilter.DISCRETIONARY
                        }
                        Box {
                            val categoryLabel = expenseUiState.categories
                                .firstOrNull { it.id == selectedCategoryId }
                                ?.name
                                ?: "Category"
                            FilterChipItem(
                                "$categoryLabel ▾",
                                selectedFilter == ExpenseActivityFilter.CATEGORY
                            ) {
                                categoryMenuExpanded = true
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                expenseUiState.categories
                                    .filter { !it.archived }
                                    .forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                selectedCategoryId = category.id
                                                selectedFilter = ExpenseActivityFilter.CATEGORY
                                                categoryMenuExpanded = false
                                            }
                                        )
                                    }
                            }
                        }
                    }
                }

                // Outflow Summary Card
                item {
                    OutflowSummaryCard(
                        totalMinor = summary.totalLkrMinor,
                        count = filteredExpenses.size,
                        committedMinor = summary.committedLkrMinor,
                        discretionaryMinor = summary.discretionaryLkrMinor,
                        committedPct = summary.committedPercentage,
                        discretionaryPct = summary.discretionaryPercentage
                    )
                }

                // Real Date-Grouped Transactions
                if (groupedExpenses.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🧾", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No expenses recorded this month",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ Record Expense' to add your first transaction",
                                    fontSize = 13.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                } else {
                    groupedExpenses.forEach { group ->
                        val dailyTotalLkr = group.totalLkrMinor.toDouble() / 100.0

                        // Date Group Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatGroupDateHeader(group.date),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "-LKR ${String.format(Locale.US, "%,.2f", dailyTotalLkr)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MutedCoral
                                )
                            }
                        }

                        // Expense Cards under this Date
                        items(group.expenses, key = { it.id }) { expense ->
                            val lkrAmount = expense.money.baseAmountMinor.toDouble() / 100.0
                            val iconEmoji = getCategoryEmoji(expense.categoryNameSnapshot)

                            ActivityTransactionCard(
                                title = expense.note?.ifBlank { expense.categoryNameSnapshot } ?: expense.categoryNameSnapshot,
                                subtitle = "${expense.categoryNameSnapshot} • ${formatPaymentChannel(expense.paymentChannel)}",
                                amountText = "-LKR ${String.format(Locale.US, "%,.2f", lkrAmount)}",
                                iconSymbol = iconEmoji,
                                onClick = { onExpenseClicked(expense) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun OutflowSummaryCard(
    totalMinor: Long,
    count: Int,
    committedMinor: Long,
    discretionaryMinor: Long,
    committedPct: Int,
    discretionaryPct: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
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
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$count items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Outflow Amount Row
            val totalLkr = totalMinor.toDouble() / 100.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓", color = MutedCoral, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "-LKR ${String.format(Locale.US, "%,.2f", totalLkr)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedCoral
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PrimaryDarkGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Committed: LKR ${formatShortLkr(committedMinor)} ($committedPct%)",
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentLime, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Discretionary: LKR ${formatShortLkr(discretionaryMinor)} ($discretionaryPct%)",
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
            ) {
                val committedWeight = (committedPct / 100f).coerceIn(0f, 1f)
                val discretionaryWeight = (discretionaryPct / 100f).coerceIn(0f, 1f)

                Row(modifier = Modifier.fillMaxSize()) {
                    if (committedWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(committedWeight)
                                .background(PrimaryDarkGreen, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        )
                    }
                    if (discretionaryWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .background(AccentLime, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) PrimaryDarkGreen else SurfaceWhite,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryDarkGreen else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SurfaceWhite else TextPrimaryDark
        )
    }
}

@Composable
private fun ActivityTransactionCard(
    title: String,
    subtitle: String,
    amountText: String,
    iconSymbol: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
            }
        }
    }
}

private fun formatGroupDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.US)}"
        today.minusDays(1) -> "Yesterday, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.US)}"
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.US)} ${date.year}"
    }
}

private fun formatShortLkr(amountMinor: Long): String {
    val lkr = amountMinor / 100.0
    return if (lkr >= 1000.0) {
        String.format(Locale.US, "%.1fk", lkr / 1000.0)
    } else {
        String.format(Locale.US, "%.0f", lkr)
    }
}

private fun formatPaymentChannel(channel: PaymentChannel): String {
    return when (channel) {
        PaymentChannel.CASH -> "Cash"
        PaymentChannel.CARD -> "Debit Card"
        PaymentChannel.BANK -> "Transfer"
        PaymentChannel.OTHER -> "Auto-debit"
    }
}

private fun getCategoryEmoji(categoryName: String): String {
    val lower = categoryName.lowercase()
    return when {
        lower.contains("grocer") || lower.contains("supermarket") || lower.contains("food") -> "🛒"
        lower.contains("utilit") || lower.contains("bill") || lower.contains("dialog") -> "📠"
        lower.contains("education") || lower.contains("tuition") || lower.contains("class") -> "🎓"
        lower.contains("host") || lower.contains("digital") || lower.contains("server") -> "☁"
        lower.contains("rent") || lower.contains("home") -> "🏠"
        else -> "💳"
    }
}
