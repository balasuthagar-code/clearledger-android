package com.example.clearledger.ui.freelance

import com.example.clearledger.currency.CurrencyRateField
import com.example.clearledger.currency.currencyCompletionDependencies
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.core.common.FreelanceProjectValidator
import com.example.clearledger.data.repository.FreelanceProjectRepository
import com.example.clearledger.domain.model.FreelanceProject
import java.math.BigDecimal
import java.time.LocalDate


@Composable
fun FreelanceProjectsScreen() {
    val currencyDeps = currencyCompletionDependencies()
    val currencyOwner = remember { currencyDeps.account().currentUid() }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val repository = remember {
        FreelanceProjectRepository()
    }

    var projects by remember { mutableStateOf<List<FreelanceProject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteProject by remember { mutableStateOf<FreelanceProject?>(null) }

    DisposableEffect(repository) {
        val registration = repository.listenToProjects(
            onSuccess = { result ->
                projects = result
                loading = false
                errorMessage = null
            },
            onError = { error ->
                loading = false
                errorMessage = error
            }
        )

        onDispose {
            registration?.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Freelance Projects",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage clients, project amounts and milestones.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Project")
        }

        Spacer(modifier = Modifier.height(12.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (loading) {
            CircularProgressIndicator()
        } else if (projects.isEmpty()) {
            Text("No freelance projects yet. Add your first project.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = projects,
                    key = { it.id }
                ) { project ->
                    FreelanceProjectCard(
                        project = project,
                        onDelete = { deleteProject = project }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddFreelanceProjectDialog(
            saveError = errorMessage,
            isSaving = saving,
            onDismiss = { showAddDialog = false },
            onSave = { name, client, amount, currency, rate, notes ->
                if (saving) return@AddFreelanceProjectDialog
                if (!currencyDeps.recorder().isOwner(currencyOwner)) {
                    errorMessage = "Account changed. Reopen this form."
                    return@AddFreelanceProjectDialog
                }
                val validationError = FreelanceProjectValidator.validate(
                    name = name,
                    client = client,
                    agreedAmount = amount,
                    currency = currency,
                    rate = rate
                )

                if (validationError != null) {
                    errorMessage = validationError
                    return@AddFreelanceProjectDialog
                }

                val values = FreelanceProjectValidator.convert(
                    agreedAmount = amount,
                    currency = currency,
                    rate = rate
                )

                if (values == null) {
                    errorMessage = "Please check the project amount and exchange rate."
                    return@AddFreelanceProjectDialog
                }

                val remembered = currencyDeps.recorder().capture(currency, "LKR", values.exchangeRate, currencyOwner)
                saving = true
                repository.addProject(
                    name = name,
                    client = client,
                    agreedAmountMinor = values.agreedAmountMinor,
                    currency = currency,
                    exchangeRate = values.exchangeRate,
                    baseAmountMinor = values.baseAmountMinor,
                    notes = notes,
                    onSuccess = {
                        saving = false
                        scope.launch { currencyDeps.recorder().afterSave(remembered) }
                        errorMessage = null
                        showAddDialog = false
                    },
                    onError = { message ->
                        saving = false
                        errorMessage = message
                    }
                )
            }
        )
    }

    deleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deleteProject = null },
            title = { Text("Delete project?") },
            text = {
                Text("Are you sure you want to delete ${project.name}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteProject(
                            projectId = project.id,
                            onSuccess = {
                                deleteProject = null
                                errorMessage = null
                            },
                            onError = { message ->
                                errorMessage = message
                                deleteProject = null
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteProject = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FreelanceProjectCard(
    project: FreelanceProject,
    onDelete: () -> Unit
) {
    if (runCatching { project.completedAmountMinor }.isFailure) {
        Text("Milestone total needs review: ${project.name}")
        return
    }
    val today = LocalDate.now().toString()
    val overdueCount = project.milestones.count { it.isOverdue(today) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Client: ${project.client}")

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Agreed amount: ${
                    formatMinorAmount(project.agreedAmountMinor)
                } ${project.currency}"
            )

            Text(
                "Base amount (LKR): ${
                    formatMinorAmount(project.baseAmountMinor)
                }"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Progress: ${(project.progress * 100).toInt()}%")

            androidx.compose.material3.LinearProgressIndicator(
                progress = { project.progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Milestones completed: ${
                    project.milestones.count {
                        it.status == "Completed"
                    }
                } / ${project.milestones.size}"
            )

            if (overdueCount > 0) {
                Text(
                    text = "Overdue milestones: $overdueCount",
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (project.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Notes: ${project.notes}")
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
private fun AddFreelanceProjectDialog(
    saveError: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        client: String,
        amount: String,
        currency: String,
        rate: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var rate by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Freelance Project") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = client,
                        onValueChange = { client = it },
                        label = { Text("Client name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Agreed amount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        OutlinedButton(
                            onClick = {
                                currencyMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Currency: $currency")
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = {
                                currencyMenuExpanded = false
                            }
                        ) {
                            listOf("LKR", "USD", "EUR", "GBP", "AUD", "INR")
                                .forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            currency = option
                                            rate = if (option == "LKR") "1" else ""
                                            currencyMenuExpanded = false
                                        }
                                    )
                                }
                        }
                    }
                }

                item {
                    CurrencyRateField(original = currency, value = rate, onChange = { rate = it }, enabled = !isSaving)
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
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
                enabled = !isSaving,
                onClick = {
                    val error = FreelanceProjectValidator.validate(
                        name = name,
                        client = client,
                        agreedAmount = amount,
                        currency = currency,
                        rate = rate
                    )

                    if (error != null) {
                        validationError = error
                    } else {
                        onSave(
                            name,
                            client,
                            amount,
                            currency,
                            rate,
                            notes
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

private fun formatMinorAmount(amountMinor: Long): String {
    return BigDecimal.valueOf(amountMinor, 2)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString()
}
 // thisanth