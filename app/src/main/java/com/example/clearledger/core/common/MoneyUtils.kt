package com.example.clearledger.core.common

import java.math.BigDecimal

object MoneyUtils {

    fun minorToMajorString(amountMinor: Long, scale: Int = 2): String {
        require(scale in 0..4) { "Scale must be between 0 and 4, got $scale" }
        return MoneyArithmetic.format(amountMinor, scale)
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
            MoneyArithmetic.toMinor(bd, scale)
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Monetary amount exceeds Long range (overflow): $majorString", e)
        }
    }
}
