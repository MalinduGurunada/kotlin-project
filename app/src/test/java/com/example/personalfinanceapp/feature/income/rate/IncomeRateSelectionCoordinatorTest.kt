package com.example.personalfinanceapp.feature.income.rate

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateException
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateProvider
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateQuote
import com.example.personalfinanceapp.feature.exchangerate.ExchangeRateRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeRateSelectionCoordinatorTest {

    @Test
    fun `LKR uses fixed rate transaction date and never requests API`() = runTest {
        val repository = ImmediateRepository(Result.success(validQuote()))
        val coordinator = coordinator(repository)

        coordinator.updateAmountInput("125.50")
        val result = coordinator.prepareMoneySnapshot()

        assertTrue(result.isSuccess)
        val money = result.getOrThrow()
        assertEquals(CurrencyCode.LKR, money.originalCurrency)
        assertEquals("1", money.rateLkrPerUnitDecimal)
        assertEquals(RateSource.LKR_FIXED, money.rateSource)
        assertEquals(TRANSACTION_DATE, money.rateDate)
        assertEquals(12_550L, money.baseAmountMinor)
        assertEquals(0, repository.requestCount)
    }

    @Test
    fun `valid automatic quote preserves metadata and produces exact save-ready estimate`() = runTest {
        val quote = validQuote(rate = "305.25")
        val repository = ImmediateRepository(Result.success(quote))
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("125.50")

        coordinator.selectCurrency(CurrencyCode.USD)
        assertTrue(coordinator.state.value.isLoadingAutomaticRate)
        runCurrent()

        val state = coordinator.state.value
        assertFalse(state.isLoadingAutomaticRate)
        assertSame(quote, state.automaticQuote)
        assertEquals(ExchangeRateProvider.EXCHANGE_RATE_API_OPEN, state.automaticQuote?.provider)
        assertEquals(UPDATED_AT, state.automaticQuote?.providerUpdatedAt)
        assertEquals(NEXT_UPDATE_AT, state.automaticQuote?.nextProviderUpdateAt)
        assertNull(state.automaticRateError)
        val estimate = state.estimatedMoney ?: error("Expected automatic estimate")
        assertEquals("305.25", estimate.rateLkrPerUnitDecimal)
        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, estimate.rateSource)
        assertEquals(RATE_DATE, estimate.rateDate)
        assertEquals(3_830_888L, estimate.baseAmountMinor)

        assertEquals(estimate, coordinator.prepareMoneySnapshot().getOrThrow())
    }

    @Test
    fun `successful retry clears prior API failure without losing amount`() = runTest {
        val failure = ExchangeRateException.NetworkFailure(IOException("offline"))
        val repository = SequenceRepository(
            listOf(
                Result.failure(failure),
                Result.success(validQuote())
            )
        )
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("10.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        assertSame(failure, coordinator.state.value.automaticRateError)

        coordinator.retryAutomaticRate()
        assertTrue(coordinator.state.value.isLoadingAutomaticRate)
        runCurrent()

        assertNull(coordinator.state.value.automaticRateError)
        assertEquals("10.00", coordinator.state.value.amountInput)
        assertTrue(coordinator.prepareMoneySnapshot().isSuccess)
        assertEquals(2, repository.requestCount)
    }

    @Test
    fun `manual transfer rate creates truthful Money snapshot`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateAmountInput("10.00")
        coordinator.updateManualRateInput("305.123456")
        coordinator.selectManualRateSource(IncomeManualRateSource.ACTUAL_TRANSFER)

        val money = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals(RateSource.MANUAL_TRANSFER, money.rateSource)
        assertEquals("305.123456", money.rateLkrPerUnitDecimal)
        assertEquals(305_123L, money.baseAmountMinor)
    }

    @Test
    fun `manual reference rate and explicitly selected historical date are preserved`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateAmountInput("2.00")
        coordinator.updateManualRateInput("300.5")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)
        coordinator.selectManualRateDate(HISTORICAL_RATE_DATE)

        coordinator.updateTransactionDate(TRANSACTION_DATE.plusDays(5))
        val money = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals(RateSource.MANUAL_REFERENCE, money.rateSource)
        assertEquals(HISTORICAL_RATE_DATE, money.rateDate)
        assertEquals(HISTORICAL_RATE_DATE, coordinator.state.value.manualRateDate)
    }

    @Test
    fun `manual date follows transaction date only until user explicitly selects one`() = runTest {
        val coordinator = manualCoordinator()
        val changedTransactionDate = TRANSACTION_DATE.plusDays(2)

        coordinator.updateTransactionDate(changedTransactionDate)
        assertEquals(changedTransactionDate, coordinator.state.value.manualRateDate)

        coordinator.selectManualRateDate(HISTORICAL_RATE_DATE)
        coordinator.updateTransactionDate(changedTransactionDate.plusDays(1))
        assertEquals(HISTORICAL_RATE_DATE, coordinator.state.value.manualRateDate)
    }

    @Test
    fun `manual mode rejects missing source`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateAmountInput("10.00")
        coordinator.updateManualRateInput("300")

        val result = coordinator.prepareMoneySnapshot()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("actual transfer or reference") == true)
        assertNull(coordinator.state.value.estimatedMoney)
    }

    @Test
    fun `manual mode rejects malformed nonpositive and overprecision rates`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateAmountInput("10.00")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)

        listOf("", "abc", "0", "-1", "300.1234567", "3e2").forEach { invalidRate ->
            coordinator.updateManualRateInput(invalidRate)
            val result = coordinator.prepareMoneySnapshot()

            assertTrue("Expected '$invalidRate' to fail", result.isFailure)
            assertNull(coordinator.state.value.estimatedMoney)
            assertTrue(coordinator.state.value.validationError?.isNotBlank() == true)
        }
    }

    @Test
    fun `automatic failures remain typed and never produce a snapshot`() = runTest {
        val failures = listOf(
            ExchangeRateException.NetworkFailure(IOException("offline")),
            ExchangeRateException.Timeout(SocketTimeoutException("timeout")),
            ExchangeRateException.RateLimited(60L)
        )

        failures.forEach { failure ->
            val coordinator = coordinator(ImmediateRepository(Result.failure(failure)))
            coordinator.updateAmountInput("10.00")
            coordinator.selectCurrency(CurrencyCode.USD)
            runCurrent()

            assertSame(failure, coordinator.state.value.automaticRateError)
            assertNull(coordinator.state.value.estimatedMoney)
            assertSame(failure, coordinator.prepareMoneySnapshot().exceptionOrNull())
            assertEquals("10.00", coordinator.state.value.amountInput)
            coordinator.close()
        }
    }

    @Test
    fun `wrong pair invalid precision inconsistent date and stale quotes are rejected`() = runTest {
        val cases = listOf(
            validQuote(base = CurrencyCode.LKR, target = CurrencyCode.USD) to
                ExchangeRateException.ProviderError::class.java,
            validQuote(rate = "305.1234567") to
                ExchangeRateException.InvalidRate::class.java,
            validQuote(applicableDate = RATE_DATE.minusDays(1)) to
                ExchangeRateException.MalformedResponse::class.java,
            validQuote(
                updatedAt = UPDATED_AT.minusSeconds(8 * 86_400L),
                nextUpdateAt = NEXT_UPDATE_AT.minusSeconds(8 * 86_400L)
            ) to ExchangeRateException.StaleQuote::class.java
        )

        cases.forEach { (quote, expectedFailure) ->
            val coordinator = coordinator(ImmediateRepository(Result.success(quote)))
            coordinator.updateAmountInput("10.00")
            coordinator.selectCurrency(CurrencyCode.USD)
            runCurrent()

            assertTrue(expectedFailure.isInstance(coordinator.state.value.automaticRateError))
            assertNull(coordinator.state.value.automaticQuote)
            assertNull(coordinator.state.value.estimatedMoney)
            coordinator.close()
        }
    }

    @Test
    fun `manual fallback after API failure works offline and preserves input`() = runTest {
        val repository = ImmediateRepository(
            Result.failure(ExchangeRateException.NetworkFailure(IOException("offline")))
        )
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("15.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        assertTrue(coordinator.state.value.automaticRateError is ExchangeRateException.NetworkFailure)

        coordinator.selectRateMode(IncomeRateSelectionMode.MANUAL)
        coordinator.updateManualRateInput("300")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)

        val money = coordinator.prepareMoneySnapshot().getOrThrow()
        assertEquals("15.00", money.originalAmountDecimal)
        assertEquals(RateSource.MANUAL_REFERENCE, money.rateSource)
        assertNull(coordinator.state.value.automaticRateError)
        assertEquals(1, repository.requestCount)
    }

    @Test
    fun `late automatic response cannot overwrite manual selection`() = runTest {
        val repository = ControlledRepository(ignoreCancellation = true)
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("10.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val request = repository.requests.single()

        coordinator.selectRateMode(IncomeRateSelectionMode.MANUAL)
        coordinator.updateManualRateInput("299")
        coordinator.selectManualRateSource(IncomeManualRateSource.ACTUAL_TRANSFER)
        request.result.complete(Result.success(validQuote(rate = "310")))
        runCurrent()

        val state = coordinator.state.value
        assertEquals(IncomeRateSelectionMode.MANUAL, state.selectedRateMode)
        assertNull(state.automaticQuote)
        assertEquals("299", state.estimatedMoney?.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_TRANSFER, state.estimatedMoney?.rateSource)
    }

    @Test
    fun `late automatic response cannot overwrite LKR selection`() = runTest {
        val repository = ControlledRepository(ignoreCancellation = true)
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("10.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val request = repository.requests.single()

        coordinator.selectCurrency(CurrencyCode.LKR)
        request.result.complete(Result.success(validQuote()))
        runCurrent()

        val state = coordinator.state.value
        assertEquals(CurrencyCode.LKR, state.selectedCurrency)
        assertNull(state.selectedRateMode)
        assertNull(state.automaticQuote)
        assertEquals(RateSource.LKR_FIXED, state.estimatedMoney?.rateSource)
    }

    @Test
    fun `latest automatic request wins when responses complete out of order`() = runTest {
        val repository = ControlledRepository(ignoreCancellation = true)
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("1.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val first = repository.requests[0]

        coordinator.retryAutomaticRate()
        runCurrent()
        val second = repository.requests[1]
        second.result.complete(Result.success(validQuote(rate = "320")))
        runCurrent()
        first.result.complete(Result.success(validQuote(rate = "300")))
        runCurrent()

        assertEquals("320", coordinator.state.value.automaticQuote?.rate?.toPlainString())
        assertEquals("320", coordinator.state.value.estimatedMoney?.rateLkrPerUnitDecimal)
    }

    @Test
    fun `changing amount during lookup calculates with latest amount`() = runTest {
        val repository = ControlledRepository()
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("1.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        assertTrue(coordinator.state.value.isLoadingAutomaticRate)

        coordinator.updateAmountInput("2.50")
        assertNull(coordinator.state.value.validationError)
        repository.requests.single().result.complete(Result.success(validQuote(rate = "300")))
        runCurrent()

        val money = coordinator.prepareMoneySnapshot().getOrThrow()
        assertEquals("2.50", money.originalAmountDecimal)
        assertEquals(75_000L, money.baseAmountMinor)
    }

    @Test
    fun `switching mode cancels active cooperative request`() = runTest {
        val repository = ControlledRepository()
        val coordinator = coordinator(repository)
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val request = repository.requests.single()

        coordinator.selectRateMode(IncomeRateSelectionMode.MANUAL)
        runCurrent()

        assertTrue(request.cancelled.isCompleted)
        assertFalse(coordinator.state.value.isLoadingAutomaticRate)
        assertEquals(IncomeRateSelectionMode.MANUAL, coordinator.state.value.selectedRateMode)
    }

    @Test
    fun `closing coordinator cancels work and prevents stale lifecycle updates`() = runTest {
        val repository = ControlledRepository()
        val coordinator = coordinator(repository)
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val request = repository.requests.single()

        coordinator.close()
        runCurrent()

        assertTrue(request.cancelled.isCompleted)
        assertFalse(coordinator.state.value.isLoadingAutomaticRate)
        assertTrue(runCatching { coordinator.updateAmountInput("10") }.isFailure)
    }

    @Test
    fun `late noncancellable response cannot mutate state after close`() = runTest {
        val repository = ControlledRepository(ignoreCancellation = true)
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("10.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val request = repository.requests.single()

        coordinator.close()
        val stateAfterClose = coordinator.state.value
        request.result.complete(Result.success(validQuote(rate = "320")))
        runCurrent()

        assertEquals(stateAfterClose, coordinator.state.value)
        assertFalse(coordinator.state.value.isLoadingAutomaticRate)
        assertNull(coordinator.state.value.automaticQuote)
        assertNull(coordinator.state.value.estimatedMoney)
    }

    @Test
    fun `amount update recomputes estimate instead of reusing old snapshot`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateManualRateInput("300")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)
        coordinator.updateAmountInput("1.00")
        val first = coordinator.state.value.estimatedMoney

        coordinator.updateAmountInput("2.00")
        val second = coordinator.state.value.estimatedMoney

        assertEquals(30_000L, first?.baseAmountMinor)
        assertEquals(60_000L, second?.baseAmountMinor)
        assertTrue(first !== second)
    }

    @Test
    fun `manual conversion delegates HALF_UP rounding to MoneyConverter`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.updateAmountInput("125.50")
        coordinator.updateManualRateInput("305.25")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)

        assertEquals(3_830_888L, coordinator.prepareMoneySnapshot().getOrThrow().baseAmountMinor)
    }

    @Test
    fun `overflow and zero-rounded conversions cannot become save-ready`() = runTest {
        val coordinator = manualCoordinator()
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)

        coordinator.updateAmountInput("999999999999999999.99")
        coordinator.updateManualRateInput("999999")
        assertTrue(coordinator.prepareMoneySnapshot().isFailure)
        assertNull(coordinator.state.value.estimatedMoney)

        coordinator.updateAmountInput("0.01")
        coordinator.updateManualRateInput("0.000001")
        assertTrue(coordinator.prepareMoneySnapshot().isFailure)
        assertNull(coordinator.state.value.estimatedMoney)
    }

    @Test
    fun `quote that expires while form is open cannot be prepared for saving`() = runTest {
        val mutableClock = MutableClock(NOW)
        val coordinator = coordinator(
            repository = ImmediateRepository(Result.success(validQuote())),
            clock = mutableClock
        )
        coordinator.updateAmountInput("10.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        assertTrue(coordinator.prepareMoneySnapshot().isSuccess)

        mutableClock.currentInstant = NEXT_UPDATE_AT.plusSeconds(6 * 3_600L).plusNanos(1)
        val expiredResult = coordinator.prepareMoneySnapshot()

        assertTrue(expiredResult.exceptionOrNull() is ExchangeRateException.StaleQuote)
        assertTrue(coordinator.state.value.automaticRateError is ExchangeRateException.StaleQuote)
        assertNull(coordinator.state.value.estimatedMoney)
    }

    @Test
    fun `snapshot preparation always uses current selected mode`() = runTest {
        val coordinator = coordinator(ImmediateRepository(Result.success(validQuote(rate = "310"))))
        coordinator.updateAmountInput("1.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        assertEquals("310", coordinator.prepareMoneySnapshot().getOrThrow().rateLkrPerUnitDecimal)

        coordinator.selectRateMode(IncomeRateSelectionMode.MANUAL)
        coordinator.updateManualRateInput("299")
        coordinator.selectManualRateSource(IncomeManualRateSource.REFERENCE)

        val manualMoney = coordinator.prepareMoneySnapshot().getOrThrow()
        assertEquals("299", manualMoney.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_REFERENCE, manualMoney.rateSource)
    }

    @Test
    fun `previously prepared Money remains historical when a newer quote arrives`() = runTest {
        val repository = SequenceRepository(
            listOf(
                Result.success(validQuote(rate = "300")),
                Result.success(validQuote(rate = "320"))
            )
        )
        val coordinator = coordinator(repository)
        coordinator.updateAmountInput("1.00")
        coordinator.selectCurrency(CurrencyCode.USD)
        runCurrent()
        val historicalSnapshot = coordinator.prepareMoneySnapshot().getOrThrow()

        coordinator.retryAutomaticRate()
        runCurrent()
        val currentSnapshot = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals("300", historicalSnapshot.rateLkrPerUnitDecimal)
        assertEquals(30_000L, historicalSnapshot.baseAmountMinor)
        assertEquals("320", currentSnapshot.rateLkrPerUnitDecimal)
        assertEquals(32_000L, currentSnapshot.baseAmountMinor)
    }

    @Test
    fun `hydrating historical API money preserves exact snapshot without network lookup`() = runTest {
        val repository = ImmediateRepository(Result.success(validQuote(rate = "320")))
        val coordinator = coordinator(repository)
        val historical = MoneyConverter.fromUsd(
            amountInput = "125.00",
            rateLkrPerUsdInput = "305.123456",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = HISTORICAL_RATE_DATE
        ).getOrThrow()

        assertTrue(coordinator.hydrateUsdMoney(historical, TRANSACTION_DATE).isSuccess)
        val prepared = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals(historical, prepared)
        assertEquals(IncomeRateSelectionMode.AUTOMATIC, coordinator.state.value.selectedRateMode)
        assertEquals(0, repository.requestCount)
    }

    @Test
    fun `reselecting hydrated USD and automatic mode repeatedly preserves exact snapshot`() = runTest {
        val repository = ImmediateRepository(Result.success(validQuote(rate = "320")))
        val coordinator = coordinator(repository)
        val historical = MoneyConverter.fromUsd(
            amountInput = "125.00",
            rateLkrPerUsdInput = "305.123456",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = HISTORICAL_RATE_DATE
        ).getOrThrow()
        coordinator.hydrateUsdMoney(historical, TRANSACTION_DATE).getOrThrow()

        repeat(3) {
            coordinator.selectCurrency(CurrencyCode.USD)
            coordinator.selectRateMode(IncomeRateSelectionMode.AUTOMATIC)
        }

        assertEquals(0, repository.requestCount)
        assertEquals(historical, coordinator.state.value.preservedUsdMoney)
        assertEquals(historical, coordinator.state.value.estimatedMoney)
        assertEquals(historical, coordinator.prepareMoneySnapshot().getOrThrow())
    }

    @Test
    fun `historical amount change recalculates with preserved rate and rate date`() = runTest {
        val repository = ImmediateRepository(Result.success(validQuote()))
        val coordinator = coordinator(repository)
        val historical = MoneyConverter.fromUsd(
            amountInput = "10.00",
            rateLkrPerUsdInput = "300.50",
            rateSource = RateSource.MANUAL_TRANSFER,
            rateDate = HISTORICAL_RATE_DATE
        ).getOrThrow()
        coordinator.hydrateUsdMoney(historical, TRANSACTION_DATE).getOrThrow()

        coordinator.updateTransactionDate(TRANSACTION_DATE.plusDays(3))
        assertEquals(historical, coordinator.prepareMoneySnapshot().getOrThrow())
        coordinator.updateAmountInput("12.00")
        val changed = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals("300.50", changed.rateLkrPerUnitDecimal)
        assertEquals(RateSource.MANUAL_TRANSFER, changed.rateSource)
        assertEquals(HISTORICAL_RATE_DATE, changed.rateDate)
        assertEquals(360_600L, changed.baseAmountMinor)
        assertEquals(0, repository.requestCount)
    }

    @Test
    fun `explicit refresh replaces hydrated API snapshot with latest quote`() = runTest {
        val repository = ImmediateRepository(Result.success(validQuote(rate = "320")))
        val coordinator = coordinator(repository)
        val historical = MoneyConverter.fromUsd(
            amountInput = "10.00",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = HISTORICAL_RATE_DATE
        ).getOrThrow()
        coordinator.hydrateUsdMoney(historical, TRANSACTION_DATE).getOrThrow()

        coordinator.requestAutomaticRate()
        runCurrent()
        val refreshed = coordinator.prepareMoneySnapshot().getOrThrow()

        assertEquals(1, repository.requestCount)
        assertEquals("320", refreshed.rateLkrPerUnitDecimal)
        assertEquals(RATE_DATE, refreshed.rateDate)
        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, refreshed.rateSource)
    }

    @Test
    fun `failed refresh keeps hydrated historical snapshot available`() = runTest {
        val failure = ExchangeRateException.NetworkFailure(IOException("offline"))
        val repository = ImmediateRepository(Result.failure(failure))
        val coordinator = coordinator(repository)
        val historical = MoneyConverter.fromUsd(
            amountInput = "10.00",
            rateLkrPerUsdInput = "300",
            rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE,
            rateDate = HISTORICAL_RATE_DATE
        ).getOrThrow()
        coordinator.hydrateUsdMoney(historical, TRANSACTION_DATE).getOrThrow()

        coordinator.requestAutomaticRate()
        runCurrent()

        assertSame(failure, coordinator.state.value.automaticRateError)
        assertEquals(historical, coordinator.state.value.estimatedMoney)
        assertEquals(historical, coordinator.prepareMoneySnapshot().getOrThrow())
    }

    private fun TestScope.coordinator(
        repository: ExchangeRateRepository,
        clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    ): IncomeRateSelectionCoordinator = IncomeRateSelectionCoordinator(
        exchangeRateRepository = repository,
        clock = clock,
        parentScope = backgroundScope,
        initialTransactionDate = TRANSACTION_DATE
    )

    private fun TestScope.manualCoordinator(): IncomeRateSelectionCoordinator {
        val repository = ImmediateRepository(Result.success(validQuote()))
        return coordinator(repository).also {
            it.selectCurrency(CurrencyCode.USD)
            it.selectRateMode(IncomeRateSelectionMode.MANUAL)
            runCurrent()
            assertEquals(0, repository.requestCount)
        }
    }

    private class ImmediateRepository(
        private val result: Result<ExchangeRateQuote>
    ) : ExchangeRateRepository {
        var requestCount: Int = 0
            private set

        override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> {
            requestCount += 1
            return result
        }
    }

    private class SequenceRepository(
        results: List<Result<ExchangeRateQuote>>
    ) : ExchangeRateRepository {
        private val remaining = ArrayDeque(results)
        var requestCount: Int = 0
            private set

        override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> {
            requestCount += 1
            return remaining.removeFirst()
        }
    }

    private class ControlledRepository(
        private val ignoreCancellation: Boolean = false
    ) : ExchangeRateRepository {
        val requests = mutableListOf<PendingRequest>()

        override suspend fun getLatestUsdToLkr(): Result<ExchangeRateQuote> {
            val request = PendingRequest()
            requests += request
            return if (ignoreCancellation) {
                withContext(NonCancellable) { request.result.await() }
            } else {
                try {
                    request.result.await()
                } finally {
                    if (!request.result.isCompleted) {
                        request.cancelled.complete(Unit)
                    }
                }
            }
        }
    }

    private class PendingRequest {
        val result = CompletableDeferred<Result<ExchangeRateQuote>>()
        val cancelled = CompletableDeferred<Unit>()
    }

    private class MutableClock(
        var currentInstant: Instant,
        private val zone: ZoneId = ZoneOffset.UTC
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)
        override fun instant(): Instant = currentInstant
    }

    companion object {
        private val TRANSACTION_DATE: LocalDate = LocalDate.of(2026, 10, 21)
        private val HISTORICAL_RATE_DATE: LocalDate = LocalDate.of(2026, 10, 10)
        private val RATE_DATE: LocalDate = LocalDate.of(2026, 10, 20)
        private val UPDATED_AT: Instant = Instant.parse("2026-10-20T00:00:00Z")
        private val NEXT_UPDATE_AT: Instant = Instant.parse("2026-10-21T00:00:00Z")
        private val NOW: Instant = Instant.parse("2026-10-20T03:30:00Z")

        private fun validQuote(
            rate: String = "305.25",
            base: CurrencyCode = CurrencyCode.USD,
            target: CurrencyCode = CurrencyCode.LKR,
            updatedAt: Instant = UPDATED_AT,
            nextUpdateAt: Instant = NEXT_UPDATE_AT,
            applicableDate: LocalDate = updatedAt.atZone(ZoneOffset.UTC).toLocalDate()
        ): ExchangeRateQuote = ExchangeRateQuote(
            baseCurrency = base,
            targetCurrency = target,
            rate = BigDecimal(rate),
            provider = ExchangeRateProvider.EXCHANGE_RATE_API_OPEN,
            applicableDate = applicableDate,
            providerUpdatedAt = updatedAt,
            nextProviderUpdateAt = nextUpdateAt,
            fetchedAt = NOW
        )
    }
}
