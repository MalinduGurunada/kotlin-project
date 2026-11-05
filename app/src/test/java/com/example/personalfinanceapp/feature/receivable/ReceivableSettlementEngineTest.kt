package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.core.money.MoneyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceivableSettlementEngineTest {
    private val date = LocalDate.of(2026, 10, 25)

    @Test
    fun `multiple partial payments settle exactly and edits and deletes recalculate`() {
        val receivable = receivable("100.00")
        val first = payment("one", "25.25")
        val second = payment("two", "24.75")

        val partial = ReceivableSettlementEngine.calculate(receivable, listOf(first, second)).getOrThrow()
        assertEquals(ReceivableStatus.PARTIALLY_PAID, partial.status)
        assertEquals("50.00", partial.paidDecimal)
        assertEquals("50.00", partial.remainingDecimal)

        val edited = ReceivableSettlementEngine.calculate(
            receivable, listOf(first, payment("two", "74.75"))
        ).getOrThrow()
        assertEquals(ReceivableStatus.SETTLED, edited.status)
        assertEquals("0.00", edited.remainingDecimal)

        val afterDelete = ReceivableSettlementEngine.calculate(receivable, listOf(first)).getOrThrow()
        assertEquals("25.25", afterDelete.paidDecimal)
        assertEquals("74.75", afterDelete.remainingDecimal)
    }

    @Test
    fun `overpayment remains fully visible while remaining is capped at zero`() {
        val result = ReceivableSettlementEngine.calculate(
            receivable("100.00"), listOf(payment("one", "125.00"))
        ).getOrThrow()
        assertEquals(ReceivableStatus.SETTLED, result.status)
        assertEquals("125.00", result.paidDecimal)
        assertEquals("0.00", result.remainingDecimal)
    }

    @Test
    fun `historical exchange snapshot does not alter original-currency settlement`() {
        val usdPayment = payment("usd", "50.00", CurrencyCode.USD).copy(
            money = MoneyConverter.fromUsd(
                "50.00", "300.25", RateSource.MANUAL_TRANSFER, date
            ).getOrThrow()
        )
        val result = ReceivableSettlementEngine.calculate(
            receivable("80.00", CurrencyCode.USD), listOf(usdPayment)
        ).getOrThrow()
        assertEquals("50.00", result.paidDecimal)
        assertEquals("30.00", result.remainingDecimal)
        assertEquals("300.25", result.payments.single().money.rateLkrPerUnitDecimal)
    }

    @Test
    fun `invalid links duplicate IDs currency mismatch and overflow are rejected`() {
        assertTrue(ReceivableSettlementEngine.calculate(
            receivable("100.00"), listOf(payment("same", "10.00"), payment("same", "10.00"))
        ).isFailure)
        assertTrue(ReceivableSettlementEngine.calculate(
            receivable("100.00"), listOf(payment("usd", "10.00", CurrencyCode.USD))
        ).isFailure)
        assertTrue(ReceivableSettlementEngine.calculate(
            receivable("100.00"), listOf(payment("bad", "10.00").copy(source = IncomeSource.SALARY))
        ).isFailure)

        val huge = payment("huge", "92233720368547758.07")
        assertTrue(ReceivableSettlementEngine.calculate(
            receivable("92233720368547758.07"), listOf(huge, payment("extra", "0.01"))
        ).isFailure)
    }

    private fun receivable(amount: String, currency: CurrencyCode = CurrencyCode.LKR) = Receivable(
        id = "receivable-1", title = "Project", projectReference = "P-1",
        expectedOriginalDecimal = amount, currency = currency, expectedDate = date
    )

    private fun payment(id: String, amount: String, currency: CurrencyCode = CurrencyCode.LKR): Income {
        val money = if (currency == CurrencyCode.LKR) MoneyConverter.fromLkr(amount, date).getOrThrow()
        else Money(
            amount, currency, "300.00", amount.toBigDecimal().multiply("30000".toBigDecimal()).longValueExact(),
            RateSource.MANUAL_TRANSFER, date
        )
        return Income(
            id = id, source = IncomeSource.FREELANCE, entryKind = IncomeEntryKind.RECEIPT,
            money = money, occurredDate = date, receivableId = "receivable-1"
        )
    }
}
