
package com.example.clearledger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val state = splashViewModel.splashState
                .collectAsStateWithLifecycle().value

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

        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val state = loginViewModel.loginState
                .collectAsStateWithLifecycle().value

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

        composable(Routes.REGISTER) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            val state = registerViewModel.registerState
                .collectAsStateWithLifecycle().value

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

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = hiltViewModel()

            HomeScreen(
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
                    // Add the weekly check-in route when its screen is ready.
                },
                onProfile = {
                    // Add the profile route when its screen is ready.
                }
            )
        }

        composable(Routes.INCOME_SOURCES) {
            val incomeSourcesViewModel: IncomeSourcesViewModel = viewModel()

            IncomeSourcesScreen(
                viewModel = incomeSourcesViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.INCOME_ENTRIES) {
            val incomeEntriesViewModel: IncomeEntriesViewModel = viewModel()

            IncomeEntriesScreen(
                viewModel = incomeEntriesViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SAVINGS_GOALS) {
            CreateGoalScreen()
        }

        composable(Routes.SAVINGS_CONTRIBUTIONS) {
            SavingsContributionsScreen()
        }

        composable(Routes.FREELANCE_PROJECTS) {
            FreelanceProjectsScreen()
        }
        composable(Routes.CRYPTOCURRENCY) {
            CryptoScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
