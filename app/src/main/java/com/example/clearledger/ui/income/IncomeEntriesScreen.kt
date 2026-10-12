package com.example.clearledger.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.core.common.IncomeEntryValidator
import com.example.clearledger.domain.model.IncomeEntry
import com.example.clearledger.domain.model.IncomeSource
import java.time.LocalDate

@Composable
fun IncomeEntriesScreen(
    viewModel: IncomeEntriesViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<IncomeEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Income History",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.selectedMonth,
            onValueChange = viewModel::filterByMonth,
            label = { Text("Filter month (YYYY-MM)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        SourceFilter(
            sources = state.sources,
            selectedId = state.selectedSourceId,
            onSelect = viewModel::filterBySource
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                editingEntry = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Record Income")
        }

        state.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.isLoading) {
            Text("Loading income entries...")
        } else if (state.entries.isEmpty()) {
            Text(
                "No income entries found.",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    IncomeEntryCard(
                        entry = entry,
                        onEdit = {
                            editingEntry = entry
                            showForm = true
                        },
                        onDelete = {
                            viewModel.deleteIncomeEntry(entry.id)
                        }
                    )
                }
            }
        }
    }

    if (showForm) {
        IncomeEntryForm(
            sources = state.sources.filter { it.active },
            initialEntry = editingEntry,
            saving = state.isSaving,
            onDismiss = {
                showForm = false
                editingEntry = null
                viewModel.clearError()
            },
            onSave = { sourceId, amount, currency, rate, date, note, status ->
                viewModel.saveIncomeEntry(
                    entryId = editingEntry?.id,
                    sourceId = sourceId,
                    amount = amount,
                    currency = currency,
                    exchangeRate = rate,
                    date = date,
                    note = note,
                    status = status
                )
            }
        )
    }
}

@Composable
private fun IncomeEntryCard(
    entry: IncomeEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                entry.sourceName,
                style = MaterialTheme.typography.titleMedium
            )
            Text("${entry.currency} ${IncomeEntryValidator.formatAmount(entry.originalAmountMinor)}")
            Text("Base amount (LKR): ${IncomeEntryValidator.formatAmount(entry.baseAmountMinor)}")
            Text("Date: ${entry.date}")
            Text("Status: ${entry.status}")
            if (entry.note.isNotBlank()) {
                Text("Note: ${entry.note}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceFilter(
    sources: List<IncomeSource>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = sources.firstOrNull { it.id == selectedId }?.name
        ?: "All income sources"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Income source filter") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All income sources") },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            sources.forEach { source ->
                DropdownMenuItem(
                    text = { Text(source.name) },
                    onClick = {
                        onSelect(source.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeEntryForm(
    sources: List<IncomeSource>,
    initialEntry: IncomeEntry?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String, String
    ) -> Unit
) {
    var sourceId by remember(initialEntry, sources) {
        mutableStateOf(
            initialEntry?.sourceId ?: sources.firstOrNull()?.id.orEmpty()
        )
    }
    var amount by remember(initialEntry) {
        mutableStateOf(
            initialEntry?.let {
                IncomeEntryValidator.formatAmount(it.originalAmountMinor)
            }.orEmpty()
        )
    }
    var currency by remember(initialEntry) {
        mutableStateOf(initialEntry?.currency ?: "LKR")
    }
    var rate by remember(initialEntry) {
        mutableStateOf(initialEntry?.exchangeRate ?: "1")
    }
    var date by remember(initialEntry) {
        mutableStateOf(initialEntry?.date ?: LocalDate.now().toString())
    }
    var note by remember(initialEntry) {
        mutableStateOf(initialEntry?.note.orEmpty())
    }
    var status by remember(initialEntry) {
        mutableStateOf(initialEntry?.status ?: "EXPECTED")
    }
    var sourceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEntry == null) "Record Income" else "Edit Income")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded,
                        onExpandedChange = { sourceExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = sources.firstOrNull { it.id == sourceId }?.name
                                ?: "Select source",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Income source") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = sourceExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false }
                        ) {
                            sources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.name) },
                                    onClick = {
                                        sourceId = source.id
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = currencyExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            IncomeEntryValidator.supportedCurrencies.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        currency = item
                                        if (item == "LKR") rate = "1"
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Exchange rate (to LKR)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = currency != "LKR"
                    )
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Income status") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = statusExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            IncomeEntryValidator.statuses.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (item == "EXPECTED") "Expected"
                                            else "Received"
                                        )
                                    },
                                    onClick = {
                                        status = item
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && sources.isNotEmpty(),
                onClick = {
                    onSave(
                        sourceId,
                        amount,
                        currency,
                        rate,
                        date,
                        note,
                        status
                    )
                }
            ) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
