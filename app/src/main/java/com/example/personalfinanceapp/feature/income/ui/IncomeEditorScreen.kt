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
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.feature.income.rate.IncomeManualRateSource
import com.example.personalfinanceapp.feature.income.rate.IncomeRateSelectionMode
import java.math.BigDecimal
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
    onCurrencyChanged: (CurrencyCode) -> Unit,
    onRateModeChanged: (IncomeRateSelectionMode) -> Unit,
    onManualRateChanged: (String) -> Unit,
    onManualRateSourceChanged: (IncomeManualRateSource) -> Unit,
    onManualRateDateChanged: (LocalDate) -> Unit,
    onRefreshAutomaticRate: () -> Unit,
    onProjectReferenceChanged: (String) -> Unit,
    onReceivableChanged: (String?) -> Unit,
    onAssetSymbolChanged: (String) -> Unit,
    onPnlDirectionChanged: (PnlDirection) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var receivableMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showRateDatePicker by remember { mutableStateOf(false) }

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

    if (showRateDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.manualRateDate?.toUtcEpochMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showRateDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis
                            ?.toLocalDateUtc()
                            ?.let(onManualRateDateChanged)
                        showRateDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDatePicker = false }) {
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
                text = "Choose LKR or USD. USD values are stored with their exchange-rate snapshot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.testTag("income-currency"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Currency", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyCode.entries.forEach { currency ->
                        RadioButton(
                            selected = uiState.selectedCurrency == currency,
                            onClick = { onCurrencyChanged(currency) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.testTag("income-currency-${currency.name}")
                        )
                        Text(currency.name)
                    }
                }
            }

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
                label = { Text("Amount (${uiState.selectedCurrency.name})") },
                supportingText = (uiState.errors.amount ?: uiState.amountValidationError)?.let { error ->
                    { Text(error) }
                },
                isError = uiState.errors.amount != null || uiState.amountValidationError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income-amount")
            )

            if (uiState.selectedCurrency == CurrencyCode.USD) {
                UsdRateControls(
                    uiState = uiState,
                    onRateModeChanged = onRateModeChanged,
                    onManualRateChanged = onManualRateChanged,
                    onManualRateSourceChanged = onManualRateSourceChanged,
                    onSelectManualRateDate = { showRateDatePicker = true },
                    onRefreshAutomaticRate = onRefreshAutomaticRate
                )
            }

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

                val eligibleReceivables = uiState.availableReceivables.filter {
                    !it.archived && it.currency == uiState.selectedCurrency
                }
                ExposedDropdownMenuBox(
                    expanded = receivableMenuExpanded,
                    onExpandedChange = { if (!uiState.isSaving) receivableMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selected = uiState.availableReceivables.firstOrNull {
                        it.id == uiState.selectedReceivableId
                    }
                    OutlinedTextField(
                        value = selected?.let { "${it.title} · ${it.currency} ${it.expectedOriginalDecimal}" }
                            ?: "Not linked",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !uiState.isSaving,
                        label = { Text("Receivable (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(receivableMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("income-receivable")
                    )
                    ExposedDropdownMenu(
                        expanded = receivableMenuExpanded,
                        onDismissRequest = { receivableMenuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Not linked") }, onClick = {
                            onReceivableChanged(null); receivableMenuExpanded = false
                        })
                        eligibleReceivables.forEach { receivable ->
                            DropdownMenuItem(
                                text = { Text("${receivable.title} · ${receivable.currency} ${receivable.expectedOriginalDecimal}") },
                                onClick = { onReceivableChanged(receivable.id); receivableMenuExpanded = false }
                            )
                        }
                    }
                }
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
                    enabled = !uiState.isSaving &&
                        !uiState.isLoadingAutomaticRate &&
                        (uiState.selectedCurrency == CurrencyCode.LKR ||
                            uiState.estimatedMoney != null),
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

@Composable
private fun UsdRateControls(
    uiState: IncomeFormUiState,
    onRateModeChanged: (IncomeRateSelectionMode) -> Unit,
    onManualRateChanged: (String) -> Unit,
    onManualRateSourceChanged: (IncomeManualRateSource) -> Unit,
    onSelectManualRateDate: () -> Unit,
    onRefreshAutomaticRate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("income-usd-rate-controls"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("USD/LKR exchange rate", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RateModeOption(
                label = "Automatic reference",
                selected = uiState.selectedRateMode == IncomeRateSelectionMode.AUTOMATIC,
                enabled = !uiState.isSaving,
                tag = "income-rate-mode-automatic",
                onClick = { onRateModeChanged(IncomeRateSelectionMode.AUTOMATIC) }
            )
            RateModeOption(
                label = "Manual",
                selected = uiState.selectedRateMode == IncomeRateSelectionMode.MANUAL,
                enabled = !uiState.isSaving,
                tag = "income-rate-mode-manual",
                onClick = { onRateModeChanged(IncomeRateSelectionMode.MANUAL) }
            )
        }

        if (uiState.selectedRateMode == IncomeRateSelectionMode.AUTOMATIC) {
            Text(
                "Automatic rates are reference suggestions and may differ from your transfer rate.",
                style = MaterialTheme.typography.bodySmall
            )
            if (uiState.isLoadingAutomaticRate) {
                Row(
                    modifier = Modifier.testTag("income-rate-loading"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading USD/LKR reference rate")
                }
            }
            uiState.automaticRateError?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("income-rate-network-error")
                )
            }
            Button(
                onClick = onRefreshAutomaticRate,
                enabled = !uiState.isSaving && !uiState.isLoadingAutomaticRate,
                modifier = Modifier.testTag("income-rate-refresh")
            ) {
                Text(if (uiState.automaticRateError == null) "Refresh reference rate" else "Retry rate")
            }
            if (uiState.automaticRateError != null) {
                TextButton(
                    onClick = { onRateModeChanged(IncomeRateSelectionMode.MANUAL) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("income-rate-manual-fallback")
                ) {
                    Text("Enter a manual rate")
                }
            }
        } else if (uiState.selectedRateMode == IncomeRateSelectionMode.MANUAL) {
            OutlinedTextField(
                value = uiState.manualRateText,
                onValueChange = onManualRateChanged,
                enabled = !uiState.isSaving,
                label = { Text("LKR per USD") },
                supportingText = (uiState.errors.rate ?: uiState.rateValidationError)?.let { error ->
                    { Text(error) }
                },
                isError = uiState.errors.rate != null || uiState.rateValidationError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income-manual-rate")
            )
            Text("Manual rate type", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RateSourceOption(
                    label = "Actual transfer",
                    source = IncomeManualRateSource.ACTUAL_TRANSFER,
                    uiState = uiState,
                    onChanged = onManualRateSourceChanged
                )
                RateSourceOption(
                    label = "Reference",
                    source = IncomeManualRateSource.REFERENCE,
                    uiState = uiState,
                    onChanged = onManualRateSourceChanged
                )
            }
            Button(
                onClick = onSelectManualRateDate,
                enabled = !uiState.isSaving,
                modifier = Modifier.testTag("income-manual-rate-date")
            ) {
                Text("Rate date: ${uiState.manualRateDate ?: "Select date"}")
            }
        }

        uiState.errors.rate?.takeIf {
            uiState.selectedRateMode != IncomeRateSelectionMode.MANUAL
        }?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        uiState.estimatedMoney?.let { money ->
            Column(
                modifier = Modifier.testTag("income-conversion-preview"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Selected rate: ${money.rateLkrPerUnitDecimal} LKR/USD")
                Text("Converted amount: LKR ${formatMinorUnits(money.baseAmountMinor)}")
                Text("Rate source: ${money.rateSource.displayName}")
                Text("Rate date: ${money.rateDate}")
            }
        }
    }
}

@Composable
private fun RateModeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(tag)
        )
        Text(label)
    }
}

@Composable
private fun RateSourceOption(
    label: String,
    source: IncomeManualRateSource,
    uiState: IncomeFormUiState,
    onChanged: (IncomeManualRateSource) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = uiState.manualRateSource == source,
            onClick = { onChanged(source) },
            enabled = !uiState.isSaving,
            modifier = Modifier.testTag("income-manual-source-${source.name}")
        )
        Text(label)
    }
}

private val RateSource.displayName: String
    get() = when (this) {
        RateSource.LKR_FIXED -> "LKR fixed"
        RateSource.MANUAL_TRANSFER -> "Manual transfer"
        RateSource.MANUAL_REFERENCE -> "Manual reference"
        RateSource.EXCHANGE_RATE_API_REFERENCE -> "Automatic API reference"
    }

private fun formatMinorUnits(value: Long): String =
    BigDecimal.valueOf(value, 2).toPlainString()

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
