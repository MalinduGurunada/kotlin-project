package com.example.personalfinanceapp.feature.dashboard

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
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
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.dashboard.ui.calculateGoalProgressAmounts
import com.example.personalfinanceapp.feature.expense.FakeExpenseRepository
import com.example.personalfinanceapp.feature.goal.FakeGoalRepository
import com.example.personalfinanceapp.feature.goal.GoalRepository
import com.example.personalfinanceapp.feature.income.FakeIncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeIncomeRepository: FakeIncomeRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository(initialUserId = "user-1", initialEmail = "himasha@example.com")
        fakeIncomeRepository = FakeIncomeRepository()
        fakeExpenseRepository = FakeExpenseRepository()
        fakeGoalRepository = FakeGoalRepository()

        viewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            incomeRepository = fakeIncomeRepository,
            expenseRepository = fakeExpenseRepository,
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
        assertEquals(0L, state.totalAvailableBalanceLkrMinor)
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

        // Available balance = 7,500,000 - 1,500,000 = 6,000,000 minor (60,000 LKR)
        assertEquals(6_000_000L, state.totalAvailableBalanceLkrMinor)

        // Activity timeline items
        assertEquals(3, state.recentActivity.size)
        val cryptoActivity = state.recentActivity.firstOrNull { it.id == "crypto-loss-1" }
        assertNotNull(cryptoActivity)
        assertTrue(cryptoActivity!!.isRedAmount)
        assertEquals("-LKR 25,000.00", cryptoActivity.amountText)
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
}
