package com.example.clearledger.ui.crypto

import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
import java.math.RoundingMode

object CryptoPortfolioMath {
    fun quantity(entries: List<CryptoEntry>): BigDecimal =
        entries.fold(BigDecimal.ZERO) { total, entry ->
            val quantity = entry.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (entry.type == CryptoEntryType.BUY) total + quantity else total - quantity
        }

    fun purchaseCostMinor(entries: List<CryptoEntry>): Long =
        entries
            .filter { it.type == CryptoEntryType.BUY }
            .sumOf { it.baseValueMinor }

    fun averagePurchasePriceMinor(
        purchaseCostMinor: Long,
        quantity: BigDecimal
    ): BigDecimal =
        if (quantity > BigDecimal.ZERO) {
            purchaseCostMinor.toBigDecimal().divide(quantity, 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
}
