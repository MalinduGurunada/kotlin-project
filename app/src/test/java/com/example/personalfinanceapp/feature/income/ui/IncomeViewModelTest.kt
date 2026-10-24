package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-10-24T12:00:00Z"),
        ZoneOffset.UTC
    )

    private lateinit var incomeRepository: ControlledIncomeRepository
    private lateinit var authRepository: ControlledAuthRepository
    private lateinit var viewModel: IncomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        incomeRepository = ControlledIncomeRepository()
        authRepository = ControlledAuthRepository(initialUserId = USER_A)
        viewModel = IncomeViewModel(incomeRepository, authRepository, fixedClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading without financial data`() = runTest {
        assertLoadingState(viewModel.uiState.value, OCTOBER_RANGE)

        testDispatcher.scheduler.runCurrent()

        assertEquals(1, incomeRepository.observations.size)
        assertLoadingState(viewModel.uiState.value, OCTOBER_RANGE)
    }

    @Test
    fun `default range is the current month from injected clock`() = runTest {
        assertEquals(OCTOBER_RANGE, viewModel.uiState.value.dateRange)

        testDispatcher.scheduler.runCurrent()

        assertEquals(OCTOBER_RANGE, incomeRepository.observations.single().dateRange)
    }

    @Test
    fun `successful empty observation produces empty state`() = runTest {
        startObservation()

        incomeRepository.emit(0, emptyList())
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.EMPTY, state.observationStatus)
        assertTrue(state.isEmpty)
        assertTrue(state.incomes.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
        assertNull(state.errorMessage)
        assertFalse(state.canRetry)
    }

    @Test
    fun `successful populated observation exposes an immutable snapshot`() = runTest {
        startObservation()
        val mutableResult = mutableListOf(receipt("salary", 125_00L))

        incomeRepository.emit(0, mutableResult)
        testDispatcher.scheduler.runCurrent()
        mutableResult += receipt("freelance", 75_00L, IncomeSource.FREELANCE)

        val state = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.SUCCESS, state.observationStatus)
        assertEquals(listOf("salary"), state.incomes.map { it.id })
        assertEquals(125_00L, state.totalLkrMinor)
        assertNull(state.errorMessage)
    }

    @Test
    fun `real time emissions replace the visible list and total`() = runTest {
        startObservation()

        incomeRepository.emit(0, listOf(receipt("salary", 100_00L)))
        testDispatcher.scheduler.runCurrent()
        assertEquals(100_00L, viewModel.uiState.value.totalLkrMinor)

        incomeRepository.emit(
            0,
            listOf(
                receipt("salary", 100_00L),
                receipt("adsense", 40_00L, IncomeSource.ADSENSE)
            )
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("salary", "adsense"), viewModel.uiState.value.incomes.map { it.id })
        assertEquals(140_00L, viewModel.uiState.value.totalLkrMinor)
    }

    @Test
    fun `date range change clears old values and observes the new range`() = runTest {
        startObservation()
        incomeRepository.emit(0, listOf(receipt("october", 100_00L)))
        testDispatcher.scheduler.runCurrent()

        viewModel.setDateRange(NOVEMBER_RANGE.startDate, NOVEMBER_RANGE.endDateExclusive)
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, incomeRepository.observations.size)
        assertEquals(NOVEMBER_RANGE, incomeRepository.observations[1].dateRange)
        assertLoadingState(viewModel.uiState.value, NOVEMBER_RANGE)

        incomeRepository.emit(1, listOf(receipt("november", 200_00L, date = LocalDate.of(2026, 11, 5))))
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("november"), viewModel.uiState.value.incomes.map { it.id })
    }

    @Test
    fun `date range change cancels the previous observation`() = runTest {
        startObservation()
        assertFalse(incomeRepository.observations[0].isCancelled)

        viewModel.setDateRange(NOVEMBER_RANGE.startDate, NOVEMBER_RANGE.endDateExclusive)
        testDispatcher.scheduler.runCurrent()

        assertTrue(incomeRepository.observations[0].isCancelled)
        assertFalse(incomeRepository.observations[1].isCancelled)
    }

    @Test
    fun `observation failure is an error and does not expose raw details`() = runTest {
        startObservation()

        incomeRepository.fail(0, IllegalStateException("private backend detail"))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.ERROR, state.observationStatus)
        assertTrue(state.canRetry)
        assertTrue(state.incomes.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
        assertEquals("Unable to load income records. Please try again.", state.errorMessage)
        assertFalse(state.errorMessage.orEmpty().contains("private backend detail"))
    }

    @Test
    fun `retry after failure creates a fresh observation for the same range`() = runTest {
        startObservation()
        incomeRepository.fail(0, IllegalStateException("unavailable"))
        testDispatcher.scheduler.runCurrent()

        viewModel.retryObservation()
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, incomeRepository.observations.size)
        assertEquals(OCTOBER_RANGE, incomeRepository.observations[1].dateRange)
        assertLoadingState(viewModel.uiState.value, OCTOBER_RANGE)

        incomeRepository.emit(1, emptyList())
        testDispatcher.scheduler.runCurrent()
        assertEquals(IncomeObservationStatus.EMPTY, viewModel.uiState.value.observationStatus)
    }

    @Test
    fun `total uses stored LKR snapshots for regular receipts and crypto gain`() = runTest {
        startObservation()

        incomeRepository.emit(
            0,
            listOf(
                receipt("salary", 100_00L),
                receipt("freelance", 25_00L, IncomeSource.FREELANCE),
                receipt("adsense", 10_00L, IncomeSource.ADSENSE),
                crypto("gain", 40_00L, PnlDirection.GAIN)
            )
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(175_00L, viewModel.uiState.value.totalLkrMinor)
    }

    @Test
    fun `crypto realised loss subtracts its positive stored magnitude`() = runTest {
        startObservation()

        incomeRepository.emit(
            0,
            listOf(
                receipt("salary", 100_00L),
                crypto("gain", 50_00L, PnlDirection.GAIN),
                crypto("loss", 70_00L, PnlDirection.LOSS)
            )
        )
        testDispatcher.scheduler.runCurrent()

        assertEquals(80_00L, viewModel.uiState.value.totalLkrMinor)
    }

    @Test
    fun `financial overflow produces a recoverable safe error`() = runTest {
        startObservation()

        incomeRepository.emit(
            0,
            listOf(
                receipt("maximum", Long.MAX_VALUE),
                receipt("overflow", 1L)
            )
        )
        testDispatcher.scheduler.runCurrent()

        val overflowState = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.ERROR, overflowState.observationStatus)
        assertEquals("Income total is too large to display.", overflowState.errorMessage)
        assertTrue(overflowState.incomes.isEmpty())
        assertEquals(0L, overflowState.totalLkrMinor)

        incomeRepository.emit(0, listOf(receipt("recovered", 20_00L)))
        testDispatcher.scheduler.runCurrent()
        assertEquals(IncomeObservationStatus.SUCCESS, viewModel.uiState.value.observationStatus)
        assertEquals(20_00L, viewModel.uiState.value.totalLkrMinor)
    }

    @Test
    fun `sign out clears records and total and cancels observation`() = runTest {
        startObservation()
        incomeRepository.emit(0, listOf(receipt("user-a-income", 100_00L)))
        testDispatcher.scheduler.runCurrent()

        authRepository.setUser(null)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.EMPTY, state.observationStatus)
        assertTrue(state.incomes.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
        assertTrue(incomeRepository.observations[0].isCancelled)
    }

    @Test
    fun `direct account switch clears user A before observing user B`() = runTest {
        startObservation()
        incomeRepository.emit(0, listOf(receipt("user-a-income", 100_00L)))
        testDispatcher.scheduler.runCurrent()

        authRepository.setUser(USER_B)
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, incomeRepository.observations.size)
        assertTrue(incomeRepository.observations[0].isCancelled)
        assertLoadingState(viewModel.uiState.value, OCTOBER_RANGE)

        incomeRepository.emit(1, listOf(receipt("user-b-income", 250_00L)))
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("user-b-income"), viewModel.uiState.value.incomes.map { it.id })
    }

    @Test
    fun `late emission from user A cannot appear after account switch`() = runTest {
        startObservation()
        incomeRepository.emit(0, listOf(receipt("user-a-income", 100_00L)))
        testDispatcher.scheduler.runCurrent()

        authRepository.setUser(USER_B)
        testDispatcher.scheduler.runCurrent()
        incomeRepository.emit(0, listOf(receipt("late-user-a-income", 900_00L)))
        testDispatcher.scheduler.runCurrent()

        assertLoadingState(viewModel.uiState.value, OCTOBER_RANGE)
        assertNotEquals(
            listOf("late-user-a-income"),
            viewModel.uiState.value.incomes.map { it.id }
        )

        incomeRepository.emit(1, emptyList())
        testDispatcher.scheduler.runCurrent()
        assertEquals(IncomeObservationStatus.EMPTY, viewModel.uiState.value.observationStatus)
    }

    @Test
    fun `cancellation is not converted into an observation error`() = runTest {
        startObservation()

        incomeRepository.fail(0, CancellationException("cancelled collection"))
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(IncomeObservationStatus.LOADING, state.observationStatus)
        assertNull(state.errorMessage)
        assertFalse(state.canRetry)
    }

    @Test
    fun `invalid date range is rejected before repository observation changes`() = runTest {
        startObservation()
        val initialObservationCount = incomeRepository.observations.size

        try {
            viewModel.setDateRange(
                startDate = LocalDate.of(2026, 11, 1),
                endDateExclusive = LocalDate.of(2026, 11, 1)
            )
            fail("Expected invalid range to throw")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
        testDispatcher.scheduler.runCurrent()

        assertEquals(initialObservationCount, incomeRepository.observations.size)
        assertEquals(OCTOBER_RANGE, viewModel.uiState.value.dateRange)
    }

    private fun startObservation() {
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, incomeRepository.observations.size)
    }

    private fun assertLoadingState(
        state: IncomeUiState,
        expectedRange: IncomeDateRange
    ) {
        assertEquals(expectedRange, state.dateRange)
        assertEquals(IncomeObservationStatus.LOADING, state.observationStatus)
        assertTrue(state.isLoading)
        assertTrue(state.incomes.isEmpty())
        assertEquals(0L, state.totalLkrMinor)
        assertNull(state.errorMessage)
        assertFalse(state.canRetry)
    }

    private fun receipt(
        id: String,
        baseAmountMinor: Long,
        source: IncomeSource = IncomeSource.SALARY,
        date: LocalDate = LocalDate.of(2026, 10, 10)
    ): Income = Income(
        id = id,
        source = source,
        entryKind = IncomeEntryKind.RECEIPT,
        money = lkrMoney(baseAmountMinor, date),
        occurredDate = date
    )

    private fun crypto(
        id: String,
        baseAmountMinor: Long,
        direction: PnlDirection,
        date: LocalDate = LocalDate.of(2026, 10, 10)
    ): Income = Income(
        id = id,
        source = IncomeSource.CRYPTO,
        entryKind = IncomeEntryKind.REALIZED_PNL,
        money = lkrMoney(baseAmountMinor, date),
        occurredDate = date,
        pnlDirection = direction
    )

    private fun lkrMoney(baseAmountMinor: Long, date: LocalDate): Money = Money(
        originalAmountDecimal = "1.00",
        originalCurrency = CurrencyCode.LKR,
        rateLkrPerUnitDecimal = "1",
        baseAmountMinor = baseAmountMinor,
        rateSource = RateSource.LKR_FIXED,
        rateDate = date
    )

    private class ControlledAuthRepository(
        initialUserId: UserId?
    ) : AuthRepository {
        private val authState = MutableStateFlow(initialUserId)

        override fun observeAuthState(): Flow<UserId?> = authState.asStateFlow()

        fun setUser(userId: UserId?) {
            authState.value = userId
        }

        override suspend fun login(email: String, password: String): Result<UserId> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        override suspend fun register(email: String, password: String): Result<UserId> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        override suspend fun sendPasswordReset(email: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        override suspend fun signOut(): Result<Unit> {
            authState.value = null
            return Result.success(Unit)
        }
    }

    private class ControlledIncomeRepository : IncomeRepository {
        val observations = mutableListOf<Observation>()

        override fun observeIncomes(
            startDate: LocalDate,
            endDateExclusive: LocalDate
        ): Flow<List<Income>> = callbackFlow {
            val observation = Observation(
                dateRange = IncomeDateRange(startDate, endDateExclusive)
            )
            observations += observation

            val forwardingJob = launch {
                for (event in observation.events) {
                    event.fold(
                        onSuccess = { send(it) },
                        onFailure = {
                            close(it)
                            return@launch
                        }
                    )
                }
            }

            awaitClose {
                observation.isCancelled = true
                forwardingJob.cancel()
            }
        }

        suspend fun emit(index: Int, incomes: List<Income>) {
            observations[index].events.send(Result.success(incomes))
        }

        suspend fun fail(index: Int, exception: Throwable) {
            observations[index].events.send(Result.failure(exception))
        }

        override suspend fun saveIncome(income: Income): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        override suspend fun saveIncomeForOwner(
            expectedOwnerId: UserId,
            income: Income
        ): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        override suspend fun deleteIncome(id: DocumentId): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used by this test"))

        class Observation(
            val dateRange: IncomeDateRange,
            val events: Channel<Result<List<Income>>> = Channel(Channel.UNLIMITED),
            var isCancelled: Boolean = false
        )
    }

    private companion object {
        const val USER_A = "user-a"
        const val USER_B = "user-b"

        val OCTOBER_RANGE = IncomeDateRange(
            startDate = LocalDate.of(2026, 10, 1),
            endDateExclusive = LocalDate.of(2026, 11, 1)
        )
        val NOVEMBER_RANGE = IncomeDateRange(
            startDate = LocalDate.of(2026, 11, 1),
            endDateExclusive = LocalDate.of(2026, 12, 1)
        )
    }
}
