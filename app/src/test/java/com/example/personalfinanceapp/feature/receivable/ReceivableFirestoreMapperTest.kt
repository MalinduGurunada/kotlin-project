package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceivableFirestoreMapperTest {
    private val receivable = Receivable(
        "r-1", "  Website  ", "  WEB-1  ", "500.25", CurrencyCode.USD,
        LocalDate.of(2026, 10, 30)
    )

    @Test
    fun `round trip uses exact versioned schema and stable id`() {
        val fields = ReceivableFirestoreMapper.toFirestoreMap(receivable)
        assertEquals(8, fields.size)
        assertEquals(1L, fields["schemaVersion"])
        val restored = ReceivableFirestoreMapper.fromFirestoreMap("r-1", fields)
        assertEquals("Website", restored.title)
        assertEquals("WEB-1", restored.projectReference)
        assertEquals("500.25", restored.expectedOriginalDecimal)
    }

    @Test
    fun `unknown fields id mismatch bad dates and unsafe ids are rejected`() {
        val fields = ReceivableFirestoreMapper.toFirestoreMap(receivable)
        assertTrue(runCatching { ReceivableFirestoreMapper.fromFirestoreMap("other", fields) }.isFailure)
        assertTrue(runCatching { ReceivableFirestoreMapper.fromFirestoreMap("r-1", fields + ("extra" to true)) }.isFailure)
        assertTrue(runCatching { ReceivableFirestoreMapper.fromFirestoreMap("r-1", fields + ("expectedDate" to "2026-02-30")) }.isFailure)
        assertTrue(runCatching { ReceivableFirestoreMapper.toFirestoreMap(receivable.copy(id = "bad/id")) }.isFailure)
    }
}
