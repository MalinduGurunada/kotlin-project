package com.example.personalfinanceapp.feature.recurring.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.RecurringStatus
import com.example.personalfinanceapp.ui.theme.MutedCoral
import com.example.personalfinanceapp.ui.theme.PrimaryDarkGreen
import com.example.personalfinanceapp.ui.theme.SecondaryPaleGreen
import com.example.personalfinanceapp.ui.theme.SurfaceWhite
import com.example.personalfinanceapp.ui.theme.TextPrimaryDark
import com.example.personalfinanceapp.ui.theme.TextSecondaryMuted

private val RecurringBackground = Color(0xFFF7F9F7)
private val OverdueBackground = Color(0xFFFFECEA)
private val WarningRed = Color(0xFFB33A32)

@Composable
fun RecurringCommitmentsScreen(
    onBack: () -> Unit,
    onOpenDueItems: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.operationMessage) {
        uiState.operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (formState != null) {
        RecurringCommitmentEditorScreen(
            state = formState!!,
            categories = uiState.categories,
            onBack = viewModel::closeForm,
            onTitleChanged = viewModel::updateTitle,
            onCategoryChanged = viewModel::updateCategory,
            onAmountChanged = viewModel::updateAmount,
            onCadenceChanged = viewModel::updateCadence,
            onPaymentChannelChanged = viewModel::updatePaymentChannel,
            onChannelLabelChanged = viewModel::updateChannelLabel,
            onDueDateChanged = viewModel::updateDueDate,
            onReminderEnabledChanged = viewModel::updateReminderEnabled,
            onReminderLeadDaysChanged = viewModel::updateReminderLeadDays,
            onSave = viewModel::saveForm
        )
        return
    }

    paymentState?.let { payment ->
        PaymentConfirmationDialog(
            state = payment,
            onAmountChanged = viewModel::updatePaymentAmount,
            onDateChanged = viewModel::updatePaymentDate,
            onDismiss = viewModel::closePayment,
            onConfirm = viewModel::confirmPayment
        )
    }

    RecurringCommitmentsContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAdd = viewModel::openCreate,
        onOpenDueItems = onOpenDueItems,
        onFilterChanged = viewModel::selectFilter,
        onEdit = viewModel::openEdit,
        onStatusChanged = viewModel::setStatus,
        onRecordPayment = viewModel::openPayment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCommitmentsContent(
    state: RecurringUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenDueItems: () -> Unit,
    onFilterChanged: (RecurringFilter) -> Unit,
    onEdit: (RecurringExpense) -> Unit,
    onStatusChanged: (RecurringExpense, RecurringStatus) -> Unit,
    onRecordPayment: (RecurringDueItem) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag("recurring_commitments_screen"),
        containerColor = RecurringBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recurring Commitments", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                actions = {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                        modifier = Modifier.padding(end = 12.dp).testTag("add_commitment")
                    ) { Text("+ Add") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SummaryCard(state, onOpenDueItems)
            }
            item {
                FilterRow(state.selectedFilter, onFilterChanged)
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> item {
                    Text(state.errorMessage, color = WarningRed)
                }
                filteredCommitments(state).isEmpty() -> item {
                    EmptyCommitments(state.selectedFilter, onAdd)
                }
                else -> items(filteredCommitments(state), key = { it.id }) { recurring ->
                    CommitmentCard(
                        recurring = recurring,
                        category = state.categories.firstOrNull { it.id == recurring.categoryId },
                        nextItem = nextPendingItem(state, recurring),
                        today = state.today,
                        onEdit = { onEdit(recurring) },
                        onPauseResume = {
                            onStatusChanged(
                                recurring,
                                if (recurring.status == RecurringStatus.PAUSED) {
                                    RecurringStatus.ACTIVE
                                } else {
                                    RecurringStatus.PAUSED
                                }
                            )
                        },
                        onCancel = { onStatusChanged(recurring, RecurringStatus.CANCELLED) },
                        onRecordPayment = { nextPendingItem(state, recurring)?.let(onRecordPayment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: RecurringUiState, onOpenDueItems: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("TOTAL MONTHLY COMMITTED", color = Color.White.copy(alpha = .75f))
            Text(
                "LKR ${minorToDecimal(state.monthlyCommittedMinor)} / mo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Active ledger", "${state.activeExpenses.size} Active (${state.pausedExpenses.size} Paused)")
                Metric("Due this week", "LKR ${minorToDecimal(state.dueThisWeekMinor)}")
            }
            OutlinedButton(
                onClick = onOpenDueItems,
                modifier = Modifier.fillMaxWidth().testTag("open_due_items")
            ) { Text("View Due Items", color = Color.White) }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column { Text(label, color = Color.White.copy(alpha = .72f)); Text(value, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun FilterRow(selected: RecurringFilter, onSelected: (RecurringFilter) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RecurringFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) },
                modifier = Modifier.testTag("recurring_filter_${filter.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun CommitmentCard(
    recurring: RecurringExpense,
    category: Category?,
    nextItem: RecurringDueItem?,
    today: java.time.LocalDate,
    onEdit: () -> Unit,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val overdue = nextItem?.dueDate?.isBefore(today) == true
    val background = when {
        overdue -> OverdueBackground
        recurring.status == RecurringStatus.PAUSED -> Color(0xFFF0F1F0)
        else -> SurfaceWhite
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("commitment_${recurring.id}"),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(SecondaryPaleGreen, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("↻", color = PrimaryDarkGreen, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(recurring.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text(category?.name ?: "Uncategorised", color = TextSecondaryMuted)
                }
                Text(
                    when {
                        recurring.status == RecurringStatus.PAUSED -> "Paused"
                        overdue -> "Overdue"
                        nextItem != null -> "Upcoming"
                        else -> recurring.status.name.lowercase().replaceFirstChar(Char::uppercase)
                    },
                    color = if (overdue) WarningRed else PrimaryDarkGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Next due", color = TextSecondaryMuted)
                    Text(nextItem?.dueDate?.toString() ?: "Not scheduled", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(cadenceLabel(recurring), color = TextSecondaryMuted)
                    Text("LKR ${minorToDecimal(recurring.expectedAmountMinor)}", fontWeight = FontWeight.Bold)
                }
            }
            if (recurring.reminderEnabled && nextItem != null) {
                Text(
                    "Reminder ${recurring.reminderLeadDays} day(s) before • ${recurring.paymentChannel.name}",
                    color = TextSecondaryMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (nextItem != null && recurring.active) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (overdue) WarningRed else PrimaryDarkGreen
                        ),
                        modifier = Modifier.testTag("record_payment_${nextItem.occurrence.id}")
                    ) { Text("Record Payment") }
                }
                OutlinedButton(onClick = onEdit) { Text("Manage") }
                if (recurring.status != RecurringStatus.CANCELLED) {
                    TextButton(onClick = onPauseResume) {
                        Text(if (recurring.status == RecurringStatus.PAUSED) "Resume" else "Pause")
                    }
                }
                if (recurring.status == RecurringStatus.PAUSED) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = MutedCoral) }
                }
            }
        }
    }
}

@Composable
private fun EmptyCommitments(filter: RecurringFilter, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (filter == RecurringFilter.ALL) "No recurring commitments yet" else "No ${filter.name.lowercase()} commitments")
        if (filter == RecurringFilter.ALL) Button(onClick = onAdd) { Text("Add Commitment") }
    }
}

