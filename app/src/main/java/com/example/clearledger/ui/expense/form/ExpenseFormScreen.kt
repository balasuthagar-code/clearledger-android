package com.example.clearledger.ui.expense.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.Currencies
import com.example.clearledger.domain.model.ExpenseCategory
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.MerchantSuggestion
import com.example.clearledger.domain.model.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** All the things the screen can ask the ViewModel to do - keeps the screen signature short. */
data class ExpenseFormActions(
    val onAmountChange: (String) -> Unit,
    val onCategorySelected: (String) -> Unit,
    val onClassificationSelected: (ExpenseClassification) -> Unit,
    val onCurrencyChange: (String) -> Unit,
    val onRateChange: (String) -> Unit,
    val onPaymentMethodSelected: (PaymentMethod) -> Unit,
    val onMerchantChange: (String) -> Unit,
    val onMerchantSuggestionSelected: (MerchantSuggestion) -> Unit,
    val onToggleFavourite: () -> Unit,
    val onNoteChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit
)

/** Connects the ViewModel to the screen. Contains NO business logic. */
@Composable
fun ExpenseFormRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExpenseFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val merchantSuggestions by viewModel.merchantSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ExpenseFormEvent.Saved) onDone()
        }
    }

    ExpenseFormScreen(
        state = state,
        categories = categories,
        merchantSuggestions = merchantSuggestions,
        actions = ExpenseFormActions(
            onAmountChange = viewModel::onAmountChange,
            onCategorySelected = viewModel::onCategorySelected,
            onClassificationSelected = viewModel::onClassificationSelected,
            onCurrencyChange = viewModel::onCurrencyChange,
            onRateChange = viewModel::onRateChange,
            onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
            onMerchantChange = viewModel::onMerchantChange,
            onMerchantSuggestionSelected = viewModel::onMerchantSuggestionSelected,
            onToggleFavourite = viewModel::toggleFavourite,
            onNoteChange = viewModel::onNoteChange,
            onDateChange = viewModel::onDateChange,
            onSave = viewModel::save,
            onBack = onBack
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseFormScreen(
    state: ExpenseFormState,
    categories: List<ExpenseCategory>,
    merchantSuggestions: List<MerchantSuggestion>,
    actions: ExpenseFormActions,
    modifier: Modifier = Modifier
) {
    var showMoreOptions by rememberSaveable { mutableStateOf(state.isEditing) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit expense" else "Add expense") },
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
                // ---- step 1: amount ----
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

                // ---- FR-EXP-08: remembered merchants (tap one to fill merchant + usual category) ----
                if (merchantSuggestions.isNotEmpty()) {
                    Text("Merchants", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        merchantSuggestions.forEach { suggestion ->
                            FilterChip(
                                selected = state.merchant.trim().equals(suggestion.name, ignoreCase = true),
                                onClick = { actions.onMerchantSuggestionSelected(suggestion) },
                                label = { Text(suggestion.name) }
                            )
                        }
                    }
                }
                if (state.merchant.isNotBlank()) {
                    Text("Merchant: ${state.merchant}", style = MaterialTheme.typography.bodySmall)
                }

                // ---- step 2: category ----
                Text("Category", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { actions.onCategorySelected(category.id) },
                            label = { Text(if (category.isFavourite) "★ ${category.name}" else category.name) }
                        )
                    }
                }
                // FR-EXP-09: star the selected category so it is listed first next time.
                categories.firstOrNull { it.id == state.categoryId }?.let { selected ->
                    TextButton(onClick = actions.onToggleFavourite) {
                        Text(
                            if (selected.isFavourite) "★ Remove ${selected.name} from favourites"
                            else "☆ Make ${selected.name} a favourite"
                        )
                    }
                }
                state.errors.category?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("Type", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseClassification.values().forEach { option ->
                        FilterChip(
                            selected = state.classification == option,
                            onClick = { actions.onClassificationSelected(option) },
                            label = { Text(if (option == ExpenseClassification.COMMITTED) "Committed" else "Discretionary") }
                        )
                    }
                }

                // ---- optional details (collapsed in quick-add, open when editing) ----
                TextButton(onClick = { showMoreOptions = !showMoreOptions }) {
                    Text(if (showMoreOptions) "Fewer options" else "More options")
                }
                if (showMoreOptions) {
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
                    // FR-CUR-03: the rate field is hidden when the currency is the base currency.
                    if (state.currency != Currencies.BASE) {
                        OutlinedTextField(
                            value = state.rateText,
                            onValueChange = actions.onRateChange,
                            label = { Text("Exchange rate (${Currencies.BASE} per 1 ${state.currency})") },
                            singleLine = true,
                            isError = state.errors.rate != null,
                            supportingText = { state.errors.rate?.let { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Paid with", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.values().forEach { method ->
                            FilterChip(
                                selected = state.paymentMethod == method,
                                onClick = { actions.onPaymentMethodSelected(method) },
                                label = { Text(paymentLabel(method)) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.merchant,
                        onValueChange = actions.onMerchantChange,
                        label = { Text("Merchant (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = actions.onNoteChange,
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Date: ${state.date.format(DATE_FORMAT)}")
                    }
                }

                state.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                // ---- step 3: save ----
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

    if (showDatePicker) {
        // The Material date picker works in UTC milliseconds, so convert with ZoneOffset.UTC both ways.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        actions.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun paymentLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.CARD -> "Card"
    PaymentMethod.BANK_TRANSFER -> "Bank transfer"
    PaymentMethod.DIGITAL_WALLET -> "Digital wallet"
    PaymentMethod.OTHER -> "Other"
}
