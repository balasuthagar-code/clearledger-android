package com.example.clearledger.currency

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.model.RateMemoryLookup
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun currencyCompletionDependencies(): CurrencyCompletionEntryPoint {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, CurrencyCompletionEntryPoint::class.java)
    }
}

/**
 * Loads a current rate automatically for a new foreign-currency entry. The rate remains editable,
 * and the caller can immediately show the converted amount as the user types. Existing entries
 * retain their saved rate so editing history never silently changes its recorded conversion.
 */
@Composable
fun CurrencyRateField(
    original: String,
    base: String = "LKR",
    value: String,
    onChange: (String) -> Unit,
    editing: Boolean = false,
    enabled: Boolean = true,
    error: String? = null
) {
    if (original == base) return

    val deps = currencyCompletionDependencies()
    var uid by remember { mutableStateOf(deps.account().currentUid()) }
    DisposableEffect(deps) {
        val subscription = deps.authEvents().observeUid { uid = it }
        onDispose { subscription.cancel() }
    }

    val owner = uid ?: return
    val session = remember(owner, original, base, editing) {
        RateSuggestionSession(RateMemoryContext(owner, original, base), editing)
    }
    val currentValue = rememberUpdatedState(value)
    val change = rememberUpdatedState(onChange)
    var message by remember(session) { mutableStateOf<String?>(null) }
    var fetching by remember(session) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(session) {
        onDispose { session.close() }
    }

    suspend fun loadRate(ticket: Long, explicitRetry: Boolean = false) {
        fun sessionAccepts() = if (explicitRetry) {
            session.acceptsExplicit(ticket, deps.account().currentUid())
        } else {
            session.accepts(ticket, deps.account().currentUid())
        }

        fetching = true
        try {
            when (val result = deps.hosted().quote(original, base)) {
                is Result.Success -> {
                    if (sessionAccepts() && currentValue.value.isBlank()) {
                        change.value(result.data.ratePlain)
                        message = "Latest available rate from ${result.data.source}. Check or edit it before saving."
                    }
                }
                is Result.Error -> {
                    if (!sessionAccepts()) return
                    when (val remembered = deps.memory().lookup(session.context)) {
                        is Result.Success -> {
                            if (sessionAccepts() && currentValue.value.isBlank()) {
                                val saved = remembered.data as? RateMemoryLookup.Remembered
                                if (saved != null) {
                                    change.value(saved.rate.ratePlain)
                                    message = "Using your last saved rate. Check or edit it before saving."
                                } else {
                                    message = "Automatic rate unavailable. Enter the rate manually."
                                }
                            }
                        }
                        is Result.Error -> if (currentValue.value.isBlank()) {
                            message = "Automatic rate unavailable. Enter the rate manually."
                        }
                    }
                }
            }
        } finally {
            fetching = false
        }
    }

    LaunchedEffect(session) {
        if (!editing && currentValue.value.isBlank()) {
            loadRate(session.ticket())
        }
    }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                session.typed()
                message = null
                onChange(it)
            },
            label = { Text("Exchange rate ($base per 1 $original)") },
            enabled = enabled,
            singleLine = true,
            isError = error != null,
            supportingText = {
                (error ?: message)?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (fetching) {
            Text(
                text = "Getting the latest exchange rate…",
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        if (!editing && !fetching && value.isBlank()) {
            androidx.compose.material3.TextButton(
                onClick = { scope.launch { loadRate(session.ticket(), explicitRetry = true) } },
                enabled = enabled
            ) {
                Text("Retry automatic rate")
            }
        }
    }
}
