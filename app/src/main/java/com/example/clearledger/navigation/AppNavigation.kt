package com.example.clearledger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.clearledger.ui.auth.LoginScreen
import com.example.clearledger.ui.auth.LoginViewModel
import com.example.clearledger.ui.auth.RegisterScreen
import com.example.clearledger.ui.auth.RegisterViewModel
import com.example.clearledger.ui.auth.SplashState
import com.example.clearledger.ui.auth.SplashViewModel
import com.example.clearledger.ui.home.HomeScreen
import com.example.clearledger.ui.home.HomeViewModel
import com.example.clearledger.ui.income.IncomeEntriesScreen
import com.example.clearledger.ui.income.IncomeSourcesScreen
import com.example.clearledger.ui.income.IncomeSourcesViewModel
import com.example.clearledger.ui.crypto.CryptoScreen
import com.example.clearledger.ui.freelance.FreelanceProjectsScreen
import com.example.clearledger.ui.goals.CreateGoalScreen
import com.example.clearledger.ui.goals.SavingsContributionsScreen
import com.example.clearledger.ui.weeklycheckin.WeeklyCheckInScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.clearledger.navigation.RecurringRoutes
import com.example.clearledger.navigation.recurringGraph
import com.example.clearledger.profile.*
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
@Composable
fun AppNavigation() {
    val session: ProfileSessionViewModel = hiltViewModel()
    val uid = session.uid.collectAsStateWithLifecycle().value
    key(session.navigationSession, uid) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        expenseGraph(navController)
        recurringGraph(navController)
        composable(Routes.SPLASH) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val state =
                splashViewModel.splashState.collectAsStateWithLifecycle().value

            LaunchedEffect(state) {
                ProfileRoutes.splashDestination(state)?.let { destination ->
                    navController.navigate(destination) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state is SplashState.Error) {
                    androidx.compose.foundation.layout.Column {
                        Text(state.message)
                        session.logoutError.collectAsStateWithLifecycle().value?.let { Text(it) }
                        TextButton(onClick = splashViewModel::checkSession) { Text("Retry") }
                        TextButton(onClick = session::logout) { Text("Log out") }
                    }
                } else CircularProgressIndicator()
            }
        }

        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = hiltViewModel()
            val state =
                viewModel.loginState.collectAsStateWithLifecycle().value

            LoginScreen(
                loginState = state,
                onLogin = { email, password ->
                    viewModel.login(email, password)
                },
                onNavigateRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateHome = {
                    navController.navigate(ProfileRoutes.afterAuthSuccess()) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: RegisterViewModel = hiltViewModel()
            val state =
                viewModel.registerState.collectAsStateWithLifecycle().value

            RegisterScreen(
                registerState = state,
                onRegister = { email, password ->
                    viewModel.register(email, password)
                },
                onRetry = {
                    viewModel.retryProfileSetup()
                },
                onNavigateHome = {
                    navController.navigate(ProfileRoutes.afterAuthSuccess()) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(ProfileRoutes.ONBOARDING) {
            val setup: ProfileSetupViewModel = hiltViewModel()
            val setupState = setup.state.collectAsStateWithLifecycle().value
            LaunchedEffect(setupState) {
                if (setupState is ProfileSetupState.Completed) {
                    navController.navigate(ProfileRoutes.afterSave()) { popUpTo(navController.graph.id) { inclusive = true } }
                }
                if (setupState is ProfileSetupState.SignedOut) {
                    navController.navigate(Routes.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                }
            }
            ProfileSetupScreen(setupState, setup::editName, setup::editCurrency, setup::save,
                setup::retry, setup::reload, setup::logout)
        }
        composable(ProfileRoutes.EDIT) {
            val edit: ProfileEditViewModel = hiltViewModel()
            val editState = edit.state.collectAsStateWithLifecycle().value
            LaunchedEffect(editState) {
                if (editState is ProfileSetupState.Completed) {
                    navController.navigate(ProfileRoutes.afterSave()) { popUpTo(Routes.HOME) { inclusive = true } }
                }
                if (editState is ProfileSetupState.SignedOut) {
                    navController.navigate(Routes.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
                }
            }
            ProfileSetupScreen(editState, edit::editName, edit::editCurrency, edit::save,
                edit::retry, edit::reload, edit::logout, editMode = true,
                onBack = { navController.popBackStack() })
        }

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val drawerItems = listOf(
                "Home" to Routes.HOME,
                "Income Sources" to Routes.INCOME_SOURCES,
                "Income Entries" to Routes.INCOME_ENTRIES,
                "Expense Management" to ExpenseRoutes.LIST,
                "Cryptocurrency" to Routes.CRYPTOCURRENCY,
                "Freelance Projects" to Routes.FREELANCE_PROJECTS,
                "Savings Goals" to Routes.SAVINGS_GOALS,
                "Savings Contributions" to Routes.SAVINGS_CONTRIBUTIONS,
                "Weekly Check-in" to Routes.WEEKLY_CHECK_IN
            )

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        drawerItems.forEach { (label, route) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = route == Routes.HOME,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (route != Routes.HOME) {
                                        navController.navigate(route)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { navController.navigate(ExpenseRoutes.add()) }
                        ) {
                            Text("+")
                        }
                    },
                    bottomBar = {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement =
                                androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            TextButton(onClick = { navController.navigate(ProfileRoutes.EDIT) }) { Text("Profile") }
                            TextButton(onClick = { navController.navigate(ExpenseRoutes.LIST) }) {
                                Text("All expenses")
                            }
                            TextButton(onClick = { navController.navigate(RecurringRoutes.LIST) }) {
                                Text("Recurring")
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        HomeScreen(
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onLogout = {
                                viewModel.logout()
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }

        composable(Routes.INCOME_ENTRIES) {
            IncomeEntriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.INCOME_SOURCES) {
            val sourceViewModel = remember { IncomeSourcesViewModel() }
            IncomeSourcesScreen(
                viewModel = sourceViewModel,
                onBack = { navController.popBackStack() },
                onOpenEntries = { navController.navigate(Routes.INCOME_ENTRIES) }
            )
        }
        composable(Routes.CRYPTOCURRENCY) {
            CryptoScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FREELANCE_PROJECTS) {
            FreelanceProjectsScreen()
        }
        composable(Routes.SAVINGS_GOALS) {
            CreateGoalScreen(
                onOpenContributions = {
                    navController.navigate(Routes.SAVINGS_CONTRIBUTIONS)
                }
            )
        }
        composable(Routes.SAVINGS_CONTRIBUTIONS) {
            SavingsContributionsScreen()
        }
        composable(Routes.WEEKLY_CHECK_IN) {
            val checkInViewModel: com.example.clearledger.ui.weeklycheckin.WeeklyCheckInViewModel = hiltViewModel()
            WeeklyCheckInScreen(
                viewModel = checkInViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
}
