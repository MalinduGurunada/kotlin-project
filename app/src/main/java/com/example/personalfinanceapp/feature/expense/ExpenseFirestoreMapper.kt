package com.example.personalfinanceapp.feature.expense

import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Expense
import com.example.personalfinanceapp.core.model.Money
import com.example.personalfinanceapp.core.model.PaymentChannel
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.model.SpendingClass
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

internal class ExpenseFirestoreMappingException(
    documentId: String,
    detail: String,
    cause: Throwable? = null
) : IllegalArgumentException("Expense '$documentId' is invalid: $detail", cause)

internal object ExpenseFirestoreMapper {

    fun fromFirestoreMap(documentId: String, fields: Map<String, Any?>): Expense =
        mapOrThrow(documentId) {
            mapExpense(
                documentId = documentId,
                fields = fields,
                occurredDate = LocalDate.parse(requiredString(fields, "occurredDate"))
            )
        }

    fun fromFirestoreMapInRange(
        documentId: String,
        fields: Map<String, Any?>,
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): Expense? = mapOrThrow(documentId) {
        val occurredDate = LocalDate.parse(requiredString(fields, "occurredDate"))
        if (occurredDate.isBefore(startDate) || !occurredDate.isBefore(endDateExclusive)) {
            null
        } else {
            mapExpense(documentId, fields, occurredDate)
        }
    }

    private fun mapExpense(
        documentId: String,
        fields: Map<String, Any?>,
        occurredDate: LocalDate
    ): Expense {
        val currency = enumValue<CurrencyCode>(fields, "originalCurrency") ?: CurrencyCode.LKR
        val storedBaseAmount = fields["baseAmountMinor"]?.let(::exactLong)
        val originalAmount = decimalString(fields["originalAmountDecimal"])
            ?: if (currency == CurrencyCode.LKR && storedBaseAmount != null) {
                BigDecimal.valueOf(storedBaseAmount, 2).toPlainString()
            } else {
                throw IllegalArgumentException("originalAmountDecimal is missing")
            }
        val rate = decimalString(fields["rateLkrPerUnitDecimal"])
            ?: if (currency == CurrencyCode.LKR) "1" else {
                throw IllegalArgumentException("rateLkrPerUnitDecimal is missing")
            }
        val originalDecimal = originalAmount.toBigDecimal()
        val rateDecimal = rate.toBigDecimal()
        require(originalDecimal > BigDecimal.ZERO) { "originalAmountDecimal must be positive" }
        require(rateDecimal > BigDecimal.ZERO) { "rateLkrPerUnitDecimal must be positive" }
        val reconstructedBaseAmount = originalDecimal
            .multiply(rateDecimal)
            .multiply(BigDecimal.valueOf(100L))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        val baseAmount = storedBaseAmount ?: reconstructedBaseAmount
        require(baseAmount > 0L) { "baseAmountMinor must be positive" }
        require(baseAmount == reconstructedBaseAmount) {
            "baseAmountMinor does not match the stored money snapshot"
        }

        return Expense(
            id = documentId,
            categoryId = fields["categoryId"] as? String ?: "",
            categoryNameSnapshot = fields["categoryNameSnapshot"] as? String ?: "",
            spendingClass = enumValue<SpendingClass>(fields, "spendingClass")
                ?: SpendingClass.DISCRETIONARY,
            paymentChannel = enumValue<PaymentChannel>(fields, "paymentChannel")
                ?: PaymentChannel.CASH,
            money = Money(
                originalAmountDecimal = originalAmount,
                originalCurrency = currency,
                rateLkrPerUnitDecimal = rate,
                baseAmountMinor = baseAmount,
                rateSource = enumValue<RateSource>(fields, "rateSource") ?: RateSource.LKR_FIXED,
                rateDate = LocalDate.parse(
                    (fields["rateDate"] as? String) ?: occurredDate.toString()
                )
            ),
            occurredDate = occurredDate,
            note = fields["note"] as? String,
            channelLabel = fields["channelLabel"] as? String,
            recurringExpenseId = fields["recurringExpenseId"] as? String,
            recurringOccurrenceId = fields["recurringOccurrenceId"] as? String
        )
    }

    private inline fun <T> mapOrThrow(documentId: String, block: () -> T): T =
        try {
            block()
        } catch (exception: Exception) {
            if (exception is ExpenseFirestoreMappingException) throw exception
            throw ExpenseFirestoreMappingException(
                documentId = documentId,
                detail = exception.message ?: "malformed expense data",
                cause = exception
            )
        }

    private fun requiredString(fields: Map<String, Any?>, name: String): String =
        (fields[name] as? String)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$name is missing")

    private fun decimalString(value: Any?): String? = when (value) {
        null -> null
        is String -> value.takeIf { it.isNotBlank() }
        is Byte, is Short, is Int, is Long, is Float, is Double ->
            BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
        else -> throw IllegalArgumentException("money decimal has an unsupported type")
    }

    private fun exactLong(value: Any): Long = when (value) {
        is Byte -> value.toLong()
        is Short -> value.toLong()
        is Int -> value.toLong()
        is Long -> value
        is Float, is Double -> BigDecimal(value.toString()).longValueExact()
        else -> throw IllegalArgumentException("baseAmountMinor has an unsupported type")
    }

    private inline fun <reified T : Enum<T>> enumValue(
        fields: Map<String, Any?>,
        name: String
    ): T? = (fields[name] as? String)?.let { enumValueOf<T>(it) }
}
