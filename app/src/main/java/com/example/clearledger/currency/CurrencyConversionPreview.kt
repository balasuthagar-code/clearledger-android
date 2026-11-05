package com.example.clearledger.currency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.clearledger.core.common.CurrencyCore
import com.example.clearledger.core.common.Result
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Shows the calculated base-currency amount while the user edits the original amount or rate. */
@Composable
fun CurrencyConversionPreview(
    amountText: String,
    originalCurrency: String,
    baseCurrency: String,
    exchangeRateText: String
) {
    if (amountText.isBlank() || exchangeRateText.isBlank()) return

    val originalMinor = (CurrencyCore.parseAmount(amountText, originalCurrency) as? Result.Success)?.data
        ?: return
    val rate = if (originalCurrency == baseCurrency) {
        BigDecimal.ONE
    } else {
        (CurrencyCore.parseRate(exchangeRateText) as? Result.Success)?.data ?: return
    }
    val conversion = (CurrencyCore.convert(
        originalAmountMinor = originalMinor,
        originalCurrency = originalCurrency,
        baseCurrency = baseCurrency,
        exchangeRate = rate
    ) as? Result.Success)?.data ?: return

    val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(conversion.baseAmountMinor, 2))

    Text(
        text = "Estimated amount in $baseCurrency: $formatted",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Calculated with the rate shown above. Your bank or card provider may use a different rate.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
