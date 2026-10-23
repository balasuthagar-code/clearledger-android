package com.example.clearledger.domain.model

data class FreelanceProject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val client: String = "",
    val agreedAmount: Double = 0.0,
    val currency: String = "LKR",
    val rate: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)