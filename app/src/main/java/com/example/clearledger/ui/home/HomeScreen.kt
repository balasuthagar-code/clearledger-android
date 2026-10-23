
package com.example.clearledger.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to ClearLedger",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Personal Finance Management",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 16.dp
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "This Month's Income",
                    style = MaterialTheme.typography.titleLarge
                )

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "Received: LKR ${
                            formatIncomeAmount(state.receivedIncomeMinor)
                        }"
                    )

                    Text(
                        text = "Expected: LKR ${
                            formatIncomeAmount(state.expectedIncomeMinor)
                        }"
                    )

                    state.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = viewModel::loadIncomeSummary
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        HomeButton(
            text = "Manage Income Sources",
            onClick = onIncomeSources
        )

        HomeButton(
            text = "Record Income / History",
            onClick = onIncomeEntries
        )

        HomeButton(
            text = "Multi-Currency Handling",
            onClick = onIncomeEntries
        )

        HomeButton(
            text = "Freelance Projects",
            onClick = onFreelanceProjects
        )

        HomeButton(
            text = "Cryptocurrency",
            onClick = onCryptocurrency
        )

        HomeButton(
            text = "Savings Goals",
            onClick = onSavingsGoals
        )

        HomeButton(
            text = "Savings Contributions",
            onClick = onSavingsContributions
        )

        HomeButton(
            text = "Weekly Check-in",
            onClick = onWeeklyCheckIn
        )

        HomeButton(
            text = "Profile",
            onClick = onProfile
        )

        HomeButton(
            text = "Logout",
            onClick = onLogout
        )
    }
}

private fun formatIncomeAmount(amountMinor: Long): String {
    val amount = amountMinor / 100.0

    return NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
}

@Composable
private fun HomeButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Text(text)
    }
}
