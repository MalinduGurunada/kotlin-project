package com.example.personalfinanceapp.feature.income.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import com.example.personalfinanceapp.feature.income.IncomeValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val clock: Clock
) : ViewModel() {

    private val initialDateRange = currentMonthDateRange(clock)
    private val selectedDateRange = MutableStateFlow(initialDateRange)
    private val retryVersion = MutableStateFlow(0L)
    private val authenticatedUserId = MutableStateFlow<UserId?>(null)
    private var authGeneration = 0L

    private val _formState = MutableStateFlow<IncomeFormUiState?>(null)
    val formState: StateFlow<IncomeFormUiState?> = _formState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .distinctUntilChanged()
                .collectLatest { userId ->
                    val previousUserId = authenticatedUserId.value
                    if (previousUserId != userId) {
                        val hadActiveDraft = _formState.value != null
                        authGeneration = nextGeneration(authGeneration)
                        authenticatedUserId.value = userId
                        _formState.value = null
                        if (previousUserId != null && hadActiveDraft) {
                            _userMessage.value = if (userId == null) {
                                SIGNED_OUT_MESSAGE
                            } else {
                                ACCOUNT_CHANGED_MESSAGE
                            }
                        }
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

        _formState.value = IncomeFormUiState(
            mode = IncomeEditorMode.ADD,
            draftId = UUID.randomUUID().toString(),
            ownerId = ownerId,
            originalIncome = null,
            selectedSource = IncomeSource.SALARY,
            amountText = "",
            transactionDate = LocalDate.now(clock),
            projectReference = "",
            assetSymbol = "",
            pnlDirection = null,
            note = ""
        )
        _userMessage.value = null
    }

    fun openIncomeForEdit(income: Income) {
        val ownerId = authenticatedUserId.value
        if (ownerId.isNullOrBlank()) {
            _userMessage.value = AUTH_REQUIRED_MESSAGE
            return
        }
        if (income.money.originalCurrency != CurrencyCode.LKR) {
            _userMessage.value = USD_EDIT_UNAVAILABLE_MESSAGE
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
        _userMessage.value = null
    }

    fun closeIncomeEditor() {
        if (_formState.value?.isSaving == true) return
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

    fun updateIncomeAmount(amount: String) = updateForm { current ->
        current.copy(
            amountText = amount,
            errors = current.errors.copy(amount = null, form = null),
            saveError = null
        )
    }

    fun updateIncomeDate(date: LocalDate) = updateForm { current ->
        current.copy(
            transactionDate = date,
            errors = current.errors.copy(transactionDate = null, form = null),
            saveError = null
        )
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

    private fun prepareIncome(draft: IncomeFormUiState): PreparedIncome {
        var errors = IncomeFormErrors()
        val occurredDate = draft.transactionDate
        if (occurredDate == null) {
            errors = errors.copy(transactionDate = "Select a transaction date")
        }
        if (draft.selectedSource == IncomeSource.CRYPTO && draft.pnlDirection == null) {
            errors = errors.copy(pnlDirection = "Select gain or loss")
        }

        val money = if (occurredDate == null) {
            null
        } else {
            MoneyConverter.fromLkr(
                amountInput = draft.amountText,
                transactionDate = occurredDate
            ).fold(
                onSuccess = { it },
                onFailure = { failure ->
                    errors = errors.copy(
                        amount = failure.message ?: "Enter a valid amount"
                    )
                    null
                }
            )
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
        authRepository.observeAuthState()
            .distinctUntilChanged()
            .flatMapLatest { authenticatedUid ->
                if (authenticatedUid == null) {
                    flowOf(IncomeUiState.empty(request.dateRange))
                } else {
                    observeAuthenticatedRange(request.dateRange)
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
        dateRange: IncomeDateRange
    ): Flow<IncomeUiState> = incomeRepository
        .observeIncomes(dateRange.startDate, dateRange.endDateExclusive)
        .map { incomes -> loadedState(dateRange, incomes) }
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

    private fun loadedState(
        dateRange: IncomeDateRange,
        incomes: List<Income>
    ): IncomeUiState {
        val immutableSnapshot = incomes.toList()
        if (immutableSnapshot.isEmpty()) {
            return IncomeUiState.empty(dateRange)
        }

        return try {
            IncomeUiState.success(
                dateRange = dateRange,
                incomes = immutableSnapshot,
                totalLkrMinor = calculateTotalLkrMinor(immutableSnapshot)
            )
        } catch (_: ArithmeticException) {
            IncomeUiState.error(
                dateRange = dateRange,
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
        const val USD_EDIT_UNAVAILABLE_MESSAGE =
            "USD income editing will be available in a later update. This record was not changed."
        const val SAVE_ERROR_MESSAGE =
            "Unable to save income. Check your connection and try again."
        const val INCOME_ADDED_MESSAGE = "Income saved."
        const val INCOME_UPDATED_MESSAGE = "Income changes saved."

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
}
