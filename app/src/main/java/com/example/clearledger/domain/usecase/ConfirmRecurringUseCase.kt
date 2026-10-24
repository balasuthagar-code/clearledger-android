package com.example.clearledger.domain.usecase

import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.DefaultCategories
import com.example.clearledger.domain.model.DueRecurring
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import com.example.clearledger.domain.repository.CategoryRepository
import com.example.clearledger.domain.repository.ExpenseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed interface ConfirmRecurringResult {
    data object Confirmed : ConfirmRecurringResult
    data class Failed(val message: String) : ConfirmRecurringResult
}

/**
 * FR-REC-04: confirming a due item creates ONE expense linked to it (recurringId).
 *
 * The expense id is deterministic ("rec_{recurringId}_{YYYY-MM}"), so confirming twice (double tap,
 * slow network, two devices) overwrites the same document instead of creating a duplicate.
 * Nothing is recorded until the user confirms - a recurring item stays a plan (FR-REC-05).
 */
class ConfirmRecurringUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(due: DueRecurring, today: LocalDate): ConfirmRecurringResult {
        val item = due.item
        // Confirming early (before the due day) records the expense today, never in the future.
        val date = if (due.dueDate.isAfter(today)) today else due.dueDate

        val expense = Expense(
            id = expenseIdFor(item.id, YearMonth.from(due.dueDate).toString()),
            amountMinor = item.amountMinor,
            currency = item.currency,
            exchangeRate = item.exchangeRate,
            baseAmountMinor = item.baseAmountMinor,
            categoryId = item.categoryId,
            classification = classificationFor(item.categoryId),
            paymentMethod = PaymentMethod.OTHER,
            merchant = item.name,
            note = "Recurring commitment",
            date = date,
            monthKey = MonthKey.of(date),
            recurringId = item.id
        )

        return when (val result = expenseRepository.upsertExpense(expense)) {
            is Result.Success -> ConfirmRecurringResult.Confirmed
            is Result.Error -> ConfirmRecurringResult.Failed(
                result.exception.message ?: "Could not record the expense. Please try again."
            )
        }
    }

    /** FR-CAT-02: the category's default (Rent / Subscriptions = Committed), with a safe fallback. */
    private suspend fun classificationFor(categoryId: String): ExpenseClassification {
        val fromUserCategories = try {
            categoryRepository.observeCategories().first()
                .firstOrNull { it.id == categoryId }?.defaultClassification
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return fromUserCategories
            ?: DefaultCategories.all.firstOrNull { it.id == categoryId }?.defaultClassification
            ?: ExpenseClassification.DISCRETIONARY
    }

    companion object {
        fun expenseIdFor(recurringId: String, monthKey: String): String = "rec_${recurringId}_$monthKey"
    }
}
