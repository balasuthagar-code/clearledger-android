package com.example.clearledger.ui.recurring.form

import com.example.clearledger.currency.CurrencyRateField

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.RecurringFrequency

/** All the things the screen can ask the ViewModel to do - keeps the screen signature short. */
data class RecurringFormActions(
    val onNameChange: (String) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onCurrencyChange: (String) -> Unit,
    val onRateChange: (String) -> Unit,
    val onCategorySelected: (String) -> Unit,
    val onFrequencyChange: (RecurringFrequency) -> Unit,
    val onDueDayChange: (String) -> Unit,
    val onDueMonthChange: (String) -> Unit,
    val onActiveChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit
)

/** Connects the ViewModel to the screen. Contains NO business logic. */
@Composable
fun RecurringFormRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecurringFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is RecurringFormEvent.Saved) onDone()
        }
    }

    RecurringFormScreen(
        state = state,
        categories = categories,
        actions = RecurringFormActions(
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onRateChange = viewModel::onRateChange,
            onCategorySelected = viewModel::onCategorySelected,
            onFrequencyChange = viewModel::onFrequencyChange,
            onDueDayChange = viewModel::onDueDayChange,
            onDueMonthChange = viewModel::onDueMonthChange,
            onActiveChange = viewModel::onActiveChange,
            onSave = viewModel::save,
            onBack = onBack
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringFormScreen(
    state: RecurringFormState,
    categories: List<ExpenseCategory>,
    actions: RecurringFormActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit recurring item" else "Add recurring item") },
                navigationIcon = { TextButton(onClick = actions.onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.loadError != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.loadError, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = actions.onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = actions.onNameChange,
                    label = { Text("Name (e.g. Rent, Gym)") },
                    singleLine = true,
                    isError = state.errors.name != null,
                    supportingText = { state.errors.name?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = actions.onAmountChange,
                    label = { Text("Amount (${state.currency})") },
                    singleLine = true,
                    isError = state.errors.amount != null,
                    supportingText = { state.errors.amount?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Currency", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currencies.supported.forEach { code ->
                        FilterChip(
                            selected = state.currency == code,
                            onClick = { actions.onCurrencyChange(code) },
                            label = { Text(code) }
                        )
                    }
                }
                // FR-CUR-03: no rate field for the base currency.
                if (state.currency != Currencies.BASE) {
                    CurrencyRateField(original = state.currency, value = state.rateText,
                            onChange = actions.onRateChange, editing = state.isEditing,
                            enabled = !state.isSaving, error = state.errors.rate)
                }

                Text("Category", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { actions.onCategorySelected(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
                state.errors.category?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("How often", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringFrequency.values().forEach { option ->
                        FilterChip(
                            selected = state.frequency == option,
                            onClick = { actions.onFrequencyChange(option) },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.dueDayText,
                    onValueChange = actions.onDueDayChange,
                    label = {
                        Text(
                            if (state.frequency == RecurringFrequency.WEEKLY) "Weekday (1 = Monday ... 7 = Sunday)"
                            else "Day of the month (1-31)"
                        )
                    },
                    singleLine = true,
                    isError = state.errors.dueDay != null,
                    supportingText = { state.errors.dueDay?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.frequency == RecurringFrequency.YEARLY) {
                    OutlinedTextField(
                        value = state.dueMonthText,
                        onValueChange = actions.onDueMonthChange,
                        label = { Text("Month (1-12)") },
                        singleLine = true,
                        isError = state.errors.dueMonth != null,
                        supportingText = { state.errors.dueMonth?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = state.isActive, onCheckedChange = actions.onActiveChange)
                }

                state.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = actions.onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}
