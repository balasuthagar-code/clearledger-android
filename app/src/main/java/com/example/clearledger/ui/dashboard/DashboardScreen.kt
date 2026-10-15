package com.example.clearledger.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.MonthlySummary

/**
 * Connects the ViewModel to the screen.
 * Contains no business logic.
 */
@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val categoryNames by viewModel.categoryNames.collectAsStateWithLifecycle()

    DashboardScreen(
        monthKey = month,
        state = state,
        categoryNames = categoryNames,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

/**
 * Sprint 1 placeholder UI.
 * Sprint 2 can replace this with charts.
 */
@Composable
fun DashboardScreen(
    monthKey: String,
    state: UiState<MonthlySummary>,
    categoryNames: Map<String, String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            TextButton(onClick = onPreviousMonth) {
                Text("<")
            }

            Text(
                text = monthKey,
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(onClick = onNextMonth) {
                Text(">")
            }
        }

        when (state) {
            is UiState.Loading -> {
                CenteredBox {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                CenteredBox {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Button(
                            onClick = onRetry,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is UiState.Success -> {
                if (state.data.isEmpty) {
                    CenteredBox {
                        Text(
                            text = "Nothing recorded for this month yet.\n" +
                                    "Add your income or an expense to see your summary.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    SummaryContent(
                        summary = state.data,
                        categoryNames = categoryNames
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SummaryContent(
    summary: MonthlySummary,
    categoryNames: Map<String, String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Remaining this month",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = formatMinorUnits(summary.remainingMinor),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AmountRow(
                    label = "Income",
                    minor = summary.totalIncomeMinor
                )

                AmountRow(
                    label = "Expenses",
                    minor = summary.totalExpensesMinor
                )

                AmountRow(
                    label = "  Committed",
                    minor = summary.committedMinor
                )

                AmountRow(
                    label = "  Discretionary",
                    minor = summary.discretionaryMinor
                )
            }
        }

        Text(
            text = "Spending by category",
            style = MaterialTheme.typography.titleMedium
        )

        summary.categoryTotals.forEach { item ->
            val name = categoryNames[item.categoryId] ?: item.categoryId

            AmountRow(
                label = "$name (${"%.0f".format(item.percentage)}%)",
                minor = item.totalMinor
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    minor: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = formatMinorUnits(minor),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

