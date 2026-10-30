package com.example.personalfinanceapp.feature.recurring.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

private val DueBackground = Color(0xFFF7F9F7)
private val DueRed = Color(0xFFB33A32)
private val DueRedBackground = Color(0xFFFFECEA)

@Composable
fun DueItemsScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.operationMessage) {
        state.operationMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    paymentState?.let {
        PaymentConfirmationDialog(
            state = it,
            onAmountChanged = viewModel::updatePaymentAmount,
            onDateChanged = viewModel::updatePaymentDate,
            onDismiss = viewModel::closePayment,
            onConfirm = viewModel::confirmPayment
        )
    }

    DueItemsContent(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onRecordPayment = viewModel::openPayment,
        onSkip = viewModel::skip
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueItemsContent(
    state: RecurringUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRecordPayment: (RecurringDueItem) -> Unit,
    onSkip: (RecurringDueItem) -> Unit
) {
    val overdue = state.dueItems.filter {
        it.occurrence.status == RecurringOccurrenceStatus.PENDING && it.dueDate.isBefore(state.today)
    }
    val upcoming = state.dueItems.filter {
        it.occurrence.status == RecurringOccurrenceStatus.PENDING && !it.dueDate.isBefore(state.today)
    }
    val reconciled = state.dueItems.filter {
        it.occurrence.status != RecurringOccurrenceStatus.PENDING
    }.sortedByDescending { it.dueDate }

    Scaffold(
        modifier = Modifier.testTag("due_items_screen"),
        containerColor = DueBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Due Items & Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DueSummary(state, overdue, upcoming) }
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (overdue.isNotEmpty()) {
                    item { SectionHeading("Overdue occurrences", "Requires immediate action", DueRed) }
                    items(overdue, key = { it.occurrence.id }) {
                        DueItemCard(it, true, onRecordPayment, onSkip)
                    }
                }
                if (upcoming.isNotEmpty()) {
                    item { SectionHeading("Upcoming", "Next 30 days", PrimaryDarkGreen) }
                    items(upcoming, key = { it.occurrence.id }) {
                        DueItemCard(it, false, onRecordPayment, onSkip)
                    }
                }
                if (reconciled.isNotEmpty()) {
                    item { SectionHeading("Reconciled cycle", "Safe from duplicate recording", PrimaryDarkGreen) }
                    items(reconciled, key = { it.occurrence.id }) { ReconciledCard(it) }
                }
                if (overdue.isEmpty() && upcoming.isEmpty() && reconciled.isEmpty()) {
                    item {
                        Text(
                            "No due items in this period.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            color = TextSecondaryMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DueSummary(
    state: RecurringUiState,
    overdue: List<RecurringDueItem>,
    upcoming: List<RecurringDueItem>
) {
    val pendingTotal = (overdue + upcoming).sumOf { it.recurringExpense.expectedAmountMinor }
    Card(
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BRIEFING CHANNEL ACTIVE", color = Color.White.copy(alpha = .72f))
            Text("Immediate action", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Pending scheduled drain", color = Color.White.copy(alpha = .72f))
            Text("LKR ${minorToDecimal(pendingTotal)}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${overdue.size} overdue", color = Color.White)
                Text("${upcoming.count { !it.dueDate.isAfter(state.today.plusDays(7)) }} due this week", color = Color.White)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String, color: Color) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = color)
        Text(subtitle, color = TextSecondaryMuted)
    }
}

@Composable
private fun DueItemCard(
    item: RecurringDueItem,
    overdue: Boolean,
    onRecordPayment: (RecurringDueItem) -> Unit,
    onSkip: (RecurringDueItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("due_item_${item.occurrence.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (overdue) DueRedBackground else SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(
                        if (overdue) DueRed.copy(alpha = .12f) else SecondaryPaleGreen,
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) { Text(if (overdue) "!" else "↻", color = if (overdue) DueRed else PrimaryDarkGreen, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.recurringExpense.title, fontWeight = FontWeight.Bold)
                    Text(item.category?.name ?: "Uncategorised", color = TextSecondaryMuted)
                }
                Text("LKR ${minorToDecimal(item.recurringExpense.expectedAmountMinor)}", fontWeight = FontWeight.Bold)
            }
            Text(
                if (overdue) "Due ${item.dueDate} • overdue" else "Due ${item.dueDate}",
                color = if (overdue) DueRed else TextSecondaryMuted,
                fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal
            )
            if (item.recurringExpense.reminderEnabled) {
                Text(
                    "Alert ${item.recurringExpense.reminderLeadDays} day(s) before",
                    color = TextSecondaryMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onRecordPayment(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (overdue) DueRed else PrimaryDarkGreen),
                    modifier = Modifier.testTag("due_record_${item.occurrence.id}")
                ) { Text("Record Payment") }
                OutlinedButton(
                    onClick = { onSkip(item) },
                    modifier = Modifier.testTag("due_skip_${item.occurrence.id}")
                ) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun ReconciledCard(item: RecurringDueItem) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("reconciled_${item.occurrence.id}"),
        colors = CardDefaults.cardColors(containerColor = SecondaryPaleGreen),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (item.occurrence.status == RecurringOccurrenceStatus.CONFIRMED) "✓" else "–", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {
                Text(item.recurringExpense.title, fontWeight = FontWeight.Bold)
                Text("${item.dueDate} • ${item.occurrence.status.name.lowercase().replaceFirstChar(Char::uppercase)}", color = TextSecondaryMuted)
            }
            if (item.occurrence.status == RecurringOccurrenceStatus.CONFIRMED) {
                Text("Linked to expense", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
