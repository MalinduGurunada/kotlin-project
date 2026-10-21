package com.example.personalfinanceapp.feature.income.rate

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuoteValidator
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class IncomeRateSelectionCoordinator(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val clock: Clock,
    parentScope: CoroutineScope,
    initialTransactionDate: LocalDate = LocalDate.now(clock)
) : AutoCloseable {

    private val lifecycleJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + lifecycleJob)
    private val requestVersion = AtomicLong(0L)
    private val requestLock = Any()

    @Volatile
    private var isClosed = false

    private var automaticRequestJob: Job? = null
    private var manualRateDateWasSelected = false

    private val _state = MutableStateFlow(
        IncomeRateSelectionState(
            transactionDate = initialTransactionDate,
            manualRateDate = initialTransactionDate
        )
    )
    val state: StateFlow<IncomeRateSelectionState> = _state.asStateFlow()

    fun selectCurrency(currency: CurrencyCode) {
        ensureOpen()
        cancelAutomaticRequest()

        if (currency == CurrencyCode.LKR) {
            _state.update { current ->
                current.copy(
                    selectedCurrency = CurrencyCode.LKR,
                    selectedRateMode = null,
                    automaticQuote = null,
                    isLoadingAutomaticRate = false,
                    automaticRateError = null
                ).recalculateEstimate()
            }
            return
        }

        _state.update { current ->
            current.copy(
                selectedCurrency = CurrencyCode.USD,
                selectedRateMode = IncomeRateSelectionMode.AUTOMATIC,
                automaticQuote = null,
                isLoadingAutomaticRate = false,
                validationError = null,
                automaticRateError = null,
                estimatedMoney = null
            )
        }
        requestAutomaticRate()
    }

    fun selectRateMode(mode: IncomeRateSelectionMode) {
        ensureOpen()
        if (_state.value.selectedCurrency != CurrencyCode.USD) {
            _state.update {
                it.copy(validationError = "Exchange-rate mode is only available for USD")
            }
            return
        }

        cancelAutomaticRequest()
        _state.update { current ->
            current.copy(
                selectedRateMode = mode,
                automaticQuote = null,
                isLoadingAutomaticRate = false,
                validationError = null,
                automaticRateError = null,
                estimatedMoney = null
            ).let { updated ->
                if (mode == IncomeRateSelectionMode.MANUAL) {
                    updated.recalculateEstimate()
                } else {
                    updated
                }
            }
        }

        if (mode == IncomeRateSelectionMode.AUTOMATIC) {
            requestAutomaticRate()
        }
    }

    fun updateAmountInput(amountInput: String) {
        ensureOpen()
        _state.update { current ->
            current.copy(amountInput = amountInput).recalculateEstimate()
        }
    }

    fun updateTransactionDate(transactionDate: LocalDate) {
        ensureOpen()
        _state.update { current ->
            current.copy(
                transactionDate = transactionDate,
                manualRateDate = if (manualRateDateWasSelected) {
                    current.manualRateDate
                } else {
                    transactionDate
                }
            ).recalculateEstimate()
        }
    }

    fun updateManualRateInput(rateInput: String) {
        ensureOpen()
        _state.update { current ->
            current.copy(manualRateInput = rateInput).recalculateEstimate()
        }
    }

    fun selectManualRateSource(source: IncomeManualRateSource) {
        ensureOpen()
        _state.update { current ->
            current.copy(manualRateSource = source).recalculateEstimate()
        }
    }

    fun selectManualRateDate(rateDate: LocalDate) {
        ensureOpen()
        manualRateDateWasSelected = true
        _state.update { current ->
            current.copy(manualRateDate = rateDate).recalculateEstimate()
        }
    }

    fun requestAutomaticRate() {
        ensureOpen()
        val currentState = _state.value
        if (currentState.selectedCurrency != CurrencyCode.USD ||
            currentState.selectedRateMode != IncomeRateSelectionMode.AUTOMATIC
        ) {
            _state.update {
                it.copy(validationError = "Select USD automatic rate mode before requesting a rate")
            }
            return
        }

        val requestId = beginAutomaticRequest()
        _state.update { current ->
            current.copy(
                automaticQuote = null,
                isLoadingAutomaticRate = true,
                validationError = null,
                automaticRateError = null,
                estimatedMoney = null
            )
        }

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = exchangeRateRepository.getLatestUsdToLkr()
                val failure = result.exceptionOrNull()
                if (failure is CancellationException) throw failure

                if (result.isSuccess) {
                    acceptAutomaticQuote(requestId, result.getOrThrow())
                } else {
                    rejectAutomaticQuote(
                        requestId,
                        failure.asExchangeRateException()
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                rejectAutomaticQuote(
                    requestId,
                    exception.asExchangeRateException()
                )
            } finally {
                synchronized(requestLock) {
                    if (requestVersion.get() == requestId) {
                        automaticRequestJob = null
                    }
                }
            }
        }

        synchronized(requestLock) {
            if (requestVersion.get() == requestId && !isClosed) {
                automaticRequestJob = job
                job.start()
            } else {
                job.cancel()
            }
        }
    }

    fun retryAutomaticRate() {
        requestAutomaticRate()
    }

    fun prepareMoneySnapshot(): Result<Money> {
        ensureOpen()
        val current = _state.value
        val result = calculateMoney(current)

        result.fold(
            onSuccess = { money ->
                _state.update {
                    if (it.selectionMatches(current)) {
                        it.copy(
                            estimatedMoney = money,
                            validationError = null,
                            automaticRateError = null
                        )
                    } else {
                        it
                    }
                }
            },
            onFailure = { failure ->
                _state.update {
                    if (!it.selectionMatches(current)) return@update it
                    if (failure is ExchangeRateException) {
                        it.copy(
                            estimatedMoney = null,
                            validationError = null,
                            automaticRateError = failure
                        )
                    } else {
                        it.copy(
                            estimatedMoney = null,
                            validationError = failure.message ?: "Enter valid income amount and rate details"
                        )
                    }
                }
            }
        )
        return result
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        cancelAutomaticRequest()
        scope.cancel()
        _state.update {
            it.copy(isLoadingAutomaticRate = false)
        }
    }

    private fun beginAutomaticRequest(): Long {
        val requestId = requestVersion.incrementAndGet()
        synchronized(requestLock) {
            automaticRequestJob?.cancel()
            automaticRequestJob = null
        }
        return requestId
    }

    private fun cancelAutomaticRequest() {
        requestVersion.incrementAndGet()
        synchronized(requestLock) {
            automaticRequestJob?.cancel()
            automaticRequestJob = null
        }
    }

    private fun acceptAutomaticQuote(requestId: Long, quote: ExchangeRateQuote) {
        if (!isCurrentAutomaticRequest(requestId)) return

        val validation = ExchangeRateQuoteValidator.validateUsdToLkr(
            quote = quote,
            currentTime = clock.instant()
        )
        val failure = validation.exceptionOrNull()
        if (failure != null) {
            rejectAutomaticQuote(requestId, failure.asExchangeRateException())
            return
        }

        _state.update { current ->
            if (!isCurrentAutomaticRequest(requestId) ||
                current.selectedCurrency != CurrencyCode.USD ||
                current.selectedRateMode != IncomeRateSelectionMode.AUTOMATIC
            ) {
                current
            } else {
                current.copy(
                    automaticQuote = quote,
                    isLoadingAutomaticRate = false,
                    automaticRateError = null
                ).recalculateEstimate()
            }
        }
    }

    private fun rejectAutomaticQuote(
        requestId: Long,
        failure: ExchangeRateException
    ) {
        if (!isCurrentAutomaticRequest(requestId)) return
        _state.update { current ->
            if (!isCurrentAutomaticRequest(requestId) ||
                current.selectedCurrency != CurrencyCode.USD ||
                current.selectedRateMode != IncomeRateSelectionMode.AUTOMATIC
            ) {
                current
            } else {
                current.copy(
                    automaticQuote = null,
                    isLoadingAutomaticRate = false,
                    automaticRateError = failure,
                    estimatedMoney = null
                )
            }
        }
    }

    private fun isCurrentAutomaticRequest(requestId: Long): Boolean =
        !isClosed && requestVersion.get() == requestId

    private fun IncomeRateSelectionState.recalculateEstimate(): IncomeRateSelectionState {
        if (selectedCurrency == CurrencyCode.USD &&
            selectedRateMode == IncomeRateSelectionMode.AUTOMATIC &&
            automaticQuote == null
        ) {
            return copy(
                estimatedMoney = null,
                validationError = null
            )
        }

        val result = calculateMoney(this)
        val money = result.getOrNull()
        val failure = result.exceptionOrNull()

        return when {
            money != null -> copy(
                estimatedMoney = money,
                validationError = null,
                automaticRateError = null
            )

            failure is ExchangeRateException -> copy(
                estimatedMoney = null,
                validationError = null,
                automaticRateError = failure
            )

            else -> copy(
                estimatedMoney = null,
                validationError = failure?.message
            )
        }
    }

    private fun calculateMoney(selection: IncomeRateSelectionState): Result<Money> =
        when (selection.selectedCurrency) {
            CurrencyCode.LKR -> MoneyConverter.fromLkr(
                amountInput = selection.amountInput,
                transactionDate = selection.transactionDate
            )

            CurrencyCode.USD -> when (selection.selectedRateMode) {
                IncomeRateSelectionMode.AUTOMATIC -> calculateAutomaticMoney(selection)
                IncomeRateSelectionMode.MANUAL -> calculateManualMoney(selection)
                null -> Result.failure(
                    IllegalStateException("Select an exchange-rate mode")
                )
            }
        }

    private fun calculateAutomaticMoney(
        selection: IncomeRateSelectionState
    ): Result<Money> {
        val quote = selection.automaticQuote ?: return Result.failure(
            selection.automaticRateError
                ?: IllegalStateException("Request an automatic exchange rate")
        )

        ExchangeRateQuoteValidator.validateUsdToLkr(
            quote = quote,
            currentTime = clock.instant()
        ).getOrElse {
            return Result.failure(it)
        }

        return MoneyConverter.fromUsd(
            amountInput = selection.amountInput,
            rateLkrPerUsdInput = quote.rate.toPlainString(),
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = quote.applicableDate
        )
    }

    private fun calculateManualMoney(
        selection: IncomeRateSelectionState
    ): Result<Money> {
        val source = selection.manualRateSource ?: return Result.failure(
            IllegalArgumentException("Select whether the manual rate is an actual transfer or reference rate")
        )

        return MoneyConverter.fromUsd(
            amountInput = selection.amountInput,
            rateLkrPerUsdInput = selection.manualRateInput,
            rateSource = source.moneyRateSource,
            rateDate = selection.manualRateDate
        )
    }

    private fun IncomeRateSelectionState.selectionMatches(
        other: IncomeRateSelectionState
    ): Boolean =
        selectedCurrency == other.selectedCurrency &&
            selectedRateMode == other.selectedRateMode &&
            amountInput == other.amountInput &&
            transactionDate == other.transactionDate &&
            manualRateInput == other.manualRateInput &&
            manualRateSource == other.manualRateSource &&
            manualRateDate == other.manualRateDate &&
            automaticQuote == other.automaticQuote

    private fun Throwable?.asExchangeRateException(): ExchangeRateException =
        this as? ExchangeRateException
            ?: ExchangeRateException.MalformedResponse(this)

    private fun ensureOpen() {
        check(!isClosed) { "Income rate selection coordinator is closed" }
    }
}
