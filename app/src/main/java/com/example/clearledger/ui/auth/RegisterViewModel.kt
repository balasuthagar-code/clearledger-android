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
        viewModelScope.launch {
            when (val result = authRepository.checkIncompleteSetup()) {
                is Result.Success -> {
                    if (result.data != null) {
                        _registerState.value = UiState.Success(result.data)
                    }
                }
                is Result.Error -> {}
            }
        }
    }

    fun register(email: String, password: String) {
        // Ignore repeated taps while registering or after success.
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
                            ?: "Registration failed. Please try again."
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
                            ?: "Profile save failed. Please retry."
                    )
                }
        }
    }
}