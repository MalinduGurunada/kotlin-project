package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FakeIncomeRepositoryTest {

    private val startDate = LocalDate.of(2026, 10, 1)
    private val endDate = LocalDate.of(2026, 11, 1)
    private val defaultDate = LocalDate.of(2026, 10, 12)

    private fun createLkrIncome(
        id: String = "income-001",
        source: IncomeSource = IncomeSource.SALARY,
        entryKind: IncomeEntryKind = IncomeEntryKind.RECEIPT,
        pnlDirection: PnlDirection? = null,
        date: LocalDate = defaultDate,
        projectReference: String? = null,
        receivableId: String? = null,
        assetSymbol: String? = null,
        note: String? = null
    ): Income {
        val money = MoneyConverter.fromLkr(
            amountInput = "1000.00",
            transactionDate = date
        ).getOrThrow()

        return Income(
            id = id,
            source = source,
            entryKind = entryKind,
            money = money,
            occurredDate = date,
            pnlDirection = pnlDirection,
            projectReference = projectReference,
            receivableId = receivableId,
            assetSymbol = assetSymbol,
            note = note
        )
    }

    private fun createUsdIncome(
        id: String = "income-001",
        date: LocalDate = defaultDate,
        rateDate: LocalDate = date.minusDays(1),
        rateSource: RateSource = RateSource.MANUAL_REFERENCE,
        note: String? = null
    ): Income {
        val money = MoneyConverter.fromUsd(
            amountInput = "125.50",
            rateLkrPerUsdInput = "305.25",
            rateSource = rateSource,
            rateDate = rateDate
        ).getOrThrow()

        return Income(
            id = id,
            source = IncomeSource.FREELANCE,
            entryKind = IncomeEntryKind.RECEIPT,
            money = money,
            occurredDate = date,
            projectReference = "website-redesign",
            receivableId = "receivable-001",
            assetSymbol = "USD",
            note = note
        )
    }

    private suspend fun FakeIncomeRepository.incomesIn(
        start: LocalDate = startDate,
        endExclusive: LocalDate = endDate
    ): List<Income> = observeIncomes(start, endExclusive).first()

    @Test
    fun initialIncomeListIsEmpty() = runTest {
        val repository = FakeIncomeRepository()

        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun savesValidSalaryAndPreservesAllFields() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(
            projectReference = "annual-contract",
            receivableId = "receivable-001",
            assetSymbol = "LKR",
            note = "October salary"
        )

        val result = repository.saveIncome(income)

        assertTrue(result.isSuccess)
        assertEquals(listOf(income), repository.incomesIn())
    }

    @Test
    fun savesValidFreelanceIncome() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(
            source = IncomeSource.FREELANCE,
            projectReference = "mobile-app"
        )

        assertTrue(repository.saveIncome(income).isSuccess)
        assertEquals(listOf(income), repository.incomesIn())
    }

    @Test
    fun savesValidAdsenseIncome() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(source = IncomeSource.ADSENSE)

        assertTrue(repository.saveIncome(income).isSuccess)
        assertEquals(listOf(income), repository.incomesIn())
    }

    @Test
    fun savesValidCryptoGain() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.GAIN,
            assetSymbol = "BTC"
        )

        assertTrue(repository.saveIncome(income).isSuccess)
        assertEquals(listOf(income), repository.incomesIn())
    }

    @Test
    fun savesValidCryptoLossWithPositiveMagnitude() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.LOSS,
            assetSymbol = "ETH"
        )

        assertTrue(repository.saveIncome(income).isSuccess)

        val stored = repository.incomesIn().single()
        assertEquals(PnlDirection.LOSS, stored.pnlDirection)
        assertTrue(stored.money.baseAmountMinor > 0L)
    }

    @Test
    fun rejectsBlankIncomeIdWithoutChangingState() = runTest {
        val repository = FakeIncomeRepository()

        val result = repository.saveIncome(createLkrIncome(id = "   "))

        assertTrue(result.isFailure)
        assertEquals("Income ID cannot be empty", result.exceptionOrNull()?.message)
        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun rejectsZeroAndNegativeStoredAmountsWithoutChangingState() = runTest {
        val repository = FakeIncomeRepository()
        val validIncome = createLkrIncome()

        listOf(0L, -1L).forEach { invalidAmount ->
            val result = repository.saveIncome(
                validIncome.copy(
                    id = "income-$invalidAmount",
                    money = validIncome.money.copy(baseAmountMinor = invalidAmount)
                )
            )

            assertTrue(result.isFailure)
        }

        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun rejectsIncorrectSourceAndEntryKindCombinations() = runTest {
        val repository = FakeIncomeRepository()
        val invalidIncomes = listOf(
            createLkrIncome(entryKind = IncomeEntryKind.REALIZED_PNL),
            createLkrIncome(
                id = "income-002",
                source = IncomeSource.CRYPTO,
                entryKind = IncomeEntryKind.RECEIPT,
                pnlDirection = PnlDirection.GAIN
            )
        )

        invalidIncomes.forEach { income ->
            assertTrue(repository.saveIncome(income).isFailure)
        }

        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun rejectsCryptoWithoutPnlDirection() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL
        )

        val result = repository.saveIncome(income)

        assertTrue(result.isFailure)
        assertEquals(
            "Select whether the crypto transaction is a gain or loss",
            result.exceptionOrNull()?.message
        )
        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun rejectsInconsistentMoneySnapshot() = runTest {
        val repository = FakeIncomeRepository()
        val validIncome = createUsdIncome()
        val income = validIncome.copy(
            money = validIncome.money.copy(
                baseAmountMinor = validIncome.money.baseAmountMinor + 1L
            )
        )

        val result = repository.saveIncome(income)

        assertTrue(result.isFailure)
        assertEquals(
            "Stored LKR minor-unit amount does not match the calculated amount",
            result.exceptionOrNull()?.message
        )
        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun rejectsInvalidLkrRateDate() = runTest {
        val repository = FakeIncomeRepository()
        val validIncome = createLkrIncome()
        val income = validIncome.copy(
            money = validIncome.money.copy(
                rateDate = validIncome.occurredDate.minusDays(1)
            )
        )

        val result = repository.saveIncome(income)

        assertTrue(result.isFailure)
        assertEquals(
            "LKR rate date must match the transaction date",
            result.exceptionOrNull()?.message
        )
        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun updatesExistingRecordWithoutCreatingDuplicate() = runTest {
        val repository = FakeIncomeRepository()
        val original = createLkrIncome(note = "Original")
        val updated = original.copy(note = "Updated")

        assertTrue(repository.saveIncome(original).isSuccess)
        assertTrue(repository.saveIncome(updated).isSuccess)

        assertEquals(listOf(updated), repository.incomesIn())
    }

    @Test
    fun invalidUpdatePreservesPreviouslyStoredRecord() = runTest {
        val repository = FakeIncomeRepository()
        val original = createLkrIncome(note = "Keep this record")
        repository.saveIncome(original).getOrThrow()
        val invalidUpdate = original.copy(
            note = "Must not be stored",
            money = original.money.copy(baseAmountMinor = 0L)
        )

        val result = repository.saveIncome(invalidUpdate)

        assertTrue(result.isFailure)
        assertEquals(listOf(original), repository.incomesIn())
    }

    @Test
    fun updatePreservesProvidedUsdMoneySnapshotAndMetadata() = runTest {
        val repository = FakeIncomeRepository()
        repository.saveIncome(createLkrIncome()).getOrThrow()
        val updated = createUsdIncome(note = "Paid at historical rate")

        assertTrue(repository.saveIncome(updated).isSuccess)

        val stored = repository.incomesIn().single()
        assertEquals(updated, stored)
        assertEquals(updated.money, stored.money)
    }

    @Test
    fun deletesExistingIncomeAndPreservesOtherRecords() = runTest {
        val repository = FakeIncomeRepository()
        val deleted = createLkrIncome(id = "income-001")
        val retained = createLkrIncome(id = "income-002")
        repository.saveIncome(deleted).getOrThrow()
        repository.saveIncome(retained).getOrThrow()

        val result = repository.deleteIncome(deleted.id)

        assertTrue(result.isSuccess)
        assertEquals(listOf(retained), repository.incomesIn())
    }

    @Test
    fun failedDeletePreservesRepositoryState() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome()
        repository.saveIncome(income).getOrThrow()

        val result = repository.deleteIncome("missing-id")

        assertTrue(result.isFailure)
        assertEquals("Income not found", result.exceptionOrNull()?.message)
        assertEquals(listOf(income), repository.incomesIn())
    }

    @Test
    fun ownerBoundDeleteValidatesOwnerAndUsesExistingDeleteBehavior() = runTest {
        val repository = FakeIncomeRepository()
        val income = createLkrIncome()
        repository.saveIncome(income).getOrThrow()

        val invalidOwner = repository.deleteIncomeForOwner("", income.id)
        val deleted = repository.deleteIncomeForOwner("user-a", income.id)

        assertTrue(invalidOwner.exceptionOrNull() is IllegalArgumentException)
        assertTrue(deleted.isSuccess)
        assertTrue(repository.incomesIn().isEmpty())
    }

    @Test
    fun filtersByInclusiveStartExclusiveEndAndSortsByDateThenId() = runTest {
        val repository = FakeIncomeRepository()
        val startB = createLkrIncome(
            id = "income-b",
            date = startDate
        )
        val startA = createLkrIncome(
            id = "income-a",
            date = startDate
        )
        val inside = createLkrIncome(
            id = "income-c",
            date = endDate.minusDays(1)
        )
        val excluded = createLkrIncome(
            id = "income-outside",
            date = endDate
        )

        listOf(startB, inside, excluded, startA).forEach { income ->
            repository.saveIncome(income).getOrThrow()
        }

        assertEquals(
            listOf("income-a", "income-b", "income-c"),
            repository.incomesIn().map { it.id }
        )
    }

    @Test
    fun updatingDateMovesIncomeBetweenObservedRanges() = runTest {
        val repository = FakeIncomeRepository()
        val octoberIncome = createLkrIncome(date = defaultDate)
        repository.saveIncome(octoberIncome).getOrThrow()
        assertEquals(listOf(octoberIncome), repository.incomesIn())

        val novemberDate = LocalDate.of(2026, 11, 5)
        val novemberIncome = createLkrIncome(
            id = octoberIncome.id,
            date = novemberDate,
            note = "Moved to November"
        )

        repository.saveIncome(novemberIncome).getOrThrow()

        assertTrue(repository.incomesIn().isEmpty())
        assertEquals(
            listOf(novemberIncome),
            repository.incomesIn(
                start = LocalDate.of(2026, 11, 1),
                endExclusive = LocalDate.of(2026, 12, 1)
            )
        )
    }

    @Test
    fun observerReceivesCreateUpdateAndDeleteEmissions() = runTest {
        val repository = FakeIncomeRepository()
        val original = createLkrIncome(note = "Original")
        val updated = original.copy(note = "Updated")
        val emissionsDeferred = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeIncomes(startDate, endDate)
                .take(4)
                .toList()
        }

        repository.saveIncome(original).getOrThrow()
        repository.saveIncome(updated).getOrThrow()
        repository.deleteIncome(original.id).getOrThrow()

        val emissions = emissionsDeferred.await()
        assertEquals(emptyList<Income>(), emissions[0])
        assertEquals(listOf(original), emissions[1])
        assertEquals(listOf(updated), emissions[2])
        assertEquals(emptyList<Income>(), emissions[3])
    }
}
