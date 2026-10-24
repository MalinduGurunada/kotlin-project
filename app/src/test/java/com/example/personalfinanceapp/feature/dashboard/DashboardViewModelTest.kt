package com.example.personalfinanceapp.feature.dashboard

import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.dashboard.ui.DashboardViewModel
import com.example.personalfinanceapp.feature.expense.FakeExpenseRepository
import com.example.personalfinanceapp.feature.goal.FakeGoalRepository
import com.example.personalfinanceapp.feature.income.FakeIncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

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
        fakeAuthRepository = FakeAuthRepository(initialUserId = "user-1")
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
    fun initialStateCalculatesMonthlyTotalsAndGoalProgress() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Himasha", state.userName)
        assertNotNull(state.activeGoal)
        assertEquals(70, state.activeGoal?.progressPercent)
    }
}
