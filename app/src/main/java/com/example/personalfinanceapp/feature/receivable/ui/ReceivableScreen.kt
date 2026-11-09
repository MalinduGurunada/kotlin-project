package com.example.personalfinanceapp.feature.receivable.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.feature.receivable.ReceivableSettlement
import com.example.personalfinanceapp.feature.receivable.ReceivableStatus
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted
import java.time.LocalDate

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
                title = {
                    Text(
                        text = if (formState == null) "Receivables" else if (formState.isEditing) "Edit Receivable" else "New Receivable",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (formState == null) onBack else onCloseForm) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        if (formState != null) {
            ReceivableForm(formState, onTitleChanged, onProjectChanged, onAmountChanged, onCurrencyChanged, onDateChanged, onSave, Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen, contentColor = SurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("+ Create Receivable", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = PrimaryDarkGreen
                    )
                    uiState.errorMessage != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage)
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                        ) { Text("Retry") }
                    }
                    uiState.settlements.isEmpty() -> Text(
                        "No receivables yet. Create one to track freelance payments.",
                        color = TextSecondaryMuted,
                        fontSize = 14.sp
                    )
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
    val isOverdue = settlement.status != ReceivableStatus.SETTLED &&
        receivable.expectedDate.isBefore(LocalDate.now())

    val badgeBackground = when {
        isOverdue -> Color(0xFFFFECEA)
        settlement.status == ReceivableStatus.PENDING -> Color(0xFFFFF8E1)
        settlement.status == ReceivableStatus.PARTIALLY_PAID -> Color(0xFFE0F2FE)
        else -> SecondaryPaleGreen
    }
    val badgeTextColor = when {
        isOverdue -> Color(0xFFB33A32)
        settlement.status == ReceivableStatus.PENDING -> Color(0xFFB78103)
        settlement.status == ReceivableStatus.PARTIALLY_PAID -> Color(0xFF0369A1)
        else -> PrimaryDarkGreen
    }
    val badgeLabel = if (isOverdue) "Overdue" else settlement.status.label

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(receivable.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimaryDark)
                Box(
                    modifier = Modifier
                        .background(badgeBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(badgeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                }
            }
            Text("Project: ${receivable.projectReference}", color = TextSecondaryMuted, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expected:", color = TextSecondaryMuted, fontSize = 13.sp)
                Text("${receivable.currency} ${settlement.expectedDecimal}", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paid:", color = TextSecondaryMuted, fontSize = 13.sp)
                Text("${receivable.currency} ${settlement.paidDecimal}", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remaining:", color = TextSecondaryMuted, fontSize = 13.sp)
                Text("${receivable.currency} ${settlement.remainingDecimal}", color = TextSecondaryMuted, fontWeight = FontWeight.Bold)
            }
            Text("Expected date: ${receivable.expectedDate}", color = TextSecondaryMuted, fontSize = 12.sp)
            if (settlement.payments.isNotEmpty()) {
                Text("Payment history", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                settlement.payments.forEach { payment ->
                    Text("${payment.occurredDate} · ${payment.money.originalCurrency} ${payment.money.originalAmountDecimal}", color = TextSecondaryMuted, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(receivable) }) { Text("Edit", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { onArchive(receivable, !receivable.archived) }) {
                    Text(if (receivable.archived) "Restore" else "Archive", color = TextSecondaryMuted)
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
    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen, contentColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.isSaving) CircularProgressIndicator(color = SurfaceWhite) else Text("Save Receivable", fontWeight = FontWeight.Bold)
        }
    }
}

private val ReceivableStatus.label: String get() = when (this) {
    ReceivableStatus.PENDING -> "Pending"
    ReceivableStatus.PARTIALLY_PAID -> "Partially Paid"
    ReceivableStatus.SETTLED -> "Settled"
}
