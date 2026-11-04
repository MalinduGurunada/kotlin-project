package com.example.personalfinanceapp.feature.income

import com.example.personalfinanceapp.core.model.CryptoRealizedDetails
import com.example.personalfinanceapp.core.model.PnlDirection
import com.example.personalfinanceapp.core.money.MoneyValidator
import java.math.BigDecimal

data class CryptoRealizedCalculation(
    val details: CryptoRealizedDetails,
    val signedResultOriginal: BigDecimal,
    val resultMagnitudeOriginalDecimal: String,
    val direction: PnlDirection
)

object CryptoRealizedCalculator {

    fun calculate(
        saleProceedsInput: String,
        costBasisInput: String,
        feesInput: String
    ): Result<CryptoRealizedCalculation> = runCatching {
        val proceeds = MoneyValidator.parseNonNegativeAmount(
            saleProceedsInput,
            fieldName = "Sale proceeds"
        ).getOrThrow()
        val costBasis = MoneyValidator.parseNonNegativeAmount(
            costBasisInput,
            fieldName = "Cost basis"
        ).getOrThrow()
        val fees = MoneyValidator.parseNonNegativeAmount(
            feesInput,
            fieldName = "Fees"
        ).getOrThrow()

        val signedResult = proceeds.subtract(costBasis).subtract(fees)
        val direction = when (signedResult.signum()) {
            1 -> PnlDirection.GAIN
            -1 -> PnlDirection.LOSS
            else -> PnlDirection.BREAK_EVEN
        }

        CryptoRealizedCalculation(
            details = CryptoRealizedDetails(
                costBasisOriginalDecimal = costBasis.toPlainString(),
                saleProceedsOriginalDecimal = proceeds.toPlainString(),
                feesOriginalDecimal = fees.toPlainString()
            ),
            signedResultOriginal = signedResult,
            resultMagnitudeOriginalDecimal = signedResult.abs().toPlainString(),
            direction = direction
        )
    }

    fun calculate(details: CryptoRealizedDetails): Result<CryptoRealizedCalculation> =
        calculate(
            saleProceedsInput = details.saleProceedsOriginalDecimal,
            costBasisInput = details.costBasisOriginalDecimal,
            feesInput = details.feesOriginalDecimal
        )
}
