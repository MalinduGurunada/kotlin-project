package com.example.personalfinanceapp.feature.goal

import com.example.personalfinanceapp.core.model.Contribution
import com.example.personalfinanceapp.core.model.ContributionKind
import com.example.personalfinanceapp.core.model.Goal
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException

class GoalFirestoreMappingException(
    val documentId: String,
    val fieldPath: String,
    detail: String,
    cause: Throwable? = null
) : IllegalArgumentException(
    "Goal document '$documentId' has invalid field '$fieldPath': $detail",
    cause
)

object GoalFirestoreMapper {

    private const val SCHEMA_VERSION = 1L
    private const val MAX_DOCUMENT_ID_BYTES = 1_500
    private val reservedDocumentIdPattern = Regex("__.*__")

    private val requiredGoalFields = setOf(
        "schemaVersion",
        "id",
        "name",
        "targetAmountMinor",
        "startDate",
        "targetDate",
        "archived"
    )

    private val requiredContributionFields = setOf(
        "schemaVersion",
        "id",
        "goalId",
        "amountMinor",
        "kind",
        "occurredDate"
    )

    private val optionalContributionFields = setOf("reversesId", "note")

    fun toFirestoreMap(goal: Goal): Map<String, Any> {
        validateGoal(goal)
        return linkedMapOf(
            "schemaVersion" to SCHEMA_VERSION,
            "id" to goal.id,
            "name" to goal.name,
            "targetAmountMinor" to goal.targetAmountMinor,
            "startDate" to goal.startDate.toString(),
            "targetDate" to goal.targetDate.toString(),
            "archived" to goal.archived
        )
    }

    fun goalFromFirestoreMap(
        documentId: String,
        fields: Map<String, Any?>
    ): Goal {
        validateDocumentId(documentId, documentId)
        requireExactFields(documentId, fields, requiredGoalFields, emptySet())
        requireSchemaVersion(documentId, fields)
        val storedId = requireString(documentId, fields, "id")
        validateDocumentId(documentId, storedId, "id")
        if (storedId != documentId) {
            mappingFailure(documentId, "id", "must match the Firestore document ID")
        }

        return Goal(
            id = storedId,
            name = requireString(documentId, fields, "name"),
            targetAmountMinor = requireLong(documentId, fields, "targetAmountMinor"),
            startDate = requireDate(documentId, fields, "startDate"),
            targetDate = requireDate(documentId, fields, "targetDate"),
            archived = requireBoolean(documentId, fields, "archived")
        ).also(::validateGoal)
    }

    fun contributionToFirestoreMap(contribution: Contribution): Map<String, Any> {
        validateContributionShape(contribution)
        return linkedMapOf<String, Any>(
            "schemaVersion" to SCHEMA_VERSION,
            "id" to contribution.id,
            "goalId" to contribution.goalId,
            "amountMinor" to contribution.amountMinor,
            "kind" to contribution.kind.name,
            "occurredDate" to contribution.occurredDate.toString()
        ).apply {
            contribution.reversesId?.let { put("reversesId", it) }
            contribution.note?.let { put("note", it) }
        }
    }

    fun contributionFromFirestoreMap(
        documentId: String,
        expectedGoalId: String,
        fields: Map<String, Any?>
    ): Contribution {
        validateDocumentId(documentId, documentId)
        requireExactFields(
            documentId,
            fields,
            requiredContributionFields,
            optionalContributionFields
        )
        requireSchemaVersion(documentId, fields)
        val storedId = requireString(documentId, fields, "id")
        validateDocumentId(documentId, storedId, "id")
        if (storedId != documentId) {
            mappingFailure(documentId, "id", "must match the Firestore document ID")
        }
        val goalId = requireString(documentId, fields, "goalId")
        validateDocumentId(documentId, goalId, "goalId")
        if (goalId != expectedGoalId) {
            mappingFailure(documentId, "goalId", "must match the parent goal ID")
        }

        return Contribution(
            id = storedId,
            goalId = goalId,
            amountMinor = requireLong(documentId, fields, "amountMinor"),
            kind = requireEnum(documentId, fields, "kind"),
            occurredDate = requireDate(documentId, fields, "occurredDate"),
            reversesId = optionalString(documentId, fields, "reversesId"),
            note = optionalString(documentId, fields, "note")
        ).also(::validateContributionShape)
    }

