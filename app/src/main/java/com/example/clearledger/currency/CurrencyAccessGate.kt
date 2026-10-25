package com.example.clearledger.currency

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The financial NavHost is not composed until this policy allows it. No overlay listeners. */
@Composable
fun CurrencyAccessGate(content: @Composable () -> Unit) {
    val vm: CurrencyAccessViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    when (val current = state) {
        CurrencyAccess.SignedOut -> if (vm.isSignedOut()) key("signed-out") { content() } else CircularProgressIndicator()
        is CurrencyAccess.Ready -> if (vm.isOwner(current.uid)) key(current.uid) { content() } else CircularProgressIndicator()
        else -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Base currency", style = MaterialTheme.typography.headlineSmall)
            when (current) {
                CurrencyAccess.Checking -> { CircularProgressIndicator(); Text("Checking currency and history…") }
                is CurrencyAccess.Setup -> {
                    Text("LKR — Sri Lankan Rupee")
                    Text("Set up your base currency to continue.")
                    Button(onClick = { vm.setup(current.uid) }) { Text("Continue with LKR") }
                }
                is CurrencyAccess.Blocked -> Text(current.message)
                else -> Unit
            }
            TextButton(onClick = vm::retry) { Text("Retry") }
            TextButton(onClick = vm::logout) { Text("Log out") }
        }
    }
}
