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
import androidx.compose.material3.OutlinedButton
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
fun LoginScreen(
    loginState: UiState<User>?,
    onLogin: (String, String) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isLoading = loginState is UiState.Loading
    val isSuccess = loginState is UiState.Success
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
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Log in to your ClearLedger account.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                localError = null
            },
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
            onValueChange = {
                password = it
                localError = null
            },
            label = { Text("Password") },
            singleLine = true,
            enabled = canEdit,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (localError != null) {
            Text(
                text = localError!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val cleanEmail = email.trim()
                if (cleanEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    localError = "Enter a valid email address."
                    return@Button
                }
                if (password.isEmpty()) {
                    localError = "Password cannot be empty."
                    return@Button
                }
                onLogin(cleanEmail, password)
            },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoading) "Signing in…" else "Login"
            )
        }

        OutlinedButton(
            onClick = onNavigateRegister,
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create an account")
        }

        when (loginState) {
            UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }

            is UiState.Error -> {
                Text(
                    text = loginState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                Text(
                    text = "Login successful.",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            null -> Unit
        }
    }
}
