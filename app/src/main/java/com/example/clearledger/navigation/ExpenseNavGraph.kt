package com.example.clearledger.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.clearledger.ui.expense.form.ExpenseFormRoute
import com.example.clearledger.ui.expense.list.ExpenseListRoute

/**
 * Call this ONCE inside the NavHost { ... } block of AppNavigation:
 *     expenseGraph(navController)
 */
fun NavGraphBuilder.expenseGraph(navController: NavController) {
    composable(ExpenseRoutes.LIST) {
        ExpenseListRoute(
            onAddExpense = { navController.navigate(ExpenseRoutes.add()) },
            onEditExpense = { id -> navController.navigate(ExpenseRoutes.edit(id)) }
        )
    }
    composable(
        route = ExpenseRoutes.FORM,
        arguments = listOf(
            navArgument(ExpenseRoutes.ARG_EXPENSE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        ExpenseFormRoute(
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() }
        )
    }
}
