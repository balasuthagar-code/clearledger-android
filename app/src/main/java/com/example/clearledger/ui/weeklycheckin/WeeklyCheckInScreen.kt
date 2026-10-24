
package com.example.clearledger.ui.weeklycheckin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.clearledger.domain.model.WeeklyCheckIn
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCheckInScreen(
    viewModel: WeeklyCheckInViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val actualBalanceMinor = uiState.reportedBalanceMinor
    val gapMinor = actualBalanceMinor?.minus(
        uiState.expectedBalanceMinor
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Weekly Check-in")
                },
                navigationIcon = {
                    androidx.compose.material3.TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Review your finances",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Compare your expected balance with your actual balance.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Expected balance",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = formatLkr(
                                uiState.expectedBalanceMinor
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "This value must be connected to your financial records.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.reportedBalanceText,
                    onValueChange = viewModel::onReportedBalanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Actual balance (LKR)")
                    },
                    placeholder = {
                        Text("Example: 25000.00")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Balance gap",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = gapMinor?.let(::formatLkr)
                                ?: "Enter a valid actual balance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                gapMinor == null ->
                                    MaterialTheme.colorScheme.onSurface
                                gapMinor < 0L ->
                                    MaterialTheme.colorScheme.error
                                else ->
                                    MaterialTheme.colorScheme.primary
                            }
                        )

                        Text(
                            text = "Actual balance − Expected balance",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Reflection note (optional)")
                    },
                    placeholder = {
                        Text("How was your spending this week?")
                    },
                    minLines = 3,
                    maxLines = 5
                )
            }

            item {
                Button(
                    onClick = viewModel::saveWeeklyCheckIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isSaving &&
                            !uiState.isLoading &&
                            actualBalanceMinor != null
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Weekly Check-in")
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.successMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                HorizontalDivider()

                Text(
                    text = "Previous Check-ins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!uiState.isLoading && uiState.checkIns.isEmpty()) {
                item {
                    Text(
                        text = "No weekly check-ins yet. Save your first check-in above.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(
                items = uiState.checkIns,
                key = { checkIn -> checkIn.id }
            ) { checkIn ->
                WeeklyCheckInHistoryCard(checkIn = checkIn)
            }
        }
    }
}

@Composable
private fun WeeklyCheckInHistoryCard(
    checkIn: WeeklyCheckIn
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Week starting ${checkIn.weekKey}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            BalanceRow(
                label = "Expected balance",
                amount = formatLkr(checkIn.expectedBalanceMinor)
            )

            BalanceRow(
                label = "Actual balance",
                amount = formatLkr(checkIn.reportedBalanceMinor)
            )

            BalanceRow(
                label = "Balance gap",
                amount = formatLkr(checkIn.gapMinor)
            )

            if (checkIn.note.isNotBlank()) {
                HorizontalDivider()

                Text(
                    text = "Reflection",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = checkIn.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BalanceRow(
    label: String,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatLkr(amountMinor: Long): String {
    val amount = amountMinor / 100.0
    val formatter = NumberFormat.getNumberInstance(Locale("en", "LK"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2

    return "LKR ${formatter.format(amount)}"
}
//thisanth