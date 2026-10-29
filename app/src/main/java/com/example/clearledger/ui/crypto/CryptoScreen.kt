package com.example.clearledger.ui.crypto

import com.example.clearledger.currency.CurrencyRateField
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clearledger.core.common.CryptoEntryValidator
import com.example.clearledger.domain.model.CryptoEntry
import com.example.clearledger.domain.model.CryptoEntryOptions
import com.example.clearledger.domain.model.CryptoEntryType
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun CryptoScreen(
    onBack: () -> Unit,
    viewModel: CryptoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var holdingToDelete by remember { mutableStateOf<CryptoHolding?>(null) }
    var detailsHolding by remember { mutableStateOf<CryptoHolding?>(null) }
    var sellHolding by remember { mutableStateOf<CryptoHolding?>(null) }
    var buyMoreAsset by remember { mutableStateOf<String?>(null) }
    val holdings = state.entries
        .groupBy { it.assetName.trim().lowercase() }
        .values
        .mapNotNull { entries ->
            val quantity = CryptoPortfolioMath.quantity(entries)
            if (quantity <= BigDecimal.ZERO) {
                null
            } else {
                CryptoHolding(
                    assetName = entries.first().assetName,
                    quantity = quantity,
                    purchaseCostMinor = CryptoPortfolioMath.purchaseCostMinor(entries),
                    entries = entries.sortedByDescending { it.date }
                )
            }
        }
    val realisedProfitLossMinor = state.entries
        .mapNotNull { it.realisedProfitLossMinor }
        .sum()
    val investedMinor = holdings.sumOf { it.purchaseCostMinor }
    LaunchedEffect(state.message) {
        if (state.message == "Crypto entry saved.") {
            showAddDialog = false
            buyMoreAsset = null
            sellHolding = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cryptocurrency",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Text(
            text = "Record crypto transactions and realised profit/loss.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.entries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Portfolio summary", style = MaterialTheme.typography.titleMedium)
                    Text("Invested: LKR ${formatMinor(investedMinor)}")
                    Text(
                        "Realised profit/loss: LKR ${
                            formatSignedMinor(realisedProfitLossMinor)
                        }"
                    )
                    Text("Open holdings: ${holdings.size}")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                buyMoreAsset = null
                showAddDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buy Cryptocurrency")
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.entries.isEmpty() -> {
                Text("No cryptocurrency holdings or transactions yet.")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (holdings.isNotEmpty()) {
                        item { Text("Active holdings", style = MaterialTheme.typography.titleMedium) }
                        items(holdings, key = { "holding_${it.assetName.lowercase()}" }) { holding ->
                            CryptoHoldingCard(
                                holding = holding,
                                onDetails = { detailsHolding = holding },
                                onBuyMore = {
                                    buyMoreAsset = holding.assetName
                                    showAddDialog = true
                                },
                                onSell = { sellHolding = holding },
                                onDelete = { holdingToDelete = holding }
                            )
                        }
                        item { Text("Transaction history", style = MaterialTheme.typography.titleMedium) }
                    }
                    items(
                        items = state.entries,
                        key = { it.id }
                    ) { entry ->
                        CryptoEntryCard(
                            entry = entry,
                            onDelete = { holdingToDelete = holdings.firstOrNull {
                                it.entries.any { holdingEntry -> holdingEntry.id == entry.id }
                            } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCryptoEntryDialog(
            initialAsset = buyMoreAsset.orEmpty(),
            saveError = state.error,
            isSaving = state.isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { asset, date, type, amount, quantity, pricePerUnit, currency, rate, costBasis, note ->
                viewModel.addEntry(
                    assetName = asset,
                    date = date,
                    type = type,
                    originalValue = amount,
                    quantity = quantity,
                    pricePerUnit = pricePerUnit,
                    currency = currency,
                    rate = rate,
                    costBasisLkr = costBasis,
                    note = note
                )


            }
        )
    }

    holdingToDelete?.let { holding ->
        AlertDialog(
            onDismissRequest = { holdingToDelete = null },
            title = { Text("Delete holding?") },
            text = {
                Text(
                    "Delete ${holding.assetName}, its ${holding.entries.size} transaction(s), " +
                        "and linked cryptocurrency income or expense records?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHolding(holding)
                        holdingToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { holdingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    detailsHolding?.let { holding ->
        AlertDialog(
            onDismissRequest = { detailsHolding = null },
            title = { Text("${holding.assetName} details") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Text("Quantity owned: ${holding.quantity.stripTrailingZeros().toPlainString()}") }
                    item { Text("Purchase cost: LKR ${formatMinor(holding.purchaseCostMinor)}") }
                    item { Text("Transaction history") }
                    items(holding.entries, key = { "detail_${it.id}" }) { entry ->
                        Text("${entry.type}: ${entry.quantity} for LKR ${formatMinor(entry.baseValueMinor)} (${entry.date})")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsHolding = null }) { Text("Close") }
            }
        )
    }

    sellHolding?.let { holding ->
        AddCryptoEntryDialog(
            initialAsset = holding.assetName,
            initialType = CryptoEntryType.SELL,
            availableQuantity = holding.quantity,
            saveError = state.error,
            isSaving = state.isSaving,
            onDismiss = { sellHolding = null },
            onSave = { asset, date, type, amount, quantity, pricePerUnit, currency, rate, costBasis, note ->
                viewModel.addEntry(
                    asset, date, type, amount, quantity, pricePerUnit,
                    currency, rate, costBasis, note
                )
            }
        )
    }
}

@Composable
private fun CryptoHoldingCard(
    holding: CryptoHolding,
    onDetails: () -> Unit,
    onBuyMore: () -> Unit,
    onSell: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(holding.assetName, style = MaterialTheme.typography.titleMedium)
            Text("Quantity owned: ${holding.quantity.stripTrailingZeros().toPlainString()}")
            Text("Purchase cost: LKR ${formatMinor(holding.purchaseCostMinor)}")
            Text(
                "Average purchase price: LKR ${
                    formatDecimal(holding.averagePurchasePriceMinor)
                } per unit"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDetails) { Text("View Details") }
                TextButton(onClick = onBuyMore) { Text("Buy More") }
                TextButton(onClick = onSell) { Text("Sell") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CryptoEntryCard(
    entry: CryptoEntry,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = entry.assetName,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Date: ${entry.date}")
            Text("Type: ${entry.type}")

            Text(
                "Original value: ${formatMinor(entry.originalValueMinor)} " +
                        entry.originalCurrency
            )

            if (entry.originalCurrency != "LKR") {
                Text(
                    "LKR equivalent: LKR ${formatMinor(entry.baseValueMinor)}"
                )
            }

            val profitLoss = entry.realisedProfitLossMinor

            if (profitLoss != null) {
                val resultColor = when {
                    profitLoss > 0L -> MaterialTheme.colorScheme.primary
                    profitLoss < 0L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Text(
                    text = "Realised P/L: LKR ${formatMinor(profitLoss)}",
                    color = resultColor,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (entry.note.isNotBlank()) {
                Text("Note: ${entry.note}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AddCryptoEntryDialog(
    initialAsset: String = "",
    initialType: String = CryptoEntryType.BUY,
    availableQuantity: BigDecimal? = null,
    saveError: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        asset: String,
        date: String,
        type: String,
        amount: String,
        quantity: String,
        pricePerUnit: String,
        currency: String,
        rate: String,
        costBasis: String,
        note: String
    ) -> Unit
) {
    var asset by remember(initialAsset) { mutableStateOf(initialAsset) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf("") }
    var quantity by remember(initialAsset) { mutableStateOf("") }
    var pricePerUnit by remember(initialAsset) { mutableStateOf("") }
    var currency by remember { mutableStateOf("LKR") }
    var rate by remember { mutableStateOf("1") }
    var costBasis by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialType == CryptoEntryType.BUY && initialAsset.isNotBlank()) {
                    "Buy More ${initialAsset}"
                } else {
                    "Add Crypto Entry"
                }
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                item {
                    if (availableQuantity != null) {
                        Text("Available quantity: ${availableQuantity.stripTrailingZeros().toPlainString()}")
                    }
                }

                item {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            quantity = it
                            amount = calculateTotal(it, pricePerUnit)
                        },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == CryptoEntryType.BUY) {
                    item {
                        OutlinedTextField(
                            value = pricePerUnit,
                            onValueChange = {
                                pricePerUnit = it
                                amount = calculateTotal(quantity, it)
                            },
                            label = { Text("Price per unit ($currency)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = asset,
                        onValueChange = { asset = it },
                        label = { Text("Asset name (e.g. Bitcoin)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        OutlinedButton(
                            onClick = { typeMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Type: $type")
                        }

                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            listOf(CryptoEntryType.BUY, CryptoEntryType.SELL).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        type = option
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Original value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        OutlinedButton(
                            onClick = { currencyMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Currency: $currency")
                        }

                        DropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            CryptoEntryOptions.currencies.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        currency = option
                                        rate = if (option == "LKR") "1" else ""
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    CurrencyRateField(original = currency, value = rate, onChange = { rate = it }, enabled = !isSaving)
                }

                if (type == CryptoEntryType.SELL ||
                    type == CryptoEntryType.RESULT
                ) {
                    item {
                        OutlinedTextField(
                            value = costBasis,
                            onValueChange = { costBasis = it },
                            label = { Text("Cost basis (LKR)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    validationError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    validationError = CryptoEntryValidator.validate(
                        assetName = asset,
                        date = date,
                        type = type,
                        originalValue = amount,
                        currency = currency,
                        rate = rate,
                        costBasisLkr = costBasis
                    )

                    if (validationError == null) {
                        onSave(
                            asset,
                            date,
                            type,
                            amount,
                            quantity,
                            pricePerUnit,
                            currency,
                            rate,
                            costBasis,
                            note
                        )
                    }

                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun calculateTotal(quantity: String, pricePerUnit: String): String {
    val quantityValue = quantity.toBigDecimalOrNull() ?: return ""
    val priceValue = pricePerUnit.toBigDecimalOrNull() ?: return ""
    return quantityValue
        .multiply(priceValue)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

private fun formatMinor(value: Long): String {
    return BigDecimal.valueOf(value, 2)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toPlainString()
}

private fun formatSignedMinor(value: Long): String {
    val sign = if (value > 0L) "+" else ""
    return "$sign${formatMinor(value)}"
}

private fun formatDecimal(value: BigDecimal): String {
    return value.stripTrailingZeros().toPlainString()
}
//thisanth