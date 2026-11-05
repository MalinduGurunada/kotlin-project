package com.example.personalfinanceapp.feature.receivable.ui

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.core.money.MoneyConverter
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeObservationSnapshot
import com.example.personalfinanceapp.feature.income.IncomeRepository
import com.example.personalfinanceapp.feature.receivable.ReceivableObservationSnapshot
import com.example.personalfinanceapp.feature.receivable.ReceivableRepository
import com.example.personalfinanceapp.feature.receivable.ReceivableStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivableViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 10, 29)
    private lateinit var auth: MutableAuthRepository
    private lateinit var receivables: MutableReceivableRepository
    private lateinit var incomes: MutableIncomeRepository
    private lateinit var viewModel: ReceivableViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        auth = MutableAuthRepository(USER_A)
        receivables = MutableReceivableRepository()
        incomes = MutableIncomeRepository()
        viewModel = ReceivableViewModel(
            receivableRepository = receivables,
            incomeRepository = incomes,
            authRepository = auth,
            clock = Clock.fixed(Instant.parse("2026-10-29T06:00:00Z"), ZoneOffset.UTC)
        )
        dispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `partial payment edits and deletes recalculate the settlement`() = runTest {
        receivables.emit(USER_A, listOf(receivable()))
        incomes.emit(USER_A, listOf(payment("one", "25.25"), payment("two", "24.75")))
        dispatcher.scheduler.runCurrent()

        var settlement = viewModel.uiState.value.settlements.single()
        assertEquals(ReceivableStatus.PARTIALLY_PAID, settlement.status)
        assertEquals("50.00", settlement.paidDecimal)
        assertEquals("50.00", settlement.remainingDecimal)

        incomes.emit(USER_A, listOf(payment("one", "25.25"), payment("two", "74.75")))
        dispatcher.scheduler.runCurrent()
        settlement = viewModel.uiState.value.settlements.single()
        assertEquals(ReceivableStatus.SETTLED, settlement.status)
        assertEquals("100.00", settlement.paidDecimal)

        incomes.emit(USER_A, listOf(payment("one", "25.25")))
        dispatcher.scheduler.runCurrent()
        settlement = viewModel.uiState.value.settlements.single()
        assertEquals("25.25", settlement.paidDecimal)
        assertEquals("74.75", settlement.remainingDecimal)
    }

    @Test
    fun `account switch clears user A form and isolates identical receivable IDs`() = runTest {
        receivables.emit(USER_A, listOf(receivable(title = "User A")))
        incomes.emit(USER_A, listOf(payment("a", "10.00")))
        dispatcher.scheduler.runCurrent()
        viewModel.openCreate()
        assertEquals(USER_A, viewModel.formState.value?.ownerId)

        auth.emit(USER_B)
        receivables.emit(USER_B, listOf(receivable(title = "User B")))
        incomes.emit(USER_B, listOf(payment("b", "75.00")))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.formState.value)
        assertEquals(USER_B, viewModel.uiState.value.ownerId)
        val settlement = viewModel.uiState.value.settlements.single()
        assertEquals("User B", settlement.receivable.title)
        assertEquals("75.00", settlement.paidDecimal)
        assertTrue(receivables.saveCalls.isEmpty())
    }

    private fun receivable(title: String = "Project") = Receivable(
        id = RECEIVABLE_ID,
        title = title,
        projectReference = "P-1",
        expectedOriginalDecimal = "100.00",
        currency = CurrencyCode.LKR,
        expectedDate = date
    )

    private fun payment(id: String, amount: String) = Income(
        id = id,
        source = IncomeSource.FREELANCE,
        entryKind = IncomeEntryKind.RECEIPT,
        money = MoneyConverter.fromLkr(amount, date).getOrThrow(),
        occurredDate = date,
        receivableId = RECEIVABLE_ID
    )

    private class MutableAuthRepository(initialOwner: UserId?) : AuthRepository {
        private val owner = MutableStateFlow(initialOwner)
        override fun observeAuthState(): Flow<UserId?> = owner.asStateFlow()
        fun emit(value: UserId?) { owner.value = value }
        override suspend fun login(email: String, password: String) = Result.failure<UserId>(UnsupportedOperationException())
        override suspend fun register(email: String, password: String) = Result.failure<UserId>(UnsupportedOperationException())
        override suspend fun sendPasswordReset(email: String) = Result.failure<Unit>(UnsupportedOperationException())
        override suspend fun signOut() = Result.success(Unit)
    }

    private class MutableReceivableRepository : ReceivableRepository {
        private val snapshots = MutableStateFlow(ReceivableObservationSnapshot.signedOut())
        val saveCalls = mutableListOf<Pair<UserId, Receivable>>()
        override fun observeReceivableSnapshots(): Flow<ReceivableObservationSnapshot> = snapshots.asStateFlow()
        fun emit(owner: UserId, values: List<Receivable>) {
            snapshots.value = ReceivableObservationSnapshot.forOwner(owner, values)
        }
        override suspend fun saveReceivable(receivable: Receivable) = Result.success(Unit)
        override suspend fun saveReceivableForOwner(expectedOwnerId: UserId, receivable: Receivable): Result<Unit> {
            saveCalls += expectedOwnerId to receivable
            return Result.success(Unit)
        }
        override suspend fun setReceivableArchived(id: DocumentId, archived: Boolean) = Result.success(Unit)
        override suspend fun setReceivableArchivedForOwner(
            expectedOwnerId: UserId,
            id: DocumentId,
            archived: Boolean
        ) = Result.success(Unit)
    }

    private class MutableIncomeRepository : IncomeRepository {
        private val snapshots = MutableStateFlow(IncomeObservationSnapshot.signedOut())
        override fun observeIncomeSnapshots(
            startDate: LocalDate,
            endDateExclusive: LocalDate
        ): Flow<IncomeObservationSnapshot> = snapshots.asStateFlow()
        fun emit(owner: UserId, values: List<Income>) {
            snapshots.value = IncomeObservationSnapshot.forOwner(owner, values)
        }
        override suspend fun saveIncome(income: Income) = Result.success(Unit)
        override suspend fun saveIncomeForOwner(expectedOwnerId: UserId, income: Income) = Result.success(Unit)
        override suspend fun deleteIncome(id: DocumentId) = Result.success(Unit)
        override suspend fun deleteIncomeForOwner(expectedOwnerId: UserId, id: DocumentId) = Result.success(Unit)
    }

    private companion object {
        const val USER_A = "user-a"
        const val USER_B = "user-b"
        const val RECEIVABLE_ID = "same-id"
    }
}
