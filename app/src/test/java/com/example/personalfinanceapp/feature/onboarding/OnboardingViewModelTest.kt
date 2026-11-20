package com.example.personalfinanceapp.feature.onboarding

import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.feature.auth.FakeAuthRepository
import com.example.personalfinanceapp.feature.goal.FakeGoalRepository
import com.example.personalfinanceapp.feature.onboarding.ui.OnboardingGateState
import com.example.personalfinanceapp.feature.onboarding.ui.OnboardingViewModel
import com.example.personalfinanceapp.feature.onboarding.ui.toMinorUnitsOrNull
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var onboardingRepository: FakeOnboardingRepository
    private lateinit var goalRepository: FakeGoalRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository(initialUserId = "user-1")
        onboardingRepository = FakeOnboardingRepository()
        goalRepository = FakeGoalRepository()
        viewModel = OnboardingViewModel(authRepository, onboardingRepository, goalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingProfileRequiresOnboarding() = runTest {
        advanceUntilIdle()

        assertEquals(OnboardingGateState.Required, viewModel.gateState.value)
    }

    @Test
    fun salaryRequiresValidPaydayBeforeAdvancing() = runTest {
        advanceUntilIdle()
        viewModel.nextStep()
        viewModel.toggleIncomeSource(IncomeSource.SALARY)
        viewModel.onSalaryPaydayChanged("32")
        viewModel.nextStep()

        assertEquals(1, viewModel.uiState.value.step)
        assertEquals("Enter a salary payday from 1 to 31", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun completingRequiredFieldsStoresProfileAndOpensNavigationGate() = runTest {
        advanceUntilIdle()
        reachReviewWithoutGoal()

        viewModel.finishOnboarding()
        advanceUntilIdle()

        assertEquals(OnboardingGateState.Complete, viewModel.gateState.value)
        val access = onboardingRepository.access as OnboardingAccess.Complete
        assertEquals(setOf(IncomeSource.SALARY), access.profile.incomeSources)
        assertEquals(25, access.profile.salaryPayday)
        assertEquals(15_000_000L, access.profile.expectedMonthlyIncomeMinor)
        assertEquals(9_000_000L, access.profile.expectedMonthlySpendingMinor)
        assertTrue(goalRepository.observeGoals().first().isEmpty())
    }

    @Test
    fun optionalGoalCreatesOneGoalAndOpeningContribution() = runTest {
        advanceUntilIdle()
        viewModel.nextStep()
        viewModel.toggleIncomeSource(IncomeSource.FREELANCE)
        viewModel.nextStep()
        viewModel.onExpectedMonthlyIncomeChanged("100000")
        viewModel.onExpectedMonthlySpendingChanged("60000")
        viewModel.nextStep()
        viewModel.onIncludeFirstGoalChanged(true)
        viewModel.onGoalNameChanged("Emergency fund")
        viewModel.onGoalTargetChanged("300000")
        viewModel.onGoalSavedChanged("50000")
        viewModel.onGoalTargetDateChanged(LocalDate.now().plusYears(1))

        viewModel.finishOnboarding()
        advanceUntilIdle()

        val goals = goalRepository.observeGoals().first()
        assertEquals(1, goals.size)
        assertEquals("Emergency fund", goals.single().name)
        val contributions = goalRepository.observeContributions(goals.single().id).first()
        assertEquals(1, contributions.size)
        assertEquals(ContributionKind.OPENING, contributions.single().kind)
        assertEquals(5_000_000L, contributions.single().amountMinor)
        assertEquals(OnboardingGateState.Complete, viewModel.gateState.value)
    }

    @Test
    fun exactMoneyParserRejectsNegativeAndMoreThanTwoDecimals() {
        assertEquals(123_45L, "123.45".toMinorUnitsOrNull())
        assertEquals(0L, "0".toMinorUnitsOrNull())
        assertNull("-1".toMinorUnitsOrNull())
        assertNull("10.999".toMinorUnitsOrNull())
        assertNull("not-money".toMinorUnitsOrNull())
    }

    private fun reachReviewWithoutGoal() {
        viewModel.nextStep()
        viewModel.toggleIncomeSource(IncomeSource.SALARY)
        viewModel.onSalaryPaydayChanged("25")
        viewModel.nextStep()
        viewModel.onExpectedMonthlyIncomeChanged("150000")
        viewModel.onExpectedMonthlySpendingChanged("90000")
        viewModel.nextStep()
    }
}
