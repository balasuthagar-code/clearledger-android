package com.example.clearledger.currency

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.CurrencyCore
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.repository.RateMemoryClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

/** Base major units per original major unit. Provider time is NOT local confirmation time. */
data class HostedQuote(val original: String, val base: String, val ratePlain: String,
    val quotedAtMillis: Long, val fetchedAtMillis: Long, val nextUpdateMillis: Long,
    val source: String = "ExchangeRate-API")
interface HostedRates { suspend fun quote(original: String, base: String): Result<HostedQuote> }
data class RateHttpResponse(val status: Int, val body: String)
fun interface RateTransport { suspend fun get(original: String): RateHttpResponse }
data class QuoteCacheRecord(val attemptedAt: Long, val fetchedAt: Long, val body: String)
interface QuoteCache {
    suspend fun read(original: String): QuoteCacheRecord?
    suspend fun write(original: String, record: QuoteCacheRecord)
}
enum class QuoteFailure { INVALID_PAIR, UNSUPPORTED_PAIR, NON_POSITIVE_RATE, UNAVAILABLE, HTTP, MALFORMED, STALE, THROTTLED, CACHE }
class QuoteException(val failure: QuoteFailure) : IllegalStateException(failure.name)

/** Shared across forms. Persistent public-data cache; never writes RateMemoryRepository. */
class OpenAccessRates(private val transport: RateTransport, private val cache: QuoteCache,
    private val clock: RateMemoryClock) : HostedRates {
    private val mutex = Mutex()
    override suspend fun quote(original: String, base: String): Result<HostedQuote> = try {
        requirePair(original, base)
        mutex.withLock {
            val now = clock.nowEpochMillis()
            require(now >= 0)
            val cached = try { cache.read(original) } catch (e: CancellationException) { throw e } catch (_: Exception) { fail(QuoteFailure.CACHE) }
            if (cached != null) {
                val parsed = runCatching { QuoteDecoder.decode(cached.body, original, base, cached.fetchedAt, now) }.getOrNull()
                if (parsed != null && now < parsed.nextUpdateMillis) return@withLock Result.Success(parsed)
                if (now < cached.attemptedAt || now - cached.attemptedAt < 3_600_000L) fail(QuoteFailure.THROTTLED)
            }
            // Persist cooldown before networking, including failed requests and across restarts.
            cache.write(original, QuoteCacheRecord(now, cached?.fetchedAt ?: 0, cached?.body.orEmpty()))
            val response = withTimeout(15_000) { transport.get(original) }
            if (response.status !in 200..299) fail(QuoteFailure.HTTP)
            val fetched = clock.nowEpochMillis()
            val quote = QuoteDecoder.decode(response.body, original, base, fetched, fetched)
            cache.write(original, QuoteCacheRecord(now, fetched, response.body))
            Result.Success(quote)
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        Result.Error(QuoteException(QuoteFailure.UNAVAILABLE))
    } catch (e: CancellationException) { throw e
    } catch (e: QuoteException) { Result.Error(e)
    } catch (_: Exception) { Result.Error(QuoteException(QuoteFailure.UNAVAILABLE)) }
    private fun requirePair(original: String, base: String) {
        if (original !in Currencies.supported || base !in Currencies.supported || original == base) fail(QuoteFailure.INVALID_PAIR)
    }
}
internal fun fail(reason: QuoteFailure): Nothing = throw QuoteException(reason)

internal object QuoteDecoder {
    fun decode(body: String, original: String, base: String, fetched: Long, now: Long): HostedQuote {
        val root = DecimalJson.parse(body) as? Map<*, *> ?: fail(QuoteFailure.MALFORMED)
        if (root["result"] != "success" || root["base_code"] != original) fail(QuoteFailure.MALFORMED)
        fun time(key: String): Long {
            val token = root[key] as? JsonNumber ?: fail(QuoteFailure.MALFORMED)
            val seconds = token.text.toLongOrNull()?.takeIf { it >= 0 } ?: fail(QuoteFailure.MALFORMED)
            return try { Math.multiplyExact(seconds, 1000) } catch (_: ArithmeticException) { fail(QuoteFailure.MALFORMED) }
        }
        val quoted = time("time_last_update_unix")
        val next = time("time_next_update_unix")
        val eol = time("time_eol_unix")
        if (fetched < 0 || now < fetched || quoted > now || next <= quoted ||
            now - quoted > 172_800_000L || now >= next || (eol != 0L && now >= eol)) fail(QuoteFailure.STALE)
        val rates = root["rates"] as? Map<*, *> ?: fail(QuoteFailure.MALFORMED)
        if (!rates.containsKey(base)) fail(QuoteFailure.UNSUPPORTED_PAIR)
        val raw = (rates[base] as? JsonNumber)?.text ?: fail(QuoteFailure.MALFORMED)
        if (raw.length > 64) fail(QuoteFailure.MALFORMED)
        val decimal = try { BigDecimal(raw) } catch (_: NumberFormatException) { fail(QuoteFailure.MALFORMED) }
        if (decimal.signum() <= 0) fail(QuoteFailure.NON_POSITIVE_RATE)
        // Bound exponent BEFORE expansion. JSON numeric tokens never pass through Double.
        if (decimal.scale() !in -32..32 || decimal.precision() > 64) fail(QuoteFailure.MALFORMED)
        val plain = decimal.stripTrailingZeros().toPlainString()
        if (CurrencyCore.parseRate(plain) !is Result.Success) fail(QuoteFailure.MALFORMED)
        return HostedQuote(original, base, plain, quoted, fetched, next)
    }
}
