package com.example.clearledger.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.UiState
import com.example.clearledger.domain.model.CategoryTotal
import com.example.clearledger.domain.model.GoalSummary
import com.example.clearledger.domain.model.MonthlySummary
import com.example.clearledger.domain.model.SafeToSpend
import com.example.clearledger.ui.expense.list.monthLabel

/** Connects the ViewModel to the screen. Contains NO business logic. */
@Composable
fun DashboardRoute(
modifier: Modifier = Modifier,
 viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val categoryNames by viewModel.categoryNames.collectAsStateWithLifecycle()
    val goal by viewModel.primaryGoal.collectAsStateWithLifecycle()
    val safeToSpend by viewModel.safeToSpend.collectAsStateWithLifecycle()

    DashboardScreen(
        modifier = modifier,
        monthKey = month,
        state = state,
        categoryNames = categoryNames,
        primaryGoal = goal,
        safeToSpend = safeToSpend,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onRetry = viewModel::retry
    )
}

/**
 * Monthly dashboard (FR-DASH-01..07): income vs expenses bars, committed vs discretionary,
 * spending by category (bars + list with amounts and percentages), primary goal progress,
 * and the "Based on recorded transactions" notice. Plain Compose shapes - no chart library.
 */
@Composable
fun DashboardScreen(
    monthKey: String,
    state: UiState<MonthlySummary>,
    categoryNames: Map<String, String>,
    primaryGoal: GoalSummary?,
    safeToSpend: SafeToSpend?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviousMonth) { Text("<") }
            Text(text = monthLabel(monthKey), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNextMonth) { Text(">") }
        }

        when (state) {
            is UiState.Loading -> CenteredBox { CircularProgressIndicator() }

            is UiState.Error -> CenteredBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
            }

            is UiState.Success ->
                if (state.data.isEmpty) {
                    // FR-DASH-07: nothing recorded -> guide the user.
                    CenteredBox {
                        Text(
                            "Nothing recorded for this month yet.\nAdd your income or an expense to see your summary.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    SummaryContent(state.data, categoryNames, primaryGoal, safeToSpend)
                }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun SummaryContent(
    summary: MonthlySummary,
    categoryNames: Map<String, String>,
    primaryGoal: GoalSummary?,
    safeToSpend: SafeToSpend?
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FR-DASH-07 / FR-DASH-02: figures come from recorded data only.
        Text(
            "Based on recorded transactions. These figures include only what you have entered; they are not a bank balance.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Net this month (recorded)", style = MaterialTheme.typography.labelLarge)
                Text(formatMinorUnits(summary.remainingMinor), style = MaterialTheme.typography.headlineMedium)
            }
        }

        safeToSpend?.let { SafeToSpendCard(it) }
        IncomeVsExpensesCard(summary)
        CommittedVsDiscretionaryCard(summary)
        CategoryBreakdownCard(summary, categoryNames)
        GoalCard(primaryGoal)
    }
}
// ---------- FR-DASH-08: safe to spend ----------

