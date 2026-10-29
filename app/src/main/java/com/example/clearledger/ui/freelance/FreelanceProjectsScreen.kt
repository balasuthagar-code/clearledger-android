package com.example.clearledger.ui.freelance

import com.example.clearledger.currency.CurrencyRateField
import com.example.clearledger.currency.currencyCompletionDependencies
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.foundation.clickable
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
import com.example.clearledger.domain.model.FreelanceProjectStatus
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
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteProject by remember { mutableStateOf<FreelanceProject?>(null) }
    var paymentProject by remember { mutableStateOf<FreelanceProject?>(null) }
    var statusMenuProjectId by remember { mutableStateOf<String?>(null) }
    var updatingStatusProjectId by remember { mutableStateOf<String?>(null) }

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
        successMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
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
                        ,onPayment = { paymentProject = project }
                        ,statusMenuExpanded = statusMenuProjectId == project.id
                        ,isUpdatingStatus = updatingStatusProjectId == project.id
                        ,onStatusMenuToggle = {
                            if (updatingStatusProjectId == null) {
                                statusMenuProjectId = project.id
                            }
                        }
                        ,onStatusMenuDismiss = { statusMenuProjectId = null }
                        ,onStatusSelected = { status ->
                            statusMenuProjectId = null
                            updatingStatusProjectId = project.id
                            repository.updateProjectStatus(
                                projectId = project.id,
                                status = status,
                                onSuccess = {
                                    updatingStatusProjectId = null
                                    successMessage = "Project status updated."
                                },
                                onError = { message ->
                                    updatingStatusProjectId = null
                                    successMessage = null
                                    errorMessage = message
                                }
                            )
                        }
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
            onSave = { name, client, contact, description, startDate, endDate, work, status,
                       amount, currency, rate, notes ->
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
                val detailsError = FreelanceProjectValidator.validateDetails(
                    startDate, endDate, status, work
                )

                if (validationError != null || detailsError != null) {
                    errorMessage = validationError ?: detailsError
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
                    clientContact = contact,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    workDescription = work,
                    status = status,
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

    paymentProject?.let { project ->
        FreelancePaymentDialog(
            project = project,
            isSaving = saving,
            onDismiss = { paymentProject = null },
            onSave = { amount, date, note ->
                val error = FreelanceProjectValidator.validatePayment(amount, date)
                if (error != null) {
                    errorMessage = error
                } else {
                    val amountMinor = runCatching {
                        BigDecimal(amount).movePointRight(2).longValueExact()
                    }.getOrNull()
                    if (amountMinor == null) {
                        errorMessage = "Enter a valid payment amount."
                    } else {
                        saving = true
                        scope.launch {
                            runCatching {
                                repository.recordPayment(project, amountMinor, date, note)
                            }.onSuccess {
                                saving = false
                                paymentProject = null
                                errorMessage = null
                            }.onFailure {
                                saving = false
                                errorMessage = it.message ?: "Unable to record payment."
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun FreelanceProjectCard(
    project: FreelanceProject,
    onDelete: () -> Unit,
    onPayment: () -> Unit,
    statusMenuExpanded: Boolean,
    isUpdatingStatus: Boolean,
    onStatusMenuToggle: () -> Unit,
    onStatusMenuDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
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
            if (project.clientContact.isNotBlank()) Text("Contact: ${project.clientContact}")
            Text("Dates: ${project.startDate} to ${project.endDate}")
            Box {
                OutlinedButton(
                    enabled = !isUpdatingStatus,
                    onClick = onStatusMenuToggle
                ) {
                    if (isUpdatingStatus) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Status: ${project.status}")
                    }
                }
                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = onStatusMenuDismiss
                ) {
                    FreelanceProjectStatus.values.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status) },
                            onClick = { onStatusSelected(status) }
                        )
                    }
                }
            }

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
            Text("Payment: ${project.paymentStatus}")
            Text("Received: LKR ${formatMinorAmount(project.totalPaidBaseMinor)}")
            Text("Outstanding: LKR ${formatMinorAmount(project.outstandingBalanceMinor)}")
            if (project.endDate.isNotBlank() && project.endDate <= today &&
                project.status != FreelanceProjectStatus.COMPLETED &&
                project.status != FreelanceProjectStatus.CANCELLED
            ) {
                Text("Deadline reached or passed. Confirm whether the work is completed.",
                    color = MaterialTheme.colorScheme.error)
            }

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
                if (project.status == FreelanceProjectStatus.COMPLETED &&
                    project.paymentStatus != "PAID"
                ) {
                    TextButton(onClick = onPayment) { Text("Record payment") }
                }
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
        contact: String,
        description: String,
        startDate: String,
        endDate: String,
        work: String,
        status: String,
        amount: String,
        currency: String,
        rate: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var work by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(FreelanceProjectStatus.PLANNED) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var rate by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
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
                        value = contact, onValueChange = { contact = it },
                        label = { Text("Client contact (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Project description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = startDate, onValueChange = { startDate = it },
                        label = { Text("Start date (YYYY-MM-DD)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = endDate, onValueChange = { endDate = it },
                        label = { Text("End date (YYYY-MM-DD)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = work, onValueChange = { work = it },
                        label = { Text("Work description / deliverables") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedButton(onClick = { statusMenuExpanded = true }) {
                        Text("Status: $status")
                    }
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        FreelanceProjectStatus.values.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }

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
                            name, client, contact, description, startDate, endDate, work, status,
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

@Composable
private fun FreelancePaymentDialog(
    project: FreelanceProject,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: String, date: String, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment received: ${project.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Outstanding: LKR ${formatMinorAmount(project.outstandingBalanceMinor)}")
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Actual amount received (LKR)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text("Payment date (YYYY-MM-DD)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Payment note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !isSaving, onClick = {
                val validation = FreelanceProjectValidator.validatePayment(amount, date)
                if (validation != null) error = validation else onSave(amount, date, note)
            }) { Text("Save payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatMinorAmount(amountMinor: Long): String {
    return BigDecimal.valueOf(amountMinor, 2)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString()
}
 // thisanth