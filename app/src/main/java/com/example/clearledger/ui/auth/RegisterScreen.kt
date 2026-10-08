package com.example.clearledger.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.User

@Composable
fun RegisterScreen(
    registerState: UiState<User>?,
    onRegister: (String, String) -> Unit,
    onRetry: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = registerState is UiState.Loading
    val isSuccess = registerState is UiState.Success
    val canEdit = !isLoading && !isSuccess

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            password = ""
            onNavigateHome()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Register to start using ClearLedger.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            enabled = canEdit,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            supportingText = { Text("Use at least 8 characters.") },
            singleLine = true,
            enabled = canEdit,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onRegister(email, password) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoading) "Creating account…" else "Register"
            )
        }

        when (registerState) {
            UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }

            is UiState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = registerState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (registerState.canRetryProfile) {
                        Button(onClick = onRetry) {
                            Text("Retry Profile Setup")
                        }
                    }
                }
            }

            is UiState.Success -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Account created successfully.",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(onClick = onNavigateHome) {
                        Text("Continue to Home")
                    }
                }
            }

            null -> Unit
        }
    }
}