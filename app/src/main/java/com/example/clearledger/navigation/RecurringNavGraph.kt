package com.example.clearledger.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.clearledger.ui.recurring.form.RecurringFormRoute
import com.example.clearledger.ui.recurring.list.RecurringListRoute

/** Call ONCE inside NavHost { ... }:   recurringGraph(navController) */
fun NavGraphBuilder.recurringGraph(navController: NavController) {
    composable(RecurringRoutes.LIST) {
        RecurringListRoute(
            onAddItem = { navController.navigate(RecurringRoutes.add()) },
            onEditItem = { id -> navController.navigate(RecurringRoutes.edit(id)) },
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        route = RecurringRoutes.FORM,
        arguments = listOf(
            navArgument(RecurringRoutes.ARG_RECURRING_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        RecurringFormRoute(
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() }
        )
    }
}
