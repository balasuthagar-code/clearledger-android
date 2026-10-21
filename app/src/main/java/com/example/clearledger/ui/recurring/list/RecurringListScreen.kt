package com.example.clearledger.ui.recurring.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import com.example.clearledger.ui.dashboard.formatMinorUnits
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Connects the ViewModel to the screen. Contains NO business logic. */
@Composable
fun RecurringListRoute(
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val names by viewModel.categoryNames.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    RecurringListScreen(
        state = state,
        categoryNames = names,
        message = message,
        onRetry = viewModel::retry,
        onAddItem = onAddItem,
        onEditItem = onEditItem,
        onSetActive = viewModel::setActive,
        onDeleteItem = viewModel::deleteItem,
        onMessageShown = viewModel::messageShown,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    state: UiState<RecurringListContent>,
    categoryNames: Map<String, String>,
    message: String?,
    onRetry: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Recurring commitments") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                    }
                }

                is UiState.Success -> {
                    val content = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            PlannedCard(content)
                        }
                        if (content.items.isEmpty()) {
                            item {
                                Text(
                                    "No recurring items yet.\nTap + to add rent, a subscription or your gym membership.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(content.items, key = { it.id }) { item ->
                            RecurringRow(
                                item = item,
                                categoryName = categoryNames[item.categoryId] ?: item.categoryId,
                                onClick = { onEditItem(item.id) },
                                onActiveChange = { onSetActive(item.id, it) },
                                onDelete = { pendingDeleteId = item.id }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete recurring item?") },
            text = { Text("The item is removed. Expenses you already recorded are not affected.") },
            confirmButton = { TextButton(onClick = { onDeleteItem(id); pendingDeleteId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") } }
        )
    }
}

/** FR-REC-05: the planned monthly total, clearly labelled as a plan and not as spending. */
@Composable
private fun PlannedCard(content: RecurringListContent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Planned monthly commitments", style = MaterialTheme.typography.labelLarge)
            Text(
                formatMinorUnits(content.planned.monthlyTotalBaseMinor),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "${content.planned.activeMonthlyCount} active monthly item(s). " +
                    "This is a plan: an item counts as spending only when you record the expense.",
                style = MaterialTheme.typography.bodySmall
            )
            if (content.planned.otherActiveCount > 0) {
                Text(
                    "${content.planned.otherActiveCount} active weekly/yearly item(s) are not included in this total.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecurringRow(
    item: RecurringItem,
    categoryName: String,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text("$categoryName · ${scheduleLabel(item)}", style = MaterialTheme.typography.bodySmall)
                Text(
                    formatMinorUnits(item.amountMinor, item.currency) +
                        if (item.currency != Currencies.BASE) "  ≈ ${formatMinorUnits(item.baseAmountMinor)}" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!item.isActive) Text("Inactive", style = MaterialTheme.typography.labelMedium)
            }
            Switch(checked = item.isActive, onCheckedChange = onActiveChange)
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

internal fun scheduleLabel(item: RecurringItem): String = when (item.frequency) {
    RecurringFrequency.WEEKLY ->
        "Weekly, ${dayOfWeekName(item.dueDay)}"
    RecurringFrequency.MONTHLY ->
        "Monthly, day ${item.dueDay}"
    RecurringFrequency.YEARLY ->
        "Yearly, ${item.dueDay} ${monthName(item.dueMonth)}"
}

private fun dayOfWeekName(day: Int): String =
    runCatching { DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH) }.getOrDefault("day $day")

private fun monthName(month: Int): String =
    runCatching { Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }.getOrDefault("month $month")
