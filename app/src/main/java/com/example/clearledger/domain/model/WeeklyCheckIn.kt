
package com.example.clearledger.domain.model

data class WeeklyCheckIn(
    val id: String = "",
    val weekKey: String = "",
    val expectedBalanceMinor: Long = 0L,
    val reportedBalanceMinor: Long = 0L,
    val gapMinor: Long = 0L,
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
//thinsanth