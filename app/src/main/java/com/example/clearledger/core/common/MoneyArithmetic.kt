package com.example.clearledger.core.common

import java.math.BigDecimal
import java.math.RoundingMode

/** Shared arithmetic only. Syntax and business policy belong to each caller. */
internal object MoneyArithmetic {
    fun toMinor(amount: BigDecimal, scale: Int): Long =
        amount.setScale(scale, RoundingMode.UNNECESSARY).movePointRight(scale).longValueExact()

    fun format(minor: Long, scale: Int): String = BigDecimal.valueOf(minor, scale).toPlainString()

    // Equal-scale currencies: one rounding operation, after multiplication.
    fun convertMinor(amountMinor: Long, rate: BigDecimal): Long =
        BigDecimal.valueOf(amountMinor).multiply(rate)
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
}
