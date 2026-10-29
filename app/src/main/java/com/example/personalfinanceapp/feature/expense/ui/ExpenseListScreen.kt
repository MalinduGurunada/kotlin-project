package com.example.personalfinanceapp.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.ui.theme.PersonalFinanceAppTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    uiState: ExpenseUiState,
    saveUiState: ExpenseSaveUiState = ExpenseSaveUiState(),
    onSaveExpense: (
        id: String?,
        categoryId: String,
        amountDecimal: String,
        currency: CurrencyCode,
        paymentChannel: PaymentChannel,
        spendingClass: SpendingClass,
        occurredDate: LocalDate,
        note: String?
    ) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditorDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(saveUiState.savedExpenseId) {
        if (saveUiState.savedExpenseId != null) {
            showEditorDialog = false
            expenseToEdit = null
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    if (showEditorDialog) {
        ExpenseEditorDialog(
            expenseToEdit = expenseToEdit,
            categories = uiState.categories,
            errorMessage = saveUiState.errorMessage,
            isSaving = saveUiState.isSaving,
            onDismiss = {
                showEditorDialog = false
                expenseToEdit = null
            },
            onSave = { id, categoryId, amountDecimal, currency, channel, spendingClass, date, note ->
                onSaveExpense(id, categoryId, amountDecimal, currency, channel, spendingClass, date, note)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    showEditorDialog = true
                }
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Monthly Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val lkrAmountDecimal = (uiState.totalLkrMinor.toDouble() / 100.0)
                    Text(
                        text = "LKR ${String.format("%.2f", lkrAmountDecimal)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded for this month.\nTap + to add your first expense.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onEdit = {
                                expenseToEdit = expense
                                showEditorDialog = true
                            },
                            onDelete = { onDeleteExpense(expense.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.categoryNameSnapshot,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.paymentChannel.name} • ${expense.occurredDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!expense.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${expense.money.originalCurrency.name} ${expense.money.originalAmountDecimal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (expense.money.originalCurrency != CurrencyCode.LKR) {
                    val baseLkr = expense.money.baseAmountMinor.toDouble() / 100.0
                    Text(
                        text = "(LKR ${String.format("%.2f", baseLkr)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseListScreenPreview() {
    PersonalFinanceAppTheme {
        val sampleExpense = Expense(
            id = "exp-1",
            categoryId = "food",
            categoryNameSnapshot = "Food & Dining",
            spendingClass = SpendingClass.DISCRETIONARY,
            paymentChannel = PaymentChannel.CASH,
            money = Money(
                originalAmountDecimal = "2500.00",
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1.00",
                baseAmountMinor = 250_000L,
                rateSource = RateSource.LKR_FIXED,
                rateDate = LocalDate.now()
            ),
            occurredDate = LocalDate.now(),
            note = "Team lunch at cafe"
        )

        ExpenseListScreen(
            uiState = ExpenseUiState(
                expenses = listOf(sampleExpense),
                totalLkrMinor = 250_000L
            ),
            onSaveExpense = { _, _, _, _, _, _, _, _ -> },
            onDeleteExpense = {},
            onClearError = {}
        )
    }
}
