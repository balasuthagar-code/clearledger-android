package com.example.clearledger.domain.model

import java.time.LocalDate

/**
 * The expense as the app sees it. Stored in Firestore at users/{uid}/expenses/{id}
 * (see data/dto/ExpenseMapper.kt for the exact field names).
 *
 * Money rule: every amount is a Long in MINOR units (LKR 1,250.50 -> 125050).
 * baseAmountMinor is the value in the user's base currency (LKR); every total uses THIS field.
 */
data class Expense(
    val id: String,
    val amountMinor: Long,            // as entered, in `currency`
    val currency: String,             // "LKR", "USD", ...
    val exchangeRate: String,         // decimal string, base units per 1 original unit ("1" for LKR)
    val baseAmountMinor: Long,        // converted to base currency (LKR)
    val categoryId: String,
    val classification: ExpenseClassification,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val merchant: String = "",
    val note: String = "",
    val date: LocalDate,
    val monthKey: String,             // "YYYY-MM"
    val createdAtMillis: Long = 0L    // used only to order expenses of the same day (newest first)
)
