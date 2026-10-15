package com.example.clearledger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashState {
    data object Loading : SplashState
    data object NavigateHome : SplashState
    data object NavigateLogin : SplashState
    data object NavigateRegister : SplashState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _splashState.value = when (val result = authRepository.checkIncompleteSetup()) {
                is Result.Success -> {
                    when (result.data) {
                        is AuthRepository.SetupStatus.Confirmed -> SplashState.NavigateHome
                        is AuthRepository.SetupStatus.NotAuthenticated -> SplashState.NavigateLogin
                        is AuthRepository.SetupStatus.Incomplete -> SplashState.NavigateRegister
                    }
                }
                is Result.Error -> {
                    SplashState.NavigateLogin
                }
            }
        }
    }
}
