
package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeReceivableRepositoryTest {

    private fun createReceivable(
        id: String = "receivable-001"
    ): Receivable {
        return Receivable(
            id = id,
            title = "Website Development",
            projectReference = "Website Project",
            expectedOriginalDecimal = "50000.00",
            currency = CurrencyCode.LKR,
            expectedDate = LocalDate.of(2026, 10, 20)
        )
    }

    @Test
    fun initialReceivableListIsEmpty() = runTest {
        val repository = FakeReceivableRepository()

        val receivables = repository.observeReceivables().first()

        assertTrue(receivables.isEmpty())
    }

    @Test
    fun saveReceivableSuccessfully() = runTest {
        val repository = FakeReceivableRepository()

        val result = repository.saveReceivable(
            createReceivable()
        )

        assertTrue(result.isSuccess)

        val receivables = repository.observeReceivables().first()

        assertEquals(1, receivables.size)
        assertEquals("Website Development", receivables.first().title)
    }

    @Test
    fun updateReceivableWithoutDuplicate() = runTest {
        val repository = FakeReceivableRepository()

        val receivable = createReceivable()

        repository.saveReceivable(receivable)

        val updated = receivable.copy(
            title = "Updated Website Project",
            expectedOriginalDecimal = "60000.00"
        )

        val result = repository.saveReceivable(updated)

        assertTrue(result.isSuccess)

        val receivables = repository.observeReceivables().first()

        assertEquals(1, receivables.size)
        assertEquals("Updated Website Project", receivables.first().title)
        assertEquals("60000.00", receivables.first().expectedOriginalDecimal)
    }

    @Test
    fun rejectBlankReceivableId() = runTest {
        val repository = FakeReceivableRepository()

        val result = repository.saveReceivable(
            createReceivable(id = "")
        )

        assertTrue(result.isFailure)
        assertTrue(repository.observeReceivables().first().isEmpty())
    }

    @Test
    fun rejectBlankTitle() = runTest {
        val repository = FakeReceivableRepository()

        val receivable = createReceivable().copy(
            title = "   "
        )

        val result = repository.saveReceivable(receivable)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectBlankProjectReference() = runTest {
        val repository = FakeReceivableRepository()

        val receivable = createReceivable().copy(
            projectReference = "   "
        )

        val result = repository.saveReceivable(receivable)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectInvalidAmount() = runTest {
        val repository = FakeReceivableRepository()

        val receivable = createReceivable().copy(
            expectedOriginalDecimal = "abc"
        )

        val result = repository.saveReceivable(receivable)

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectZeroAndNegativeAmounts() = runTest {
        val repository = FakeReceivableRepository()

        val zeroAmount = createReceivable().copy(
            expectedOriginalDecimal = "0"
        )

        val negativeAmount = createReceivable().copy(
            expectedOriginalDecimal = "-100"
        )

        assertTrue(repository.saveReceivable(zeroAmount).isFailure)
        assertTrue(repository.saveReceivable(negativeAmount).isFailure)

        assertTrue(repository.observeReceivables().first().isEmpty())
    }

    @Test
    fun archiveReceivableSuccessfully() = runTest {
        val repository = FakeReceivableRepository()

        repository.saveReceivable(createReceivable())

        val result = repository.setReceivableArchived(
            id = "receivable-001",
            archived = true
        )

        assertTrue(result.isSuccess)

        val receivable = repository.observeReceivables().first().first()

        assertTrue(receivable.archived)
    }

    @Test
    fun restoreArchivedReceivable() = runTest {
        val repository = FakeReceivableRepository()

        repository.saveReceivable(createReceivable())

        repository.setReceivableArchived(
            id = "receivable-001",
            archived = true
        )

        val result = repository.setReceivableArchived(
            id = "receivable-001",
            archived = false
        )

        assertTrue(result.isSuccess)

        val receivable = repository.observeReceivables().first().first()

        assertFalse(receivable.archived)
    }

    @Test
    fun rejectArchivingMissingReceivable() = runTest {
        val repository = FakeReceivableRepository()

        val result = repository.setReceivableArchived(
            id = "missing-id",
            archived = true
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun trimReceivableTextFields() = runTest {
        val repository = FakeReceivableRepository()

        val receivable = createReceivable().copy(
            title = "  Website Development  ",
            projectReference = "  Website Project  ",
            expectedOriginalDecimal = " 50000.00 "
        )

        repository.saveReceivable(receivable)

        val saved = repository.observeReceivables().first().first()

        assertEquals("Website Development", saved.title)
        assertEquals("Website Project", saved.projectReference)
        assertEquals("50000.00", saved.expectedOriginalDecimal)
    }
}
