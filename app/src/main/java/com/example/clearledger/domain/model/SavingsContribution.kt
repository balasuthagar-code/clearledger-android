package com.example.clearledger.domain.model

data class SavingsContribution(
    val id: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = ""
)