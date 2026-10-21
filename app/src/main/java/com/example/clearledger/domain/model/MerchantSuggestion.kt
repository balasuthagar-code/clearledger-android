package com.example.clearledger.domain.model

/**
 * FR-EXP-08 merchant memory: a merchant the user has used before, with the category they usually
 * file it under (blank when unknown) and how often it was used.
 */
data class MerchantSuggestion(
    val name: String,
    val categoryId: String,
    val useCount: Int
)
