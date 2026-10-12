package com.example.clearledger.core.common

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyUtils {

    fun minorToMajorString(amountMinor: Long, scale: Int = 2): String {
        require(scale in 0..4) { "Scale must be between 0 and 4, got $scale" }
        val bd = BigDecimal(amountMinor).divide(BigDecimal.TEN.pow(scale))
        return bd.setScale(scale, RoundingMode.HALF_EVEN).toPlainString()
    }

    fun majorToMinorLong(majorString: String, scale: Int = 2): Long {
        require(scale in 0..4) { "Scale must be between 0 and 4, got $scale" }
        val trimmed = majorString.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Monetary amount cannot be empty.")
        }

        val bd = try {
            BigDecimal(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid monetary amount format: $majorString", e)
        }

        if (bd.scale() > scale) {
            throw IllegalArgumentException("Precision exceeds supported scale of $scale for amount: $majorString")
        }

        return try {
            bd.setScale(scale, RoundingMode.UNNECESSARY).multiply(BigDecimal.TEN.pow(scale)).longValueExact()
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Monetary amount exceeds Long range (overflow): $majorString", e)
        }
    }
}
