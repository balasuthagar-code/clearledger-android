package com.example.clearledger.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to ClearLedger",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = onIncomeSources,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Manage Income Sources")
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Logout")
        }
        Button(
            onClick = onIncomeEntries,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Record Income / History")
        }
        Button(
            onClick = onSavingsGoals,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Savings Goals")
        }
        Button(
            onClick = onSavingsContributions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Savings Contributions")
        }
    }
}
