
package com.example.clearledger.domain.model

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currency: String = "LKR",
    val deadline: String = "",
    val startingAmount: Double = 0.0
)
