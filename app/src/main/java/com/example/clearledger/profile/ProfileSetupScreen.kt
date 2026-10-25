package com.example.clearledger.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.core.common.Currencies

@Composable
fun ProfileSetupScreen(state: ProfileSetupState, onName: (String) -> Unit, onCurrency: (String) -> Unit,
    onSave: () -> Unit, onRetry: () -> Unit, onReload: () -> Unit, onLogout: () -> Unit,
    editMode: Boolean = false, onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (editMode) "Your profile" else "Set up your profile", style = MaterialTheme.typography.headlineMedium)
        when (state) {
            ProfileSetupState.Loading -> CircularProgressIndicator()
            is ProfileSetupState.Editing, is ProfileSetupState.Saving -> {
                val draft = when (state) {
                    is ProfileSetupState.Editing -> state.draft
                    is ProfileSetupState.Saving -> state.draft
                    else -> error("Unexpected state")
                }
                val enabled = state is ProfileSetupState.Editing
                Text("Email: ${draft.email}")
                OutlinedTextField(draft.displayName, onName, label = { Text("Display name") }, singleLine = true,
                    enabled = enabled, isError = draft.nameError != null, modifier = Modifier.fillMaxWidth(),
                    supportingText = { draft.nameError?.let { Text(it) } })
                var expanded by remember { mutableStateOf(false) }
                Text("Base currency")
                Box {
                    OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(draft.baseCurrency) }
                    DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                        Currencies.supported.forEach { code ->
                            DropdownMenuItem(text = { Text(code) }, onClick = { onCurrency(code); expanded = false })
                        }
                    }
                }
                Text("Choose carefully. Account records, including categories, prevent later currency changes.")
                Button(onClick = onSave, enabled = enabled) { Text(if (enabled) "Save and continue" else "Saving…") }
                if (!enabled) CircularProgressIndicator()
            }
            is ProfileSetupState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text(if (state.draft != null && !state.saved) "Retry / edit draft" else "Retry") }
                if (state.draft != null && !state.saved) TextButton(onClick = onReload) { Text("Reload saved profile (discard draft)") }
            }
            is ProfileSetupState.Completed -> Text("Profile saved")
            ProfileSetupState.SignedOut -> Text("Signed out")
        }
        if (editMode) TextButton(onClick = onBack, enabled = state !is ProfileSetupState.Saving) { Text("Back") }
        TextButton(onClick = onLogout) { Text("Log out") }
    }
}
