
package com.example.clearledger.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.clearledger.domain.model.SavingsGoal

@Composable
fun CreateGoalScreen(
    viewModel: SavingsGoalsViewModel = hiltViewModel(),
    onOpenContributions: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var deadline by remember { mutableStateOf("") }
    var startingAmount by remember { mutableStateOf("0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Savings Goals",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Goal name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = targetAmount,
            onValueChange = { targetAmount = it },
            label = { Text("Target amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase() },
            label = { Text("Currency (LKR, USD, EUR, GBP, AUD, INR)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = deadline,
            onValueChange = { deadline = it },
            label = { Text("Deadline (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = startingAmount,
            onValueChange = { startingAmount = it },
            label = { Text("Starting amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.message != null) {
            Text(
                text = state.message!!,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                viewModel.createGoal(
                    name = name,
                    targetAmount = targetAmount,
                    currency = currency,
                    deadline = deadline,
                    startingAmount = startingAmount
                )
            },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (state.isSaving) "Saving..." else "Create Goal"
            )
        }

        TextButton(
            onClick = onOpenContributions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage contributions")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "My Savings Goals",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.goals.isEmpty() -> {
                Text("No savings goals created yet.")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.goals,
                        key = { it.id }
                    ) { goal ->
                        SavingsGoalItem(goal = goal)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalItem(goal: SavingsGoal) {
    val progress = goal.progressPercentage / 100f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Target: ${goal.currency} ${
                formatMinorAmount(goal.targetAmountMinor)
            }"
        )

        Text(
            text = "Saved: ${goal.currency} ${
                formatMinorAmount(goal.savedAmountMinor)
            }"
        )

        Text(
            text = "Remaining: ${goal.currency} ${
                formatMinorAmount(goal.remainingAmountMinor)
            }"
        )

        Text(text = "Deadline: ${goal.deadline}")

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${goal.progressPercentage.toInt()}% completed"
        )
    }
}

private fun formatMinorAmount(amountMinor: Long): String {
    return "%.2f".format(amountMinor / 100.0)
}
// thsianth