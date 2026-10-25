package com.example.clearledger.currency

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.model.RateMemoryLookup
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun currencyCompletionDependencies(): CurrencyCompletionEntryPoint {
    val context = LocalContext.current.applicationContext
    return remember(context) { EntryPointAccessors.fromApplication(context, CurrencyCompletionEntryPoint::class.java) }
}

/** Editable suggestion field, shared by the five connected forms. Caller clears rate on pair change. */
@Composable
fun CurrencyRateField(original: String, base: String = "LKR", value: String,
    onChange: (String) -> Unit, editing: Boolean = false, enabled: Boolean = true,
    error: String? = null) {
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
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    var message by remember(session) { mutableStateOf<String?>(null) }
    var quote by remember(session) { mutableStateOf<HostedQuote?>(null) }
    var quoteTicket by remember(session) { mutableStateOf(0L) }
    var fetching by remember(session) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    DisposableEffect(session) { onDispose { session.close() } }
    LaunchedEffect(session) {
        if (!editing && value.isBlank()) {
            val ticket = session.ticket()
            when (val result = deps.memory().lookup(session.context)) {
                is Result.Success -> if (session.accepts(ticket, deps.account().currentUid()) && currentValue.isBlank()) {
                    val saved = result.data as? RateMemoryLookup.Remembered
                    if (saved != null) { change(saved.rate.ratePlain); message = "Last confirmed rate — check before saving" }
                }
                is Result.Error -> if (session.accepts(ticket, deps.account().currentUid())) {
                    message = "Remembered rate unavailable. Enter a rate or request a suggestion."
                }
            }
        }
    }
    Column {
        OutlinedTextField(value = value, onValueChange = {
            session.typed(); quote = null; message = null; onChange(it)
        }, label = { Text("Exchange rate ($base per 1 $original)") },
            enabled = enabled, singleLine = true, isError = error != null,
            supportingText = { (error ?: message)?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        if (!editing) {
            TextButton(enabled = enabled && !fetching, onClick = {
                val ticket = session.ticket()
                fetching = true; quote = null
                scope.launch {
                    try {
                        when (val result = deps.hosted().quote(original, base)) {
                            is Result.Success -> if (session.acceptsExplicit(ticket, deps.account().currentUid())) {
                                quote = result.data; quoteTicket = ticket
                                message = "Suggestion only — confirm the rate you actually used"
                            }
                            is Result.Error -> if (session.acceptsExplicit(ticket, deps.account().currentUid())) {
                                message = "Rate unavailable or stale. You can enter it manually."
                            }
                        }
                    } finally { fetching = false }
                }
            }) { Text(if (fetching) "Getting suggestion…" else "Get rate suggestion") }
            quote?.let { suggestion ->
                Text("${suggestion.ratePlain} · quoted ${java.time.Instant.ofEpochMilli(suggestion.quotedAtMillis)}")
                TextButton(enabled = enabled, onClick = {
                    val now = System.currentTimeMillis()
                    if (session.acceptsExplicit(quoteTicket, deps.account().currentUid()) &&
                        now >= suggestion.quotedAtMillis && now - suggestion.quotedAtMillis <= 172_800_000L &&
                        now < suggestion.nextUpdateMillis) {
                        session.typed(); change(suggestion.ratePlain); quote = null
                    } else { quote = null; message = "Suggestion expired. Request another or enter a rate." }
                }) { Text("Use suggestion") }
            }
            TextButton(onClick = { uri.openUri("https://www.exchangerate-api.com") }) { Text("Rates By Exchange Rate API") }
        }
    }
}
