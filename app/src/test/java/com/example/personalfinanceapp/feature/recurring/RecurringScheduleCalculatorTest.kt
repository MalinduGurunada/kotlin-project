package com.example.personalfinanceapp.feature.recurring

import com.example.personalfinanceapp.core.model.RecurrenceFrequency
import com.example.personalfinanceapp.core.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurringScheduleCalculatorTest {

    @Test
    fun monthEndAnchorReturnsToOriginalDayAfterShortMonth() {
        val schedule = schedule(LocalDate.of(2025, 1, 31))

        assertEquals(LocalDate.of(2025, 1, 31), RecurringScheduleCalculator.dateAt(schedule, 0))
        assertEquals(LocalDate.of(2025, 2, 28), RecurringScheduleCalculator.dateAt(schedule, 1))
        assertEquals(LocalDate.of(2025, 3, 31), RecurringScheduleCalculator.dateAt(schedule, 2))
        assertEquals(LocalDate.of(2025, 4, 30), RecurringScheduleCalculator.dateAt(schedule, 3))
    }

    @Test
    fun leapYearFebruaryUsesTwentyNinth() {
        val schedule = schedule(LocalDate.of(2024, 1, 31))
        assertEquals(LocalDate.of(2024, 2, 29), RecurringScheduleCalculator.dateAt(schedule, 1))
    }

    @Test
    fun yearlyLeapDayReturnsInNextLeapYear() {
        val schedule = schedule(
            anchor = LocalDate.of(2024, 2, 29),
            frequency = RecurrenceFrequency.YEARLY
        )

        assertEquals(LocalDate.of(2025, 2, 28), RecurringScheduleCalculator.dateAt(schedule, 1))
        assertEquals(LocalDate.of(2028, 2, 29), RecurringScheduleCalculator.dateAt(schedule, 4))
    }

    @Test
    fun intervalSupportsBiMonthlyAndQuarterlyCadence() {
        val biMonthly = schedule(LocalDate.of(2026, 1, 31), interval = 2)
        val quarterly = schedule(LocalDate.of(2026, 1, 31), interval = 3)

        assertEquals(LocalDate.of(2026, 3, 31), RecurringScheduleCalculator.dateAt(biMonthly, 1))
        assertEquals(LocalDate.of(2026, 4, 30), RecurringScheduleCalculator.dateAt(quarterly, 1))
    }

    @Test
    fun weeklyCadenceUsesInterval() {
        val schedule = schedule(
            anchor = LocalDate.of(2026, 10, 2),
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2
        )
        assertEquals(LocalDate.of(2026, 10, 16), RecurringScheduleCalculator.dateAt(schedule, 1))
    }

    @Test
    fun dateRangeHonoursStartAndEndDates() {
        val schedule = schedule(LocalDate.of(2026, 1, 31)).copy(
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 3, 31)
        )

        assertEquals(
            listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            RecurringScheduleCalculator.datesBetween(
                schedule,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
            )
        )
        assertNull(RecurringScheduleCalculator.nextDueDate(schedule, LocalDate.of(2026, 4, 1)))
    }

    private fun schedule(
        anchor: LocalDate,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        interval: Long = 1L
    ) = RecurringExpense(
        id = "schedule",
        title = "Bill",
        categoryId = "bills",
        expectedAmountMinor = 1_000L,
        frequency = frequency,
        intervalCount = interval,
        anchorDate = anchor,
        startDate = anchor
    )
}
