
package com.example.clearledger.domain.model

data class IncomeEntry(
    val id: String = "",
    val sourceId: String = "",
    val sourceName: String = "",
    val amount: Double = 0.0,
    val currency: String = "LKR",
    val exchangeRate: Double? = null,
    val amountInBaseCurrency: Double = 0.0,
    val date: String = "",
    val monthKey: String = "",
    val note: String = ""
)
