
package com.example.clearledger.ui.goals

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.clearledger.domain.model.SavingsContribution
import java.time.LocalDate
import java.util.Locale

@Composable
fun SavingsContributionsScreen(
    viewModel: SavingsContributionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember {
        mutableStateOf<SavingsContribution?>(null)
    }

    var amount by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Savings Contributions",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Text("Loading savings goals...")
        } else if (state.goals.isEmpty()) {
            Text("Create a savings goal first.")
        } else {
            Text(
                text = "Select a goal",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    OutlinedButton(
                        onClick = { viewModel.selectGoal(goal) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.selectedGoal?.id == goal.id) {
                                "✓ ${goal.name}"
                            } else {
                                goal.name
                            }
                        )
                    }
                }
            }
        }

        state.selectedGoal?.let { goal ->
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Target: ${goal.currency} ${
                            formatAmount(goal.targetAmount)
                        }"
                    )

                    Text(
                        text = "Total saved: ${goal.currency} ${
                            formatAmount(state.totalSaved)
                        }"
                    )

                    Text(
                        text = "Remaining: ${goal.currency} ${
                            formatAmount(state.remainingAmount)
                        }"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "${(state.progress * 100).toInt()}% completed"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Required monthly saving: ${goal.currency} ${
                            formatAmount(state.requiredMonthlySaving)
                        }",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Months remaining: ${state.monthsLeft}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            editing = null
                            amount = ""
                            date = LocalDate.now().toString()
                            note = ""
                            viewModel.clearMessages()
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Contribution")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Contribution History",
                style = MaterialTheme.typography.titleLarge
            )

            if (state.contributions.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No contributions yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.contributions,
                        key = { it.id }
                    ) { contribution ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "${goal.currency} ${
                                        formatAmount(contribution.amount)
                                    }",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text("Date: ${contribution.date}")

                                if (contribution.note.isNotBlank()) {
                                    Text("Note: ${contribution.note}")
                                }

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            editing = contribution
                                            amount =
                                                contribution.amount.toString()
                                            date = contribution.date
                                            note = contribution.note
                                            viewModel.clearMessages()
                                            showDialog = true
                                        }
                                    ) {
                                        Text("Edit")
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.deleteContribution(
                                                contribution
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "Delete",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(
                    if (editing == null) {
                        "Add Contribution"
                    } else {
                        "Edit Contribution"
                    }
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        viewModel.saveContribution(
                            amountText = amount,
                            date = date,
                            note = note,
                            editing = editing
                        )
                        showDialog = false
                    }
                ) {
                    Text(
                        if (state.isSaving) "Saving..." else "Save"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}
