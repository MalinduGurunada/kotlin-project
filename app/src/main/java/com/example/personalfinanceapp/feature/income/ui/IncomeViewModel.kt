package com.example.personalfinanceapp.feature.income.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyValidator
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import com.example.personalfinanceapp.feature.income.IncomeObservationSnapshot
import com.example.personalfinanceapp.feature.income.IncomeDeleteException
import com.example.personalfinanceapp.feature.income.IncomeValidator
import com.example.personalfinanceapp.feature.income.rate.IncomeManualRateSource
import com.example.personalfinanceapp.feature.income.rate.IncomeRateSelectionCoordinator
import com.example.personalfinanceapp.feature.income.rate.IncomeRateSelectionMode
import com.example.personalfinanceapp.feature.income.rate.IncomeRateSelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val authRepository: AuthRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val clock: Clock
) : ViewModel() {

    private val initialDateRange = currentMonthDateRange(clock)
    private val selectedDateRange = MutableStateFlow(initialDateRange)
    private val retryVersion = MutableStateFlow(0L)
    private val authenticatedUserId = authRepository.observeAuthState()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    private var authGeneration = 0L
    private var rateCoordinator: IncomeRateSelectionCoordinator? = null
    private var rateStateJob: Job? = null
    private var rateDraftId: String? = null

    private val _formState = MutableStateFlow<IncomeFormUiState?>(null)
    val formState: StateFlow<IncomeFormUiState?> = _formState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _deleteState = MutableStateFlow(IncomeDeleteUiState())
    val deleteState: StateFlow<IncomeDeleteUiState> = _deleteState.asStateFlow()

    init {
        viewModelScope.launch {
            var previousUserId: UserId? = null
            authenticatedUserId.collectLatest { userId ->
                if (previousUserId != userId) {
                    val hadActiveDraft = _formState.value != null
                    authGeneration = nextGeneration(authGeneration)
                    closeRateCoordinator()
                    _formState.value = null
                    _deleteState.value = IncomeDeleteUiState()
                    if (previousUserId != null && hadActiveDraft) {
                        _userMessage.value = if (userId == null) {
                            SIGNED_OUT_MESSAGE
                        } else {
                            ACCOUNT_CHANGED_MESSAGE
                        }
                    }
                    previousUserId = userId
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<IncomeUiState> = combine(
        selectedDateRange,
        retryVersion
    ) { dateRange, retry ->
        ObservationRequest(dateRange = dateRange, retryVersion = retry)
    }.flatMapLatest { request ->
        observeRequest(request)
    }.buffer(capacity = 0)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = IncomeUiState.loading(initialDateRange)
        )

    fun setDateRange(startDate: LocalDate, endDateExclusive: LocalDate) {
        selectedDateRange.value = IncomeDateRange(startDate, endDateExclusive)
    }

    fun retryObservation() {
        if (!uiState.value.canRetry) return
        retryVersion.update { current ->
            if (current == Long.MAX_VALUE) 0L else current + 1L
        }
    }

    fun openNewIncome() {
        val ownerId = authenticatedUserId.value
        if (ownerId.isNullOrBlank()) {
            _userMessage.value = AUTH_REQUIRED_MESSAGE
            return
        }

        val draftId = UUID.randomUUID().toString()
        val transactionDate = LocalDate.now(clock)
        _formState.value = IncomeFormUiState(
            mode = IncomeEditorMode.ADD,
            draftId = draftId,
            ownerId = ownerId,
            originalIncome = null,
            selectedSource = IncomeSource.SALARY,
            amountText = "",
            transactionDate = transactionDate,
            projectReference = "",
            assetSymbol = "",
            pnlDirection = null,
            note = ""
        )
        startRateCoordinator(
            draftId = draftId,
            transactionDate = transactionDate,
            amountInput = ""
        )
        _userMessage.value = null
    }

    fun openIncomeForEdit(income: Income) {
        val ownerId = authenticatedUserId.value
        if (ownerId.isNullOrBlank()) {
            _userMessage.value = AUTH_REQUIRED_MESSAGE
            return
        }
        _formState.value = IncomeFormUiState(
            mode = IncomeEditorMode.EDIT,
            draftId = income.id,
            ownerId = ownerId,
            originalIncome = income,
            selectedSource = income.source,
            amountText = income.money.originalAmountDecimal,
            transactionDate = income.occurredDate,
            projectReference = income.projectReference.orEmpty(),
            assetSymbol = income.assetSymbol.orEmpty(),
            pnlDirection = income.pnlDirection,
            note = income.note.orEmpty()
        )
        startRateCoordinator(
            draftId = income.id,
            transactionDate = income.occurredDate,
            amountInput = income.money.originalAmountDecimal,
            existingMoney = income.money
        )
        _userMessage.value = null
    }

    fun closeIncomeEditor() {
        if (_formState.value?.isSaving == true) return
        closeRateCoordinator()
        _formState.value = null
    }

    fun updateIncomeSource(source: IncomeSource) = updateForm { current ->
        current.copy(
            selectedSource = source,
            projectReference = if (source == IncomeSource.FREELANCE) {
                current.projectReference
            } else {
                ""
            },
            assetSymbol = if (source == IncomeSource.CRYPTO) current.assetSymbol else "",
            pnlDirection = if (source == IncomeSource.CRYPTO) current.pnlDirection else null,
            errors = current.errors.copy(pnlDirection = null, form = null),
            saveError = null
        )
    }

    fun updateIncomeAmount(amount: String) {
        updateRateSelection { coordinator -> coordinator.updateAmountInput(amount) }
        updateForm { current ->
            current.copy(
                amountText = amount,
                errors = current.errors.copy(amount = null, rate = null, form = null),
                saveError = null
            )
        }
    }

    fun updateIncomeDate(date: LocalDate) {
        updateRateSelection { coordinator -> coordinator.updateTransactionDate(date) }
        updateForm { current ->
            current.copy(
                transactionDate = date,
                errors = current.errors.copy(transactionDate = null, rate = null, form = null),
                saveError = null
            )
        }
    }

    fun updateIncomeCurrency(currency: CurrencyCode) =
        updateRateSelection { coordinator -> coordinator.selectCurrency(currency) }

    fun updateRateMode(mode: IncomeRateSelectionMode) =
        updateRateSelection { coordinator -> coordinator.selectRateMode(mode) }

    fun updateManualRate(rate: String) =
        updateRateSelection { coordinator -> coordinator.updateManualRateInput(rate) }

    fun updateManualRateSource(source: IncomeManualRateSource) =
        updateRateSelection { coordinator -> coordinator.selectManualRateSource(source) }

    fun updateManualRateDate(date: LocalDate) =
        updateRateSelection { coordinator -> coordinator.selectManualRateDate(date) }

    fun refreshAutomaticRate() =
        updateRateSelection { coordinator -> coordinator.requestAutomaticRate() }

    fun requestIncomeDeletion(income: Income, snapshotOwnerId: UserId?) {
        if (snapshotOwnerId.isNullOrBlank()) {
            _userMessage.value = AUTH_REQUIRED_DELETE_MESSAGE
            return
        }
        if (_deleteState.value.selectedIncome != null) return
        val snapshot = uiState.value
        if (
            snapshot.observationStatus != IncomeObservationStatus.SUCCESS ||
            snapshot.ownerId != snapshotOwnerId ||
            authenticatedUserId.value != snapshotOwnerId ||
            income !in snapshot.incomes
        ) {
            return
        }

        _deleteState.value = IncomeDeleteUiState(
            selectedIncome = income,
            ownerId = snapshotOwnerId
        )
    }

    fun cancelIncomeDeletion() {
        if (_deleteState.value.isDeleting) return
        _deleteState.value = IncomeDeleteUiState()
    }

    fun confirmIncomeDeletion() {
        val deletion = _deleteState.value
        val income = deletion.selectedIncome ?: return
        val ownerId = deletion.ownerId ?: return
        if (deletion.isDeleting) return
        val snapshot = uiState.value
        if (
            authenticatedUserId.value != ownerId ||
            snapshot.ownerId != ownerId
        ) {
            _deleteState.value = deletion.copy(errorMessage = AUTH_REQUIRED_DELETE_MESSAGE)
            return
        }
        if (
            snapshot.observationStatus != IncomeObservationStatus.SUCCESS ||
            income !in snapshot.incomes
        ) {
            _deleteState.value = deletion.copy(errorMessage = STALE_DELETE_MESSAGE)
            return
        }

        val deleteGeneration = authGeneration
        _deleteState.value = deletion.copy(
            deletingIncomeId = income.id,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = try {
                incomeRepository.deleteIncomeForOwner(
                    expectedOwnerId = ownerId,
                    id = income.id
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }

            val current = _deleteState.value
            val sessionIsCurrent = authGeneration == deleteGeneration &&
                authenticatedUserId.value == ownerId &&
                current.ownerId == ownerId &&
                current.selectedIncome?.id == income.id &&
                current.deletingIncomeId == income.id
            if (!sessionIsCurrent) return@launch

            result.fold(
                onSuccess = {
                    _deleteState.value = IncomeDeleteUiState()
                    _userMessage.value = INCOME_DELETED_MESSAGE
                },
                onFailure = { failure ->
                    _deleteState.value = current.copy(
                        deletingIncomeId = null,
                        errorMessage = failure.toDeleteUserMessage()
                    )
                }
            )
        }
    }

    fun updateProjectReference(reference: String) = updateForm { current ->
        current.copy(projectReference = reference, saveError = null)
    }

    fun updateAssetSymbol(symbol: String) = updateForm { current ->
        current.copy(assetSymbol = symbol, saveError = null)
    }

    fun updatePnlDirection(direction: PnlDirection) = updateForm { current ->
        current.copy(
            pnlDirection = direction,
            errors = current.errors.copy(pnlDirection = null, form = null),
            saveError = null
        )
    }

    fun updateIncomeNote(note: String) = updateForm { current ->
        current.copy(note = note, saveError = null)
    }

    fun saveIncome() {
        val draft = _formState.value ?: return
        if (draft.isSaving) return

        val ownerId = draft.ownerId
        if (authenticatedUserId.value != ownerId) {
            _formState.value = draft.copy(saveError = AUTH_REQUIRED_MESSAGE)
            return
        }

        val prepared = prepareIncome(draft)
        val income = prepared.income
        if (income == null) {
            _formState.value = draft.copy(
                errors = prepared.errors,
                saveError = null
            )
            return
        }

        val saveGeneration = authGeneration
        val draftId = draft.draftId
        _formState.value = draft.copy(
            errors = IncomeFormErrors(),
            isSaving = true,
            saveError = null
        )

        viewModelScope.launch {
            val result = try {
                incomeRepository.saveIncomeForOwner(
                    expectedOwnerId = ownerId,
                    income = income
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }

            val current = _formState.value
            val sessionIsCurrent = authGeneration == saveGeneration &&
                authenticatedUserId.value == ownerId &&
                current?.draftId == draftId
            if (!sessionIsCurrent) return@launch

            result.fold(
                onSuccess = {
                    closeRateCoordinator()
                    _formState.value = null
                    _userMessage.value = if (draft.mode == IncomeEditorMode.ADD) {
                        INCOME_ADDED_MESSAGE
                    } else {
                        INCOME_UPDATED_MESSAGE
                    }
                },
                onFailure = {
                    _formState.value = current.copy(
                        isSaving = false,
                        saveError = SAVE_ERROR_MESSAGE
                    )
                }
            )
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun updateForm(transform: (IncomeFormUiState) -> IncomeFormUiState) {
        _formState.update { current ->
            if (current == null || current.isSaving) current else transform(current)
        }
    }

    private fun updateRateSelection(
        action: (IncomeRateSelectionCoordinator) -> Unit
    ) {
        val draft = _formState.value ?: return
        if (draft.isSaving || rateDraftId != draft.draftId) return
        val coordinator = rateCoordinator ?: return
        action(coordinator)
        applyRateState(draft.draftId, coordinator.state.value)
        _formState.update { current ->
            if (current?.draftId == draft.draftId) {
                current.copy(
                    errors = current.errors.copy(rate = null, form = null),
                    saveError = null
                )
            } else {
                current
            }
        }
    }

    private fun startRateCoordinator(
        draftId: String,
        transactionDate: LocalDate,
        amountInput: String,
        existingMoney: com.example.personalfinanceapp.core.model.Money? = null
    ) {
        closeRateCoordinator()
        val coordinator = IncomeRateSelectionCoordinator(
            exchangeRateRepository = exchangeRateRepository,
            clock = clock,
            parentScope = viewModelScope,
            initialTransactionDate = transactionDate
        )
        rateCoordinator = coordinator
        rateDraftId = draftId

        if (existingMoney?.originalCurrency == CurrencyCode.USD) {
            coordinator.hydrateUsdMoney(existingMoney, transactionDate).getOrThrow()
        } else if (amountInput.isNotBlank()) {
            coordinator.updateAmountInput(amountInput)
        }
        applyRateState(draftId, coordinator.state.value)

        rateStateJob = viewModelScope.launch {
            coordinator.state.collect { state ->
                if (rateCoordinator === coordinator && rateDraftId == draftId) {
                    applyRateState(draftId, state)
                }
            }
        }
    }

    private fun applyRateState(
        draftId: String,
        state: IncomeRateSelectionState
    ) {
        _formState.update { current ->
            if (current?.draftId != draftId) return@update current
            current.copy(
                amountText = state.amountInput,
                transactionDate = state.transactionDate,
                selectedCurrency = state.selectedCurrency,
                selectedRateMode = state.selectedRateMode,
                manualRateText = state.manualRateInput,
                manualRateSource = state.manualRateSource,
                manualRateDate = state.manualRateDate,
                estimatedMoney = state.estimatedMoney,
                isLoadingAutomaticRate = state.isLoadingAutomaticRate,
                amountValidationError = state.validationError?.takeIf { it.isAmountError() },
                rateValidationError = state.validationError?.takeUnless { it.isAmountError() },
                automaticRateError = state.automaticRateError?.toUserMessage()
            )
        }
    }

    private fun closeRateCoordinator() {
        rateStateJob?.cancel()
        rateStateJob = null
        rateCoordinator?.close()
        rateCoordinator = null
        rateDraftId = null
    }

    private fun prepareIncome(draft: IncomeFormUiState): PreparedIncome {
        var errors = IncomeFormErrors()
        val occurredDate = draft.transactionDate
        if (occurredDate == null) {
            errors = errors.copy(transactionDate = "Select a transaction date")
        }
        if (draft.selectedSource == IncomeSource.CRYPTO && draft.pnlDirection == null) {
            errors = errors.copy(pnlDirection = "Select gain or loss")
        }

        val amountFailure = MoneyValidator.parsePositiveAmount(draft.amountText).exceptionOrNull()
        if (amountFailure != null) {
            errors = errors.copy(amount = amountFailure.message ?: "Enter a valid amount")
        }

        val coordinator = rateCoordinator
            ?.takeIf { rateDraftId == draft.draftId }
        val moneyResult = if (occurredDate == null || amountFailure != null || coordinator == null) {
            null
        } else {
            coordinator.prepareMoneySnapshot()
        }
        val money = moneyResult?.getOrNull()
        moneyResult?.exceptionOrNull()?.let { failure ->
            errors = errors.copy(
                rate = if (failure is ExchangeRateException) {
                    failure.toUserMessage()
                } else {
                    failure.message ?: "Enter valid exchange-rate details"
                }
            )
        }
        if (coordinator == null) {
            errors = errors.copy(form = "Income conversion is unavailable. Reopen the editor and try again.")
        }

        if (errors.hasErrors || occurredDate == null || money == null) {
            return PreparedIncome(errors = errors)
        }

        val isCrypto = draft.selectedSource == IncomeSource.CRYPTO
        val income = Income(
            id = draft.draftId,
            source = draft.selectedSource,
            entryKind = if (isCrypto) {
                IncomeEntryKind.REALIZED_PNL
            } else {
                IncomeEntryKind.RECEIPT
            },
            money = money,
            occurredDate = occurredDate,
            pnlDirection = if (isCrypto) draft.pnlDirection else null,
            projectReference = if (draft.selectedSource == IncomeSource.FREELANCE) {
                draft.projectReference.trim().ifBlank { null }
            } else {
                null
            },
            receivableId = draft.originalIncome?.receivableId,
            assetSymbol = if (isCrypto) {
                draft.assetSymbol.trim().ifBlank { null }
            } else {
                null
            },
            note = draft.note.trim().ifBlank { null }
        )

        val validationFailure = IncomeValidator.validate(income).exceptionOrNull()
        return if (validationFailure == null) {
            PreparedIncome(income = income)
        } else {
            PreparedIncome(
                errors = errors.copy(
                    form = validationFailure.message ?: "Check the income details"
                )
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeRequest(request: ObservationRequest): Flow<IncomeUiState> =
        authenticatedUserId
            .flatMapLatest { authenticatedUid ->
                if (authenticatedUid == null) {
                    flowOf(IncomeUiState.empty(request.dateRange))
                } else {
                    observeAuthenticatedRange(
                        dateRange = request.dateRange,
                        authenticatedUid = authenticatedUid
                    )
                }
            }
            .buffer(capacity = 0)
            .onStart {
                emit(IncomeUiState.loading(request.dateRange))
            }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(
                    IncomeUiState.error(
                        dateRange = request.dateRange,
                        message = OBSERVATION_ERROR_MESSAGE
                    )
                )
            }

    private fun observeAuthenticatedRange(
        dateRange: IncomeDateRange,
        authenticatedUid: UserId
    ): Flow<IncomeUiState> = incomeRepository
        .observeIncomeSnapshots(dateRange.startDate, dateRange.endDateExclusive)
        .map { snapshot ->
            stateForSnapshot(
                dateRange = dateRange,
                authenticatedUid = authenticatedUid,
                snapshot = snapshot
            )
        }
        .onStart {
            emit(IncomeUiState.loading(dateRange))
        }
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(
                IncomeUiState.error(
                    dateRange = dateRange,
                    message = OBSERVATION_ERROR_MESSAGE
                )
            )
        }

    private fun stateForSnapshot(
        dateRange: IncomeDateRange,
        authenticatedUid: UserId,
        snapshot: IncomeObservationSnapshot
    ): IncomeUiState {
        val snapshotOwnerId = snapshot.ownerId
        if (
            snapshotOwnerId == null ||
            snapshotOwnerId != authenticatedUid ||
            authenticatedUserId.value != authenticatedUid
        ) {
            return IncomeUiState.loading(dateRange)
        }

        return loadedState(dateRange, snapshotOwnerId, snapshot.incomes)
    }

    private fun loadedState(
        dateRange: IncomeDateRange,
        ownerId: UserId,
        incomes: List<Income>
    ): IncomeUiState {
        val immutableSnapshot = incomes.toList()
        if (immutableSnapshot.isEmpty()) {
            return IncomeUiState.empty(dateRange, ownerId)
        }

        return try {
            IncomeUiState.success(
                dateRange = dateRange,
                ownerId = ownerId,
                incomes = immutableSnapshot,
                totalLkrMinor = calculateTotalLkrMinor(immutableSnapshot)
            )
        } catch (_: ArithmeticException) {
            IncomeUiState.error(
                dateRange = dateRange,
                ownerId = ownerId,
                message = TOTAL_OVERFLOW_ERROR_MESSAGE
            )
        }
    }

    /**
     * Stored money is already an LKR snapshot. Receipts and realised gains add that snapshot,
     * while a crypto realised loss subtracts its positive stored magnitude.
     */
    private fun calculateTotalLkrMinor(incomes: List<Income>): Long =
        incomes.fold(0L) { total, income ->
            val signedAmount = if (
                income.source == IncomeSource.CRYPTO &&
                income.entryKind == IncomeEntryKind.REALIZED_PNL &&
                income.pnlDirection == PnlDirection.LOSS
            ) {
                Math.negateExact(income.money.baseAmountMinor)
            } else {
                income.money.baseAmountMinor
            }
            Math.addExact(total, signedAmount)
        }

    private data class ObservationRequest(
        val dateRange: IncomeDateRange,
        val retryVersion: Long
    )

    private data class PreparedIncome(
        val income: Income? = null,
        val errors: IncomeFormErrors = IncomeFormErrors()
    )

    private companion object {
        const val OBSERVATION_ERROR_MESSAGE =
            "Unable to load income records. Please try again."
        const val TOTAL_OVERFLOW_ERROR_MESSAGE =
            "Income total is too large to display."
        const val AUTH_REQUIRED_MESSAGE =
            "Sign in again before saving income."
        const val SIGNED_OUT_MESSAGE =
            "The income editor was closed because you signed out."
        const val ACCOUNT_CHANGED_MESSAGE =
            "The income editor was closed because the account changed."
        const val SAVE_ERROR_MESSAGE =
            "Unable to save income. Check your connection and try again."
        const val INCOME_ADDED_MESSAGE = "Income saved."
        const val INCOME_UPDATED_MESSAGE = "Income changes saved."
        const val INCOME_DELETED_MESSAGE = "Income deleted."
        const val AUTH_REQUIRED_DELETE_MESSAGE =
            "Sign in again before deleting income."
        const val STALE_DELETE_MESSAGE =
            "This income is no longer in the current list. Refresh and try again."

        fun nextGeneration(current: Long): Long =
            if (current == Long.MAX_VALUE) 0L else current + 1L

        fun currentMonthDateRange(clock: Clock): IncomeDateRange {
            val currentMonthStart = LocalDate.now(clock).withDayOfMonth(1)
            return IncomeDateRange(
                startDate = currentMonthStart,
                endDateExclusive = currentMonthStart.plusMonths(1)
            )
        }
    }

    override fun onCleared() {
        closeRateCoordinator()
        super.onCleared()
    }
}

private fun ExchangeRateException.toUserMessage(): String = when (this) {
    is ExchangeRateException.NetworkFailure ->
        "Unable to load the USD/LKR reference rate. Try again or enter a manual rate."
    is ExchangeRateException.Timeout ->
        "The USD/LKR rate request timed out. Try again or enter a manual rate."
    is ExchangeRateException.RateLimited ->
        "The rate service is temporarily busy. Try again later or enter a manual rate."
    is ExchangeRateException.StaleQuote ->
        "The USD/LKR reference rate expired. Refresh it or enter a manual rate."
    else ->
        "The USD/LKR reference rate is unavailable. Try again or enter a manual rate."
}

private fun String.isAmountError(): Boolean =
    startsWith("Amount") || startsWith("Enter a valid amount") ||
        startsWith("Converted amount")

private fun Throwable.toDeleteUserMessage(): String = when (this) {
    is NoSuchElementException ->
        "This income no longer exists. Refresh the list and try again."
    is IncomeDeleteException.PermissionDenied ->
        "You do not have permission to delete this income."
    is IncomeDeleteException.NetworkUnavailable ->
        "Unable to delete income while offline. Check your connection and try again."
    is IllegalStateException -> "Sign in again before deleting income."
    else -> "Unable to delete income. Check your connection and try again."
}
