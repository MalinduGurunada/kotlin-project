package com.example.personalfinanceapp.feature.income.ui

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(
        Instant.parse("2026-10-24T12:00:00Z"),
        ZoneOffset.UTC
    )

    private lateinit var repository: SavingIncomeRepository
    private lateinit var authRepository: FormAuthRepository
    private lateinit var viewModel: IncomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = SavingIncomeRepository()
        authRepository = FormAuthRepository(USER_A)
        viewModel = IncomeViewModel(repository, authRepository, clock)
        dispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening new income creates an LKR salary draft for today`() = runTest {
        viewModel.openNewIncome()

        val draft = requireNotNull(viewModel.formState.value)
        assertEquals(IncomeEditorMode.ADD, draft.mode)
        assertEquals(IncomeSource.SALARY, draft.selectedSource)
        assertEquals(TODAY, draft.transactionDate)
        assertTrue(draft.draftId.isNotBlank())
        assertEquals(USER_A, draft.ownerId)
        assertNull(draft.originalIncome)
    }

    @Test
    fun `new draft generates its stable ID once across edits`() = runTest {
        val draft = openNewDraft()

        viewModel.updateIncomeAmount("100.00")
        viewModel.updateIncomeNote("October salary")

        assertEquals(draft.draftId, viewModel.formState.value?.draftId)
    }

    @Test
    fun `valid salary creates a receipt with optional note`() = runTest {
        openNewDraft()
        viewModel.updateIncomeAmount("125000.50")
        viewModel.updateIncomeNote("  October salary  ")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(listOf(USER_A), repository.savedOwnerIds)
        assertEquals(IncomeSource.SALARY, saved.source)
        assertEquals(IncomeEntryKind.RECEIPT, saved.entryKind)
        assertEquals("October salary", saved.note)
        assertNull(saved.pnlDirection)
    }

    @Test
    fun `valid freelance creates a receipt with project reference`() = runTest {
        openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.FREELANCE)
        viewModel.updateIncomeAmount("4500")
        viewModel.updateProjectReference("  mobile-redesign  ")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(IncomeEntryKind.RECEIPT, saved.entryKind)
        assertEquals("mobile-redesign", saved.projectReference)
    }

    @Test
    fun `valid AdSense creates an LKR receipt`() = runTest {
        openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.ADSENSE)
        viewModel.updateIncomeAmount("99.99")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(IncomeSource.ADSENSE, saved.source)
        assertEquals(IncomeEntryKind.RECEIPT, saved.entryKind)
        assertEquals(CurrencyCode.LKR, saved.money.originalCurrency)
    }

    @Test
    fun `crypto gain uses realized PNL entry kind and asset`() = runTest {
        openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.CRYPTO)
        viewModel.updatePnlDirection(PnlDirection.GAIN)
        viewModel.updateAssetSymbol("  BTC  ")
        viewModel.updateIncomeAmount("250.25")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(IncomeEntryKind.REALIZED_PNL, saved.entryKind)
        assertEquals(PnlDirection.GAIN, saved.pnlDirection)
        assertEquals("BTC", saved.assetSymbol)
    }

    @Test
    fun `crypto loss stores a positive magnitude and loss direction`() = runTest {
        openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.CRYPTO)
        viewModel.updatePnlDirection(PnlDirection.LOSS)
        viewModel.updateIncomeAmount("25")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(PnlDirection.LOSS, saved.pnlDirection)
        assertTrue(saved.money.baseAmountMinor > 0L)
    }

    @Test
    fun `invalid amounts expose converter errors and are not saved`() = runTest {
        val invalidAmounts = listOf("", "abc", "0", "-1", "1.999")

        invalidAmounts.forEach { amount ->
            openNewDraft()
            viewModel.updateIncomeAmount(amount)
            viewModel.saveIncome()
            assertNotNull("Expected an error for '$amount'", viewModel.formState.value?.errors?.amount)
        }

        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `crypto without gain or loss exposes required metadata error`() = runTest {
        openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.CRYPTO)
        viewModel.updateIncomeAmount("100")

        viewModel.saveIncome()

        assertEquals("Select gain or loss", viewModel.formState.value?.errors?.pnlDirection)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `LKR snapshot uses fixed rate exact minor units and transaction date`() = runTest {
        openNewDraft()
        viewModel.updateIncomeAmount("1234.56")
        viewModel.updateIncomeDate(LocalDate.of(2026, 9, 30))

        saveAndRun()

        val money = repository.saved.single().money
        assertEquals("1234.56", money.originalAmountDecimal)
        assertEquals(CurrencyCode.LKR, money.originalCurrency)
        assertEquals("1", money.rateLkrPerUnitDecimal)
        assertEquals(123_456L, money.baseAmountMinor)
        assertEquals(RateSource.LKR_FIXED, money.rateSource)
        assertEquals(LocalDate.of(2026, 9, 30), money.rateDate)
    }

    @Test
    fun `existing LKR income opens with original values`() = runTest {
        val existing = income(id = "existing", source = IncomeSource.FREELANCE).copy(
            projectReference = "project-7",
            receivableId = "receivable-7",
            note = "Original note"
        )

        viewModel.openIncomeForEdit(existing)

        val draft = requireNotNull(viewModel.formState.value)
        assertEquals(IncomeEditorMode.EDIT, draft.mode)
        assertEquals(existing, draft.originalIncome)
        assertEquals("existing", draft.draftId)
        assertEquals("project-7", draft.projectReference)
    }

    @Test
    fun `editing preserves ID and untouched linked receivable`() = runTest {
        val existing = income(id = "existing", source = IncomeSource.FREELANCE).copy(
            projectReference = "old-project",
            receivableId = "receivable-7"
        )
        viewModel.openIncomeForEdit(existing)
        viewModel.updateIncomeAmount("800")
        viewModel.updateProjectReference("new-project")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals("existing", saved.id)
        assertEquals("receivable-7", saved.receivableId)
        assertEquals("new-project", saved.projectReference)
    }

    @Test
    fun `changing source removes stale source-specific metadata`() = runTest {
        viewModel.openIncomeForEdit(
            income(id = "crypto", source = IncomeSource.CRYPTO).copy(
                pnlDirection = PnlDirection.GAIN,
                assetSymbol = "ETH"
            )
        )
        viewModel.updateIncomeSource(IncomeSource.SALARY)
        viewModel.updateIncomeAmount("100")

        saveAndRun()

        val saved = repository.saved.single()
        assertEquals(IncomeSource.SALARY, saved.source)
        assertEquals(IncomeEntryKind.RECEIPT, saved.entryKind)
        assertNull(saved.pnlDirection)
        assertNull(saved.assetSymbol)
    }

    @Test
    fun `failed save retains every entered value and exposes safe message`() = runTest {
        repository.saveBlock = { Result.failure(IllegalStateException("private Firebase path")) }
        val draft = openNewDraft()
        viewModel.updateIncomeSource(IncomeSource.FREELANCE)
        viewModel.updateIncomeAmount("500.25")
        viewModel.updateProjectReference("project-x")
        viewModel.updateIncomeNote("keep me")

        saveAndRun()

        val retained = requireNotNull(viewModel.formState.value)
        assertEquals(draft.draftId, retained.draftId)
        assertEquals("500.25", retained.amountText)
        assertEquals("project-x", retained.projectReference)
        assertEquals("keep me", retained.note)
        assertFalse(retained.isSaving)
        assertEquals(
            "Unable to save income. Check your connection and try again.",
            retained.saveError
        )
        assertFalse(retained.saveError.orEmpty().contains("Firebase"))
    }

    @Test
    fun `retry after failure reuses the same stable income ID`() = runTest {
        var call = 0
        repository.saveBlock = {
            call += 1
            if (call == 1) Result.failure(IllegalStateException("offline")) else Result.success(Unit)
        }
        val draft = openNewDraft()
        viewModel.updateIncomeAmount("40")

        saveAndRun()
        saveAndRun()

        assertEquals(2, repository.saved.size)
        assertEquals(draft.draftId, repository.saved[0].id)
        assertEquals(repository.saved[0].id, repository.saved[1].id)
    }

    @Test
    fun `duplicate save submission is ignored while persistence is active`() = runTest {
        val gate = CompletableDeferred<Result<Unit>>()
        repository.saveBlock = { gate.await() }
        openNewDraft()
        viewModel.updateIncomeAmount("75")

        viewModel.saveIncome()
        viewModel.saveIncome()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.saved.size)
        assertTrue(viewModel.formState.value?.isSaving == true)

        gate.complete(Result.success(Unit))
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.formState.value)
    }

    @Test
    fun `successful save closes editor and publishes one success message`() = runTest {
        openNewDraft()
        viewModel.updateIncomeAmount("75")

        saveAndRun()

        assertNull(viewModel.formState.value)
        assertEquals("Income saved.", viewModel.userMessage.value)
        viewModel.clearUserMessage()
        assertNull(viewModel.userMessage.value)
    }

    @Test
    fun `sign out clears the active draft and prevents later save`() = runTest {
        openNewDraft()
        viewModel.updateIncomeAmount("90")

        authRepository.setUser(null)
        dispatcher.scheduler.runCurrent()
        viewModel.saveIncome()

        assertNull(viewModel.formState.value)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `direct account switch clears user A draft before user B can save`() = runTest {
        val firstDraft = openNewDraft()
        authRepository.setUser(USER_B)
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.formState.value)
        viewModel.openNewIncome()
        val secondDraft = requireNotNull(viewModel.formState.value)
        assertNotEquals(firstDraft.draftId, secondDraft.draftId)
    }

    @Test
    fun `authentication change during save suppresses stale success completion`() = runTest {
        val gate = CompletableDeferred<Result<Unit>>()
        repository.saveBlock = { gate.await() }
        openNewDraft()
        viewModel.updateIncomeAmount("200")
        viewModel.saveIncome()
        dispatcher.scheduler.runCurrent()

        authRepository.setUser(USER_B)
        dispatcher.scheduler.runCurrent()
        gate.complete(Result.success(Unit))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.formState.value)
        assertEquals(
            "The income editor was closed because the account changed.",
            viewModel.userMessage.value
        )
        assertNotEquals("Income saved.", viewModel.userMessage.value)
    }

    @Test
    fun `existing USD record cannot open editor or be silently repriced`() = runTest {
        val existing = usdIncome("usd-income")

        viewModel.openIncomeForEdit(existing)

        assertNull(viewModel.formState.value)
        assertTrue(viewModel.userMessage.value.orEmpty().contains("USD"))
        assertTrue(repository.saved.isEmpty())
        assertEquals(CurrencyCode.USD, existing.money.originalCurrency)
        assertEquals("305.25", existing.money.rateLkrPerUnitDecimal)
    }

    private fun openNewDraft(): IncomeFormUiState {
        viewModel.openNewIncome()
        return requireNotNull(viewModel.formState.value)
    }

    private fun saveAndRun() {
        viewModel.saveIncome()
        dispatcher.scheduler.runCurrent()
    }

    private fun income(
        id: String,
        source: IncomeSource = IncomeSource.SALARY,
        date: LocalDate = TODAY
    ): Income = Income(
        id = id,
        source = source,
        entryKind = if (source == IncomeSource.CRYPTO) {
            IncomeEntryKind.REALIZED_PNL
        } else {
            IncomeEntryKind.RECEIPT
        },
        money = MoneyConverter.fromLkr("500", date).getOrThrow(),
        occurredDate = date,
        pnlDirection = if (source == IncomeSource.CRYPTO) PnlDirection.GAIN else null
    )

    private fun usdIncome(id: String): Income = Income(
        id = id,
        source = IncomeSource.ADSENSE,
        entryKind = IncomeEntryKind.RECEIPT,
        money = MoneyConverter.fromUsd(
            amountInput = "100",
            rateLkrPerUsdInput = "305.25",
            rateSource = RateSource.MANUAL_REFERENCE,
            rateDate = TODAY.minusDays(1)
        ).getOrThrow(),
        occurredDate = TODAY
    )

    private class FormAuthRepository(initialUserId: UserId?) : AuthRepository {
        private val userId = MutableStateFlow(initialUserId)

        override fun observeAuthState(): Flow<UserId?> = userId.asStateFlow()

        fun setUser(value: UserId?) {
            userId.value = value
        }

        override suspend fun login(email: String, password: String): Result<UserId> =
            Result.failure(UnsupportedOperationException("Not used"))

        override suspend fun register(email: String, password: String): Result<UserId> =
            Result.failure(UnsupportedOperationException("Not used"))

        override suspend fun sendPasswordReset(email: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used"))

        override suspend fun signOut(): Result<Unit> {
            userId.value = null
            return Result.success(Unit)
        }
    }

    private class SavingIncomeRepository : IncomeRepository {
        private val incomes = MutableStateFlow<List<Income>>(emptyList())
        val saved = mutableListOf<Income>()
        val savedOwnerIds = mutableListOf<UserId>()
        var saveBlock: suspend (Income) -> Result<Unit> = { Result.success(Unit) }

        override fun observeIncomes(
            startDate: LocalDate,
            endDateExclusive: LocalDate
        ): Flow<List<Income>> = incomes.map { records ->
            records.filter { record ->
                !record.occurredDate.isBefore(startDate) &&
                    record.occurredDate.isBefore(endDateExclusive)
            }
        }

        override suspend fun saveIncome(income: Income): Result<Unit> =
            persist(income)

        override suspend fun saveIncomeForOwner(
            expectedOwnerId: UserId,
            income: Income
        ): Result<Unit> {
            savedOwnerIds += expectedOwnerId
            return persist(income)
        }

        private suspend fun persist(income: Income): Result<Unit> {
            saved += income
            val result = saveBlock(income)
            if (result.isSuccess) {
                incomes.value = incomes.value.filterNot { it.id == income.id } + income
            }
            return result
        }

        override suspend fun deleteIncome(id: DocumentId): Result<Unit> =
            Result.failure(UnsupportedOperationException("Not used"))
    }

    private companion object {
        const val USER_A = "user-a"
        const val USER_B = "user-b"
        val TODAY: LocalDate = LocalDate.of(2026, 10, 24)
    }
}
