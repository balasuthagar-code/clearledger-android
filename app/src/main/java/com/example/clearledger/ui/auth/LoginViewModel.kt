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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState =
        MutableStateFlow<UiState<User>?>(null)

    val loginState: StateFlow<UiState<User>?> =
        _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (_loginState.value is UiState.Loading ||
            _loginState.value is UiState.Success
        ) {
            return
        }

        _loginState.value = UiState.Loading

        viewModelScope.launch {
            _loginState.value =
                when (val result = authRepository.login(email, password)) {
                    is Result.Success -> UiState.Success(result.data)

                    is Result.Error -> UiState.Error(
                        result.exception.message
                            ?: "Login failed. Please try again."
                    )
                }
        }
    }
}
