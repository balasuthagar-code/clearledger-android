
package com.example.clearledger.domain.model

data class IncomeSource(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = "SALARY",
    val customType: String = "",
    val defaultCurrency: String = "LKR",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object IncomeSourceTypes {
    val values = listOf(
        "SALARY",
        "FREELANCE",
        "BUSINESS",
        "INVESTMENT",
        "CRYPTO",
        "OTHER"
    )
}

object SupportedCurrencies {
    val values = listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")
}
