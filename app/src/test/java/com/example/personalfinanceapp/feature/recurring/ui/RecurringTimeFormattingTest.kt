package com.example.personalfinanceapp.feature.recurring.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringTimeFormattingTest {

    @Test
    fun formatsMidnightAndMorningWithAm() {
        assertEquals("12:00 AM", formatReminderTime(0))
        assertEquals("9:05 AM", formatReminderTime(9 * 60 + 5))
    }

    @Test
    fun formatsNoonAndEveningWithPm() {
        assertEquals("12:00 PM", formatReminderTime(12 * 60))
        assertEquals("6:30 PM", formatReminderTime(18 * 60 + 30))
    }
}
