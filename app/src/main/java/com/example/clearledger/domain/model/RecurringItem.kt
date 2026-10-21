package com.example.clearledger.domain.model

enum class RecurringFrequency { WEEKLY, MONTHLY, YEARLY }

/**
 * A recurring commitment (rent, gym, a subscription). It is a PLAN, not an expense: it only becomes
 * an expense when a matching expense is recorded (SRS FR-REC-05).
 *
 * dueDay:   WEEKLY -> 1..7 (1 = Monday); MONTHLY / YEARLY -> day of the month 1..31.
 * dueMonth: only meaningful for YEARLY (1..12); ignored otherwise.
 * Money: like an expense, baseAmountMinor is the amount in the base currency (LKR).
 */
data class RecurringItem(
    val id: String,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val exchangeRate: String,
    val baseAmountMinor: Long,
    val categoryId: String,
    val frequency: RecurringFrequency,
    val dueDay: Int,
    val dueMonth: Int = 1,
    val isActive: Boolean = true
)
