package com.example.clearledger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clearledger.core.common.Result
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.User
import com.example.clearledger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerState =
        MutableStateFlow<UiState<User>?>(null)

    val registerState: StateFlow<UiState<User>?> =
        _registerState.asStateFlow()

    init {
        checkIncompleteSetup()
    }

    fun checkIncompleteSetup() {
        if (_registerState.value is UiState.Loading ||
            _registerState.value is UiState.Success
        ) {
            return
        }

        _registerState.value = UiState.Loading

        viewModelScope.launch {
            _registerState.value =
                when (val result = authRepository.checkIncompleteSetup()) {
                    is Result.Success -> {
                        when (val status = result.data) {
                            is AuthRepository.SetupStatus.NotAuthenticated -> null
                            is AuthRepository.SetupStatus.Confirmed -> UiState.Success(status.user)
                            is AuthRepository.SetupStatus.Incomplete -> UiState.Error(
                                "Your account was created, but your profile could not be saved. Account setup is incomplete.",
                                canRetryProfile = true
                            )
                        }
                    }

                    is Result.Error -> UiState.Error(
                        result.exception.message ?: "Could not verify setup status.",
                        canRetryProfile = false
                    )
                }
        }
    }

    fun register(email: String, password: String) {
        if (_registerState.value is UiState.Loading ||
            _registerState.value is UiState.Success
        ) {
            return
        }

        _registerState.value = UiState.Loading

        viewModelScope.launch {
            _registerState.value =
                when (val result = authRepository.register(email, password)) {
                    is Result.Success -> UiState.Success(result.data)

                    is Result.Error -> UiState.Error(
                        result.exception.message
                            ?: "Registration failed. Please try again.",
                        canRetryProfile = false
                    )
                }
        }
    }

    fun retryProfileSetup() {
        if (_registerState.value is UiState.Loading ||
            _registerState.value is UiState.Success
        ) {
            return
        }

        _registerState.value = UiState.Loading

        viewModelScope.launch {
            _registerState.value =
                when (val result = authRepository.retryProfileSetup()) {
                    is Result.Success -> UiState.Success(result.data)
                    is Result.Error -> UiState.Error(
                        result.exception.message
                            ?: "Profile save failed. Please retry.",
                        canRetryProfile = true
                    )
                }
        }
    }
}
