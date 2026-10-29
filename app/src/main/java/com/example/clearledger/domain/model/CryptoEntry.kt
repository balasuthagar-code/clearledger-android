package com.example.clearledger.domain.model

data class CryptoEntry(
    val id: String = "",
    val userId: String = "",
    val assetName: String = "",
    val date: String = "",
    val monthKey: String = "",
    val type: String = CryptoEntryType.BUY,
    val quantity: String = "0",
    val pricePerUnit: String = "",
    val originalCurrency: String = "LKR",
    val originalValueMinor: Long = 0L,
    val exchangeRate: String = "1",
    val baseValueMinor: Long = 0L,
    val costBasisBaseMinor: Long = 0L,
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    // Buy entries do not have a realised profit/loss.
    // Sell and Result entries calculate proceeds minus cost basis.
    val realisedProfitLossMinor: Long?
        get() = when (type) {
            CryptoEntryType.SELL,
            CryptoEntryType.RESULT -> baseValueMinor - costBasisBaseMinor

            else -> null
        }
}

object CryptoEntryType {
    const val BUY = "Buy"
    const val SELL = "Sell"
    const val RESULT = "Result"

    val values = listOf(BUY, SELL, RESULT)
}

object CryptoEntryOptions {
    val currencies = listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")
}
// thisanth
