
package com.example.clearledger.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearledger.ui.dashboard.DashboardRoute

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onIncomeSources: () -> Unit,
    onIncomeEntries: () -> Unit,
    onSavingsGoals: () -> Unit,
    onSavingsContributions: () -> Unit,
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
            Text(
                text = "ClearLedger",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(onClick = onLogout) {
                Text("Logout")
            }
        }

        DashboardRoute(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Text(
            text = "Income Management",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = onIncomeSources,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Manage Income Sources")
        }

        Button(
            onClick = onIncomeEntries,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Record Income / History")
        }

        Text(
            text = "Savings Management",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = onSavingsGoals,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Savings Goals")
        }

        Button(
            onClick = onSavingsContributions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Savings Contributions")
        }
    }
}
