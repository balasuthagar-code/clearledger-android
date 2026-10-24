
package com.example.clearledger.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onIncomeSources: () -> Unit,
    onIncomeEntries: () -> Unit,
    onSavingsGoals: () -> Unit,
    onSavingsContributions: () -> Unit,
    onFreelanceProjects: () -> Unit,
    onCryptocurrency: () -> Unit,
    onWeeklyCheckIn: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ClearLedger",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Personal Finance Dashboard",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text("Menu ☰")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Income Sources") },
                        onClick = {
                            menuExpanded = false
                            onIncomeSources()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Income Entries") },
                        onClick = {
                            menuExpanded = false
                            onIncomeEntries()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Savings Goals") },
                        onClick = {
                            menuExpanded = false
                            onSavingsGoals()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Savings Contributions") },
                        onClick = {
                            menuExpanded = false
                            onSavingsContributions()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Freelance Projects") },
                        onClick = {
                            menuExpanded = false
                            onFreelanceProjects()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cryptocurrency") },
                        onClick = {
                            menuExpanded = false
                            onCryptocurrency()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Weekly Check-in") },
                        onClick = {
                            menuExpanded = false
                            onWeeklyCheckIn()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Profile") },
                        onClick = {
                            menuExpanded = false
                            onProfile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Log Out") },
                        onClick = {
                            menuExpanded = false
                            onLogout()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Income Summary",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                when {
                    state.isLoading -> {
                        CircularProgressIndicator()
                    }

                    state.error != null -> {
                        Text(
                            text = state.error
                                ?: "Unable to load income summary",
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.loadIncomeSummary() }
                        ) {
                            Text("Retry")
                        }
                    }

                    else -> {
                        Text(
                            text = "Received: ${
                                formatIncomeAmount(state.receivedIncomeMinor)
                            }"
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Expected: ${
                                formatIncomeAmount(state.expectedIncomeMinor)
                            }"
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Manage Your Finances",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Open the menu in the top-right corner to access " +
                            "your income, savings goals, weekly check-in, and " +
                            "other features."
                )
            }
        }
    }
}

private fun formatIncomeAmount(amountMinor: Long): String {
    return NumberFormat
        .getCurrencyInstance(Locale("en", "LK"))
        .format(amountMinor / 100.0)
}
