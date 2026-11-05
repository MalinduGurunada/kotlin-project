package com.example.personalfinanceapp.feature.savings

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.recurring.RecurringScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

class SavingsCoachCalculatorTest {

    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun recommendationDeductsRecordedCommitmentsAndReservesTwentyPercent() {
        val goal = goal("goal", 10_000_000L, LocalDate.of(2026, 11, 30))
        val result = calculate(
            incomes = listOf(
                income("salary", 10_000_000L),
                income("crypto-gain", 3_000_000L, IncomeSource.CRYPTO, PnlDirection.GAIN),
                income("crypto-loss", 500_000L, IncomeSource.CRYPTO, PnlDirection.LOSS)
            ),
            expenses = listOf(expense("rent", 2_000_000L)),
            recurringExpenses = listOf(recurring("bill", 1_000_000L, today.plusDays(10))),
            goals = listOf(goal),
            contributions = listOf(
                contribution("saved", goal.id, 2_000_000L, ContributionKind.OPENING, today.minusMonths(1)),
                contribution("allocation", goal.id, 500_000L, ContributionKind.ALLOCATION, today)
            )
        )

        assertEquals(big(10_000_000L), result.qualifyingReceiptsMinor)
        assertEquals(big(2_000_000L), result.recordedExpensesMinor)
        assertEquals(big(1_000_000L), result.upcomingBillsMinor)
        assertEquals(big(500_000L), result.currentMonthGoalAllocationsMinor)
        assertEquals(big(500_000L), result.cryptoLossAdjustmentMinor)
        assertEquals(big(6_000_000L), result.estimatedCapacityBeforeReserveMinor)
        assertEquals(big(1_200_000L), result.safetyReserveMinor)
        assertEquals(big(4_800_000L), result.suggestedContributionMinor)
        assertTrue(result.hasSuggestion)
    }

    @Test
    fun confirmedSkippedAndExpenseLinkedBillsAreNotCountedAgain() {
        val confirmed = recurring("confirmed", 1_000_000L, today.plusDays(2))
        val skipped = recurring("skipped", 2_000_000L, today.plusDays(3))
        val linked = recurring("linked", 3_000_000L, today.plusDays(4))
        val confirmedId = RecurringScheduleCalculator.occurrenceId(confirmed.id, confirmed.anchorDate)
        val skippedId = RecurringScheduleCalculator.occurrenceId(skipped.id, skipped.anchorDate)
        val linkedId = RecurringScheduleCalculator.occurrenceId(linked.id, linked.anchorDate)

        val result = calculate(
            incomes = listOf(income("salary", 10_000_000L)),
            expenses = listOf(expense("linked-expense", 3_000_000L, recurringOccurrenceId = linkedId)),
            recurringExpenses = listOf(confirmed, skipped, linked),
            occurrences = listOf(
                occurrence(confirmedId, confirmed, RecurringOccurrenceStatus.CONFIRMED),
                occurrence(skippedId, skipped, RecurringOccurrenceStatus.SKIPPED)
            ),
            goals = listOf(goal("goal", 20_000_000L, today.plusMonths(1)))
        )

        assertEquals(BigInteger.ZERO, result.upcomingBillsMinor)
        assertEquals(big(3_000_000L), result.recordedExpensesMinor)
    }

    @Test
    fun reversalsOffsetCurrentAllocationsWithoutOpeningOrPriorMonthDoubleCounting() {
        val goal = goal("goal", 20_000_000L, today.plusMonths(2))
        val result = calculate(
            incomes = listOf(income("salary", 10_000_000L)),
            goals = listOf(goal),
            contributions = listOf(
                contribution("opening", goal.id, 5_000_000L, ContributionKind.OPENING, today),
                contribution("prior", goal.id, 2_000_000L, ContributionKind.ALLOCATION, today.minusMonths(1)),
                contribution("allocation", goal.id, 1_000_000L, ContributionKind.ALLOCATION, today),
                contribution("reversal", goal.id, -400_000L, ContributionKind.REVERSAL, today)
            )
        )

        assertEquals(big(600_000L), result.currentMonthGoalAllocationsMinor)
        assertEquals(big(7_520_000L), result.estimatedCapacityAfterReserveMinor)
    }

    @Test
    fun earliestValidDeadlineWinsAndExpiredOnlyGoalsDoNotProduceSuggestion() {
        val expired = goal("expired", 5_000_000L, today.minusDays(1))
        val later = goal("later", 5_000_000L, today.plusMonths(2))
        val earlier = goal("earlier", 5_000_000L, today.plusMonths(1))
        val archived = goal("archived", 5_000_000L, today.plusDays(1)).copy(archived = true)

        val selected = calculate(
            incomes = listOf(income("salary", 10_000_000L)),
            goals = listOf(expired, later, archived, earlier)
        )
        assertEquals("earlier", selected.goal?.goalId)

        val expiredOnly = calculate(
            incomes = listOf(income("salary", 10_000_000L)),
            goals = listOf(expired)
        )
        assertEquals("expired", expiredOnly.goal?.goalId)
        assertEquals(SavingsCoachUnavailableReason.EXPIRED_DEADLINE, expiredOnly.unavailableReason)
        assertNull(expiredOnly.suggestedContributionMinor)
    }