private fun filteredCommitments(state: RecurringUiState): List<RecurringExpense> =
    state.recurringExpenses.filter { recurring ->
        when (state.selectedFilter) {
            RecurringFilter.ALL -> recurring.status != RecurringStatus.CANCELLED
            RecurringFilter.PAUSED -> recurring.status == RecurringStatus.PAUSED
            RecurringFilter.UPCOMING -> nextPendingItem(state, recurring)?.dueDate?.let {
                !it.isBefore(state.today)
            } == true
            RecurringFilter.OVERDUE -> nextPendingItem(state, recurring)?.dueDate?.isBefore(state.today) == true
        }
    }

private fun nextPendingItem(state: RecurringUiState, recurring: RecurringExpense): RecurringDueItem? =
    state.dueItems.firstOrNull {
        it.recurringExpense.id == recurring.id &&
            it.occurrence.status == RecurringOccurrenceStatus.PENDING
    }

private fun cadenceLabel(item: RecurringExpense): String = when {
    item.frequency == RecurrenceFrequency.MONTHLY && item.intervalCount == 2L -> "Every 2 months"
    item.frequency == RecurrenceFrequency.MONTHLY && item.intervalCount == 3L -> "Quarterly"
    item.intervalCount == 1L -> item.frequency.name.lowercase().replaceFirstChar(Char::uppercase)
    else -> "Every ${item.intervalCount} ${item.frequency.name.lowercase()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCommitmentEditorScreen(
    state: RecurringFormState,
    categories: List<Category>,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCadenceChanged: (RecurrenceFrequency, Long) -> Unit,
    onPaymentChannelChanged: (PaymentChannel) -> Unit,
    onChannelLabelChanged: (String) -> Unit,
    onDueDateChanged: (String) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderLeadDaysChanged: (Long) -> Unit,
    onSave: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var channelExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onReminderEnabledChanged(granted)
    }
    val onReminderToggle: (Boolean) -> Unit = { enabled ->
        if (
            enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminderEnabledChanged(enabled)
        }
    }
    Scaffold(
        modifier = Modifier.testTag("recurring_editor_screen"),
        containerColor = RecurringBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Commitment" else "Add Commitment", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle("Service & Scope", "Commitment particulars") }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChanged,
                    label = { Text("Commitment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("commitment_name")
                )
            }
            item {
                ExposedDropdownMenuBox(categoryExpanded, { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == state.categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                            .testTag("commitment_category")
                    )
                    ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { onCategoryChanged(category.id); categoryExpanded = false }
                            )
                        }
                    }
                }
            }
            item { SectionTitle("Outflow Cadence", "Recurring amount & cycle") }
            item {
                OutlinedTextField(
                    state.amount,
                    onAmountChanged,
                    label = { Text("Amount (LKR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("commitment_amount")
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Billing frequency", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CadenceChip("Weekly", state, RecurrenceFrequency.WEEKLY, 1L, onCadenceChanged)
                        CadenceChip("Monthly", state, RecurrenceFrequency.MONTHLY, 1L, onCadenceChanged)
                        CadenceChip("2 Months", state, RecurrenceFrequency.MONTHLY, 2L, onCadenceChanged)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CadenceChip("Quarterly", state, RecurrenceFrequency.MONTHLY, 3L, onCadenceChanged)
                        CadenceChip("Annually", state, RecurrenceFrequency.YEARLY, 1L, onCadenceChanged)
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(channelExpanded, { channelExpanded = it }) {
                    OutlinedTextField(
                        state.paymentChannel.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment channel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(channelExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(channelExpanded, { channelExpanded = false }) {
                        PaymentChannel.entries.forEach { channel ->
                            DropdownMenuItem(
                                text = { Text(channel.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                onClick = { onPaymentChannelChanged(channel); channelExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    state.channelLabel,
                    onChannelLabelChanged,
                    label = { Text("Account or channel label (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    state.dueDate,
                    onDueDateChanged,
                    label = { Text("Next scheduled due date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("commitment_due_date")
                )
            }
            item { SectionTitle("Alert Frequency", "Push & in-app reminders") }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", fontWeight = FontWeight.SemiBold)
                        Text("Create an alert before each due date", color = TextSecondaryMuted)
                    }
                    Switch(state.reminderEnabled, onReminderToggle)
                }
            }
            if (state.reminderEnabled) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0L to "On due date", 1L to "1 day before", 2L to "2 days before").forEach { (days, label) ->
                        FilterChip(
                            selected = state.reminderLeadDays == days,
                            onClick = { onReminderLeadDaysChanged(days) },
                            label = { Text(label) }
                        )
                    }
                }
            }
            state.errorMessage?.let { message -> item { Text(message, color = WarningRed) } }
            item {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_commitment"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                ) {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White)
                    else Text("Save Commitment")
                }
            }
            item { TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel / Discard") } }
        }
    }
}

@Composable
private fun CadenceChip(
    label: String,
    state: RecurringFormState,
    frequency: RecurrenceFrequency,
    interval: Long,
    onChanged: (RecurrenceFrequency, Long) -> Unit
) {
    FilterChip(
        selected = state.frequency == frequency && state.intervalCount == interval,
        onClick = { onChanged(frequency, interval) },
        label = { Text(label) }
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondaryMuted)
    }
}

@Composable
internal fun PaymentConfirmationDialog(
    state: PaymentConfirmationState,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record ${state.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Due ${state.dueDate}. Confirming creates one linked expense.")
                OutlinedTextField(state.amount, onAmountChanged, label = { Text("Paid amount (LKR)") })
                OutlinedTextField(state.paidDate, onDateChanged, label = { Text("Payment date (YYYY-MM-DD)") })
                state.errorMessage?.let { Text(it, color = WarningRed) }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isSaving,
                modifier = Modifier.testTag("confirm_recurring_payment")
            ) { Text(if (state.isSaving) "Recording…" else "Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
