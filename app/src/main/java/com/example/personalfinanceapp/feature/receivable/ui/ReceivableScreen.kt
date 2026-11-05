package com.example.personalfinanceapp.feature.receivable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.feature.receivable.ReceivableSettlement
import com.example.personalfinanceapp.feature.receivable.ReceivableStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableScreen(
    uiState: ReceivableUiState,
    formState: ReceivableFormState?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Receivable) -> Unit,
    onArchive: (Receivable, Boolean) -> Unit,
    onCloseForm: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onProjectChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (CurrencyCode) -> Unit,
    onDateChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (formState == null) "Receivables" else if (formState.isEditing) "Edit Receivable" else "New Receivable") },
                navigationIcon = { TextButton(onClick = if (formState == null) onBack else onCloseForm) { Text("Back") } }
            )
        }
    ) { padding ->
        if (formState != null) {
            ReceivableForm(formState, onTitleChanged, onProjectChanged, onAmountChanged, onCurrencyChanged, onDateChanged, onSave, Modifier.padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create Receivable") }
                Spacer(Modifier.height(12.dp))
                when {
                    uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    uiState.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage)
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    uiState.settlements.isEmpty() -> Text("No receivables yet. Create one to track freelance payments.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.settlements, key = { it.receivable.id }) { settlement ->
                            ReceivableCard(settlement, onEdit, onArchive)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivableCard(
    settlement: ReceivableSettlement,
    onEdit: (Receivable) -> Unit,
    onArchive: (Receivable, Boolean) -> Unit
) {
    val receivable = settlement.receivable
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(receivable.title, fontWeight = FontWeight.Bold)
                Text(settlement.status.label)
            }
            Text(receivable.projectReference)
            Text("Expected: ${receivable.currency} ${settlement.expectedDecimal}")
            Text("Paid: ${receivable.currency} ${settlement.paidDecimal}")
            Text("Remaining: ${receivable.currency} ${settlement.remainingDecimal}")
            Text("Expected date: ${receivable.expectedDate}")
            if (settlement.payments.isNotEmpty()) {
                Text("Payment history", fontWeight = FontWeight.SemiBold)
                settlement.payments.forEach { payment ->
                    Text("${payment.occurredDate} · ${payment.money.originalCurrency} ${payment.money.originalAmountDecimal}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(receivable) }) { Text("Edit") }
                OutlinedButton(onClick = { onArchive(receivable, !receivable.archived) }) {
                    Text(if (receivable.archived) "Restore" else "Archive")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivableForm(
    state: ReceivableFormState,
    onTitleChanged: (String) -> Unit,
    onProjectChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCurrencyChanged: (CurrencyCode) -> Unit,
    onDateChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(state.title, onTitleChanged, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(state.projectReference, onProjectChanged, label = { Text("Project reference") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(state.amount, onAmountChanged, label = { Text("Expected amount") }, modifier = Modifier.fillMaxWidth())
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!state.isEditing) expanded = it }
        ) {
            OutlinedTextField(
                value = state.currency.name, onValueChange = {}, readOnly = true,
                enabled = !state.isEditing,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Currency") }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CurrencyCode.entries.forEach { currency ->
                    DropdownMenuItem(text = { Text(currency.name) }, onClick = { onCurrencyChanged(currency); expanded = false })
                }
            }
        }
        OutlinedTextField(state.expectedDate, onDateChanged, label = { Text("Expected date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        state.errorMessage?.let { Text(it) }
        Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            if (state.isSaving) CircularProgressIndicator() else Text("Save Receivable")
        }
    }
}

private val ReceivableStatus.label: String get() = when (this) {
    ReceivableStatus.PENDING -> "Pending"
    ReceivableStatus.PARTIALLY_PAID -> "Partially Paid"
    ReceivableStatus.SETTLED -> "Settled"
}
