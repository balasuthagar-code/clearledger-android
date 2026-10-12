
package com.example.clearledger.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.domain.model.IncomeSource

@Composable
fun IncomeSourcesScreen(
    viewModel: IncomeSourcesViewModel
) {
    val state by viewModel.uiState.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<IncomeSource?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Income Sources",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    editingSource = null
                    showForm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Income Source")
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )

                TextButton(onClick = viewModel::clearError) {
                    Text("Dismiss")
                }
            }

            if (state.isLoading && state.sources.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (state.sources.isEmpty()) {
                Text(
                    text = "No income sources yet. Add your first source.",
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.sources,
                        key = { it.id }
                    ) { source ->
                        IncomeSourceCard(
                            source = source,
                            onEdit = {
                                editingSource = source
                                showForm = true
                            },
                            onToggleActive = {
                                viewModel.toggleActive(source)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        IncomeSourceFormDialog(
            source = editingSource,
            onDismiss = {
                showForm = false
                editingSource = null
            },
            onSave = { name, type, currency ->
                viewModel.saveSource(
                    id = editingSource?.id.orEmpty(),
                    name = name,
                    type = type,
                    currency = currency
                )

                showForm = false
                editingSource = null
            }
        )
    }
}

@Composable
private fun IncomeSourceCard(
    source: IncomeSource,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Type: ${source.type}")
            Text("Default currency: ${source.defaultCurrency}")
            Text(if (source.active) "Status: Active" else "Status: Inactive")

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Text("Edit")
                }

                OutlinedButton(onClick = onToggleActive) {
                    Text(if (source.active) "Deactivate" else "Activate")
                }
            }
        }
    }
}

@Composable
private fun IncomeSourceFormDialog(
    source: IncomeSource?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, currency: String) -> Unit
) {
    var name by remember(source?.id) {
        mutableStateOf(source?.name.orEmpty())
    }
    var type by remember(source?.id) {
        mutableStateOf(source?.type.orEmpty())
    }
    var currency by remember(source?.id) {
        mutableStateOf(source?.defaultCurrency ?: "LKR")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (source == null) "Add Income Source" else "Edit Income Source")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type (e.g. Salary)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    label = { Text("Default currency (e.g. LKR)") },
                    singleLine = true
                )

                validationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        name.isBlank() ->
                            validationError = "Enter a source name."

                        type.isBlank() ->
                            validationError = "Enter a source type."

                        currency.trim().length != 3 ->
                            validationError = "Enter a 3-letter currency code."

                        else -> {
                            onSave(name.trim(), type.trim(), currency.trim())
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
