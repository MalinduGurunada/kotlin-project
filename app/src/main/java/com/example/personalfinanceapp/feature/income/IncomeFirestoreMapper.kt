package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException

class IncomeFirestoreMappingException(
    val documentId: String,
    val fieldPath: String,
    detail: String,
    cause: Throwable? = null
) : IllegalArgumentException(
    "Income document '$documentId' has invalid field '$fieldPath': $detail",
    cause
)

object IncomeFirestoreMapper {

    private const val SCHEMA_VERSION = 1L
    private const val MAX_DOCUMENT_ID_BYTES = 1_500

    private val reservedDocumentIdPattern = Regex("__.*__")
    private val isoDatePattern = Regex("""\d{4}-\d{2}-\d{2}""")

    private val requiredIncomeFields = setOf(
        "schemaVersion",
        "id",
        "source",
        "entryKind",
        "money",
        "occurredDate"
    )

    private val optionalIncomeFields = setOf(
        "pnlDirection",
        "projectReference",
        "receivableId",
        "assetSymbol",
        "note"
    )

    private val requiredMoneyFields = setOf(
        "originalAmountDecimal",
        "originalCurrency",
        "rateLkrPerUnitDecimal",
        "baseAmountMinor",
        "rateSource",
        "rateDate"
    )

    fun toFirestoreMap(income: Income): Map<String, Any> {
        IncomeValidator.validate(income).getOrThrow()
        validateFirestoreDocumentId(income.id, income.id, "id")

        val money = linkedMapOf<String, Any>(
            "originalAmountDecimal" to income.money.originalAmountDecimal,
            "originalCurrency" to income.money.originalCurrency.name,
            "rateLkrPerUnitDecimal" to income.money.rateLkrPerUnitDecimal,
            "baseAmountMinor" to income.money.baseAmountMinor,
            "rateSource" to income.money.rateSource.name,
            "rateDate" to income.money.rateDate.toString()
        )

        return linkedMapOf<String, Any>(
            "schemaVersion" to SCHEMA_VERSION,
            "id" to income.id,
            "source" to income.source.name,
            "entryKind" to income.entryKind.name,
            "money" to money,
            "occurredDate" to income.occurredDate.toString()
        ).apply {
            income.pnlDirection?.let { put("pnlDirection", it.name) }
            income.projectReference?.let { put("projectReference", it) }
            income.receivableId?.let { put("receivableId", it) }
            income.assetSymbol?.let { put("assetSymbol", it) }
            income.note?.let { put("note", it) }
        }
    }

    fun fromFirestoreMap(
        documentId: String,
        fields: Map<String, Any?>
    ): Income {
        validateFirestoreDocumentId(documentId, documentId)
        requireExactFields(
            documentId = documentId,
            fieldPath = "document",
            fields = fields,
            requiredFields = requiredIncomeFields,
            optionalFields = optionalIncomeFields
        )

        val schemaVersion = requireLong(documentId, fields, "schemaVersion")
        if (schemaVersion != SCHEMA_VERSION) {
            mappingFailure(
                documentId,
                "schemaVersion",
                "unsupported schema version $schemaVersion"
            )
        }

        val storedId = requireString(documentId, fields, "id")
        validateFirestoreDocumentId(documentId, storedId, "id")
        if (storedId != documentId) {
            mappingFailure(documentId, "id", "must match the Firestore document ID")
        }

        val source = requireEnum<IncomeSource>(
            documentId,
            fields,
            "source"
        )
        val entryKind = requireEnum<IncomeEntryKind>(
            documentId,
            fields,
            "entryKind"
        )
        val moneyFields = requireMap(documentId, fields, "money")
        requireExactFields(
            documentId = documentId,
            fieldPath = "money",
            fields = moneyFields,
            requiredFields = requiredMoneyFields,
            optionalFields = emptySet()
        )

        val money = Money(
            originalAmountDecimal = requireString(
                documentId,
                moneyFields,
                "originalAmountDecimal",
                "money.originalAmountDecimal"
            ),
            originalCurrency = requireEnum<CurrencyCode>(
                documentId,
                moneyFields,
                "originalCurrency",
                "money.originalCurrency"
            ),
            rateLkrPerUnitDecimal = requireString(
                documentId,
                moneyFields,
                "rateLkrPerUnitDecimal",
                "money.rateLkrPerUnitDecimal"
            ),
            baseAmountMinor = requireLong(
                documentId,
                moneyFields,
                "baseAmountMinor",
                "money.baseAmountMinor"
            ),
            rateSource = requireEnum<RateSource>(
                documentId,
                moneyFields,
                "rateSource",
                "money.rateSource"
            ),
            rateDate = requireDate(
                documentId,
                moneyFields,
                "rateDate",
                "money.rateDate"
            )
        )

        val income = Income(
            id = storedId,
            source = source,
            entryKind = entryKind,
            money = money,
            occurredDate = requireDate(documentId, fields, "occurredDate"),
            pnlDirection = optionalEnum<PnlDirection>(documentId, fields, "pnlDirection"),
            projectReference = optionalString(documentId, fields, "projectReference"),
            receivableId = optionalString(documentId, fields, "receivableId"),
            assetSymbol = optionalString(documentId, fields, "assetSymbol"),
            note = optionalString(documentId, fields, "note")
        )

        IncomeValidator.validate(income).exceptionOrNull()?.let { cause ->
            throw IncomeFirestoreMappingException(
                documentId = documentId,
                fieldPath = validationFieldPath(cause.message),
                detail = cause.message ?: "income violates the domain contract",
                cause = cause
            )
        }

        return income
    }

