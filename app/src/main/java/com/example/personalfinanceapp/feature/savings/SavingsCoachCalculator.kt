package com.example.personalfinanceapp.feature.savings

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Goal
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RecurringExpense
import com.example.personalfinanceapp.core.model.RecurringOccurrence
import com.example.personalfinanceapp.core.model.RecurringOccurrenceStatus
import com.example.personalfinanceapp.core.model.RecurringStatus
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.recurring.RecurringScheduleCalculator
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class SavingsCoachUnavailableReason {
    REQUIRED_DATA_UNAVAILABLE,
    NO_ACTIVE_GOAL,
    EXPIRED_DEADLINE,
    NO_QUALIFYING_RECEIPTS,
    NO_POSITIVE_CAPACITY,
    BELOW_ONE_RUPEE
}

data class SavingsCoachInput(
    val today: LocalDate,
    val incomes: List<Income>,
    val expenses: List<Expense>,
    val recurringExpenses: List<RecurringExpense>,
    val recurringOccurrences: List<RecurringOccurrence>,
    val goals: List<Goal>,
    val contributions: List<Contribution>,
    val requiredDataAvailable: Boolean = true
)

data class SavingsCoachGoalProgress(
    val goalId: String,
    val goalName: String,
    val targetAmountMinor: BigInteger,
    val savedAmountMinor: BigInteger,
    val remainingAmountMinor: BigInteger,
    val progressPercent: Int,
    val targetDate: LocalDate
)

data class SavingsCoachResult(
    val goal: SavingsCoachGoalProgress?,
    val qualifyingReceiptsMinor: BigInteger?,
    val recordedExpensesMinor: BigInteger?,
    val upcomingBillsMinor: BigInteger?,
    val currentMonthGoalAllocationsMinor: BigInteger?,
    val cryptoLossAdjustmentMinor: BigInteger?,
    val estimatedCapacityBeforeReserveMinor: BigInteger?,
    val safetyReserveMinor: BigInteger?,
    val estimatedCapacityAfterReserveMinor: BigInteger?,
    val monthlyNeedMinor: BigInteger?,
    val suggestedContributionMinor: BigInteger?,
    val discretionarySpendingMinor: BigInteger?,
    val discretionaryReductionTipMinor: BigInteger?,
    val unavailableReason: SavingsCoachUnavailableReason?
) {
    val hasSuggestion: Boolean
        get() = suggestedContributionMinor != null && suggestedContributionMinor.signum() > 0
}

object SavingsCoachCalculator {

    private val zero = BigInteger.ZERO
    private val oneHundred = BigInteger.valueOf(100L)

