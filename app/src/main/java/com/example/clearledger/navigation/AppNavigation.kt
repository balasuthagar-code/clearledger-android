package com.example.clearledger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val state = splashViewModel.splashState.collectAsStateWithLifecycle().value

            LaunchedEffect(state) {
                when (state) {
                    SplashState.NavigateHome -> {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                    SplashState.NavigateLogin -> {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                    SplashState.NavigateRegister -> {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
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
            val state = viewModel.loginState.collectAsStateWithLifecycle().value

            LoginScreen(
                loginState = state,
                onLogin = { email, password -> viewModel.login(email, password) },
                onNavigateRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: RegisterViewModel = hiltViewModel()
            val state = viewModel.registerState.collectAsStateWithLifecycle().value

            RegisterScreen(
                registerState = state,
                onRegister = { email, password -> viewModel.register(email, password) },
                onRetry = { viewModel.retryProfileSetup() },
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()

            HomeScreen(
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
