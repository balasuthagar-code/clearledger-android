package com.example.clearledger.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

import com.example.clearledger.ui.income.IncomeSourcesScreen
import com.example.clearledger.ui.income.IncomeSourcesViewModel
import com.example.clearledger.ui.income.IncomeEntriesScreen
import com.example.clearledger.ui.income.IncomeEntriesViewModel

import com.example.clearledger.ui.goals.CreateGoalScreen
import com.example.clearledger.ui.goals.SavingsContributionsScreen
import com.example.clearledger.ui.freelance.FreelanceProjectsScreen
import com.example.clearledger.ui.crypto.CryptoScreen

import com.example.clearledger.ui.weeklycheckin.WeeklyCheckInScreen
import com.example.clearledger.ui.weeklycheckin.WeeklyCheckInViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Main branch: expense and recurring features
        expenseGraph(navController)
        recurringGraph(navController)

        // Splash screen
        composable(Routes.SPLASH) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val state =
                splashViewModel.splashState.collectAsStateWithLifecycle().value

            LaunchedEffect(state) {
                when (state) {
                    SplashState.NavigateHome -> {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    }

                    SplashState.NavigateLogin -> {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    }

                    SplashState.NavigateRegister -> {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                        }
                    }

                    SplashState.Loading -> Unit
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Login
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val state =
                loginViewModel.loginState.collectAsStateWithLifecycle().value

            LoginScreen(
                loginState = state,
                onLogin = { email, password ->
                    loginViewModel.login(email, password)
                },
                onNavigateRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Registration
        composable(Routes.REGISTER) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val state =
                registerViewModel.registerState.collectAsStateWithLifecycle().value

            RegisterScreen(
                registerState = state,
                onRegister = { email, password ->
                    registerViewModel.register(email, password)
                },
                onRetry = {
                    registerViewModel.retryProfileSetup()
                },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Home screen
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = hiltViewModel()

            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(ExpenseRoutes.add())
                        }
                    ) {
                        Text("+")
                    }
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                navController.navigate(ExpenseRoutes.LIST)
                            }
                        ) {
                            Text("All expenses")
                        }

                        TextButton(
                            onClick = {
                                navController.navigate(RecurringRoutes.LIST)
                            }
                        ) {
                            Text("Recurring")
                        }
                    }
                }
            ) { innerPadding ->
                HomeScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    viewModel = homeViewModel,

                    onLogout = {
                        homeViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) {
                                inclusive = true
                            }
                        }
                    },

                    onIncomeSources = {
                        navController.navigate(Routes.INCOME_SOURCES)
                    },

                    onIncomeEntries = {
                        navController.navigate(Routes.INCOME_ENTRIES)
                    },

                    onSavingsGoals = {
                        navController.navigate(Routes.SAVINGS_GOALS)
                    },

                    onSavingsContributions = {
                        navController.navigate(Routes.SAVINGS_CONTRIBUTIONS)
                    },

                    onFreelanceProjects = {
                        navController.navigate(Routes.FREELANCE_PROJECTS)
                    },

                    onCryptocurrency = {
                        navController.navigate(Routes.CRYPTOCURRENCY)
                    },

                    onWeeklyCheckIn = {
                        navController.navigate(Routes.WEEKLY_CHECK_IN)
                    },

                    onProfile = {
                        // Connect your Profile destination when available.
                    }
                )
            }
        }

        // Income sources
        composable(Routes.INCOME_SOURCES) {
            val incomeSourcesViewModel: IncomeSourcesViewModel = viewModel()

            IncomeSourcesScreen(
                viewModel = incomeSourcesViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Income entries
        composable(Routes.INCOME_ENTRIES) {
            val incomeEntriesViewModel: IncomeEntriesViewModel = viewModel()

            IncomeEntriesScreen(
                viewModel = incomeEntriesViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Savings goals
        composable(Routes.SAVINGS_GOALS) {
            CreateGoalScreen()
        }

        // Savings contributions
        composable(Routes.SAVINGS_CONTRIBUTIONS) {
            SavingsContributionsScreen()
        }

        // Freelance projects
        composable(Routes.FREELANCE_PROJECTS) {
            FreelanceProjectsScreen()
        }

        // Cryptocurrency
        composable(Routes.CRYPTOCURRENCY) {
            CryptoScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Weekly Check-in
        composable(Routes.WEEKLY_CHECK_IN) {
            val weeklyCheckInViewModel: WeeklyCheckInViewModel = hiltViewModel()

            WeeklyCheckInScreen(
                viewModel = weeklyCheckInViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
