
package com.example.clearledger.domain.model

data class IncomeEntry(
    val id: String = "",
    val sourceId: String = "",
    val sourceName: String = "",
    val originalAmountMinor: Long = 0L,
    val currency: String = "LKR",
    val exchangeRate: String = "1",
    val baseAmountMinor: Long = 0L,
    val date: String = "",
    val monthKey: String = "",
    val note: String = "",
    val status: String = "EXPECTED"
)
