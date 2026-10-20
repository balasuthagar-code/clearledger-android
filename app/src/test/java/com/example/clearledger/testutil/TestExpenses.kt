package com.example.clearledger.testutil

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import java.time.LocalDate

/** Builds an expense with sensible defaults; override only what a test cares about. */
fun testExpense(
    id: String = "e1",
    baseMinor: Long = 100_000,
    categoryId: String = "food",
    date: LocalDate = LocalDate.of(2026, 10, 20),
    currency: String = "LKR",
    amountMinor: Long = baseMinor,
    createdAtMillis: Long = 0L,
    classification: ExpenseClassification = ExpenseClassification.DISCRETIONARY,
    merchant: String = "",
    monthKey: String = MonthKey.of(date)
) = Expense(
    id = id,
    amountMinor = amountMinor,
    currency = currency,
    exchangeRate = if (currency == "LKR") "1" else "300",
    baseAmountMinor = baseMinor,
    categoryId = categoryId,
    classification = classification,
    paymentMethod = PaymentMethod.CARD,
    merchant = merchant,
    date = date,
    monthKey = monthKey,
    createdAtMillis = createdAtMillis
)
