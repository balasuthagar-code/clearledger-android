package com.example.clearledger.navigation

/**
 * Routes of the expense feature. They live in this file (not in Member 1's Routes.kt) so merging
 * never conflicts. The form route has an OPTIONAL argument: no id = add, with id = edit.
 */
object ExpenseRoutes {
    const val ARG_EXPENSE_ID = "expenseId"
    const val LIST = "expenses"
    const val FORM = "expense_form?$ARG_EXPENSE_ID={$ARG_EXPENSE_ID}"

    fun add(): String = "expense_form"
    fun edit(expenseId: String): String = "expense_form?$ARG_EXPENSE_ID=$expenseId"
}
