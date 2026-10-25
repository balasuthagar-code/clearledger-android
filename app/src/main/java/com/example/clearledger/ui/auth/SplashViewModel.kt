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
import com.example.clearledger.profile.ProfileCategories
import kotlinx.coroutines.Job

sealed interface SplashState {
    data object Loading : SplashState
    data object NavigateHome : SplashState
    data object NavigateLogin : SplashState
    data object NavigateRegister : SplashState // Retained, no longer emitted.
    data object NavigateOnboarding : SplashState
    data class Error(val message: String) : SplashState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val categories: ProfileCategories
) : ViewModel() {

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    private var job: Job? = null

    init {
        checkSession()
    }

    fun checkSession() {
        if (job?.isActive == true) return
        _splashState.value = SplashState.Loading
        job = viewModelScope.launch {
            _splashState.value = when (val result = authRepository.checkIncompleteSetup()) {
                is Result.Success -> {
                    when (val status = result.data) {
                        is AuthRepository.SetupStatus.Confirmed -> when (val ready = categories.ensureReady(status.user.uid)) {
                            is Result.Success -> SplashState.NavigateHome
                            is Result.Error -> SplashState.Error(ready.exception.message ?: "Setup failed. Please retry.")
                        }
                        is AuthRepository.SetupStatus.NotAuthenticated -> SplashState.NavigateLogin
                        is AuthRepository.SetupStatus.Incomplete -> SplashState.NavigateOnboarding
                    }
                }
                is Result.Error -> {
                    SplashState.Error(result.exception.message ?: "Could not load profile. Check your connection and retry.")
                }
            }
        }
    }
}