    private fun requireFirestoreDocumentId(documentId: String) {
        require(documentId != "." && documentId != "..") {
            "Income ID is not a valid Firestore document ID"
        }
        require('/' !in documentId) {
            "Income ID cannot contain Firestore path separators"
        }
        require(!reservedDocumentIdPattern.matches(documentId)) {
            "Income ID uses a reserved Firestore document ID format"
        }
        val encodedByteCount = try {
            StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(documentId))
                .remaining()
        } catch (exception: CharacterCodingException) {
            throw IllegalArgumentException(
                "Income ID must contain valid UTF-8 characters",
                exception
            )
        }
        require(encodedByteCount <= MAX_DOCUMENT_ID_BYTES) {
            "Income ID exceeds the Firestore document ID size limit"
        }
    }

    private fun validateFirestoreDocumentId(
        actualDocumentId: String,
        documentIdToValidate: String,
        fieldPath: String = "documentId"
    ) {
        try {
            require(documentIdToValidate.isNotBlank()) {
                "Income ID cannot be empty"
            }
            requireFirestoreDocumentId(documentIdToValidate)
        } catch (exception: IllegalArgumentException) {
            throw IncomeFirestoreMappingException(
                documentId = actualDocumentId,
                fieldPath = fieldPath,
                detail = exception.message ?: "invalid Firestore document ID",
                cause = exception
            )
        }
    }

    private fun requireExactFields(
        documentId: String,
        fieldPath: String,
        fields: Map<*, *>,
        requiredFields: Set<String>,
        optionalFields: Set<String>
    ) {
        val nonStringKey = fields.keys.firstOrNull { it !is String }
        if (nonStringKey != null) {
            mappingFailure(documentId, fieldPath, "contains a non-string field name")
        }

        val actualFields = fields.keys.filterIsInstance<String>().toSet()
        val missingFields = requiredFields - actualFields
        if (missingFields.isNotEmpty()) {
            val missing = missingFields.sorted().first()
            mappingFailure(
                documentId,
                qualify(fieldPath, missing),
                "required field is missing"
            )
        }

        val unexpectedFields = actualFields - requiredFields - optionalFields
        if (unexpectedFields.isNotEmpty()) {
            val unexpected = unexpectedFields.sorted().first()
            mappingFailure(
                documentId,
                qualify(fieldPath, unexpected),
                "field is not supported by schema version $SCHEMA_VERSION"
            )
        }
    }

    private fun requireString(
        documentId: String,
        fields: Map<*, *>,
        key: String,
        fieldPath: String = key
    ): String {
        val value = fields[key]
        if (value !is String) {
            mappingFailure(documentId, fieldPath, "must be a string")
        }
        return value
    }

    private fun optionalString(
        documentId: String,
        fields: Map<*, *>,
        key: String
    ): String? {
        if (!fields.containsKey(key)) {
            return null
        }
        return requireString(documentId, fields, key)
    }

    private fun requireLong(
        documentId: String,
        fields: Map<*, *>,
        key: String,
        fieldPath: String = key
    ): Long {
        val value = fields[key]
        if (value !is Long) {
            mappingFailure(documentId, fieldPath, "must be a 64-bit Firestore integer")
        }
        return value
    }

    private fun requireMap(
        documentId: String,
        fields: Map<*, *>,
        key: String
    ): Map<*, *> {
        val value = fields[key]
        if (value !is Map<*, *>) {
            mappingFailure(documentId, key, "must be a map")
        }
        return value
    }

    private fun requireDate(
        documentId: String,
        fields: Map<*, *>,
        key: String,
        fieldPath: String = key
    ): LocalDate {
        val value = requireString(documentId, fields, key, fieldPath)
        if (!isoDatePattern.matches(value)) {
            mappingFailure(documentId, fieldPath, "must be a valid ISO YYYY-MM-DD date")
        }
        return try {
            LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw IncomeFirestoreMappingException(
                documentId = documentId,
                fieldPath = fieldPath,
                detail = "must be a valid ISO YYYY-MM-DD date",
                cause = exception
            )
        }
    }

    private inline fun <reified T : Enum<T>> requireEnum(
        documentId: String,
        fields: Map<*, *>,
        key: String,
        fieldPath: String = key
    ): T {
        val value = requireString(documentId, fields, key, fieldPath)
        return enumValues<T>().firstOrNull { it.name == value }
            ?: mappingFailure(documentId, fieldPath, "contains an unsupported enum value")
    }

    private inline fun <reified T : Enum<T>> optionalEnum(
        documentId: String,
        fields: Map<*, *>,
        key: String
    ): T? {
        if (!fields.containsKey(key)) {
            return null
        }
        return requireEnum<T>(documentId, fields, key)
    }

    private fun validationFieldPath(message: String?): String {
        return when {
            message?.contains("Income ID") == true -> "id"
            message?.contains("Income amount") == true -> "money.baseAmountMinor"
            message?.contains("LKR rate date") == true -> "money.rateDate"
            message?.contains("exchange rate source", ignoreCase = true) == true ->
                "money.rateSource"
            message?.contains("exchange rate", ignoreCase = true) == true ->
                "money.rateLkrPerUnitDecimal"
            message?.contains("Stored LKR minor-unit") == true -> "money.baseAmountMinor"
            message?.contains("amount", ignoreCase = true) == true -> "money"
            message?.contains("Crypto income") == true -> "entryKind"
            message?.contains("Regular income") == true -> "entryKind"
            message?.contains("gain or loss") == true -> "pnlDirection"
            message?.contains("Profit or loss direction") == true -> "pnlDirection"
            else -> "document"
        }
    }

    private fun qualify(parent: String, child: String): String {
        return if (parent == "document") child else "$parent.$child"
    }

    private fun mappingFailure(
        documentId: String,
        fieldPath: String,
        detail: String
    ): Nothing {
        throw IncomeFirestoreMappingException(documentId, fieldPath, detail)
    }
}
