package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CryptoRealizedDetails
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.money.MoneyConverter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CryptoRealizedCalculatorTest {

    @Test
    fun `profit is proceeds minus basis and fees`() {
        val result = CryptoRealizedCalculator.calculate("1000", "600", "50").getOrThrow()

        assertEquals("350", result.resultMagnitudeOriginalDecimal)
        assertEquals(PnlDirection.GAIN, result.direction)
    }

    @Test
    fun `loss keeps a positive magnitude and loss direction`() {
        val result = CryptoRealizedCalculator.calculate("500", "600", "25").getOrThrow()

        assertEquals("125", result.resultMagnitudeOriginalDecimal)
        assertEquals(PnlDirection.LOSS, result.direction)
    }

    @Test
    fun `zero result is break even and supports zero fees`() {
        val result = CryptoRealizedCalculator.calculate("600", "600", "0").getOrThrow()

        assertEquals("0", result.resultMagnitudeOriginalDecimal)
        assertEquals(PnlDirection.BREAK_EVEN, result.direction)
    }

    @Test
    fun `monthly and YTD boundaries use transaction date and combine multiple results`() {
        val incomes = listOf(
            crypto("before-year", LocalDate.of(2025, 12, 31), 90_00, PnlDirection.GAIN),
            crypto("jan-gain", LocalDate.of(2026, 1, 1), 100_00, PnlDirection.GAIN),
            crypto("oct-loss", LocalDate.of(2026, 10, 31), 30_00, PnlDirection.LOSS),
            crypto("nov-gain", LocalDate.of(2026, 11, 1), 50_00, PnlDirection.GAIN),
            crypto("after-cutoff", LocalDate.of(2026, 11, 4), 70_00, PnlDirection.GAIN)
        )

        val month = CryptoResultCalculator.calculate(
            incomes,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 4)
        )
        val ytd = CryptoResultCalculator.calculate(
            incomes,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 11, 4)
        )

        assertEquals(50_00, month.netLkrMinor)
        assertEquals(150_00, ytd.totalGainLkrMinor)
        assertEquals(30_00, ytd.totalLossLkrMinor)
        assertEquals(120_00, ytd.netLkrMinor)
    }

    @Test
    fun `sale withdrawal is metadata and only net result enters income`() {
        val income = crypto("sale", LocalDate.of(2026, 11, 2), 350_00, PnlDirection.GAIN).copy(
            cryptoRealizedDetails = CryptoRealizedDetails(
                costBasisOriginalDecimal = "600",
                saleProceedsOriginalDecimal = "1000",
                feesOriginalDecimal = "50"
            )
        )

        assertEquals(350_00, income.signedBaseAmountMinor())
    }

    private fun crypto(
        id: String,
        date: LocalDate,
        magnitudeMinor: Long,
        direction: PnlDirection
    ) = Income(
        id = id,
        source = IncomeSource.CRYPTO,
        entryKind = IncomeEntryKind.REALIZED_PNL,
        money = MoneyConverter.fromLkrNonNegative(
            amountInput = (magnitudeMinor.toBigDecimal().movePointLeft(2)).toPlainString(),
            transactionDate = date
        ).getOrThrow(),
        occurredDate = date,
        pnlDirection = direction
    )
}
