package com.example.clearledger.domain.model

data class ExpenseCategory(
    val id: String,
    val name: String,
    val defaultClassification: ExpenseClassification,
    val isFavourite: Boolean = false,
    val isBuiltIn: Boolean = true,
    val sortOrder: Int = 0
)
