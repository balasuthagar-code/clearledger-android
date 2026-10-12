
package com.example.clearledger.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
    var editing by remember { mutableStateOf<IncomeEntry?>(null) }

    val visibleEntries = state.entries.filter { entry ->
        (state.selectedMonth.isBlank() ||
                entry.monthKey.contains(state.selectedMonth.trim())) &&
                (state.selectedSourceId.isBlank() ||
                        entry.sourceId == state.selectedSourceId)
    }.sortedByDescending { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Income History",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = {
                editing = null
                viewModel.clearError()
                showForm = true
            },
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text("Record Income")
        }

        OutlinedTextField(
            value = state.selectedMonth,
            onValueChange = viewModel::setMonthFilter,
            label = { Text("Filter month (YYYY-MM)") },
            placeholder = { Text("2026-10") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SourceSelector(
            sources = state.sources,
            selectedId = state.selectedSourceId,
            label = "Filter by source (All sources)",
            onSelected = viewModel::setSourceFilter,
            includeAll = true
        )

        state.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else if (visibleEntries.isEmpty()) {
            Text(
                text = "No income entries found. Record your first income.",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    IncomeEntryCard(
                        entry = entry,
                        onEdit = {
                            editing = entry
                            viewModel.clearError()
                            showForm = true
                        },
                        onDelete = {
                            viewModel.deleteEntry(entry.id)
                        }
                    )
                }
            }
        }
    }

    if (showForm) {
        IncomeEntryFormDialog(
            entry = editing,
            sources = state.sources,
            viewModel = viewModel,
            saving = state.isSaving,
            onDismiss = {
                showForm = false
                editing = null
            },
            onSave = { sourceId, amount, currency, rate, date, note, status ->
                viewModel.saveEntry(
                    id = editing?.id.orEmpty(),
                    sourceId = sourceId,
                    amount = amount,
                    currency = currency,
                    rate = rate,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = entry.sourceName,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            "${entry.currency} ${
                IncomeEntryValidator.formatAmount(entry.originalAmountMinor)
            }"
        )

        if (entry.currency != "LKR") {
            Text(
                "≈ LKR ${
                    IncomeEntryValidator.formatAmount(entry.baseAmountMinor)
                }"
            )
        }

        Text("Date: ${entry.date}")
        Text("Status: ${entry.status}")
        if (entry.note.isNotBlank()) Text("Note: ${entry.note}")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedButton(onClick = onEdit) {
                Text("Edit")
            }
            OutlinedButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun IncomeEntryFormDialog(
    entry: IncomeEntry?,
    sources: List<IncomeSource>,
    viewModel: IncomeEntriesViewModel,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String, String
    ) -> Unit
) {
    var sourceId by remember(entry?.id) {
        mutableStateOf(entry?.sourceId.orEmpty())
    }
    var amount by remember(entry?.id) {
        mutableStateOf(
            entry?.let {
                IncomeEntryValidator.formatAmount(it.originalAmountMinor)
            }.orEmpty()
        )
    }
    var currency by remember(entry?.id) {
        mutableStateOf(entry?.currency ?: "LKR")
    }
    var rate by remember(entry?.id) {
        mutableStateOf(entry?.exchangeRate ?: "1")
    }
    var date by remember(entry?.id) {
        mutableStateOf(entry?.date ?: LocalDate.now().toString())
    }
    var note by remember(entry?.id) {
        mutableStateOf(entry?.note.orEmpty())
    }
    var status by remember(entry?.id) {
        mutableStateOf(entry?.status ?: "EXPECTED")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (entry == null) "Record Income" else "Edit Income")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceSelector(
                    sources = sources.filter { it.active || it.id == sourceId },
                    selectedId = sourceId,
                    label = "Income source",
                    onSelected = { sourceId = it },
                    includeAll = false
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SimpleSelector(
                    label = "Currency",
                    selected = currency,
                    options = IncomeEntryValidator.supportedCurrencies,
                    onSelected = {
                        currency = it
                        rate = if (it == "LKR") "1"
                        else viewModel.lastRateFor(it)
                    }
                )

                if (currency != "LKR") {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Exchange rate (LKR per 1 $currency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                SimpleSelector(
                    label = "Status",
                    selected = status,
                    options = IncomeEntryValidator.statuses,
                    onSelected = { status = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    onSave(
                        sourceId, amount, currency, rate,
                        date, note, status
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

@Composable
private fun SourceSelector(
    sources: List<IncomeSource>,
    selectedId: String,
    label: String,
    onSelected: (String) -> Unit,
    includeAll: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = sources.firstOrNull { it.id == selectedId }?.name
        ?: if (includeAll && selectedId.isBlank()) "All sources"
        else "Select source"

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selectedName")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (includeAll) {
                DropdownMenuItem(
                    text = { Text("All sources") },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
            }

            sources.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Text(
                            source.name +
                                    if (source.active) "" else " (Inactive)"
                        )
                    },
                    onClick = {
                        onSelected(source.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SimpleSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selected")
        }

        DropdownMenu(
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
