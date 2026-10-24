package com.example.clearledger.domain.usecase

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.domain.repository.RecurringRepository
import java.math.BigDecimal
import javax.inject.Inject

/** Everything the recurring form collected, still as typed. [id] == null means a new item. */
data class RecurringDraft(
    val id: String?,
    val name: String,
    val amountText: String,
    val currency: String,
    val rateText: String,
    val categoryId: String?,
    val frequency: RecurringFrequency,
    val dueDayText: String,
    val dueMonthText: String,
    val isActive: Boolean
)

sealed interface SaveRecurringResult {
    data object Saved : SaveRecurringResult
    data class Invalid(val errors: RecurringFormErrors) : SaveRecurringResult
    data class Failed(val message: String) : SaveRecurringResult
}

/** Validate -> convert to minor units / base currency (BR-1, BR-2) -> save. Add and edit share it. */
class SaveRecurringUseCase @Inject constructor(
    private val repository: RecurringRepository,
    private val validate: ValidateRecurringInputUseCase
) {
    suspend operator fun invoke(draft: RecurringDraft): SaveRecurringResult {
        val errors = validate(
            draft.name, draft.amountText, draft.currency, draft.rateText, draft.categoryId,
            draft.frequency, draft.dueDayText, draft.dueMonthText
        )
        if (errors.hasErrors) return SaveRecurringResult.Invalid(errors)

        val amountMinor = MoneyInput.parseToMinor(draft.amountText)
            ?: return SaveRecurringResult.Invalid(errors.copy(amount = "Enter a valid amount"))
        val isBase = draft.currency == Currencies.BASE
        val rate: BigDecimal = if (isBase) BigDecimal.ONE else
            MoneyInput.parseRate(draft.rateText)
                ?: return SaveRecurringResult.Invalid(errors.copy(rate = "Enter an exchange rate greater than zero"))
        val baseAmountMinor = if (isBase) amountMinor else
            MoneyInput.toBaseMinor(amountMinor, rate)
                ?: return SaveRecurringResult.Invalid(errors.copy(rate = "Converted amount is too large"))

        val item = RecurringItem(
            id = draft.id.orEmpty(),
            name = draft.name.trim(),
            amountMinor = amountMinor,
            currency = draft.currency,
            exchangeRate = if (isBase) "1" else rate.stripTrailingZeros().toPlainString(),
            baseAmountMinor = baseAmountMinor,
            categoryId = draft.categoryId.orEmpty(),
            frequency = draft.frequency,
            dueDay = draft.dueDayText.trim().toIntOrNull() ?: 1,
            dueMonth = if (draft.frequency == RecurringFrequency.YEARLY) draft.dueMonthText.trim().toIntOrNull() ?: 1 else 1,
            isActive = draft.isActive
        )

        val result: Result<*> =
            if (draft.id == null) repository.addRecurring(item) else repository.updateRecurring(item)

        return when (result) {
            is Result.Success -> SaveRecurringResult.Saved
            is Result.Error -> SaveRecurringResult.Failed(
                result.exception.message ?: "Could not save the item. Please try again."
            )
        }
    }
}
