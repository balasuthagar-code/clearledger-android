package com.example.clearledger.domain.usecase

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.ExpenseRepository
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/** Everything the form collected, still as the user typed it. [id] == null means "new expense". */
data class ExpenseDraft(
    val id: String?,
    val amountText: String,
    val currency: String,
    val rateText: String,
    val categoryId: String?,
    val classification: ExpenseClassification,
    val paymentMethod: PaymentMethod,
    val merchant: String,
    val note: String,
    val date: LocalDate,
    val createdAtMillis: Long = 0L
)

sealed interface SaveExpenseResult {
    data object Saved : SaveExpenseResult
    data class Invalid(val errors: ExpenseFormErrors) : SaveExpenseResult
    data class Failed(val message: String) : SaveExpenseResult
}

/**
 * Validate -> convert to minor units and base currency (BR-1, BR-2) -> derive monthKey -> save.
 * One use case serves BOTH quick-add and edit, so the rules can never drift apart.
 */
class SaveExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val validate: ValidateExpenseInputUseCase
) {
    suspend operator fun invoke(draft: ExpenseDraft): SaveExpenseResult {
        val errors = validate(draft.amountText, draft.currency, draft.rateText, draft.categoryId)
        if (errors.hasErrors) return SaveExpenseResult.Invalid(errors)

        val amountMinor = MoneyInput.parseToMinor(draft.amountText)
            ?: return SaveExpenseResult.Invalid(errors.copy(amount = "Enter a valid amount"))

        val isBase = draft.currency == Currencies.BASE
        val rate: BigDecimal = if (isBase) BigDecimal.ONE else
            MoneyInput.parseRate(draft.rateText)
                ?: return SaveExpenseResult.Invalid(errors.copy(rate = "Enter an exchange rate greater than zero"))
        val baseAmountMinor = if (isBase) amountMinor else
            MoneyInput.toBaseMinor(amountMinor, rate)
                ?: return SaveExpenseResult.Invalid(errors.copy(rate = "Converted amount is too large"))

        val expense = Expense(
            id = draft.id.orEmpty(),
            amountMinor = amountMinor,
            currency = draft.currency,
            exchangeRate = if (isBase) "1" else rate.stripTrailingZeros().toPlainString(),
            baseAmountMinor = baseAmountMinor,
            categoryId = draft.categoryId.orEmpty(),
            classification = draft.classification,
            paymentMethod = draft.paymentMethod,
            merchant = draft.merchant.trim(),
            note = draft.note.trim(),
            date = draft.date,
            monthKey = MonthKey.of(draft.date),
            createdAtMillis = draft.createdAtMillis
        )

        val result: Result<*> =
            if (draft.id == null) repository.addExpense(expense) else repository.updateExpense(expense)

        return when (result) {
            is Result.Success -> SaveExpenseResult.Saved
            is Result.Error -> SaveExpenseResult.Failed(
                result.exception.message ?: "Could not save the expense. Please try again."
            )
        }
    }
}
