package com.example.personalfinanceapp.feature.recurring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.RecurringStatus
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.recurring.RecurringReminderScheduler
import com.example.personalfinanceapp.feature.recurring.RecurringRepository
import com.example.personalfinanceapp.feature.recurring.RecurringScheduleCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

enum class RecurringFilter { ALL, UPCOMING, OVERDUE, PAUSED }

data class RecurringDueItem(
    val recurringExpense: RecurringExpense,
    val category: Category?,
    val occurrence: RecurringOccurrence
) {
    val dueDate: LocalDate get() = occurrence.dueDate
}

data class RecurringUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val dueItems: List<RecurringDueItem> = emptyList(),
    val selectedFilter: RecurringFilter = RecurringFilter.ALL,
    val errorMessage: String? = null,
    val operationMessage: String? = null
) {
    val activeExpenses: List<RecurringExpense>
        get() = recurringExpenses.filter { it.status == RecurringStatus.ACTIVE }
    val pausedExpenses: List<RecurringExpense>
        get() = recurringExpenses.filter { it.status == RecurringStatus.PAUSED }
    val monthlyCommittedMinor: Long
        get() = activeExpenses.sumOf { expense ->
            when (expense.frequency) {
                RecurrenceFrequency.WEEKLY ->
                    (expense.expectedAmountMinor * 52L) / (12L * expense.intervalCount)
                RecurrenceFrequency.MONTHLY -> expense.expectedAmountMinor / expense.intervalCount
                RecurrenceFrequency.YEARLY -> expense.expectedAmountMinor / (12L * expense.intervalCount)
            }
        }
    val dueThisWeekMinor: Long
        get() = dueItems.filter {
            it.occurrence.status == RecurringOccurrenceStatus.PENDING &&
                !it.dueDate.isBefore(today) && !it.dueDate.isAfter(today.plusDays(7))
        }.sumOf { it.recurringExpense.expectedAmountMinor }
}