    @Test
    fun extremeLongInputsUseBigIntegerAndSuggestionRemainsBounded() {
        val result = calculate(
            incomes = listOf(
                income("one", Long.MAX_VALUE),
                income("two", Long.MAX_VALUE)
            ),
            goals = listOf(goal("goal", Long.MAX_VALUE, today))
        )

        assertEquals(
            BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO),
            result.qualifyingReceiptsMinor
        )
        assertTrue(result.suggestedContributionMinor!! <= BigInteger.valueOf(Long.MAX_VALUE))
        assertEquals(BigInteger.ZERO, result.suggestedContributionMinor!!.remainder(big(100L)))
    }

    @Test
    fun unavailableDataAndNoReceiptsNeverProduceSuggestion() {
        val unavailable = calculate(
            goals = listOf(goal("goal", 1_000_000L, today.plusMonths(1))),
            requiredDataAvailable = false
        )
        assertEquals(SavingsCoachUnavailableReason.REQUIRED_DATA_UNAVAILABLE, unavailable.unavailableReason)
        assertNull(unavailable.estimatedCapacityAfterReserveMinor)
        assertFalse(unavailable.hasSuggestion)

        val noReceipts = calculate(goals = listOf(goal("goal", 1_000_000L, today.plusMonths(1))))
        assertEquals(SavingsCoachUnavailableReason.NO_QUALIFYING_RECEIPTS, noReceipts.unavailableReason)
        assertNull(noReceipts.suggestedContributionMinor)
    }

    @Test
    fun discretionaryTipUsesOnlyRecordedDiscretionaryExpenses() {
        val result = calculate(
            incomes = listOf(income("salary", 10_000_000L)),
            expenses = listOf(
                expense("food", 1_234_567L, SpendingClass.DISCRETIONARY),
                expense("rent", 2_000_000L, SpendingClass.COMMITTED)
            ),
            goals = listOf(goal("goal", 10_000_000L, today.plusMonths(1)))
        )

        assertEquals(big(1_234_567L), result.discretionarySpendingMinor)
        assertEquals(big(123_400L), result.discretionaryReductionTipMinor)
    }

    private fun calculate(
        incomes: List<Income> = emptyList(),
        expenses: List<Expense> = emptyList(),
        recurringExpenses: List<RecurringExpense> = emptyList(),
        occurrences: List<RecurringOccurrence> = emptyList(),
        goals: List<Goal> = emptyList(),
        contributions: List<Contribution> = emptyList(),
        requiredDataAvailable: Boolean = true
    ) = SavingsCoachCalculator.calculate(
        SavingsCoachInput(
            today = today,
            incomes = incomes,
            expenses = expenses,
            recurringExpenses = recurringExpenses,
            recurringOccurrences = occurrences,
            goals = goals,
            contributions = contributions,
            requiredDataAvailable = requiredDataAvailable
        )
    )

    private fun goal(id: String, targetMinor: Long, targetDate: LocalDate) = Goal(
        id = id,
        name = "Goal $id",
        targetAmountMinor = targetMinor,
        startDate = today.minusMonths(1),
        targetDate = targetDate
    )

    private fun income(
        id: String,
        amountMinor: Long,
        source: IncomeSource = IncomeSource.SALARY,
        direction: PnlDirection? = null
    ) = Income(
        id = id,
        source = source,
        entryKind = if (source == IncomeSource.CRYPTO) IncomeEntryKind.REALIZED_PNL else IncomeEntryKind.RECEIPT,
        money = money(amountMinor),
        occurredDate = today,
        pnlDirection = direction
    )

    private fun expense(
        id: String,
        amountMinor: Long,
        spendingClass: SpendingClass = SpendingClass.COMMITTED,
        recurringOccurrenceId: String? = null
    ) = Expense(
        id = id,
        categoryId = "category",
        categoryNameSnapshot = "Category",
        spendingClass = spendingClass,
        paymentChannel = PaymentChannel.BANK,
        money = money(amountMinor),
        occurredDate = today,
        recurringOccurrenceId = recurringOccurrenceId
    )

    private fun money(amountMinor: Long) = Money(
        originalAmountDecimal = "1.00",
        originalCurrency = CurrencyCode.LKR,
        rateLkrPerUnitDecimal = "1",
        baseAmountMinor = amountMinor,
        rateSource = RateSource.LKR_FIXED,
        rateDate = today
    )

    private fun recurring(id: String, amountMinor: Long, dueDate: LocalDate) = RecurringExpense(
        id = id,
        title = "Bill $id",
        categoryId = "bills",
        expectedAmountMinor = amountMinor,
        frequency = RecurrenceFrequency.MONTHLY,
        anchorDate = dueDate,
        startDate = dueDate
    )

    private fun occurrence(
        id: String,
        recurring: RecurringExpense,
        status: RecurringOccurrenceStatus
    ) = RecurringOccurrence(
        id = id,
        recurringExpenseId = recurring.id,
        dueDate = recurring.anchorDate,
        status = status
    )

    private fun contribution(
        id: String,
        goalId: String,
        amountMinor: Long,
        kind: ContributionKind,
        date: LocalDate
    ) = Contribution(
        id = id,
        goalId = goalId,
        amountMinor = amountMinor,
        kind = kind,
        occurredDate = date,
        reversesId = if (kind == ContributionKind.REVERSAL) "original" else null
    )

    private fun big(value: Long): BigInteger = BigInteger.valueOf(value)
}
