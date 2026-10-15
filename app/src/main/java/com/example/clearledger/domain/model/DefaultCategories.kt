package com.example.clearledger.domain.model

import com.example.clearledger.domain.model.ExpenseClassification.COMMITTED
import com.example.clearledger.domain.model.ExpenseClassification.DISCRETIONARY

/**
 * The seven default categories from the plan. IDs are fixed strings (not random), so seeding the
 * same user twice can never create duplicates. Rent and Subscriptions default to COMMITTED;
 * the user can still change the classification per expense.
 */
object DefaultCategories {
    val all: List<ExpenseCategory> = listOf(
        ExpenseCategory("food", "Food", DISCRETIONARY, sortOrder = 0),
        ExpenseCategory("transport", "Transport", DISCRETIONARY, sortOrder = 1),
        ExpenseCategory("rent", "Rent", COMMITTED, sortOrder = 2),
        ExpenseCategory("entertainment", "Entertainment", DISCRETIONARY, sortOrder = 3),
        ExpenseCategory("health_fitness", "Health/Fitness", DISCRETIONARY, sortOrder = 4),
        ExpenseCategory("subscriptions", "Subscriptions", COMMITTED, sortOrder = 5),
        ExpenseCategory("other", "Other", DISCRETIONARY, sortOrder = 6)
    )
}
