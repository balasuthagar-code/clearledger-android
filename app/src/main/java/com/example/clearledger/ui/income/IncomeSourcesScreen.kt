
package com.example.clearledger.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.clearledger.domain.model.IncomeSource
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox

private val incomeSourceTypes = listOf(
    "SALARY",
    "FREELANCE",
    "BUSINESS",
    "INVESTMENT",
    "OTHER"
)

private val incomeCurrencies = listOf(
    "LKR",
    "USD",
    "EUR",
    "GBP",
    "AUD",
    "INR"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSourcesScreen(
    viewModel: IncomeSourcesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<IncomeSource?>(null) }

    var sourceName by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("SALARY") }
    var sourceCurrency by remember { mutableStateOf("LKR") }
    var sourceActive by remember { mutableStateOf(true) }

    fun openAddDialog() {
        editingSource = null
        sourceName = ""
        sourceType = "SALARY"
        sourceCurrency = "LKR"
        sourceActive = true
        viewModel.clearError()
        viewModel.clearMessage()
        showDialog = true
    }

    fun openEditDialog(source: IncomeSource) {
        editingSource = source
        sourceName = source.name
        sourceType = source.type
        sourceCurrency = source.defaultCurrency
        sourceActive = source.active
        viewModel.clearError()
        viewModel.clearMessage()
        showDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income Sources") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manage where your income comes from.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = { openAddDialog() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Income Source")
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading income sources...",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                state.incomeSources.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No income sources yet.")
                        Text(
                            text = "Add your salary, freelance work, or other income.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.incomeSources,
                            key = { it.id }
                        ) { source ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = source.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text("Type: ${source.type}")
                                    Text("Default currency: ${source.defaultCurrency}")
                                    Text(
                                        text = if (source.active) {
                                            "Status: Active"
                                        } else {
                                            "Status: Inactive"
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                openEditDialog(source)
                                            }
                                        ) {
                                            Text("Edit")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                viewModel.updateIncomeSource(
                                                    source.copy(active = !source.active)
                                                )
                                            },
                                            enabled = !state.isSaving
                                        ) {
                                            Text(
                                                if (source.active) {
                                                    "Deactivate"
                                                } else {
                                                    "Activate"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    showDialog = false
                    viewModel.clearError()
                }
            },
            title = {
                Text(
                    if (editingSource == null) {
                        "Add Income Source"
                    } else {
                        "Edit Income Source"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("Source name") },
                        placeholder = { Text("e.g. Salary") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    )

                    SourceDropdown(
                        label = "Income type",
                        selectedValue = sourceType,
                        options = incomeSourceTypes,
                        onSelected = { sourceType = it },
                        enabled = !state.isSaving
                    )

                    SourceDropdown(
                        label = "Default currency",
                        selectedValue = sourceCurrency,
                        options = incomeCurrencies,
                        onSelected = { sourceCurrency = it },
                        enabled = !state.isSaving
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Active")
                        androidx.compose.material3.Switch(
                            checked = sourceActive,
                            onCheckedChange = { sourceActive = it },
                            enabled = !state.isSaving
                        )
                    }

                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.isSaving && sourceName.isNotBlank(),
                    onClick = {
                        val existingSource = editingSource

                        val source = IncomeSource(
                            id = existingSource?.id ?: "",
                            name = sourceName.trim(),
                            type = sourceType,
                            defaultCurrency = sourceCurrency,
                            active = sourceActive
                        )

                        if (existingSource == null) {
                            viewModel.addIncomeSource(source)
                        } else {
                            viewModel.updateIncomeSource(source)
                        }

                        // The dialog stays open while saving.
                        // Close it when the ViewModel reports success.
                    }
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        showDialog = false
                        viewModel.clearError()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Close the dialog after a successful save.
    androidx.compose.runtime.LaunchedEffect(state.isSaving, state.message) {
        if (showDialog && !state.isSaving && state.message != null) {
            showDialog = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(text = label)
            },
            trailingIcon = {
                Text(text = if (expanded) "▲" else "▼")
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
