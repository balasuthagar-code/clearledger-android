
package com.example.clearledger.domain.model

data class IncomeEntry(
    val id: String = "",
    val userId: String = "",
    val sourceId: String = "",
    val originalAmountMinor: Long = 0L,
    val originalCurrency: String = "LKR",
    val exchangeRate: String = "1",
    val baseAmountMinor: Long = 0L,
    val date: String = "",
    val monthKey: String = "",
    val status: String = "EXPECTED",
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

object IncomeEntryOptions {
    val currencies = listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")
    val statuses = listOf("EXPECTED", "RECEIVED")
}
// thisanth