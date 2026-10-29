package com.example.personalfinanceapp.feature.receivable

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import java.time.LocalDate

class ReceivableFirestoreMappingException(
    documentId: String,
    detail: String,
    cause: Throwable? = null
) : IllegalArgumentException("Receivable document '$documentId' is invalid: $detail", cause)

object ReceivableFirestoreMapper {
    private const val SCHEMA_VERSION = 1L
    private val fields = setOf(
        "schemaVersion", "id", "title", "projectReference",
        "expectedOriginalDecimal", "currency", "expectedDate", "archived"
    )

    fun toFirestoreMap(receivable: Receivable): Map<String, Any> {
        val normalized = ReceivableValidator.normalize(receivable).getOrThrow()
        validateReceivableDocumentId(normalized.id)
        return linkedMapOf(
            "schemaVersion" to SCHEMA_VERSION,
            "id" to normalized.id,
            "title" to normalized.title,
            "projectReference" to normalized.projectReference,
            "expectedOriginalDecimal" to normalized.expectedOriginalDecimal,
            "currency" to normalized.currency.name,
            "expectedDate" to normalized.expectedDate.toString(),
            "archived" to normalized.archived
        )
    }

    fun fromFirestoreMap(documentId: String, data: Map<String, Any?>): Receivable {
        fun invalid(detail: String, cause: Throwable? = null): Nothing =
            throw ReceivableFirestoreMappingException(documentId, detail, cause)

        if (data.keys != fields) invalid("field set must exactly match schema version 1")
        if (data["schemaVersion"] != SCHEMA_VERSION) invalid("unsupported schema version")
        val id = data["id"] as? String ?: invalid("id must be a string")
        if (id != documentId) invalid("stored id must match the document ID")
        try {
            validateReceivableDocumentId(id)
        } catch (failure: IllegalArgumentException) {
            invalid(failure.message ?: "invalid document ID", failure)
        }
        val title = data["title"] as? String ?: invalid("title must be a string")
        val reference = data["projectReference"] as? String
            ?: invalid("projectReference must be a string")
        val amount = data["expectedOriginalDecimal"] as? String
            ?: invalid("expectedOriginalDecimal must be a string")
        val currency = (data["currency"] as? String)?.let {
            runCatching { CurrencyCode.valueOf(it) }.getOrNull()
        } ?: invalid("currency is unsupported")
        val dateText = data["expectedDate"] as? String ?: invalid("expectedDate must be a string")
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(dateText)) invalid("expectedDate must be ISO-8601")
        val date = runCatching { LocalDate.parse(dateText) }.getOrElse {
            invalid("expectedDate is not a calendar date", it)
        }
        val archived = data["archived"] as? Boolean ?: invalid("archived must be a boolean")
        return ReceivableValidator.normalize(
            Receivable(id, title, reference, amount, currency, date, archived)
        ).getOrElse { invalid(it.message ?: "domain validation failed", it) }
    }
}
