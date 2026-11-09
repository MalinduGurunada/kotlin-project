package com.example.personalfinanceapp.feature.income.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.ui.theme.AccentLime
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    uiState: IncomeUiState,
    deleteUiState: IncomeDeleteUiState = IncomeDeleteUiState(),
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddIncome: () -> Unit = {},
    onEditIncome: (Income, UserId?) -> Unit = { _, _ -> },
    onDeleteIncome: (Income, UserId?) -> Unit = { _, _ -> },
    onCancelDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    userMessage: String? = null,
    onUserMessageShown: () -> Unit = {},
    canNavigateToPreviousMonth: Boolean = true,
    canNavigateToNextMonth: Boolean = true
) {
    val snackbarHostState = remember { SnackbarHostState() }

    deleteUiState.selectedIncome?.let { income ->
        IncomeDeleteConfirmationDialog(
            income = income,
            isDeleting = deleteUiState.isDeleting,
            errorMessage = deleteUiState.errorMessage,
            onCancel = onCancelDelete,
            onConfirm = onConfirmDelete
        )
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Income",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddIncome,
                containerColor = PrimaryDarkGreen,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add-income")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+ Add Income", fontWeight = FontWeight.Bold, color = SurfaceWhite, fontSize = 14.sp)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            MonthNavigation(
                dateRange = uiState.dateRange,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                canNavigateToPreviousMonth = canNavigateToPreviousMonth,
                canNavigateToNextMonth = canNavigateToNextMonth
            )

            when (uiState.observationStatus) {
                IncomeObservationStatus.LOADING -> LoadingContent()
                IncomeObservationStatus.EMPTY -> EmptyContent(uiState.totalLkrMinor)
                IncomeObservationStatus.ERROR -> ErrorContent(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = onRetry
                )
                IncomeObservationStatus.SUCCESS -> SuccessContent(
                    uiState = uiState,
                    deletingIncomeId = deleteUiState.deletingIncomeId,
                    onEditIncome = onEditIncome,
                    onDeleteIncome = onDeleteIncome
                )
            }
        }
    }
}

@Composable
private fun MonthNavigation(
    dateRange: IncomeDateRange,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canNavigateToPreviousMonth: Boolean,
    canNavigateToNextMonth: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPreviousMonth,
            enabled = canNavigateToPreviousMonth
        ) {
            Text("Previous", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
        }
        Text(
            text = formatIncomeDateRange(dateRange),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )
        TextButton(
            onClick = onNextMonth,
            enabled = canNavigateToNextMonth
        ) {
            Text("Next", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {}
            .testTag("income-loading"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryDarkGreen)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Loading income records", color = TextSecondaryMuted)
        }
    }
}

@Composable
private fun EmptyContent(totalLkrMinor: Long) {
    Column(modifier = Modifier.fillMaxSize()) {
        IncomeSummary(totalLkrMinor)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No income records for this period.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MutedCoral
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen, contentColor = SurfaceWhite)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SuccessContent(
    uiState: IncomeUiState,
    deletingIncomeId: String?,
    onEditIncome: (Income, UserId?) -> Unit,
    onDeleteIncome: (Income, UserId?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            IncomeSummary(uiState.totalLkrMinor)
        }
        items(
            items = uiState.incomes,
            key = { income -> "income:${income.id}" }
        ) { income ->
            IncomeItemCard(
                income = income,
                isDeleting = deletingIncomeId == income.id,
                onEdit = { onEditIncome(income, uiState.ownerId) },
                onDelete = { onDeleteIncome(income, uiState.ownerId) }
            )
        }
    }
}

@Composable
private fun IncomeSummary(totalLkrMinor: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("income-summary"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "NET MONTHLY INCOME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatLkrMinorAmount(totalLkrMinor),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )
        }
    }
}

@Composable
private fun IncomeItemCard(
    income: Income,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCryptoLoss = income.source == IncomeSource.CRYPTO &&
        income.pnlDirection == PnlDirection.LOSS
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("income-item-${income.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incomeSourceLabel(income),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCryptoLoss) MutedCoral else TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTransactionDate(income),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                income.assetSymbol?.takeIf(String::isNotBlank)?.let { symbol ->
                    Text(
                        text = "Asset: $symbol",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
                income.projectReference?.takeIf(String::isNotBlank)?.let { reference ->
                    Text(
                        text = "Project: $reference",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
                income.note?.takeIf(String::isNotBlank)?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatOriginalAmount(income.money),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCryptoLoss) MutedCoral else PrimaryDarkGreen
                )
                Row {
                    TextButton(
                        onClick = onEdit,
                        enabled = !isDeleting,
                        modifier = Modifier.testTag("edit-income-${income.id}")
                    ) {
                        Text("Edit", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDelete,
                        enabled = !isDeleting,
                        modifier = Modifier.testTag("delete-income-${income.id}")
                    ) {
                        Text("Delete", color = MutedCoral, fontWeight = FontWeight.Bold)
                    }
                }
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("deleting-income-${income.id}"),
                        color = PrimaryDarkGreen,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeDeleteConfirmationDialog(
    income: Income,
    isDeleting: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onCancel()
        },
        title = { Text("Delete income?", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This action cannot be undone.")
                Text("Record: ${incomeSourceLabel(income)}")
                Text("Amount: ${formatOriginalAmount(income.money)}")
                Text("Date: ${formatTransactionDate(income)}")
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MutedCoral,
                        modifier = Modifier.testTag("income-delete-error")
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isDeleting,
                modifier = Modifier.testTag("income-delete-cancel")
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MutedCoral, contentColor = SurfaceWhite),
                modifier = Modifier.testTag("income-delete-confirm")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("income-delete-progress"),
                        color = SurfaceWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Deleting")
                } else {
                    Text("Delete")
                }
            }
        },
        modifier = Modifier.testTag("income-delete-dialog")
    )
}
