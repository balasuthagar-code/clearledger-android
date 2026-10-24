package com.example.clearledger.domain.usecase

import com.example.clearledger.domain.model.RecurringFrequency
import java.time.YearMonth
import javax.inject.Inject

data class RecurringFormErrors(
    val name: String? = null,
    val amount: String? = null,
    val category: String? = null,
    val rate: String? = null,
    val dueDay: String? = null,
    val dueMonth: String? = null
) {
    val hasErrors: Boolean
        get() = name != null || amount != null || category != null || rate != null || dueDay != null || dueMonth != null
}

/**
 * Rules for a recurring commitment (FR-REC-01).
 * Amount / currency / rate / category use the SAME rules as an expense (ValidateExpenseInputUseCase),
 * so the two forms can never disagree.
 */
class ValidateRecurringInputUseCase @Inject constructor(
    private val validateExpenseInput: ValidateExpenseInputUseCase
) {
    operator fun invoke(
        name: String,
        amountText: String,
        currency: String,
        rateText: String,
        categoryId: String?,
        frequency: RecurringFrequency,
        dueDayText: String,
        dueMonthText: String
    ): RecurringFormErrors {
        val money = validateExpenseInput(amountText, currency, rateText, categoryId)

        val nameError = when {
            name.isBlank() -> "Enter a name, for example Rent or Gym"
            name.trim().length > MAX_NAME_LENGTH -> "Keep the name under $MAX_NAME_LENGTH characters"
            else -> null
        }

        val dueDay = dueDayText.trim().toIntOrNull()
        val dueMonth = dueMonthText.trim().toIntOrNull()

        val dueDayError = when (frequency) {
            RecurringFrequency.WEEKLY ->
                if (dueDay == null || dueDay !in 1..7) "Enter a weekday from 1 (Monday) to 7 (Sunday)" else null
            RecurringFrequency.MONTHLY ->
                if (dueDay == null || dueDay !in 1..31) "Enter a day of the month from 1 to 31" else null
            RecurringFrequency.YEARLY -> when {
                dueDay == null || dueDay !in 1..31 -> "Enter a day of the month from 1 to 31"
                dueMonth != null && dueMonth in 1..12 &&
                    dueDay > YearMonth.of(LEAP_YEAR, dueMonth).lengthOfMonth() -> "That month has fewer days"
                else -> null
            }
        }

        val dueMonthError =
            if (frequency == RecurringFrequency.YEARLY && (dueMonth == null || dueMonth !in 1..12))
                "Enter a month from 1 to 12" else null

        return RecurringFormErrors(
            name = nameError,
            amount = money.amount,
            category = money.category,
            rate = money.rate,
            dueDay = dueDayError,
            dueMonth = dueMonthError
        )
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val LEAP_YEAR = 2024        // so "29 February" is accepted for a yearly item
    }
}
