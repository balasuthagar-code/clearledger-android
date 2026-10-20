package com.example.clearledger.ui.expense.list

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.usecase.ExpenseDayGroup
import com.example.clearledger.ui.dashboard.formatMinorUnits
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** Connects the ViewModel to the screen. Contains NO business logic. */
@Composable
fun ExpenseListRoute(
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    ExpenseListScreen(
        monthKey = month,
        selectedCategoryId = selectedCategory,
        categories = categories,
        state = state,
        message = message,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onSelectCategory = viewModel::selectCategory,
        onRetry = viewModel::retry,
        onAddExpense = onAddExpense,
        onEditExpense = onEditExpense,
        onDeleteExpense = viewModel::deleteExpense,
        onMessageShown = viewModel::messageShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    monthKey: String,
    selectedCategoryId: String?,
    categories: List<ExpenseCategory>,
    state: UiState<ExpenseListContent>,
    message: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onRetry: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Only the id survives rotation; the dialog looks the expense up again.
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryNames = categories.associate { it.id to it.name }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // ---- month navigation ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPreviousMonth) { Text("<") }
                Text(text = monthLabel(monthKey), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNextMonth) { Text(">") }
            }

            // ---- category filter ----
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            when (state) {
                is UiState.Loading -> CenteredBox { CircularProgressIndicator() }

                is UiState.Error -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                    }
                }

                is UiState.Success -> {
                    val content = state.data
                    if (content.groups.isEmpty()) {
                        CenteredBox {
                            Text(
                                if (content.monthHasExpenses) "No expenses in this category this month."
                                else "No expenses recorded for this month yet.\nTap + to add one.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Text(
                            "${content.shownCount} expense(s) · ${formatMinorUnits(content.shownTotalMinor)}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        ExpenseGroups(
                            groups = content.groups,
                            categoryNames = categoryNames,
                            onEdit = onEditExpense,
                            onDelete = { pendingDeleteId = it }
                        )
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete expense?") },
            text = { Text("This removes the expense and updates your totals. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteExpense(id); pendingDeleteId = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExpenseGroups(
    groups: List<ExpenseDayGroup>,
    categoryNames: Map<String, String>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),      // keeps the last row clear of the + button
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            item(key = "header-${group.date}") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(group.date.format(DAY_FORMAT), style = MaterialTheme.typography.titleSmall)
                    Text(formatMinorUnits(group.totalBaseMinor), style = MaterialTheme.typography.titleSmall)
                }
            }
            items(group.expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    categoryName = categoryNames[expense.categoryId] ?: expense.categoryId,
                    onClick = { onEdit(expense.id) },
                    onDelete = { onDelete(expense.id) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    categoryName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.merchant.ifBlank { categoryName },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$categoryName · ${classificationLabel(expense.classification)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (expense.note.isNotBlank()) {
                    Text(expense.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMinorUnits(expense.amountMinor, expense.currency),
                    style = MaterialTheme.typography.titleMedium
                )
                // FR-CUR-06: show the base-currency equivalent when the currency differs.
                if (expense.currency != Currencies.BASE) {
                    Text("≈ ${formatMinorUnits(expense.baseAmountMinor)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

internal fun classificationLabel(c: ExpenseClassification): String =
    if (c == ExpenseClassification.COMMITTED) "Committed" else "Discretionary"

internal fun monthLabel(monthKey: String): String =
    runCatching { YearMonth.parse(monthKey).format(MONTH_FORMAT) }.getOrDefault(monthKey)

