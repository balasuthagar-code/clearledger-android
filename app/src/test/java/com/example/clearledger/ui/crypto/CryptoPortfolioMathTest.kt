package com.example.clearledger.ui.crypto

import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoPortfolioMathTest {
    @Test
    fun repeatedBuysAtDifferentPricesAggregateQuantityCostAndAverage() {
        val entries = listOf(
            CryptoEntry(type = CryptoEntryType.BUY, quantity = "0.01", baseValueMinor = 10_000_000L),
            CryptoEntry(type = CryptoEntryType.BUY, quantity = "0.01", baseValueMinor = 9_000_000L)
        )

        assertEquals(BigDecimal("0.02"), CryptoPortfolioMath.quantity(entries))
        assertEquals(19_000_000L, CryptoPortfolioMath.purchaseCostMinor(entries))
        assertEquals(
            BigDecimal("950000000.00000000"),
            CryptoPortfolioMath.averagePurchasePriceMinor(
                19_000_000L,
                BigDecimal("0.02")
            )
        )
    }

    @Test
    fun partialAndFullSalesReduceAvailableQuantityWithoutChangingPurchaseHistory() {
        val buys = listOf(
            CryptoEntry(type = CryptoEntryType.BUY, quantity = "0.02", baseValueMinor = 19_000_000L)
        )
        val partialSale = buys + CryptoEntry(
            type = CryptoEntryType.SELL,
            quantity = "0.005",
            baseValueMinor = 6_000_000L
        )
        val fullSale = buys + CryptoEntry(
            type = CryptoEntryType.SELL,
            quantity = "0.02",
            baseValueMinor = 19_000_000L
        )

        assertEquals(BigDecimal("0.015"), CryptoPortfolioMath.quantity(partialSale))
        assertEquals(19_000_000L, CryptoPortfolioMath.purchaseCostMinor(partialSale))
        assertTrue(CryptoPortfolioMath.quantity(fullSale) <= BigDecimal.ZERO)
    }
}
