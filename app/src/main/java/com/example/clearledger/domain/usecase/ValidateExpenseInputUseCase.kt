package com.example.clearledger.domain.usecase

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MoneyInput
import javax.inject.Inject

/** Field-level error messages for the expense form; null = that field is fine. */
data class ExpenseFormErrors(
    val amount: String? = null,
    val category: String? = null,
    val rate: String? = null
) {
    val hasErrors: Boolean get() = amount != null || category != null || rate != null
}

/**
 * FR-EXP-05: reject non-positive or non-numeric amounts and a missing category.
 * FR-CUR: a foreign-currency expense also needs a positive exchange rate.
 * Pure function - no Android, easy to unit test.
 */
class ValidateExpenseInputUseCase @Inject constructor() {

    operator fun invoke(
        amountText: String,
        currency: String,
        rateText: String,
        categoryId: String?
    ): ExpenseFormErrors {
        val amountMinor = MoneyInput.parseToMinor(amountText)
        val amountError = when {
            amountText.isBlank() -> "Enter an amount"
            amountMinor == null -> "Enter a valid amount, for example 1250.50"
            amountMinor <= 0L -> "Amount must be greater than zero"
            else -> null
        }

        val categoryError = if (categoryId.isNullOrBlank()) "Choose a category" else null

        val rateError = if (currency == Currencies.BASE) {
            null                                           // FR-CUR-03: base currency -> rate is 1
        } else {
            val rate = MoneyInput.parseRate(rateText)
            when {
                rate == null -> "Enter an exchange rate greater than zero"
                amountMinor != null && amountMinor > 0L &&
                    (MoneyInput.toBaseMinor(amountMinor, rate) ?: 0L) <= 0L ->
                    "Converted amount is too small"
                else -> null
            }
        }

        return ExpenseFormErrors(amount = amountError, category = categoryError, rate = rateError)
    }
}
