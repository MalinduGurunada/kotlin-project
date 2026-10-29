package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PnlDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import java.util.Locale

private val transactionDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH)
private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)

internal fun formatLkrMinorAmount(amountMinor: Long): String =
    "LKR ${formatDecimal(BigDecimal.valueOf(amountMinor, 2))}"

internal fun formatOriginalAmount(money: Money): String {
    val amount = money.originalAmountDecimal.toBigDecimalOrNull()
        ?.let { decimal ->
            runCatching { decimal.setScale(2, RoundingMode.UNNECESSARY) }.getOrNull()
        }
    val formattedAmount = amount?.let(::formatDecimal) ?: money.originalAmountDecimal
    return "${money.originalCurrency.name} $formattedAmount"
}

internal fun incomeSourceLabel(income: Income): String = when (income.source) {
    IncomeSource.SALARY -> "Salary"
    IncomeSource.FREELANCE -> "Freelance"
    IncomeSource.ADSENSE -> "AdSense"
    IncomeSource.CRYPTO -> when (income.pnlDirection) {
        PnlDirection.GAIN -> "Crypto Gain"
        PnlDirection.LOSS -> "Crypto Loss"
        null -> "Crypto"
    }
}

internal fun formatTransactionDate(income: Income): String =
    income.occurredDate.format(transactionDateFormatter)

internal fun formatIncomeDateRange(dateRange: IncomeDateRange): String {
    val isWholeMonth = dateRange.startDate.dayOfMonth == 1 &&
        dateRange.endDateExclusive == dateRange.startDate.plusMonths(1)
    return if (isWholeMonth) {
        dateRange.startDate.format(monthFormatter)
    } else {
        val inclusiveEnd = dateRange.endDateExclusive.minusDays(1)
        "${dateRange.startDate.format(transactionDateFormatter)} - " +
            inclusiveEnd.format(transactionDateFormatter)
    }
}

internal fun shiftIncomeMonth(
    dateRange: IncomeDateRange,
    monthOffset: Long
): IncomeDateRange? = try {
    val shiftedStart = dateRange.startDate
        .withDayOfMonth(1)
        .plusMonths(monthOffset)
    IncomeDateRange(
        startDate = shiftedStart,
        endDateExclusive = shiftedStart.plusMonths(1)
    )
} catch (_: DateTimeException) {
    null
} catch (_: ArithmeticException) {
    null
}

private fun formatDecimal(value: BigDecimal): String = DecimalFormat(
    "#,##0.00",
    DecimalFormatSymbols.getInstance(Locale.US)
).format(value)
