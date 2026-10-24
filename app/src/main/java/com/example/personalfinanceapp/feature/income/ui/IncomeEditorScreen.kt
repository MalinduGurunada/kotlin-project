package com.example.personalfinanceapp.feature.income.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEditorScreen(
    uiState: IncomeFormUiState,
    onBack: () -> Unit,
    onSourceChanged: (IncomeSource) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onProjectReferenceChanged: (String) -> Unit,
    onAssetSymbolChanged: (String) -> Unit,
    onPnlDirectionChanged: (PnlDirection) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        if (!uiState.isSaving) onBack()
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.transactionDate?.toUtcEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis
                            ?.toLocalDateUtc()
                            ?.let(onDateChanged)
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.mode == IncomeEditorMode.ADD) {
                            "Add Income"
                        } else {
                            "Edit Income"
                        }
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !uiState.isSaving
                    ) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "All amounts in this form are recorded in LKR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = sourceMenuExpanded,
                onExpandedChange = {
                    if (!uiState.isSaving) sourceMenuExpanded = !sourceMenuExpanded
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedSource.displayName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !uiState.isSaving,
                    label = { Text("Income source") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(sourceMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("income-source")
                )
                ExposedDropdownMenu(
                    expanded = sourceMenuExpanded,
                    onDismissRequest = { sourceMenuExpanded = false }
                ) {
                    IncomeSource.entries.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source.displayName) },
                            onClick = {
                                onSourceChanged(source)
                                sourceMenuExpanded = false
                            },
                            modifier = Modifier.testTag("income-source-option-${source.name}")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = onAmountChanged,
                enabled = !uiState.isSaving,
                label = { Text("Amount (LKR)") },
                supportingText = uiState.errors.amount?.let { error ->
                    { Text(error) }
                },
                isError = uiState.errors.amount != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income-amount")
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("income-date")
                ) {
                    Text(
                        uiState.transactionDate?.let { "Transaction date: $it" }
                            ?: "Select transaction date"
                    )
                }
                uiState.errors.transactionDate?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.selectedSource == IncomeSource.FREELANCE) {
                OutlinedTextField(
                    value = uiState.projectReference,
                    onValueChange = onProjectReferenceChanged,
                    enabled = !uiState.isSaving,
                    label = { Text("Project reference (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income-project-reference")
                )
            }

            if (uiState.selectedSource == IncomeSource.CRYPTO) {
                Column(
                    modifier = Modifier.testTag("income-pnl-direction"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Realized result",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.pnlDirection == PnlDirection.GAIN,
                            onClick = { onPnlDirectionChanged(PnlDirection.GAIN) },
                            enabled = !uiState.isSaving
                        )
                        Text("Gain")
                        RadioButton(
                            selected = uiState.pnlDirection == PnlDirection.LOSS,
                            onClick = { onPnlDirectionChanged(PnlDirection.LOSS) },
                            enabled = !uiState.isSaving
                        )
                        Text("Loss")
                    }
                    uiState.errors.pnlDirection?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.assetSymbol,
                    onValueChange = onAssetSymbolChanged,
                    enabled = !uiState.isSaving,
                    label = { Text("Asset symbol (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income-asset-symbol")
                )
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChanged,
                enabled = !uiState.isSaving,
                label = { Text("Note (optional)") },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income-note")
            )

            uiState.errors.form?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            uiState.saveError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("income-save-error")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = !uiState.isSaving
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("income-save")
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("income-saving"),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving")
                    } else {
                        Text(
                            if (uiState.mode == IncomeEditorMode.ADD) {
                                "Save Income"
                            } else {
                                "Save Changes"
                            }
                        )
                    }
                }
            }
        }
    }
}

private val IncomeSource.displayName: String
    get() = when (this) {
        IncomeSource.SALARY -> "Salary"
        IncomeSource.FREELANCE -> "Freelance"
        IncomeSource.ADSENSE -> "AdSense"
        IncomeSource.CRYPTO -> "Crypto"
    }

private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
