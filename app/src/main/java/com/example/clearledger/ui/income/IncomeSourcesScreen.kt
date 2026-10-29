
package com.example.clearledger.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.data.repository.IncomeSourceSummary
import com.example.clearledger.domain.model.IncomeSource
import com.example.clearledger.domain.model.IncomeSourceTypes
import com.example.clearledger.domain.model.SupportedCurrencies
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSourcesScreen(
    viewModel: IncomeSourcesViewModel,
    onBack: () -> Unit = {},
    onOpenEntries: () -> Unit = {}
) {
    val state = viewModel.state

    var showEditor by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<IncomeSource?>(null) }
    var deletingSource by remember { mutableStateOf<IncomeSource?>(null) }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            showEditor = false
            editingSource = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income Sources") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Manage where your income comes from",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    editingSource = null
                    viewModel.clearMessages()
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Income Source")
            }

            TextButton(
                onClick = onOpenEntries,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage income entries")
            }

            Spacer(Modifier.height(12.dp))

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.isLoading && state.summaries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                }
            } else if (state.summaries.isEmpty()) {
                Text("No income sources yet. Add your first source.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.summaries,
                        key = { it.source.id }
                    ) { summary ->
                        IncomeSourceCard(
                            summary = summary,
                            onEdit = {
                                editingSource = summary.source
                                viewModel.clearMessages()
                                showEditor = true
                            },
                            onDelete = {
                                deletingSource = summary.source
                            },
                            onToggleActive = {
                                viewModel.toggleActive(summary.source)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        IncomeSourceEditorDialog(
            existing = editingSource,
            onDismiss = {
                showEditor = false
                editingSource = null
            },
            onSave = viewModel::saveSource
        )
    }

    deletingSource?.let { source ->
        AlertDialog(
            onDismissRequest = { deletingSource = null },
            title = { Text("Delete income source?") },
            text = {
                Text(
                    "Delete \"${source.name}\"? If this source has recorded " +
                            "income entries, it cannot be deleted. Deactivate it instead."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSource(source.id)
                        deletingSource = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSource = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IncomeSourceCard(
    summary: IncomeSourceSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val source = summary.source

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "${source.defaultCurrency} · ${sourceTypeLabel(source)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (source.isActive) "Active" else "Inactive",
                    color = if (source.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(14.dp))

            if (summary.amountEntryCount > 0) {
                Text(
                    text = formatMoney(summary.total, source.defaultCurrency),
                    style = MaterialTheme.typography.headlineSmall
                )

                Text("${summary.entryCount} income entries")
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (source.isActive) "Active source" else "Inactive source")

                Switch(
                    checked = source.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }
        }
    }
}

@Composable
private fun IncomeSourceEditorDialog(
    existing: IncomeSource?,
    onDismiss: () -> Unit,
    onSave: (IncomeSource) -> Unit
) {
    var name by remember(existing?.id) {
        mutableStateOf(existing?.name.orEmpty())
    }
    var type by remember(existing?.id) {
        mutableStateOf(existing?.type ?: "SALARY")
    }
    var customType by remember(existing?.id) {
        mutableStateOf(existing?.customType.orEmpty())
    }
    var currency by remember(existing?.id) {
        mutableStateOf(existing?.defaultCurrency ?: "LKR")
    }
    var active by remember(existing?.id) {
        mutableStateOf(existing?.isActive ?: true)
    }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add Income Source" else "Edit Income Source")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Source name") },
                    placeholder = { Text("e.g. Salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                SimpleDropdown(
                    label = "Source type",
                    selected = type,
                    options = IncomeSourceTypes.values,
                    onSelect = {
                        type = it
                        localError = null
                        if (it != "OTHER") customType = ""
                    }
                )

                if (type == "OTHER") {
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text("Specify other source type") },
                        placeholder = { Text("e.g. Rental income") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SimpleDropdown(
                    label = "Default currency",
                    selected = currency,
                    options = SupportedCurrencies.values,
                    onSelect = { currency = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active")
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it }
                    )
                }

                localError?.let {
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
                    if (name.isBlank()) {
                        localError = "Please enter a source name."
                    } else if (type == "OTHER" && customType.isBlank()) {
                        localError = "Please specify the other source type."
                    } else {
                        onSave(
                            IncomeSource(
                                id = existing?.id.orEmpty(),
                                userId = existing?.userId.orEmpty(),
                                name = name.trim(),
                                type = type,
                                customType = customType.trim(),
                                defaultCurrency = currency,
                                isActive = active,
                                createdAt = existing?.createdAt
                                    ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
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

@Composable
private fun SimpleDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selected.replace("_", " "),
                    modifier = Modifier.weight(1f)
                )
                Text("⌄")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replace("_", " ")) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun sourceTypeLabel(source: IncomeSource): String {
    return if (source.type == "OTHER" && source.customType.isNotBlank()) {
        source.customType
    } else {
        source.type.lowercase()
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }
    }
}

private fun formatMoney(amount: Double, currency: String): String {
    val locale = when (currency) {
        "LKR" -> Locale("en", "LK")
        "USD" -> Locale.US
        "EUR" -> Locale.GERMANY
        "GBP" -> Locale.UK
        "AUD" -> Locale("en", "AU")
        "INR" -> Locale("en", "IN")
        else -> Locale.US
    }

    return try {
        NumberFormat.getCurrencyInstance(locale).format(amount)
    } catch (_: Exception) {
        "$currency ${"%.2f".format(amount)}"
    }
}