    fun calculate(input: SavingsCoachInput): SavingsCoachResult {
        val monthStart = input.today.withDayOfMonth(1)
        val monthEndExclusive = monthStart.plusMonths(1)
        val contributionsByGoal = input.contributions.groupBy(Contribution::goalId)
        val activeIncompleteGoals = input.goals.asSequence()
            .filterNot(Goal::archived)
            .filter { it.targetAmountMinor > 0L }
            .map { goal -> goal to goalProgress(goal, contributionsByGoal[goal.id].orEmpty()) }
            .filter { (_, progress) -> progress.remainingAmountMinor.signum() > 0 }
            .sortedWith(compareBy<Pair<Goal, SavingsCoachGoalProgress>> { it.first.targetDate }
                .thenBy { it.first.id })
            .toList()
        val selected = activeIncompleteGoals.firstOrNull { (goal, _) ->
            !goal.targetDate.isBefore(input.today)
        } ?: activeIncompleteGoals.firstOrNull()
        val goalProgress = selected?.second

        if (!input.requiredDataAvailable) {
            return unavailable(goalProgress, SavingsCoachUnavailableReason.REQUIRED_DATA_UNAVAILABLE)
        }

        val qualifyingReceipts = input.incomes.asSequence()
            .filter { it.inCurrentMonth(monthStart, monthEndExclusive) }
            .filter { it.entryKind == IncomeEntryKind.RECEIPT && it.source != IncomeSource.CRYPTO }
            .fold(zero) { total, income -> total + income.money.baseAmountMinor.nonNegativeBigInteger() }
        val recordedExpenses = input.expenses.asSequence()
            .filter { it.occurredDate.inRange(monthStart, monthEndExclusive) }
            .fold(zero) { total, expense -> total + expense.money.baseAmountMinor.nonNegativeBigInteger() }
        val cryptoLossAdjustment = input.incomes.asSequence()
            .filter { it.inCurrentMonth(monthStart, monthEndExclusive) }
            .filter {
                it.source == IncomeSource.CRYPTO &&
                    it.entryKind == IncomeEntryKind.REALIZED_PNL &&
                    it.pnlDirection == PnlDirection.LOSS
            }
            .fold(zero) { total, income -> total + income.money.baseAmountMinor.nonNegativeBigInteger() }
        val currentMonthAllocations = input.contributions.asSequence()
            .filter { it.occurredDate.inRange(monthStart, monthEndExclusive) }
            .filter { it.kind != ContributionKind.OPENING }
            .fold(zero) { total, contribution -> total + BigInteger.valueOf(contribution.amountMinor) }
            .max(zero)
        val discretionarySpending = input.expenses.asSequence()
            .filter { it.occurredDate.inRange(monthStart, monthEndExclusive) }
            .filter { it.spendingClass == SpendingClass.DISCRETIONARY }
            .fold(zero) { total, expense -> total + expense.money.baseAmountMinor.nonNegativeBigInteger() }

        val upcomingBills = calculateUpcomingBills(input).getOrElse {
            return unavailable(goalProgress, SavingsCoachUnavailableReason.REQUIRED_DATA_UNAVAILABLE)
        }
        val provisionalCapacity = qualifyingReceipts
            .subtract(recordedExpenses)
            .subtract(upcomingBills)
            .subtract(currentMonthAllocations)
            .subtract(cryptoLossAdjustment)
            .max(zero)
        val safetyReserve = provisionalCapacity
            .multiply(BigInteger.valueOf(20L))
            .add(BigInteger.valueOf(99L))
            .divide(oneHundred)
        val capacityAfterReserve = provisionalCapacity.subtract(safetyReserve).max(zero)
        val discretionaryTip = discretionarySpending
            .divide(BigInteger.TEN)
            .roundDownToWholeLkr()
            .takeIf { it.signum() > 0 }

        val base = SavingsCoachResult(
            goal = goalProgress,
            qualifyingReceiptsMinor = qualifyingReceipts,
            recordedExpensesMinor = recordedExpenses,
            upcomingBillsMinor = upcomingBills,
            currentMonthGoalAllocationsMinor = currentMonthAllocations,
            cryptoLossAdjustmentMinor = cryptoLossAdjustment,
            estimatedCapacityBeforeReserveMinor = provisionalCapacity,
            safetyReserveMinor = safetyReserve,
            estimatedCapacityAfterReserveMinor = capacityAfterReserve,
            monthlyNeedMinor = null,
            suggestedContributionMinor = null,
            discretionarySpendingMinor = discretionarySpending,
            discretionaryReductionTipMinor = discretionaryTip,
            unavailableReason = null
        )

        if (selected == null || goalProgress == null) {
            return base.copy(unavailableReason = SavingsCoachUnavailableReason.NO_ACTIVE_GOAL)
        }
        if (selected.first.targetDate.isBefore(input.today)) {
            return base.copy(unavailableReason = SavingsCoachUnavailableReason.EXPIRED_DEADLINE)
        }

        val monthsRemaining = ChronoUnit.MONTHS.between(
            YearMonth.from(input.today),
            YearMonth.from(selected.first.targetDate)
        ).coerceAtLeast(0L) + 1L
        val monthlyNeed = divideCeiling(
            goalProgress.remainingAmountMinor,
            BigInteger.valueOf(monthsRemaining)
        )
        val rawSuggestion = minOf(
            capacityAfterReserve,
            goalProgress.remainingAmountMinor,
            monthlyNeed
        )
        val suggestion = rawSuggestion.roundDownToWholeLkr()
        val reason = when {
            qualifyingReceipts.signum() <= 0 -> SavingsCoachUnavailableReason.NO_QUALIFYING_RECEIPTS
            capacityAfterReserve.signum() <= 0 -> SavingsCoachUnavailableReason.NO_POSITIVE_CAPACITY
            suggestion.signum() <= 0 -> SavingsCoachUnavailableReason.BELOW_ONE_RUPEE
            else -> null
        }

        return base.copy(
            monthlyNeedMinor = monthlyNeed,
            suggestedContributionMinor = suggestion.takeIf { reason == null },
            unavailableReason = reason
        )
    }