data class RecurringFormState(
    val id: String,
    val isEditing: Boolean,
    val title: String = "",
    val categoryId: String = "",
    val amount: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalCount: Long = 1L,
    val paymentChannel: PaymentChannel = PaymentChannel.BANK,
    val channelLabel: String = "",
    val dueDate: String,
    val reminderEnabled: Boolean = false,
    val reminderLeadDays: Long = 2L,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class PaymentConfirmationState(
    val recurringExpenseId: String,
    val title: String,
    val dueDate: LocalDate,
    val amount: String,
    val paidDate: String,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: RecurringReminderScheduler,
    private val clock: Clock
) : ViewModel() {

    private val reminderSignatures = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow<RecurringFormState?>(null)
    val formState: StateFlow<RecurringFormState?> = _formState.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentConfirmationState?>(null)
    val paymentState: StateFlow<PaymentConfirmationState?> = _paymentState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                recurringRepository.observeRecurringExpenses(),
                recurringRepository.observeOccurrences(),
                categoryRepository.observeCategories()
            ) { recurring, storedOccurrences, categories ->
                val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
                val occurrenceById = storedOccurrences.associateBy { it.id }
                val categoryById = categories.associateBy { it.id }
                val dueItems = recurring.flatMap { schedule ->
                    buildDueItems(schedule, occurrenceById, categoryById[schedule.categoryId], today)
                }.sortedWith(compareBy<RecurringDueItem> { it.dueDate }.thenBy { it.recurringExpense.title })
                RecurringUiState(
                    isLoading = false,
                    today = today,
                    recurringExpenses = recurring,
                    categories = categories.filterNot { it.archived },
                    dueItems = dueItems,
                    selectedFilter = _uiState.value.selectedFilter,
                    operationMessage = _uiState.value.operationMessage
                )
            }.catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load recurring commitments"
                    )
                }
            }.collect { state ->
                _uiState.value = state
                refreshReminderSchedules(state)
            }
        }
    }

    private suspend fun refreshReminderSchedules(state: RecurringUiState) {
        state.recurringExpenses.forEach { recurring ->
            val pendingDates = state.dueItems
                .filter {
                    it.recurringExpense.id == recurring.id &&
                        it.occurrence.status == RecurringOccurrenceStatus.PENDING &&
                        !it.dueDate.isBefore(state.today)
                }
                .map { it.dueDate }
            val signature = listOf(
                recurring.status.name,
                recurring.reminderEnabled.toString(),
                recurring.reminderLeadDays.toString(),
                pendingDates.joinToString()
            ).joinToString("|")
            if (reminderSignatures[recurring.id] != signature) {
                reminderScheduler.sync(recurring, pendingDates).onSuccess {
                    reminderSignatures[recurring.id] = signature
                }
            }
        }
        val currentIds = state.recurringExpenses.mapTo(mutableSetOf()) { it.id }
        reminderSignatures.keys.retainAll(currentIds)
    }

    private fun buildDueItems(
        schedule: RecurringExpense,
        stored: Map<String, RecurringOccurrence>,
        category: Category?,
        today: LocalDate
    ): List<RecurringDueItem> {
        val start = maxOf(schedule.startDate, today.minusDays(90))
        val end = today.plusDays(30)
        return RecurringScheduleCalculator.datesBetween(schedule, start, end).mapNotNull { dueDate ->
            val id = RecurringScheduleCalculator.occurrenceId(schedule.id, dueDate)
            val existing = stored[id]
            if (schedule.status != RecurringStatus.ACTIVE && existing == null) return@mapNotNull null
            RecurringDueItem(
                recurringExpense = schedule,
                category = category,
                occurrence = existing ?: RecurringOccurrence(
                    id = id,
                    recurringExpenseId = schedule.id,
                    dueDate = dueDate
                )
            )
        }
    }

    fun selectFilter(filter: RecurringFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun openCreate() {
        val today = _uiState.value.today
        _formState.value = RecurringFormState(
            id = UUID.randomUUID().toString(),
            isEditing = false,
            categoryId = _uiState.value.categories.firstOrNull()?.id.orEmpty(),
            dueDate = today.toString()
        )
    }

    fun openEdit(item: RecurringExpense) {
        _formState.value = RecurringFormState(
            id = item.id,
            isEditing = true,
            title = item.title,
            categoryId = item.categoryId,
            amount = minorToDecimal(item.expectedAmountMinor),
            frequency = item.frequency,
            intervalCount = item.intervalCount,
            paymentChannel = item.paymentChannel,
            channelLabel = item.channelLabel.orEmpty(),
            dueDate = item.anchorDate.toString(),
            reminderEnabled = item.reminderEnabled,
            reminderLeadDays = item.reminderLeadDays,
            status = item.status
        )
    }

    fun closeForm() {
        if (_formState.value?.isSaving != true) _formState.value = null
    }

    fun updateTitle(value: String) = updateForm { it.copy(title = value, errorMessage = null) }
    fun updateCategory(value: String) = updateForm { it.copy(categoryId = value, errorMessage = null) }
    fun updateAmount(value: String) = updateForm { it.copy(amount = value, errorMessage = null) }
    fun updateCadence(frequency: RecurrenceFrequency, interval: Long) =
        updateForm { it.copy(frequency = frequency, intervalCount = interval, errorMessage = null) }
    fun updatePaymentChannel(value: PaymentChannel) =
        updateForm { it.copy(paymentChannel = value, errorMessage = null) }
    fun updateChannelLabel(value: String) = updateForm { it.copy(channelLabel = value) }
    fun updateDueDate(value: String) = updateForm { it.copy(dueDate = value, errorMessage = null) }
    fun updateReminderEnabled(value: Boolean) = updateForm { it.copy(reminderEnabled = value) }
    fun updateReminderLeadDays(value: Long) = updateForm { it.copy(reminderLeadDays = value) }

    fun saveForm() {
        val form = _formState.value ?: return
        val dueDate = runCatching { LocalDate.parse(form.dueDate.trim()) }.getOrNull()
        val amountMinor = decimalToMinor(form.amount)
        val error = when {
            form.title.isBlank() -> "Enter a commitment name"
            form.categoryId.isBlank() -> "Choose a category"
            amountMinor == null || amountMinor <= 0L -> "Enter a valid positive amount"
            dueDate == null -> "Enter the due date as YYYY-MM-DD"
            else -> null
        }
        if (error != null || dueDate == null || amountMinor == null) {
            _formState.value = form.copy(errorMessage = error)
            return
        }

        val item = RecurringExpense(
            id = form.id,
            title = form.title.trim(),
            categoryId = form.categoryId,
            expectedAmountMinor = amountMinor,
            frequency = form.frequency,
            intervalCount = form.intervalCount,
            anchorDate = dueDate,
            startDate = dueDate,
            status = form.status,
            paymentChannel = form.paymentChannel,
            channelLabel = form.channelLabel.trim().takeIf(String::isNotEmpty),
            reminderEnabled = form.reminderEnabled,
            reminderLeadDays = form.reminderLeadDays
        )
        _formState.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            recurringRepository.saveRecurringExpense(item).fold(
                onSuccess = {
                    syncReminders(item)
                    _formState.value = null
                    showMessage(if (form.isEditing) "Commitment updated" else "Commitment created")
                },
                onFailure = { errorValue ->
                    _formState.value = form.copy(
                        isSaving = false,
                        errorMessage = errorValue.message ?: "Unable to save commitment"
                    )
                }
            )
        }
    }

    fun setStatus(item: RecurringExpense, status: RecurringStatus) {
        viewModelScope.launch {
            recurringRepository.setRecurringExpenseStatus(item.id, status).fold(
                onSuccess = {
                    syncReminders(item.copy(status = status))
                    showMessage(
                        when (status) {
                            RecurringStatus.ACTIVE -> "Commitment resumed"
                            RecurringStatus.PAUSED -> "Commitment paused"
                            RecurringStatus.CANCELLED -> "Commitment cancelled"
                        }
                    )
                },
                onFailure = { showMessage(it.message ?: "Unable to update commitment") }
            )
        }
    }

    private suspend fun syncReminders(item: RecurringExpense) {
        val today = _uiState.value.today
        val dates = if (item.active) {
            RecurringScheduleCalculator.datesBetween(item, today, today.plusDays(90))
        } else {
            emptyList()
        }
        reminderScheduler.sync(item, dates).onFailure {
            showMessage("Saved, but reminder synchronization failed")
        }
    }

    fun openPayment(item: RecurringDueItem) {
        _paymentState.value = PaymentConfirmationState(
            recurringExpenseId = item.recurringExpense.id,
            title = item.recurringExpense.title,
            dueDate = item.dueDate,
            amount = minorToDecimal(item.recurringExpense.expectedAmountMinor),
            paidDate = _uiState.value.today.toString()
        )
    }

    fun updatePaymentAmount(value: String) {
        _paymentState.update { it?.copy(amount = value, errorMessage = null) }
    }

    fun updatePaymentDate(value: String) {
        _paymentState.update { it?.copy(paidDate = value, errorMessage = null) }
    }

    fun closePayment() {
        if (_paymentState.value?.isSaving != true) _paymentState.value = null
    }

    fun confirmPayment() {
        val payment = _paymentState.value ?: return
        val paidDate = runCatching { LocalDate.parse(payment.paidDate.trim()) }.getOrNull()
        val amountMinor = decimalToMinor(payment.amount)
        val error = when {
            amountMinor == null || amountMinor <= 0L -> "Enter a valid positive amount"
            paidDate == null -> "Enter the payment date as YYYY-MM-DD"
            else -> null
        }
        if (error != null || paidDate == null || amountMinor == null) {
            _paymentState.value = payment.copy(errorMessage = error)
            return
        }
        _paymentState.value = payment.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            recurringRepository.confirmPayment(
                payment.recurringExpenseId,
                payment.dueDate,
                paidDate,
                amountMinor
            ).fold(
                onSuccess = {
                    reminderScheduler.cancelOccurrence(payment.recurringExpenseId, payment.dueDate)
                    _paymentState.value = null
                    showMessage("Payment recorded as an expense")
                },
                onFailure = {
                    _paymentState.value = payment.copy(
                        isSaving = false,
                        errorMessage = it.message ?: "Unable to record payment"
                    )
                }
            )
        }
    }

    fun skip(item: RecurringDueItem) {
        viewModelScope.launch {
            recurringRepository.skipOccurrence(item.recurringExpense.id, item.dueDate).fold(
                onSuccess = {
                    reminderScheduler.cancelOccurrence(item.recurringExpense.id, item.dueDate)
                    showMessage("Occurrence skipped")
                },
                onFailure = { showMessage(it.message ?: "Unable to skip occurrence") }
            )
        }
    }

    fun clearMessage() = _uiState.update { it.copy(operationMessage = null) }

    private fun showMessage(message: String) =
        _uiState.update { it.copy(operationMessage = message) }

    private fun updateForm(block: (RecurringFormState) -> RecurringFormState) {
        _formState.update { current ->
            if (current == null || current.isSaving) current else block(current)
        }
    }
}

internal fun minorToDecimal(value: Long): String =
    BigDecimal.valueOf(value, 2).setScale(2).toPlainString()

internal fun decimalToMinor(value: String): Long? = runCatching {
    value.trim().toBigDecimal()
        .multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()