    fun validateGoal(goal: Goal) {
        validateDocumentId(goal.id, goal.id, "id")
        require(goal.name.isNotBlank()) { "Goal name cannot be blank" }
        require(goal.targetAmountMinor > 0L) { "Goal target must be positive" }
        require(!goal.targetDate.isBefore(goal.startDate)) {
            "Target date cannot be before start date"
        }
    }

    fun validateContributionShape(contribution: Contribution) {
        validateDocumentId(contribution.id, contribution.id, "id")
        validateDocumentId(contribution.id, contribution.goalId, "goalId")
        contribution.reversesId?.let {
            validateDocumentId(contribution.id, it, "reversesId")
        }
        when (contribution.kind) {
            ContributionKind.REVERSAL -> {
                require(contribution.amountMinor < 0L) {
                    "Reversal contribution amount must be negative"
                }
                require(!contribution.reversesId.isNullOrBlank()) {
                    "Reversal contribution must reference the original contribution"
                }
            }
            ContributionKind.OPENING,
            ContributionKind.ALLOCATION -> {
                require(contribution.amountMinor > 0L) {
                    "Contribution amount must be positive"
                }
                require(contribution.reversesId == null) {
                    "Only reversal contributions can reference another contribution"
                }
            }
        }
    }

    fun validateContributionGoalId(goalId: String) {
        validateDocumentId(goalId, goalId, "goalId")
    }

    private fun requireSchemaVersion(documentId: String, fields: Map<String, Any?>) {
        val version = requireLong(documentId, fields, "schemaVersion")
        if (version != SCHEMA_VERSION) {
            mappingFailure(documentId, "schemaVersion", "unsupported schema version $version")
        }
    }

    private fun requireExactFields(
        documentId: String,
        fields: Map<String, Any?>,
        required: Set<String>,
        optional: Set<String>
    ) {
        val missing = required - fields.keys
        if (missing.isNotEmpty()) {
            mappingFailure(documentId, missing.sorted().first(), "required field is missing")
        }
        val unexpected = fields.keys - required - optional
        if (unexpected.isNotEmpty()) {
            mappingFailure(documentId, unexpected.sorted().first(), "field is not supported")
        }
    }

    private fun requireString(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): String = (fields[key] as? String)
        ?: mappingFailure(documentId, key, "must be a string")

    private fun optionalString(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): String? {
        if (!fields.containsKey(key)) return null
        return fields[key] as? String
            ?: mappingFailure(documentId, key, "must be a string")
    }

    private fun requireLong(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): Long = when (val value = fields[key]) {
        is Long -> value
        is Int -> value.toLong()
        else -> mappingFailure(documentId, key, "must be an integer")
    }

    private fun requireBoolean(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): Boolean = (fields[key] as? Boolean)
        ?: mappingFailure(documentId, key, "must be a boolean")

    private inline fun <reified T : Enum<T>> requireEnum(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): T {
        val value = requireString(documentId, fields, key)
        return enumValues<T>().firstOrNull { it.name == value }
            ?: mappingFailure(documentId, key, "contains unsupported value '$value'")
    }

    private fun requireDate(
        documentId: String,
        fields: Map<String, Any?>,
        key: String
    ): LocalDate {
        val value = requireString(documentId, fields, key)
        return try {
            LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            mappingFailure(documentId, key, "must be an ISO-8601 date", exception)
        }
    }

    private fun validateDocumentId(
        actualDocumentId: String,
        value: String,
        fieldPath: String = "documentId"
    ) {
        try {
            require(value.isNotBlank()) { "Document ID cannot be empty" }
            require(value != "." && value != "..") { "Invalid Firestore document ID" }
            require('/' !in value) { "Document ID cannot contain path separators" }
            require(!reservedDocumentIdPattern.matches(value)) {
                "Document ID uses a reserved format"
            }
            val encodedBytes = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(value))
                .remaining()
            require(encodedBytes <= MAX_DOCUMENT_ID_BYTES) {
                "Document ID exceeds the Firestore size limit"
            }
        } catch (exception: CharacterCodingException) {
            throw GoalFirestoreMappingException(
                actualDocumentId,
                fieldPath,
                "must contain valid UTF-8 characters",
                exception
            )
        } catch (exception: IllegalArgumentException) {
            throw GoalFirestoreMappingException(
                actualDocumentId,
                fieldPath,
                exception.message ?: "invalid document ID",
                exception
            )
        }
    }

    private fun mappingFailure(
        documentId: String,
        fieldPath: String,
        detail: String,
        cause: Throwable? = null
    ): Nothing = throw GoalFirestoreMappingException(documentId, fieldPath, detail, cause)
}
