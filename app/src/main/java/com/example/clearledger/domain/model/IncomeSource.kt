
package com.example.clearledger.domain.model

data class IncomeSource(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val defaultCurrency: String = "LKR",
    val active: Boolean = true
)
