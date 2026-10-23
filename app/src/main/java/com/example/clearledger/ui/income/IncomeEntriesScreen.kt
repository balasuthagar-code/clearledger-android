//
//package com.example.clearledger.ui.income
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.DropdownMenu
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.clearledger.domain.model.IncomeEntry
//import com.example.clearledger.domain.model.IncomeEntryOptions
//import com.example.clearledger.domain.model.IncomeSource
//import java.math.BigDecimal
//import java.text.NumberFormat
//import java.time.LocalDate
//import java.util.Locale
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun IncomeEntriesScreen(
//    onBack: () -> Unit,
//    viewModel: IncomeEntriesViewModel = viewModel()
//) {
//    val state = viewModel.state
//    var editingEntry by remember { mutableStateOf<IncomeEntry?>(null) }
//    var showEditor by remember { mutableStateOf(false) }
//    var pendingDelete by remember { mutableStateOf<IncomeEntry?>(null) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Income Entries") },
//                navigationIcon = {
//                    TextButton(onClick = onBack) {
//                        Text("Back")
//                    }
//                },
//                actions = {
//                    TextButton(onClick = { viewModel.refresh() }) {
//                        Text("Refresh")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Text(
//                "Record and manage your income",
//                style = MaterialTheme.typography.bodyLarge
//            )
//
//            if (state.error != null) {
//                Text(
//                    state.error,
//                    color = MaterialTheme.colorScheme.error
//                )
//            }
//
//            if (state.message != null) {
//                Text(
//                    state.message,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
//
//            Button(
//                onClick = {
//                    editingEntry = null
//                    showEditor = true
//                    viewModel.clearMessages()
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Add Income Entry")
//            }
//
//            if (state.isLoading) {
//                CircularProgressIndicator()
//            } else if (state.entries.isEmpty()) {
//                Text(
//                    "No income entries yet.",
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            } else {
//                LazyColumn(
//                    verticalArrangement = Arrangement.spacedBy(10.dp)
//                ) {
//                    items(state.entries, key = { it.id }) { entry ->
//                        val sourceName = state.sources
//                            .firstOrNull { it.id == entry.sourceId }
//                            ?.name ?: "Income source"
//
//                        IncomeEntryCard(
//                            entry = entry,
//                            sourceName = sourceName,
//                            onEdit = {
//                                editingEntry = entry
//                                showEditor = true
//                                viewModel.clearMessages()
//                            },
//                            onDelete = {
//                                pendingDelete = entry
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    if (showEditor) {
//        IncomeEntryEditorDialog(
//            entry = editingEntry,
//            sources = state.sources,
//            isSaving = state.isSaving,
//            onDismiss = { showEditor = false },
//            onSave = { sourceId, amount, currency, rate, date, note, status ->
//                viewModel.saveEntry(
//                    existing = editingEntry,
//                    sourceId = sourceId,
//                    amountText = amount,
//                    currency = currency,
//                    rateText = rate,
//                    date = date,
//                    note = note,
//                    status = status
//                )
//                // Close only after a save has been requested.
//                // If validation fails, keep the form open.
//                if (amount.toBigDecimalOrNull() != null &&
//                    sourceId.isNotBlank() &&
//                    date.isNotBlank() &&
//                    (currency == "LKR" ||
//                            (rate.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) ?: 0) > 0)
//                ) {
//                    showEditor = false
//                }
//            }
//        )
//    }
//
//    if (pendingDelete != null) {
//        AlertDialog(
//            onDismissRequest = { pendingDelete = null },
//            title = { Text("Delete income entry?") },
//            text = { Text("This action cannot be undone.") },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        pendingDelete?.let { viewModel.deleteEntry(it.id) }
//                        pendingDelete = null
//                    }
//                ) {
//                    Text("Delete")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { pendingDelete = null }) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//}
//
//@Composable
//private fun IncomeEntryCard(
//    entry: IncomeEntry,
//    sourceName: String,
//    onEdit: () -> Unit,
//    onDelete: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceContainer
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(6.dp)
//        ) {
//            Text(sourceName, style = MaterialTheme.typography.titleMedium)
//            Text(entry.date)
//            Text(
//                "${entry.originalCurrency} ${formatMinor(entry.originalAmountMinor)}",
//                style = MaterialTheme.typography.headlineSmall
//            )
//
//            if (entry.originalCurrency != "LKR") {
//                Text("LKR ${formatMinor(entry.baseAmountMinor)}")
//            }
//
//            Text("Status: ${entry.status.lowercase().replaceFirstChar { it.uppercase() }}")
//
//            if (entry.note.isNotBlank()) {
//                Text("Note: ${entry.note}")
//            }
//
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedButton(onClick = onEdit) {
//                    Text("Edit")
//                }
//                OutlinedButton(onClick = onDelete) {
//                    Text("Delete")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun IncomeEntryEditorDialog(
//    entry: IncomeEntry?,
//    sources: List<IncomeSource>,
//    isSaving: Boolean,
//    onDismiss: () -> Unit,
//    onSave: (
//        sourceId: String,
//        amount: String,
//        currency: String,
//        rate: String,
//        date: String,
//        note: String,
//        status: String
//    ) -> Unit
//) {
//    var sourceId by remember(entry?.id) {
//        mutableStateOf(entry?.sourceId ?: sources.firstOrNull()?.id.orEmpty())
//    }
//    var amount by remember(entry?.id) {
//        mutableStateOf(
//            entry?.let {
//                BigDecimal.valueOf(it.originalAmountMinor, 2).toPlainString()
//            } ?: ""
//        )
//    }
//    var currency by remember(entry?.id) {
//        mutableStateOf(entry?.originalCurrency ?: "LKR")
//    }
//    var rate by remember(entry?.id) {
//        mutableStateOf(entry?.exchangeRate ?: "1")
//    }
//    var date by remember(entry?.id) {
//        mutableStateOf(entry?.date ?: LocalDate.now().toString())
//    }
//    var note by remember(entry?.id) {
//        mutableStateOf(entry?.note ?: "")
//    }
//    var status by remember(entry?.id) {
//        mutableStateOf(entry?.status ?: "EXPECTED")
//    }
//    var formError by remember(entry?.id) { mutableStateOf<String?>(null) }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = {
//            Text(if (entry == null) "Add Income Entry" else "Edit Income Entry")
//        },
//        text = {
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                if (sources.isEmpty()) {
//                    Text(
//                        "Create an active income source first.",
//                        color = MaterialTheme.colorScheme.error
//                    )
//                } else {
//                    SimpleDropdown(
//                        label = "Income source",
//                        selected = sources.firstOrNull { it.id == sourceId }?.name
//                            ?: "Select source",
//                        options = sources.map { it.name },
//                        onSelect = { selectedName ->
//                            sourceId = sources.first { it.name == selectedName }.id
//                        }
//                    )
//                }
//
//                OutlinedTextField(
//                    value = amount,
//                    onValueChange = { amount = it },
//                    label = { Text("Amount") },
//                    placeholder = { Text("e.g. 25000.00") },
//                    singleLine = true,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                SimpleDropdown(
//                    label = "Currency",
//                    selected = currency,
//                    options = IncomeEntryOptions.currencies,
//                    onSelect = {
//                        currency = it
//                        if (it == "LKR") rate = "1"
//                        else if (rate == "1") rate = ""
//                    }
//                )
//
//                if (currency != "LKR") {
//                    OutlinedTextField(
//                        value = rate,
//                        onValueChange = { rate = it },
//                        label = { Text("Exchange rate (LKR per 1 $currency)") },
//                        singleLine = true,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//
//                OutlinedTextField(
//                    value = date,
//                    onValueChange = { date = it },
//                    label = { Text("Date (YYYY-MM-DD)") },
//                    placeholder = { Text("2026-10-24") },
//                    singleLine = true,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                SimpleDropdown(
//                    label = "Status",
//                    selected = status,
//                    options = IncomeEntryOptions.statuses,
//                    onSelect = { status = it }
//                )
//
//                OutlinedTextField(
//                    value = note,
//                    onValueChange = { note = it },
//                    label = { Text("Note (optional)") },
//                    modifier = Modifier.fillMaxWidth(),
//                    minLines = 2
//                )
//
//                if (formError != null) {
//                    Text(
//                        formError!!,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//            }
//        },
//        confirmButton = {
//            TextButton(
//                enabled = !isSaving && sources.isNotEmpty(),
//                onClick = {
//                    val amountValue = amount.toBigDecimalOrNull()
//                    val rateValue = rate.toBigDecimalOrNull()
//                    val dateValid = try {
//                        LocalDate.parse(date)
//                        true
//                    } catch (_: Exception) {
//                        false
//                    }
//
//                    formError = when {
//                        sourceId.isBlank() -> "Select an income source."
//                        amountValue == null || amountValue <= BigDecimal.ZERO ->
//                            "Enter an amount greater than zero."
//                        currency != "LKR" &&
//                                (rateValue == null || rateValue <= BigDecimal.ZERO) ->
//                            "Enter a valid exchange rate."
//                        !dateValid -> "Enter the date as YYYY-MM-DD."
//                        else -> null
//                    }
//
//                    if (formError == null) {
//                        onSave(
//                            sourceId,
//                            amount,
//                            currency,
//                            if (currency == "LKR") "1" else rate,
//                            date,
//                            note,
//                            status
//                        )
//                    }
//                }
//            ) {
//                Text(if (isSaving) "Saving..." else "Save")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
//    )
//}
//
//@Composable
//private fun SimpleDropdown(
//    label: String,
//    selected: String,
//    options: List<String>,
//    onSelect: (String) -> Unit
//) {
//    var expanded by remember { mutableStateOf(false) }
//
//    Column {
//        Text(label, style = MaterialTheme.typography.labelMedium)
//        Spacer(Modifier.height(4.dp))
//
//        OutlinedButton(
//            onClick = { expanded = true },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(selected)
//        }
//
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            options.forEach { option ->
//                DropdownMenuItem(
//                    text = { Text(option) },
//                    onClick = {
//                        onSelect(option)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//private fun formatMinor(amountMinor: Long): String {
//    return NumberFormat.getNumberInstance(Locale.US).apply {
//        minimumFractionDigits = 2
//        maximumFractionDigits = 2
//    }.format(BigDecimal.valueOf(amountMinor, 2))
//}
