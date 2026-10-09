package com.example.clearledger.core.common

sealed interface UiState<out T> {

    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T
    ) : UiState<T>

    data class Error(
        val message: String,
        val canRetryProfile: Boolean = false
    ) : UiState<Nothing>
}