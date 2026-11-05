package com.example.personalfinanceapp.feature.dashboard

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.CryptoRealizedDetails
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.category.FakeCategoryRepository
import com.example.personalfinanceapp.feature.category.CategoryRepository
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.dashboard.ui.FinancialSummaryPeriod
import com.example.personalfinanceapp.feature.dashboard.ui.calculateNetCashFlowMarginPercent
import com.example.personalfinanceapp.feature.dashboard.ui.calculateGoalProgressAmounts
import com.example.personalfinanceapp.feature.dashboard.ui.financialMagnitude
import com.example.personalfinanceapp.feature.dashboard.ui.financialSummaryBounds
import com.example.personalfinanceapp.feature.dashboard.ui.financialSummaryLocalDate
import com.example.personalfinanceapp.feature.expense.FakeExpenseRepository
import com.example.personalfinanceapp.feature.expense.ExpenseRepository
import com.example.personalfinanceapp.feature.goal.FakeGoalRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import com.example.personalfinanceapp.feature.income.FakeIncomeRepository
import com.example.personalfinanceapp.feature.income.IncomeObservationSnapshot
import com.example.personalfinanceapp.feature.income.IncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeIncomeRepository: FakeIncomeRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository(initialUserId = "user-1", initialEmail = "himasha@example.com")
        fakeIncomeRepository = FakeIncomeRepository()
        fakeExpenseRepository = FakeExpenseRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakeGoalRepository = FakeGoalRepository()

        viewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = fakeIncomeRepository,
            expenseRepository = fakeExpenseRepository,
            categoryRepository = fakeCategoryRepository,
            goalRepository = fakeGoalRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsCleanZeroValuesWhenNoData() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Himasha", state.userName)
        assertEquals(0L, state.monthlyNetCashFlowLkrMinor)
        assertEquals(0L, state.totalIncomeLkrMinor)
        assertEquals(0L, state.totalExpenseLkrMinor)
        assertTrue(state.recentActivity.isEmpty())
        assertNull(state.activeGoal)
    }

    @Test
    fun cryptoLossCorrectlySubtractedFromNetIncomeAndBalance() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val now = LocalDate.now()
        val today = now.withDayOfMonth(10)

        // 1. Regular Salary Income: +100,000 LKR (10,000,000 minor)
        fakeIncomeRepository.saveIncome(
            Income(
                id = "inc-1",
                source = IncomeSource.SALARY,
                entryKind = IncomeEntryKind.RECEIPT,
                money = Money(
                    originalAmountDecimal = "100000.00",
                    originalCurrency = CurrencyCode.LKR,
                    rateLkrPerUnitDecimal = "1.00",
                    baseAmountMinor = 10_000_000L,
                    rateSource = RateSource.LKR_FIXED,
                    rateDate = today
                ),
                occurredDate = today
            )
        )

        // 2. Realized Crypto Loss: 25,000 LKR (2,500,000 minor)
        fakeIncomeRepository.saveIncome(
            Income(
                id = "crypto-loss-1",
                source = IncomeSource.CRYPTO,
                entryKind = IncomeEntryKind.REALIZED_PNL,
                pnlDirection = PnlDirection.LOSS,
                money = Money(
                    originalAmountDecimal = "25000.00",
                    originalCurrency = CurrencyCode.LKR,
                    rateLkrPerUnitDecimal = "1.00",
                    baseAmountMinor = 2_500_000L,
                    rateSource = RateSource.LKR_FIXED,
                    rateDate = today
                ),
                occurredDate = today
            )
        )

        // 3. Expense: -15,000 LKR (1,500,000 minor)
        fakeExpenseRepository.saveExpense(
            Expense(
                id = "exp-1",
                categoryId = "cat-food",
                categoryNameSnapshot = "Groceries",
                spendingClass = SpendingClass.COMMITTED,
                paymentChannel = PaymentChannel.CARD,
                money = Money(
                    originalAmountDecimal = "15000.00",
                    originalCurrency = CurrencyCode.LKR,
                    rateLkrPerUnitDecimal = "1.00",
                    baseAmountMinor = 1_500_000L,
                    rateSource = RateSource.LKR_FIXED,
                    rateDate = today
                ),
                occurredDate = today
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Net income = 10,000,000 - 2,500,000 = 7,500,000 minor (75,000 LKR)
        assertEquals(7_500_000L, state.totalIncomeLkrMinor)
        assertEquals(2, state.incomeCount)

        // Expense = 1,500,000 minor (15,000 LKR)
        assertEquals(1_500_000L, state.totalExpenseLkrMinor)
        assertEquals(1, state.expenseCount)

        // Monthly net cash flow = 7,500,000 - 1,500,000 = 6,000,000 minor (60,000 LKR)
        assertEquals(6_000_000L, state.monthlyNetCashFlowLkrMinor)

        // Activity timeline items
        assertEquals(3, state.recentActivity.size)
        val cryptoActivity = state.recentActivity.firstOrNull { it.id == "crypto-loss-1" }
        assertNotNull(cryptoActivity)
        assertTrue(cryptoActivity!!.isRedAmount)
        assertEquals("-LKR 25,000.00", cryptoActivity.amountText)
    }

    @Test
    fun structuredCryptoResultsIntegrateWithAllIncomeSourcesAndExpenseClasses() = runTest {
        val fixedClock = Clock.fixed(
            Instant.parse("2026-11-04T00:00:00Z"),
            ZoneOffset.UTC
        )
        val integratedViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = fakeIncomeRepository,
            expenseRepository = fakeExpenseRepository,
            categoryRepository = fakeCategoryRepository,
            goalRepository = fakeGoalRepository,
            clock = fixedClock
        )
        backgroundScope.launch { integratedViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionDate = LocalDate.of(2026, 11, 3)
        suspend fun saveIncome(
            id: String,
            source: IncomeSource,
            amountDecimal: String,
            amountMinor: Long,
            pnlDirection: PnlDirection? = null,
            cryptoDetails: CryptoRealizedDetails? = null
        ) {
            fakeIncomeRepository.saveIncome(
                Income(
                    id = id,
                    source = source,
                    entryKind = if (source == IncomeSource.CRYPTO) {
                        IncomeEntryKind.REALIZED_PNL
                    } else {
                        IncomeEntryKind.RECEIPT
                    },
                    money = Money(
                        originalAmountDecimal = amountDecimal,
                        originalCurrency = CurrencyCode.LKR,
                        rateLkrPerUnitDecimal = "1.00",
                        baseAmountMinor = amountMinor,
                        rateSource = RateSource.LKR_FIXED,
                        rateDate = transactionDate
                    ),
                    occurredDate = transactionDate,
                    pnlDirection = pnlDirection,
                    cryptoRealizedDetails = cryptoDetails
                )
            ).getOrThrow()
        }

        saveIncome("salary", IncomeSource.SALARY, "1000.00", 100_000L)
        saveIncome("freelance", IncomeSource.FREELANCE, "200.00", 20_000L)
        saveIncome("adsense", IncomeSource.ADSENSE, "100.00", 10_000L)
        saveIncome(
            id = "crypto-gain",
            source = IncomeSource.CRYPTO,
            amountDecimal = "350.00",
            amountMinor = 35_000L,
            pnlDirection = PnlDirection.GAIN,
            cryptoDetails = CryptoRealizedDetails(
                costBasisOriginalDecimal = "600.00",
                saleProceedsOriginalDecimal = "1000.00",
                feesOriginalDecimal = "50.00"
            )
        )
        saveIncome(
            id = "crypto-loss",
            source = IncomeSource.CRYPTO,
            amountDecimal = "125.00",
            amountMinor = 12_500L,
            pnlDirection = PnlDirection.LOSS,
            cryptoDetails = CryptoRealizedDetails(
                costBasisOriginalDecimal = "600.00",
                saleProceedsOriginalDecimal = "500.00",
                feesOriginalDecimal = "25.00"
            )
        )

        fakeExpenseRepository.saveExpense(
            Expense(
                id = "committed-expense",
                categoryId = "bills",
                categoryNameSnapshot = "Bills",
                spendingClass = SpendingClass.COMMITTED,
                paymentChannel = PaymentChannel.BANK,
                money = Money(
                    originalAmountDecimal = "305.00",
                    originalCurrency = CurrencyCode.LKR,
                    rateLkrPerUnitDecimal = "1.00",
                    baseAmountMinor = 30_500L,
                    rateSource = RateSource.LKR_FIXED,
                    rateDate = transactionDate
                ),
                occurredDate = transactionDate
            )
        ).getOrThrow()
        fakeExpenseRepository.saveExpense(
            Expense(
                id = "discretionary-expense",
                categoryId = "food",
                categoryNameSnapshot = "Food",
                spendingClass = SpendingClass.DISCRETIONARY,
                paymentChannel = PaymentChannel.CARD,
                money = Money(
                    originalAmountDecimal = "200.00",
                    originalCurrency = CurrencyCode.LKR,
                    rateLkrPerUnitDecimal = "1.00",
                    baseAmountMinor = 20_000L,
                    rateSource = RateSource.LKR_FIXED,
                    rateDate = transactionDate
                ),
                occurredDate = transactionDate
            )
        ).getOrThrow()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = integratedViewModel.uiState.value
        assertEquals(5, state.incomeCount)
        assertEquals(152_500L, state.totalIncomeLkrMinor)
        assertEquals(35_000L, state.monthlyCryptoResult.totalGainLkrMinor)
        assertEquals(12_500L, state.monthlyCryptoResult.totalLossLkrMinor)
        assertEquals(22_500L, state.monthlyCryptoResult.netLkrMinor)
        assertEquals(state.monthlyCryptoResult, state.yearToDateCryptoResult)
        assertEquals(2, state.expenseCount)
        assertEquals(50_500L, state.totalExpenseLkrMinor)
        assertEquals(30_500L, state.committedExpenseLkrMinor)
        assertEquals(20_000L, state.discretionaryExpenseLkrMinor)
        assertEquals(60, state.committedPercent)
        assertEquals(40, state.discretionaryPercent)
        assertEquals(102_000L, state.monthlyNetCashFlowLkrMinor)
        assertEquals(7, state.recentActivity.size)
    }

    @Test
    fun goalProgressCalculatedFromPersistedContributions() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        fakeGoalRepository.saveGoal(
            Goal(
                id = "goal-1",
                name = "MacBook Pro M3 Fund",
                targetAmountMinor = 55_000_000L, // 550,000 LKR
                startDate = LocalDate.now(),
                targetDate = LocalDate.now().plusMonths(6),
                archived = false
            )
        )
        fakeGoalRepository.addContribution(
            Contribution(
                id = "contribution-1",
                goalId = "goal-1",
                amountMinor = 13_750_025L,
                kind = ContributionKind.ALLOCATION,
                occurredDate = LocalDate.now()
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeGoal)
        assertEquals("MacBook Pro M3 Fund", state.activeGoal?.name)
        assertEquals(55_000_000L, state.activeGoal?.targetAmountLkrMinor)
        assertEquals(13_750_025L, state.activeGoal?.currentSavedLkrMinor)
        assertEquals(41_249_975L, state.activeGoal?.remainingLkrMinor)
        assertEquals(25, state.activeGoal?.progressPercent)
    }

    @Test
    fun goalProgressIncludesAllocationsAndTheirReversals() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val goal = Goal(
            id = "goal-with-reversal",
            name = "Emergency fund",
            targetAmountMinor = 100_000L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusMonths(3)
        )
        fakeGoalRepository.saveGoal(goal)
        val allocation = Contribution(
            id = "allocation-1",
            goalId = goal.id,
            amountMinor = 40_000L,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now()
        )
        fakeGoalRepository.addContribution(allocation)
        fakeGoalRepository.addContribution(
            Contribution(
                id = "reversal-1",
                goalId = goal.id,
                amountMinor = -40_000L,
                kind = ContributionKind.REVERSAL,
                occurredDate = LocalDate.now(),
                reversesId = allocation.id
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val activeGoal = viewModel.uiState.value.activeGoal
        assertEquals(0L, activeGoal?.currentSavedLkrMinor)
        assertEquals(100_000L, activeGoal?.remainingLkrMinor)
        assertEquals(0, activeGoal?.progressPercent)
    }

    @Test
    fun goalProgressIsExplicitlyUnavailableWhenContributionObservationFails() = runTest {
        val backingRepository = FakeGoalRepository()
        backingRepository.saveGoal(
            Goal(
                id = "goal-unavailable",
                name = "Emergency fund",
                targetAmountMinor = 10_000_000L,
                startDate = LocalDate.now(),
                targetDate = LocalDate.now().plusMonths(3)
            )
        )
        val failingRepository = object : GoalRepository by backingRepository {
            override fun observeContributions(goalId: String): Flow<List<Contribution>> = flow {
                throw IllegalStateException("Contributions unavailable")
            }
        }
        val unavailableViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = fakeIncomeRepository,
            expenseRepository = fakeExpenseRepository,
            categoryRepository = fakeCategoryRepository,
            goalRepository = failingRepository
        )
        backgroundScope.launch { unavailableViewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val goal = unavailableViewModel.uiState.value.activeGoal
        assertNotNull(goal)
        assertNull(goal?.currentSavedLkrMinor)
        assertNull(goal?.remainingLkrMinor)
        assertNull(goal?.progressPercent)
    }

    @Test
    fun goalProgressUsesExactMinorUnitArithmeticAndRejectsOverflow() {
        val goal = Goal(
            id = "exact-goal",
            name = "Exact goal",
            targetAmountMinor = 3L,
            startDate = LocalDate.now(),
            targetDate = LocalDate.now().plusDays(1)
        )
        val oneMinor = Contribution(
            id = "one",
            goalId = goal.id,
            amountMinor = 1L,
            kind = ContributionKind.ALLOCATION,
            occurredDate = LocalDate.now()
        )
        val exact = calculateGoalProgressAmounts(goal, listOf(oneMinor))
        assertEquals(1L, exact?.savedMinor)
        assertEquals(2L, exact?.remainingMinor)
        assertEquals(33, exact?.progressPercent)

        val overflowing = listOf(
            oneMinor.copy(id = "max", amountMinor = Long.MAX_VALUE),
            oneMinor.copy(id = "overflow", amountMinor = 1L)
        )
        assertNull(calculateGoalProgressAmounts(goal, overflowing))
    }

    @Test
    fun stoppedDashboardDoesNotReplayPreviousAccountGoalData() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect() }
        fakeGoalRepository.saveGoal(
            Goal(
                id = "private-goal",
                name = "User A private goal",
                targetAmountMinor = 10_000L,
                startDate = LocalDate.now(),
                targetDate = LocalDate.now().plusDays(1)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("private-goal", viewModel.uiState.value.activeGoal?.id)

        collector.cancelAndJoin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeGoal)
    }

    @Test
    fun financialSummaryPeriodBoundsFollowCalendarMonthAndQuarter() {
        val today = LocalDate.of(2026, 11, 4)

        val month = financialSummaryBounds(today, FinancialSummaryPeriod.MONTH)
        assertEquals(LocalDate.of(2026, 11, 1), month.startDate)
        assertEquals(LocalDate.of(2026, 12, 1), month.endDateExclusive)
        assertEquals(LocalDate.of(2026, 10, 1), month.previousStartDate)
        assertEquals("November 2026", month.label)

        val quarter = financialSummaryBounds(today, FinancialSummaryPeriod.QUARTER)
        assertEquals(LocalDate.of(2026, 10, 1), quarter.startDate)
        assertEquals(LocalDate.of(2027, 1, 1), quarter.endDateExclusive)
        assertEquals(LocalDate.of(2026, 7, 1), quarter.previousStartDate)
        assertEquals("Q4 2026", quarter.label)
    }

    @Test
    fun financialSummaryUsesDeviceZoneAtMonthAndQuarterBoundary() {
        val instant = Instant.parse("2026-12-31T20:00:00Z")
        val utcClock = Clock.fixed(instant, ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 12, 31), LocalDate.now(utcClock))
        val localDate = financialSummaryLocalDate(utcClock, ZoneId.of("Asia/Colombo"))
        assertEquals(LocalDate.of(2027, 1, 1), localDate)

        val bounds = financialSummaryBounds(localDate, FinancialSummaryPeriod.QUARTER)
        assertEquals(LocalDate.of(2027, 1, 1), bounds.startDate)
        assertEquals(LocalDate.of(2027, 4, 1), bounds.endDateExclusive)
        assertEquals("Q1 2027", bounds.label)
    }

    @Test
    fun dashboardMonthUsesDeviceLocalDateAtUtcBoundary() = runTest {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"))
            val occurredDate = LocalDate.of(2027, 1, 1)
            fakeIncomeRepository.saveIncome(
                Income(
                    id = "local-new-year",
                    source = IncomeSource.SALARY,
                    entryKind = IncomeEntryKind.RECEIPT,
                    money = Money(
                        originalAmountDecimal = "100.00",
                        originalCurrency = CurrencyCode.LKR,
                        rateLkrPerUnitDecimal = "1",
                        baseAmountMinor = 10_000L,
                        rateSource = RateSource.LKR_FIXED,
                        rateDate = occurredDate
                    ),
                    occurredDate = occurredDate
                )
            ).getOrThrow()
            val localViewModel = DashboardViewModel(
                authRepository = fakeAuthRepository,
                incomeRepository = fakeIncomeRepository,
                expenseRepository = fakeExpenseRepository,
                categoryRepository = fakeCategoryRepository,
                goalRepository = fakeGoalRepository,
                clock = Clock.fixed(
                    Instant.parse("2026-12-31T20:00:00Z"),
                    ZoneOffset.UTC
                )
            )

            backgroundScope.launch { localViewModel.uiState.collect() }
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, localViewModel.uiState.value.incomeCount)
            assertEquals(10_000L, localViewModel.uiState.value.monthlyNetCashFlowLkrMinor)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun dashboardOverflowShowsErrorInsteadOfWrappedTotals() = runTest {
        val occurredDate = LocalDate.now().withDayOfMonth(1)
        fun expense(id: String, amountMinor: Long) = Expense(
            id = id,
            categoryId = "food",
            categoryNameSnapshot = "Food",
            spendingClass = SpendingClass.DISCRETIONARY,
            paymentChannel = PaymentChannel.CASH,
            money = Money(
                originalAmountDecimal = BigDecimal.valueOf(amountMinor, 2).toPlainString(),
                originalCurrency = CurrencyCode.LKR,
                rateLkrPerUnitDecimal = "1",
                baseAmountMinor = amountMinor,
                rateSource = RateSource.LKR_FIXED,
                rateDate = occurredDate
            ),
            occurredDate = occurredDate
        )
        fakeExpenseRepository.saveExpense(expense("max", Long.MAX_VALUE)).getOrThrow()
        fakeExpenseRepository.saveExpense(expense("one", 1L)).getOrThrow()

        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Dashboard totals are too large to calculate safely", state.errorMessage)
        assertEquals(0L, state.monthlyNetCashFlowLkrMinor)
        assertEquals(0L, state.totalExpenseLkrMinor)
    }

    @Test
    fun financialSummaryMarginAndChartMagnitudeHandleExtremeLongValues() {
        val expectedMargin = BigInteger.valueOf(Long.MIN_VALUE)
            .multiply(BigInteger.valueOf(100L))

        assertEquals(
            expectedMargin,
            calculateNetCashFlowMarginPercent(
                totalIncomeMinor = 1L,
                netCashFlowMinor = Long.MIN_VALUE
            )
        )
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), financialMagnitude(Long.MIN_VALUE))
        assertNull(calculateNetCashFlowMarginPercent(0L, Long.MIN_VALUE))
    }

    @Test
    fun financialSummaryFiltersPersistedDataAndClearsOnSignOut() = runTest {
        val fixedClock = Clock.fixed(
            Instant.parse("2026-11-04T00:00:00Z"),
            ZoneOffset.UTC
        )
        val summaryViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = fakeIncomeRepository,
            expenseRepository = fakeExpenseRepository,
            categoryRepository = fakeCategoryRepository,
            goalRepository = fakeGoalRepository,
            clock = fixedClock
        )
        backgroundScope.launch { summaryViewModel.financialSummaryState.collect() }

        fun money(amountMinor: Long, date: LocalDate) = Money(
            originalAmountDecimal = BigDecimal.valueOf(amountMinor, 2).toPlainString(),
            originalCurrency = CurrencyCode.LKR,
            rateLkrPerUnitDecimal = "1.00",
            baseAmountMinor = amountMinor,
            rateSource = RateSource.LKR_FIXED,
            rateDate = date
        )

        val novemberDate = LocalDate.of(2026, 11, 3)
        val octoberDate = LocalDate.of(2026, 10, 12)
        fakeIncomeRepository.saveIncome(
            Income(
                id = "november-salary",
                source = IncomeSource.SALARY,
                entryKind = IncomeEntryKind.RECEIPT,
                money = money(10_000_000L, novemberDate),
                occurredDate = novemberDate
            )
        ).getOrThrow()
        fakeIncomeRepository.saveIncome(
            Income(
                id = "october-crypto-loss",
                source = IncomeSource.CRYPTO,
                entryKind = IncomeEntryKind.REALIZED_PNL,
                money = money(1_000_000L, octoberDate),
                occurredDate = octoberDate,
                pnlDirection = PnlDirection.LOSS
            )
        ).getOrThrow()
        fakeExpenseRepository.saveExpense(
            Expense(
                id = "november-food",
                categoryId = "food",
                categoryNameSnapshot = "Old category snapshot",
                spendingClass = SpendingClass.DISCRETIONARY,
                paymentChannel = PaymentChannel.CARD,
                money = money(2_500_000L, novemberDate),
                occurredDate = novemberDate
            )
        ).getOrThrow()
        testDispatcher.scheduler.advanceUntilIdle()

        val month = summaryViewModel.financialSummaryState.value
        assertEquals(10_000_000L, month.totalIncomeLkrMinor)
        assertEquals(2_500_000L, month.totalExpenseLkrMinor)
        assertEquals(7_500_000L, month.netCashFlowLkrMinor)
        assertEquals(BigInteger.valueOf(75L), month.netCashFlowMarginPercent)
        assertEquals("Food", month.analytics.expenseCategoryBreakdown.single().categoryName)
        assertEquals(0L, month.analytics.cryptoYtdResult.netPnlLkrMinor)

        summaryViewModel.selectFinancialSummaryPeriod(FinancialSummaryPeriod.QUARTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val quarter = summaryViewModel.financialSummaryState.value
        assertEquals("Q4 2026", quarter.periodLabel)
        assertEquals(9_000_000L, quarter.totalIncomeLkrMinor)
        assertEquals(2_500_000L, quarter.totalExpenseLkrMinor)
        assertEquals(6_500_000L, quarter.netCashFlowLkrMinor)
        assertEquals(-1_000_000L, quarter.analytics.cryptoYtdResult.netPnlLkrMinor)
        assertEquals(3, quarter.trendPoints.size)

        fakeAuthRepository.signOut().getOrThrow()
        testDispatcher.scheduler.advanceUntilIdle()

        val signedOut = summaryViewModel.financialSummaryState.value
        assertEquals(0L, signedOut.totalIncomeLkrMinor)
        assertEquals(0L, signedOut.totalExpenseLkrMinor)
        assertTrue(signedOut.analytics.incomeSourceBreakdown.isEmpty())
    }

    @Test
    fun financialSummaryAccountSwitchReplacesAllAccountScopedData() = runTest {
        val date = LocalDate.of(2026, 11, 3)
        fun money(amountMinor: Long) = Money(
            originalAmountDecimal = BigDecimal.valueOf(amountMinor, 2).toPlainString(),
            originalCurrency = CurrencyCode.LKR,
            rateLkrPerUnitDecimal = "1.00",
            baseAmountMinor = amountMinor,
            rateSource = RateSource.LKR_FIXED,
            rateDate = date
        )
        fun income(id: String, amountMinor: Long) = Income(
            id = id,
            source = IncomeSource.SALARY,
            entryKind = IncomeEntryKind.RECEIPT,
            money = money(amountMinor),
            occurredDate = date
        )
        fun expense(id: String, categoryId: String, amountMinor: Long) = Expense(
            id = id,
            categoryId = categoryId,
            categoryNameSnapshot = "Snapshot $categoryId",
            spendingClass = SpendingClass.DISCRETIONARY,
            paymentChannel = PaymentChannel.CARD,
            money = money(amountMinor),
            occurredDate = date
        )

        val incomesByOwner = mapOf(
            "user-1" to listOf(income("a-income", 10_000_000L)),
            "user-2" to listOf(income("b-income", 20_000_000L))
        )
        val expensesByOwner = mapOf(
            "user-1" to listOf(expense("a-expense", "a-category", 1_000_000L)),
            "user-2" to listOf(expense("b-expense", "b-category", 2_500_000L))
        )
        val categoriesByOwner = mapOf(
            "user-1" to listOf(Category("a-category", "Account A category", SpendingClass.DISCRETIONARY)),
            "user-2" to listOf(Category("b-category", "Account B category", SpendingClass.DISCRETIONARY))
        )
        val accountIncomeRepository = object : IncomeRepository {
            override fun observeIncomeSnapshots(startDate: LocalDate, endDateExclusive: LocalDate) =
                fakeAuthRepository.observeAuthState().map { ownerId ->
                    if (ownerId == null) {
                        IncomeObservationSnapshot.signedOut()
                    } else {
                        IncomeObservationSnapshot.forOwner(
                            ownerId,
                            incomesByOwner[ownerId].orEmpty().filter {
                                !it.occurredDate.isBefore(startDate) &&
                                    it.occurredDate.isBefore(endDateExclusive)
                            }
                        )
                    }
                }

            override suspend fun saveIncome(income: Income) = unsupportedWrite()
            override suspend fun saveIncomeForOwner(expectedOwnerId: String, income: Income) = unsupportedWrite()
            override suspend fun deleteIncome(id: String) = unsupportedWrite()
            override suspend fun deleteIncomeForOwner(expectedOwnerId: String, id: String) = unsupportedWrite()
        }
        val accountExpenseRepository = object : ExpenseRepository {
            override fun observeExpenses(startDate: LocalDate, endDateExclusive: LocalDate) =
                fakeAuthRepository.observeAuthState().map { ownerId ->
                    ownerId?.let(expensesByOwner::get).orEmpty().filter {
                        !it.occurredDate.isBefore(startDate) &&
                            it.occurredDate.isBefore(endDateExclusive)
                    }
                }

            override suspend fun saveExpense(expense: Expense) = unsupportedWrite()
            override suspend fun deleteExpense(id: String) = unsupportedWrite()
        }
        val accountCategoryRepository = object : CategoryRepository {
            override fun observeCategories() = fakeAuthRepository.observeAuthState()
                .map { ownerId -> ownerId?.let(categoriesByOwner::get).orEmpty() }

            override suspend fun addCategory(category: Category) = unsupportedWrite()
            override suspend fun renameCategory(id: String, newName: String) = unsupportedWrite()
            override suspend fun setCategoryArchived(id: String, archived: Boolean) = unsupportedWrite()
        }
        val summaryViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = accountIncomeRepository,
            expenseRepository = accountExpenseRepository,
            categoryRepository = accountCategoryRepository,
            goalRepository = fakeGoalRepository,
            clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC)
        )
        backgroundScope.launch { summaryViewModel.financialSummaryState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val accountA = summaryViewModel.financialSummaryState.value
        assertEquals(10_000_000L, accountA.totalIncomeLkrMinor)
        assertEquals(1_000_000L, accountA.totalExpenseLkrMinor)
        assertEquals("Account A category", accountA.analytics.expenseCategoryBreakdown.single().categoryName)

        fakeAuthRepository.simulateAccountSwitch("user-2", "user2@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        val accountB = summaryViewModel.financialSummaryState.value
        assertEquals(20_000_000L, accountB.totalIncomeLkrMinor)
        assertEquals(2_500_000L, accountB.totalExpenseLkrMinor)
        assertEquals(17_500_000L, accountB.netCashFlowLkrMinor)
        assertEquals("Account B category", accountB.analytics.expenseCategoryBreakdown.single().categoryName)
    }

    private fun unsupportedWrite(): Result<Unit> = Result.failure(UnsupportedOperationException())
}
