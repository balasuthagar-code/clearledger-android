package com.example.clearledger.currency

import android.content.Context
import com.example.clearledger.core.common.Currencies
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpsRateTransport(private val io: CoroutineDispatcher) : RateTransport {
    override suspend fun get(original: String): RateHttpResponse = withContext(io) {
        require(original in Currencies.supported)
        val context = currentCoroutineContext()
        val deadline = System.nanoTime() + 12_000_000_000L
        val connection = URL("https://open.er-api.com/v6/latest/$original").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            context.ensureActive()
            val status = connection.responseCode
            if (status !in 200..299) return@withContext RateHttpResponse(status, "")
            val body = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    context.ensureActive()
                    if (System.nanoTime() > deadline) throw IOException("Quote timeout")
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 65_536) throw IOException("Quote too large")
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
            RateHttpResponse(status, body)
        } finally { connection.disconnect() }
    }
}

/** Public quote cache, separate from per-user confirmed memory. Atomic single-record commits. */
class PreferencesQuoteCache(context: Context, private val io: CoroutineDispatcher) : QuoteCache {
    private val app = context.applicationContext
    private val mutex = Mutex()
    private val uncertain = mutableSetOf<String>()
    override suspend fun read(original: String): QuoteCacheRecord? = withContext(io) {
        mutex.withLock {
            require(original in Currencies.supported)
            if (original in uncertain) throw IOException("Cache unavailable")
            val raw = app.getSharedPreferences("currency_public_quotes_v1", Context.MODE_PRIVATE)
                .getString(original, null) ?: return@withLock null
            if (raw.length > 65_600) throw IOException("Invalid cache")
            val parts = raw.split('\n', limit = 3)
            if (parts.size != 3) throw IOException("Invalid cache")
            QuoteCacheRecord(parts[0].toLongOrNull()?.takeIf { it >= 0 } ?: throw IOException("Invalid cache"),
                parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: throw IOException("Invalid cache"), parts[2])
        }
    }
    override suspend fun write(original: String, record: QuoteCacheRecord): Unit = withContext(io) {
        mutex.withLock {
            require(original in Currencies.supported && record.body.length <= 65_536)
            uncertain += original
            val ok = app.getSharedPreferences("currency_public_quotes_v1", Context.MODE_PRIVATE).edit()
                .putString(original, "${record.attemptedAt}\n${record.fetchedAt}\n${record.body}").commit()
            if (!ok) throw IOException("Cache write failed")
            uncertain -= original
        }
    }
}
