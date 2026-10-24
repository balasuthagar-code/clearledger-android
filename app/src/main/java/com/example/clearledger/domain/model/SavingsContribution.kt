package com.example.clearledger.domain.model

data class SavingsContribution(
    val id: String = "",
    val goalId: String = "",
    val amountMinor: Long = 0L,
    val date: String = "",
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
// thisanth