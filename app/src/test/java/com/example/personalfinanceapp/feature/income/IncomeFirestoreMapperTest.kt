package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.Income
import com.example.personalfinanceapp.core.model.IncomeEntryKind
import com.example.personalfinanceapp.core.model.IncomeSource
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.model.RateSource
import com.example.personalfinanceapp.core.money.MoneyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IncomeFirestoreMapperTest {

    private val occurredDate = LocalDate.of(2026, 10, 21)

    private fun lkrIncome(
        id: String = "income-001",
        source: IncomeSource = IncomeSource.SALARY,
        entryKind: IncomeEntryKind = IncomeEntryKind.RECEIPT,
        pnlDirection: PnlDirection? = null,
        amount: String = "1000.00",
        projectReference: String? = null,
        receivableId: String? = null,
        assetSymbol: String? = null,
        note: String? = null
    ): Income {
        return Income(
            id = id,
            source = source,
            entryKind = entryKind,
            money = MoneyConverter.fromLkr(amount, occurredDate).getOrThrow(),
            occurredDate = occurredDate,
            pnlDirection = pnlDirection,
            projectReference = projectReference,
            receivableId = receivableId,
            assetSymbol = assetSymbol,
            note = note
        )
    }

    private fun usdIncome(
        id: String = "income-usd-001",
        source: IncomeSource = IncomeSource.FREELANCE,
        entryKind: IncomeEntryKind = IncomeEntryKind.RECEIPT,
        pnlDirection: PnlDirection? = null,
        rateSource: RateSource = RateSource.MANUAL_TRANSFER,
        rateDate: LocalDate = LocalDate.of(2026, 10, 18),
        projectReference: String? = null,
        receivableId: String? = null,
        assetSymbol: String? = null,
        note: String? = null
    ): Income {
        return Income(
            id = id,
            source = source,
            entryKind = entryKind,
            money = MoneyConverter.fromUsd(
                amountInput = "125.00",
                rateLkrPerUsdInput = "310.123456",
                rateSource = rateSource,
                rateDate = rateDate
            ).getOrThrow(),
            occurredDate = occurredDate,
            pnlDirection = pnlDirection,
            projectReference = projectReference,
            receivableId = receivableId,
            assetSymbol = assetSymbol,
            note = note
        )
    }

    private fun mutableDocument(income: Income = lkrIncome()): MutableMap<String, Any?> {
        return IncomeFirestoreMapper.toFirestoreMap(income)
            .mapValuesTo(linkedMapOf()) { it.value }
    }

    private fun mutableMoney(document: MutableMap<String, Any?>): MutableMap<String, Any?> {
        val money = (document["money"] as Map<*, *>)
            .entries
            .associateTo(linkedMapOf()) { (key, value) -> key as String to value }
        document["money"] = money
        return money
    }

    private fun assertMappingFailure(
        document: Map<String, Any?>,
        expectedFieldPath: String,
        documentId: String = "income-001"
    ): IncomeFirestoreMappingException {
        val exception = assertThrows(IncomeFirestoreMappingException::class.java) {
            IncomeFirestoreMapper.fromFirestoreMap(documentId, document)
        }
        assertEquals(documentId, exception.documentId)
        assertEquals(expectedFieldPath, exception.fieldPath)
        assertTrue(exception.message.orEmpty().contains(expectedFieldPath))
        return exception
    }

    @Test
    fun mapsSalaryReceiptWithNestedLkrSnapshot() {
        val income = lkrIncome()

        val document = IncomeFirestoreMapper.toFirestoreMap(income)

        assertEquals(
            setOf("schemaVersion", "id", "source", "entryKind", "money", "occurredDate"),
            document.keys
        )
        assertEquals(1L, document["schemaVersion"])
        assertEquals("income-001", document["id"])
        assertEquals("SALARY", document["source"])
        assertEquals("RECEIPT", document["entryKind"])
        assertEquals("2026-10-21", document["occurredDate"])

        val money = document["money"] as Map<*, *>
        assertEquals("1000.00", money["originalAmountDecimal"])
        assertEquals("LKR", money["originalCurrency"])
        assertEquals("1", money["rateLkrPerUnitDecimal"])
        assertEquals(100_000L, money["baseAmountMinor"])
        assertEquals("LKR_FIXED", money["rateSource"])
        assertEquals("2026-10-21", money["rateDate"])
    }

    @Test
    fun roundTripsFreelanceReceipt() {
        val income = usdIncome(
            projectReference = "website-redesign",
            receivableId = "receivable-001"
        )

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(income, reconstructed)
    }

    @Test
    fun roundTripsAdsenseReceipt() {
        val income = lkrIncome(source = IncomeSource.ADSENSE)

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(income, reconstructed)
    }

    @Test
    fun roundTripsCryptoRealizedGain() {
        val income = usdIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.GAIN,
            assetSymbol = "BTC"
        )

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(income, reconstructed)
    }

    @Test
    fun roundTripsCryptoRealizedLossWithPositiveMagnitude() {
        val income = usdIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.LOSS,
            assetSymbol = "ETH"
        )

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(PnlDirection.LOSS, reconstructed.pnlDirection)
        assertTrue(reconstructed.money.baseAmountMinor > 0L)
        assertEquals(income, reconstructed)
    }

    @Test
    fun preservesAutomaticUsdReferenceSnapshot() {
        val income = usdIncome(rateSource = RateSource.EXCHANGE_RATE_API_REFERENCE)

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(RateSource.EXCHANGE_RATE_API_REFERENCE, reconstructed.money.rateSource)
        assertEquals(income.money, reconstructed.money)
    }

    @Test
    fun preservesManualUsdTransferSnapshotExactly() {
        val income = usdIncome(rateSource = RateSource.MANUAL_TRANSFER)

        val document = IncomeFirestoreMapper.toFirestoreMap(income)
        val money = document["money"] as Map<*, *>

        assertEquals("125.00", money["originalAmountDecimal"])
        assertEquals("310.123456", money["rateLkrPerUnitDecimal"])
        assertEquals(3_876_543L, money["baseAmountMinor"])
        assertEquals("MANUAL_TRANSFER", money["rateSource"])
        assertEquals(income, IncomeFirestoreMapper.fromFirestoreMap(income.id, document))
    }

    @Test
    fun preservesManualUsdReferenceSnapshot() {
        val income = usdIncome(rateSource = RateSource.MANUAL_REFERENCE)

        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(
            income.id,
            IncomeFirestoreMapper.toFirestoreMap(income)
        )

        assertEquals(RateSource.MANUAL_REFERENCE, reconstructed.money.rateSource)
        assertEquals(income, reconstructed)
    }

    @Test
    fun preservesAllOptionalMetadata() {
        val income = usdIncome(
            projectReference = "final-milestone",
            receivableId = "receivable-001",
            assetSymbol = "USD",
            note = "Paid at the historical transfer rate"
        )

        val document = IncomeFirestoreMapper.toFirestoreMap(income)
        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(income.id, document)

        assertEquals("final-milestone", document["projectReference"])
        assertEquals("receivable-001", document["receivableId"])
        assertEquals("USD", document["assetSymbol"])
        assertEquals("Paid at the historical transfer rate", document["note"])
        assertEquals(income, reconstructed)
    }

    @Test
    fun omitsAbsentOptionalMetadataAndDecodesItAsNull() {
        val income = lkrIncome()

        val document = IncomeFirestoreMapper.toFirestoreMap(income)
        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(income.id, document)

        listOf("pnlDirection", "projectReference", "receivableId", "assetSymbol", "note")
            .forEach { assertFalse(document.containsKey(it)) }
        assertEquals(null, reconstructed.pnlDirection)
        assertEquals(null, reconstructed.projectReference)
        assertEquals(null, reconstructed.receivableId)
        assertEquals(null, reconstructed.assetSymbol)
        assertEquals(null, reconstructed.note)
    }

    @Test
    fun preservesLongMinorUnitValuesBeyondIntRange() {
        val income = lkrIncome(amount = "30000000.00")

        val document = IncomeFirestoreMapper.toFirestoreMap(income)
        val money = document["money"] as Map<*, *>
        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(income.id, document)

        assertEquals(3_000_000_000L, money["baseAmountMinor"])
        assertEquals(3_000_000_000L, reconstructed.money.baseAmountMinor)
    }

    @Test
    fun preservesHistoricalExchangeRateDate() {
        val historicalDate = LocalDate.of(2026, 9, 30)
        val income = usdIncome(rateDate = historicalDate)

        val document = IncomeFirestoreMapper.toFirestoreMap(income)
        val money = document["money"] as Map<*, *>
        val reconstructed = IncomeFirestoreMapper.fromFirestoreMap(income.id, document)

        assertEquals("2026-09-30", money["rateDate"])
        assertEquals(historicalDate, reconstructed.money.rateDate)
        assertEquals(occurredDate, reconstructed.occurredDate)
    }

    @Test
    fun rejectsInvalidIncomeBeforeEncoding() {
        val validIncome = usdIncome()
        val invalidIncome = validIncome.copy(
            money = validIncome.money.copy(baseAmountMinor = validIncome.money.baseAmountMinor + 1L)
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            IncomeFirestoreMapper.toFirestoreMap(invalidIncome)
        }

        assertEquals(
            "Stored LKR minor-unit amount does not match the calculated amount",
            exception.message
        )
    }

    @Test
    fun rejectsStorageIncompatibleDocumentIdsWithoutReplacingThem() {
        val invalidIds = listOf("income/001", ".", "..", "__reserved__", "a".repeat(1_501))

        invalidIds.forEach { invalidId ->
            assertThrows(IllegalArgumentException::class.java) {
                IncomeFirestoreMapper.toFirestoreMap(lkrIncome(id = invalidId))
            }
        }
    }

    @Test
    fun rejectsMalformedUtf16SurrogatesWhenEncodingDocumentIds() {
        val invalidIds = listOf(
            "\uD800",
            "\uDC00",
            "income-\uD800-001"
        )

        invalidIds.forEach { invalidId ->
            val exception = assertThrows(IncomeFirestoreMappingException::class.java) {
                IncomeFirestoreMapper.toFirestoreMap(lkrIncome(id = invalidId))
            }

            assertEquals("id", exception.fieldPath)
            assertTrue(exception.message.orEmpty().contains("valid UTF-8"))
            assertTrue(exception.cause?.cause is java.nio.charset.CharacterCodingException)
        }
    }

    @Test
    fun rejectsMalformedUtf16SurrogatesWhenDecodingDocumentIds() {
        val invalidIds = listOf(
            "\uD800",
            "\uDC00",
            "income-\uD800-001"
        )

        invalidIds.forEach { invalidId ->
            assertMappingFailure(
                document = mutableDocument(),
                expectedFieldPath = "documentId",
                documentId = invalidId
            )

            val documentWithInvalidStoredId = mutableDocument().apply {
                this["id"] = invalidId
            }
            assertMappingFailure(documentWithInvalidStoredId, "id")
        }
    }

    @Test
    fun acceptsValidUnicodeDocumentIdsInBothDirections() {
        val validIds = listOf(
            "income-\uD83D\uDE80",
            "income-\u00E9"
        )

        validIds.forEach { validId ->
            val income = lkrIncome(id = validId)
            val document = IncomeFirestoreMapper.toFirestoreMap(income)

            assertEquals(validId, document["id"])
            assertEquals(
                income,
                IncomeFirestoreMapper.fromFirestoreMap(validId, document)
            )
        }
    }

    @Test
    fun enforcesUtf8ByteLimitForMultibyteDocumentIds() {
        val exactlyAtLimit = "\u20AC".repeat(500)
        val overLimit = "\u20AC".repeat(501)

        val incomeAtLimit = lkrIncome(id = exactlyAtLimit)
        val documentAtLimit = IncomeFirestoreMapper.toFirestoreMap(incomeAtLimit)
        assertEquals(
            incomeAtLimit,
            IncomeFirestoreMapper.fromFirestoreMap(exactlyAtLimit, documentAtLimit)
        )

        val encodingException = assertThrows(IncomeFirestoreMappingException::class.java) {
            IncomeFirestoreMapper.toFirestoreMap(lkrIncome(id = overLimit))
        }
        assertEquals("id", encodingException.fieldPath)

        assertMappingFailure(
            document = mutableDocument(),
            expectedFieldPath = "documentId",
            documentId = overLimit
        )
    }

    @Test
    fun rejectsInvalidActualFirestoreDocumentId() {
        val document = mutableDocument()

        assertMappingFailure(document, "documentId", documentId = "income/001")
    }

    @Test
    fun rejectsMissingRequiredField() {
        val document = mutableDocument().apply { remove("source") }

        assertMappingFailure(document, "source")
    }

    @Test
    fun rejectsIncorrectRequiredFieldType() {
        val document = mutableDocument().apply { this["source"] = 7L }

        assertMappingFailure(document, "source")
    }

    @Test
    fun rejectsMissingNestedMoneyMap() {
        val document = mutableDocument().apply { remove("money") }

        assertMappingFailure(document, "money")
    }

    @Test
    fun rejectsMoneyStoredAsWrongType() {
        val document = mutableDocument().apply { this["money"] = "not-a-map" }

        assertMappingFailure(document, "money")
    }

    @Test
    fun rejectsMissingNestedMoneyField() {
        val document = mutableDocument()
        mutableMoney(document).remove("rateDate")

        assertMappingFailure(document, "money.rateDate")
    }

    @Test
    fun rejectsInvalidNestedMoneyValue() {
        val document = mutableDocument()
        mutableMoney(document)["originalAmountDecimal"] = "not-money"

        assertMappingFailure(document, "money")
    }

    @Test
    fun rejectsInvalidCurrencyEnum() {
        val document = mutableDocument()
        mutableMoney(document)["originalCurrency"] = "EUR"

        assertMappingFailure(document, "money.originalCurrency")
    }

    @Test
    fun rejectsInvalidRateSourceEnum() {
        val document = mutableDocument()
        mutableMoney(document)["rateSource"] = "LATEST_RATE"

        assertMappingFailure(document, "money.rateSource")
    }

    @Test
    fun rejectsInvalidIncomeSourceEnum() {
        val document = mutableDocument().apply { this["source"] = "BONUS" }

        assertMappingFailure(document, "source")
    }

    @Test
    fun rejectsInvalidEntryKindEnum() {
        val document = mutableDocument().apply { this["entryKind"] = "ESTIMATE" }

        assertMappingFailure(document, "entryKind")
    }

    @Test
    fun rejectsInvalidPnlDirectionEnum() {
        val income = usdIncome(
            source = IncomeSource.CRYPTO,
            entryKind = IncomeEntryKind.REALIZED_PNL,
            pnlDirection = PnlDirection.GAIN
        )
        val document = mutableDocument(income).apply { this["pnlDirection"] = "PROFIT" }

        assertMappingFailure(document, "pnlDirection", income.id)
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val document = mutableDocument().apply { this["schemaVersion"] = 2L }

        assertMappingFailure(document, "schemaVersion")
    }

    @Test
    fun rejectsMissingSchemaVersion() {
        val document = mutableDocument().apply { remove("schemaVersion") }

        assertMappingFailure(document, "schemaVersion")
    }

    @Test
    fun rejectsWrongSchemaVersionType() {
        listOf(1, "1", 1.0).forEach { wrongVersion ->
            val document = mutableDocument().apply { this["schemaVersion"] = wrongVersion }

            assertMappingFailure(document, "schemaVersion")
        }
    }

    @Test
    fun rejectsInvalidIsoDate() {
        listOf("2026-02-30", "2026-2-03", "+10000-01-01").forEach { invalidDate ->
            val document = mutableDocument().apply { this["occurredDate"] = invalidDate }

            assertMappingFailure(document, "occurredDate")
        }
    }

    @Test
    fun rejectsDateStoredAsWrongType() {
        val document = mutableDocument().apply { this["occurredDate"] = occurredDate }

        assertMappingFailure(document, "occurredDate")
    }

    @Test
    fun rejectsStoredIdThatDiffersFromDocumentId() {
        val document = mutableDocument().apply { this["id"] = "income-other" }

        assertMappingFailure(document, "id")
    }

    @Test
    fun rejectsWrongNumericRepresentationsWithoutCoercion() {
        listOf(100_000, 100_000.0, "100000").forEach { wrongAmount ->
            val document = mutableDocument()
            mutableMoney(document)["baseAmountMinor"] = wrongAmount

            assertMappingFailure(document, "money.baseAmountMinor")
        }

        val decimalAsDouble = mutableDocument()
        mutableMoney(decimalAsDouble)["originalAmountDecimal"] = 1000.0
        assertMappingFailure(decimalAsDouble, "money.originalAmountDecimal")
    }

    @Test
    fun rejectsInconsistentConvertedLkrAmount() {
        val income = usdIncome()
        val document = mutableDocument(income)
        mutableMoney(document)["baseAmountMinor"] = income.money.baseAmountMinor + 1L

        assertMappingFailure(document, "money.baseAmountMinor", income.id)
    }

    @Test
    fun rejectsInvalidLkrRate() {
        val document = mutableDocument()
        mutableMoney(document)["rateLkrPerUnitDecimal"] = "2"

        assertMappingFailure(document, "money.rateLkrPerUnitDecimal")
    }

    @Test
    fun rejectsIncompatibleLkrRateSourceAndIdentifiesItsField() {
        val document = mutableDocument()
        mutableMoney(document)["rateSource"] = "MANUAL_REFERENCE"

        assertMappingFailure(document, "money.rateSource")
    }

    @Test
    fun rejectsInvalidLkrRateDate() {
        val document = mutableDocument()
        mutableMoney(document)["rateDate"] = "2026-10-20"

        assertMappingFailure(document, "money.rateDate")
    }

    @Test
    fun rejectsInvalidCryptoFieldCombination() {
        val document = mutableDocument().apply {
            this["source"] = "CRYPTO"
            this["entryKind"] = "RECEIPT"
            this["pnlDirection"] = "GAIN"
        }

        assertMappingFailure(document, "entryKind")
    }

    @Test
    fun rejectsInvalidNonCryptoPnlCombination() {
        val document = mutableDocument().apply { this["pnlDirection"] = "GAIN" }

        assertMappingFailure(document, "pnlDirection")
    }

    @Test
    fun rejectsPresentNullOptionalField() {
        val document = mutableDocument().apply { this["note"] = null }

        assertMappingFailure(document, "note")
    }

    @Test
    fun rejectsUnknownTopLevelAndNestedFields() {
        val topLevel = mutableDocument().apply { this["ownerUid"] = "user-001" }
        assertMappingFailure(topLevel, "ownerUid")

        val nested = mutableDocument()
        mutableMoney(nested)["exchangeRateTimestamp"] = "2026-10-21T00:00:00Z"
        assertMappingFailure(nested, "money.exchangeRateTimestamp")
    }
}
