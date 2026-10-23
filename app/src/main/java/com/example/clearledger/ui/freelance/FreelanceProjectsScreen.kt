package com.example.clearledger.ui.freelance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.clearledger.data.repository.FreelanceProjectRepository
import com.example.clearledger.domain.model.FreelanceProject
import com.example.clearledger.domain.model.FreelanceProjectValidator
import java.util.Locale

@Composable
fun FreelanceProjectsScreen(
    modifier: Modifier = Modifier
) {
    val repository = remember { FreelanceProjectRepository() }

    var projects by remember {
        mutableStateOf<List<FreelanceProject>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var projectName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var rate by remember { mutableStateOf("1.0") }

    DisposableEffect(repository) {
        val registration = repository.listenToProjects(
            onSuccess = { result ->
                projects = result
                isLoading = false
                errorMessage = null
            },
            onError = { message ->
                errorMessage = message
                isLoading = false
            }
        )

        onDispose {
            registration?.remove()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Freelance Projects",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Track your clients and agreed project payments.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!showForm) {
            Button(
                onClick = {
                    showForm = true
                    errorMessage = null
                    infoMessage = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Project")
            }
        }

        if (showForm) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "New Freelance Project",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Agreed amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = {
                            currency = it.uppercase()
                            if (currency == "LKR") {
                                rate = "1.0"
                            }
                        },
                        label = { Text("Currency (e.g. LKR, USD)") },
                        supportingText = {
                            Text("Supported: LKR, USD, EUR, GBP, AUD, INR")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Exchange rate to LKR") },
                        supportingText = {
                            Text("For LKR, enter 1.0.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showForm = false
                                errorMessage = null
                            },
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val validationError =
                                    FreelanceProjectValidator.validate(
                                        name = projectName,
                                        client = clientName,
                                        agreedAmount = amount,
                                        currency = currency,
                                        rate = rate
                                    )

                                if (validationError != null) {
                                    errorMessage = validationError
                                    return@Button
                                }

                                isSaving = true
                                errorMessage = null
                                infoMessage = null

                                repository.addProject(
                                    name = projectName,
                                    client = clientName,
                                    agreedAmount = amount.toDouble(),
                                    currency = currency.trim().uppercase(),
                                    rate = rate.toDouble(),
                                    onSuccess = {
                                        isSaving = false
                                        showForm = false
                                        projectName = ""
                                        clientName = ""
                                        amount = ""
                                        currency = "LKR"
                                        rate = "1.0"
                                        infoMessage = "Project saved successfully."
                                    },
                                    onError = { message ->
                                        isSaving = false
                                        errorMessage = message
                                    }
                                )
                            },
                            enabled = !isSaving
                        ) {
                            Text(if (isSaving) "Saving..." else "Save Project")
                        }
                    }
                }
            }
        }

        if (infoMessage != null) {
            Text(
                text = infoMessage.orEmpty(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        Text(
            text = "Your Projects (${projects.size})",
            style = MaterialTheme.typography.titleMedium
        )

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage != null && projects.isEmpty() -> {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            projects.isEmpty() -> {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No freelance projects yet.",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "Select Add Project to record your first client project.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = projects,
                        key = { it.id }
                    ) { project ->
                        FreelanceProjectCard(project = project)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreelanceProjectCard(
    project: FreelanceProject
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Client: ${project.client}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Agreed amount: ${
                    String.format(
                        Locale.US,
                        "%,.2f",
                        project.agreedAmount
                    )
                } ${project.currency}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Exchange rate to LKR: ${
                    String.format(Locale.US, "%,.4f", project.rate)
                }",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}