package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import java.time.LocalDate
import java.time.YearMonth

object RecurringScheduleCalculator {

    fun occurrenceId(recurringExpenseId: String, dueDate: LocalDate): String =
        "${recurringExpenseId}_${dueDate}"

    fun expenseId(recurringExpenseId: String, dueDate: LocalDate): String =
        "recurring_${occurrenceId(recurringExpenseId, dueDate)}"

    fun dateAt(schedule: RecurringExpense, occurrenceIndex: Long): LocalDate {
        require(occurrenceIndex >= 0L) { "Occurrence index cannot be negative" }
        require(schedule.intervalCount > 0L) { "Interval count must be positive" }

        val distance = Math.multiplyExact(occurrenceIndex, schedule.intervalCount)
        return when (schedule.frequency) {
            RecurrenceFrequency.WEEKLY -> schedule.anchorDate.plusWeeks(distance)
            RecurrenceFrequency.MONTHLY -> monthlyDate(schedule.anchorDate, distance)
            RecurrenceFrequency.YEARLY -> yearlyDate(schedule.anchorDate, distance)
        }
    }

    fun datesBetween(
        schedule: RecurringExpense,
        startInclusive: LocalDate,
        endInclusive: LocalDate
    ): List<LocalDate> {
        require(!endInclusive.isBefore(startInclusive)) {
            "End date cannot be before start date"
        }

        val effectiveStart = maxOf(startInclusive, schedule.startDate)
        val effectiveEnd = schedule.endDate?.let { minOf(endInclusive, it) } ?: endInclusive
        if (effectiveEnd.isBefore(effectiveStart)) return emptyList()

        val dates = mutableListOf<LocalDate>()
        var index = firstCandidateIndex(schedule, effectiveStart)
        while (true) {
            val date = dateAt(schedule, index)
            if (date.isAfter(effectiveEnd)) break
            if (!date.isBefore(effectiveStart)) dates += date
            index++
        }
        return dates
    }

    fun nextDueDate(schedule: RecurringExpense, fromInclusive: LocalDate): LocalDate? {
        val end = schedule.endDate ?: fromInclusive.plusYears(200)
        if (end.isBefore(fromInclusive)) return null
        return datesBetween(schedule, fromInclusive, end).firstOrNull()
    }

    private fun firstCandidateIndex(schedule: RecurringExpense, target: LocalDate): Long {
        if (!schedule.anchorDate.isBefore(target)) return 0L

        val approximateUnits = when (schedule.frequency) {
            RecurrenceFrequency.WEEKLY ->
                java.time.temporal.ChronoUnit.WEEKS.between(schedule.anchorDate, target)
            RecurrenceFrequency.MONTHLY ->
                java.time.temporal.ChronoUnit.MONTHS.between(
                    YearMonth.from(schedule.anchorDate),
                    YearMonth.from(target)
                )
            RecurrenceFrequency.YEARLY ->
                (target.year - schedule.anchorDate.year).toLong()
        }

        var index = (approximateUnits / schedule.intervalCount).coerceAtLeast(0L)
        while (index > 0L && !dateAt(schedule, index - 1L).isBefore(target)) index--
        while (dateAt(schedule, index).isBefore(target)) index++
        return index
    }

    private fun monthlyDate(anchor: LocalDate, months: Long): LocalDate {
        val targetMonth = YearMonth.from(anchor).plusMonths(months)
        return targetMonth.atDay(anchor.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth()))
    }

    private fun yearlyDate(anchor: LocalDate, years: Long): LocalDate {
        val targetYear = Math.addExact(anchor.year.toLong(), years).toInt()
        val targetMonth = YearMonth.of(targetYear, anchor.month)
        return targetMonth.atDay(anchor.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth()))
    }
}
