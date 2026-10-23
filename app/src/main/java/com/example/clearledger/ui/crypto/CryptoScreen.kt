package com.example.clearledger.ui.crypto

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.CryptoEntryValidator
import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryOptions
import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun CryptoScreen(
    onBack: () -> Unit,
    viewModel: CryptoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<CryptoEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cryptocurrency",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Text(
            text = "Record crypto transactions and realised profit/loss.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Crypto Entry")
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.entries.isEmpty() -> {
                Text("No cryptocurrency entries yet. Add your first entry.")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.entries,
                        key = { it.id }
                    ) { entry ->
                        CryptoEntryCard(
                            entry = entry,
                            onDelete = { entryToDelete = entry }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCryptoEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { asset, date, type, amount, currency, rate, costBasis, note ->
                val previousError = state.error

                viewModel.addEntry(
                    assetName = asset,
                    date = date,
                    type = type,
                    originalValue = amount,
                    currency = currency,
                    rate = rate,
                    costBasisLkr = costBasis,
                    note = note
                )

                // The ViewModel validates and reports errors in uiState.
                // Close the dialog only when the local validation succeeds.
                val localError = CryptoEntryValidator.validate(
                    assetName = asset,
                    date = date,
                    type = type,
                    originalValue = amount,
                    currency = currency,
                    rate = rate,
                    costBasisLkr = costBasis
                )

                if (localError == null && previousError == null) {
                    showAddDialog = false
                } else if (localError == null) {
                    showAddDialog = false
                }
            }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete entry?") },
            text = {
                Text("Delete the ${entry.assetName} entry dated ${entry.date}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry.id)
                        entryToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CryptoEntryCard(
    entry: CryptoEntry,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = entry.assetName,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Date: ${entry.date}")
            Text("Type: ${entry.type}")

            Text(
                "Original value: ${formatMinor(entry.originalValueMinor)} " +
                        entry.originalCurrency
            )

            if (entry.originalCurrency != "LKR") {
                Text(
                    "LKR equivalent: LKR ${formatMinor(entry.baseValueMinor)}"
                )
            }

            val profitLoss = entry.realisedProfitLossMinor

            if (profitLoss != null) {
                val resultColor = when {
                    profitLoss > 0L -> MaterialTheme.colorScheme.primary
                    profitLoss < 0L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Text(
                    text = "Realised P/L: LKR ${formatMinor(profitLoss)}",
                    color = resultColor,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (entry.note.isNotBlank()) {
                Text("Note: ${entry.note}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AddCryptoEntryDialog(
    onDismiss: () -> Unit,
    onSave: (
        asset: String,
        date: String,
        type: String,
        amount: String,
        currency: String,
        rate: String,
        costBasis: String,
        note: String
    ) -> Unit
) {
    var asset by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var type by remember { mutableStateOf(CryptoEntryType.BUY) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var rate by remember { mutableStateOf("1") }
    var costBasis by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Crypto Entry") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = asset,
                        onValueChange = { asset = it },
                        label = { Text("Asset name (e.g. Bitcoin)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        OutlinedButton(
                            onClick = { typeMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Type: $type")
                        }

                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            CryptoEntryType.values.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        type = option
                                        typeMenuExpanded = false
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
                        label = { Text("Original value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        OutlinedButton(
                            onClick = { currencyMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Currency: $currency")
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            CryptoEntryOptions.currencies.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        currency = option
                                        if (option == "LKR") rate = "1"
                                        currencyMenuExpanded = false
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
                        label = { Text("Exchange rate to LKR") },
                        enabled = currency != "LKR",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == CryptoEntryType.SELL ||
                    type == CryptoEntryType.RESULT
                ) {
                    item {
                        OutlinedTextField(
                            value = costBasis,
                            onValueChange = { costBasis = it },
                            label = { Text("Cost basis (LKR)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    validationError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = CryptoEntryValidator.validate(
                        assetName = asset,
                        date = date,
                        type = type,
                        originalValue = amount,
                        currency = currency,
                        rate = rate,
                        costBasisLkr = costBasis
                    )

                    if (validationError == null) {
                        onSave(
                            asset,
                            date,
                            type,
                            amount,
                            currency,
                            rate,
                            costBasis,
                            note
                        )
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

private fun formatMinor(value: Long): String {
    return BigDecimal.valueOf(value, 2)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString()
}
//thisanth