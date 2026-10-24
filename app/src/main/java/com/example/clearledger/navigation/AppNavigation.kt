package com.example.clearledger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.clearledger.navigation.RecurringRoutes
import com.example.clearledger.navigation.recurringGraph
@Composable
fun AppNavigation() {
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) {
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()

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
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement =
                            androidx.compose.foundation.layout.Arrangement.Center
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
            ) { padding ->
                Box(
                    modifier = Modifier.padding(padding)
                ) {
                    HomeScreen(
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.HOME) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}