@Composable
private fun SafeToSpendCard(safe: SafeToSpend) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Safe to spend this month", style = MaterialTheme.typography.titleMedium)
            Text(
                formatMinorUnits(safe.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                color = if (safe.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (safe.isOverBudget) {
                Text("You are over budget for this month.", style = MaterialTheme.typography.labelLarge)
            }
            AmountRow("Recorded income", safe.incomeMinor)
            AmountRow("Recorded expenses", -safe.expensesMinor)
            AmountRow("Recurring still pending", -safe.pendingCommitmentsMinor)
            AmountRow("Goal saving needed", -safe.goalSavingMinor)
            Text(
                "An estimate from recorded data. Pending recurring items are due this month and not yet " +
                    "confirmed or skipped.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------- FR-DASH-03: income vs expenses ----------

@Composable
private fun IncomeVsExpensesCard(summary: MonthlySummary) {
    val (incomeFraction, expenseFraction) =
        ChartMath.incomeExpenseFractions(summary.totalIncomeMinor, summary.totalExpensesMinor)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Income vs expenses", style = MaterialTheme.typography.titleMedium)
            BarRow("Income", summary.totalIncomeMinor, incomeFraction, MaterialTheme.colorScheme.primary)
            BarRow("Expenses", summary.totalExpensesMinor, expenseFraction, MaterialTheme.colorScheme.error)
        }
    }
}

// ---------- committed vs discretionary ----------

@Composable
private fun CommittedVsDiscretionaryCard(summary: MonthlySummary) {
    val committedShare = ChartMath.committedShare(summary.committedMinor, summary.totalExpensesMinor)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Committed vs discretionary",
                style = MaterialTheme.typography.titleMedium
            )

            if (summary.totalExpensesMinor > 0L) {
                // 1. Committed bar
                TrackedBar(
                    committedShare,
                    MaterialTheme.colorScheme.tertiary
                )
            }

            // 2. Committed text
            AmountRow(
                "Committed (${ChartMath.percentLabel(committedShare)}%)",
                summary.committedMinor
            )

            if (summary.totalExpensesMinor > 0L) {
                // 3. Discretionary bar
                TrackedBar(
                    summary.discretionaryMinor.toFloat() /
                            summary.totalExpensesMinor.toFloat(),
                    MaterialTheme.colorScheme.primary
                )
            }

            // 4. Discretionary text
            AmountRow(
                "Discretionary (${if (summary.totalExpensesMinor > 0L) 100 - ChartMath.percentLabel(committedShare) else 0}%)",
                summary.discretionaryMinor
            )
        }
    }
}
// ---------- FR-DASH-04: spending by category ----------

private val CATEGORY_COLORS = listOf(
    Color(0xFF3F6FD8), Color(0xFFE2762D), Color(0xFF2E9E6B), Color(0xFF9B59B6),
    Color(0xFFD94F70), Color(0xFF16A2B8), Color(0xFF8D8D2E)
)

@Composable
private fun CategoryBreakdownCard(summary: MonthlySummary, categoryNames: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spending by category", style = MaterialTheme.typography.titleMedium)
            if (summary.categoryTotals.isEmpty()) {
                Text("No expenses recorded this month.", style = MaterialTheme.typography.bodyMedium)
            }
            summary.categoryTotals.forEachIndexed { index, item ->
                CategoryRow(
                    item = item,
                    name = categoryNames[item.categoryId] ?: item.categoryId,
                    color = CATEGORY_COLORS[index % CATEGORY_COLORS.size]
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryTotal, name: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
                Text(
                    "  $name (${"%.0f".format(item.percentage)}%)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(formatMinorUnits(item.totalMinor), style = MaterialTheme.typography.bodyLarge)
        }
        TrackedBar((item.percentage / 100.0).toFloat(), color)
    }
}

// ---------- FR-DASH-05: goal progress ----------

@Composable
private fun GoalCard(goal: GoalSummary?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Savings goal", style = MaterialTheme.typography.titleMedium)
            if (goal == null) {
                Text(
                    "No primary goal yet. Create a goal and mark it as primary to see your progress here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(goal.name, style = MaterialTheme.typography.titleSmall)
                TrackedBar(goal.progressFraction, MaterialTheme.colorScheme.primary)
                AmountRow("Saved (${goal.progressPercent}%)", goal.savedMinor)
                AmountRow("Target", goal.targetMinor)
                goal.requiredMonthlyMinor?.let { AmountRow("Needed per month", it) }
                goal.statusText?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

// ---------- small building blocks ----------

@Composable
private fun BarRow(label: String, minor: Long, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AmountRow(label, minor)
        TrackedBar(fraction, color)
    }
}

/** A grey track with a coloured fill; [fraction] is 0f..1f. */
@Composable
private fun TrackedBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Composable
private fun AmountRow(label: String, minor: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(formatMinorUnits(minor), style = MaterialTheme.typography.bodyLarge)
    }
}
