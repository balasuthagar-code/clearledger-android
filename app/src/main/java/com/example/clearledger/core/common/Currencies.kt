package com.example.clearledger.core.common

/**
 * Currencies from the SRS (FR-CUR-01). All use two decimal places, so "minor units" = cents.
 * BASE is the default base currency (A1 in the SRS). Until Member 1's onboarding stores the user's
 * chosen base currency, the app assumes LKR.
 */
object Currencies {
    const val BASE = "LKR"
    val supported: List<String> = listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")
}