    private fun calculateUpcomingBills(input: SavingsCoachInput): Result<BigInteger> = runCatching {
        val horizonEnd = input.today.plusDays(30L)
        val occurrenceById = input.recurringOccurrences.associateBy(RecurringOccurrence::id)
        val linkedExpenseOccurrences = input.expenses.mapNotNullTo(mutableSetOf()) {
            it.recurringOccurrenceId
        }

        input.recurringExpenses.asSequence()
            .filter { it.status == RecurringStatus.ACTIVE }
            .flatMap { recurring ->
                RecurringScheduleCalculator.datesBetween(recurring, input.today, horizonEnd)
                    .asSequence()
                    .map { dueDate -> recurring to dueDate }
            }
            .filter { (recurring, dueDate) ->
                val occurrenceId = RecurringScheduleCalculator.occurrenceId(recurring.id, dueDate)
                val status = occurrenceById[occurrenceId]?.status
                status != RecurringOccurrenceStatus.CONFIRMED &&
                    status != RecurringOccurrenceStatus.SKIPPED &&
                    occurrenceId !in linkedExpenseOccurrences
            }
            .fold(zero) { total, (recurring, _) ->
                total + recurring.expectedAmountMinor.nonNegativeBigInteger()
            }
    }

    private fun goalProgress(
        goal: Goal,
        contributions: List<Contribution>
    ): SavingsCoachGoalProgress {
        val target = BigInteger.valueOf(goal.targetAmountMinor)
        val saved = contributions.fold(zero) { total, contribution ->
            total + BigInteger.valueOf(contribution.amountMinor)
        }.max(zero)
        val remaining = target.subtract(saved).max(zero)
        val percent = if (target.signum() > 0) {
            saved.multiply(oneHundred).divide(target).min(oneHundred).toInt()
        } else {
            0
        }
        return SavingsCoachGoalProgress(
            goalId = goal.id,
            goalName = goal.name,
            targetAmountMinor = target,
            savedAmountMinor = saved,
            remainingAmountMinor = remaining,
            progressPercent = percent,
            targetDate = goal.targetDate
        )
    }

    private fun unavailable(
        goal: SavingsCoachGoalProgress?,
        reason: SavingsCoachUnavailableReason
    ) = SavingsCoachResult(
        goal = goal,
        qualifyingReceiptsMinor = null,
        recordedExpensesMinor = null,
        upcomingBillsMinor = null,
        currentMonthGoalAllocationsMinor = null,
        cryptoLossAdjustmentMinor = null,
        estimatedCapacityBeforeReserveMinor = null,
        safetyReserveMinor = null,
        estimatedCapacityAfterReserveMinor = null,
        monthlyNeedMinor = null,
        suggestedContributionMinor = null,
        discretionarySpendingMinor = null,
        discretionaryReductionTipMinor = null,
        unavailableReason = reason
    )

    private fun Income.inCurrentMonth(start: LocalDate, endExclusive: LocalDate): Boolean =
        occurredDate.inRange(start, endExclusive)

    private fun LocalDate.inRange(start: LocalDate, endExclusive: LocalDate): Boolean =
        !isBefore(start) && isBefore(endExclusive)

    private fun Long.nonNegativeBigInteger(): BigInteger = BigInteger.valueOf(this).max(zero)

    private fun BigInteger.roundDownToWholeLkr(): BigInteger =
        divide(oneHundred).multiply(oneHundred)

    private fun divideCeiling(dividend: BigInteger, divisor: BigInteger): BigInteger =
        dividend.add(divisor).subtract(BigInteger.ONE).divide(divisor)
}
