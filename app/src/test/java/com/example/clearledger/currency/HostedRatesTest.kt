package com.example.clearledger.currency

import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.repository.RateMemoryClock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test

class HostedRatesTest {
    private val now = 1_700_000_000_000L
    private fun body(rate: String = "300.5000000000000000001", original: String = "USD",
        quoted: Long = now / 1000 - 30, next: Long = now / 1000 + 86400) = """
        {"result":"success","base_code":"$original","time_last_update_unix":$quoted,
        "time_next_update_unix":$next,"time_eol_unix":0,"rates":{"LKR":$rate,"EUR":0.91}}
    """.trimIndent()
    private class Cache : QuoteCache {
        val values = mutableMapOf<String, QuoteCacheRecord>()
        override suspend fun read(original: String) = values[original]
        override suspend fun write(original: String, record: QuoteCacheRecord) { values[original] = record }
    }
    @Test fun directionPrecisionTimesAndNonLkrPair() = runTest {
        var calls = 0
        val rates = OpenAccessRates(RateTransport { calls++; RateHttpResponse(200, body()) }, Cache(), RateMemoryClock { now })
        val lkr = (rates.quote("USD", "LKR") as Result.Success).data
        assertEquals("300.5000000000000000001", lkr.ratePlain)
        assertEquals("USD", lkr.original); assertEquals("LKR", lkr.base)
        assertEquals(now - 30_000, lkr.quotedAtMillis); assertEquals(now, lkr.fetchedAtMillis)
        assertEquals("0.91", (rates.quote("USD", "EUR") as Result.Success).data.ratePlain)
        assertEquals(1, calls)
    }
    @Test fun cacheSurvivesProviderReconstructionAndDoesNotInvert() = runTest {
        val cache = Cache(); var calls = 0
        val transport = RateTransport { original -> calls++; RateHttpResponse(200, body(original = original)) }
        OpenAccessRates(transport, cache, RateMemoryClock { now }).quote("USD", "LKR")
        val rates = OpenAccessRates(transport, cache, RateMemoryClock { now })
        assertTrue(rates.quote("USD", "LKR") is Result.Success)
        assertEquals(1, calls)
        assertTrue(rates.quote("LKR", "USD") is Result.Error) // absent USD in deterministic fixture, no inversion
        assertEquals(2, calls)
    }
    @Test fun rejectsBadNumbersWrongBaseMalformedAndStale() {
        for (raw in listOf("0", "-1", "1e999999999", "\"300\"", "NaN", "1.2.3")) {
            assertTrue(runCatching { QuoteDecoder.decode(body(raw), "USD", "LKR", now, now) }.isFailure)
        }
        for (raw in listOf(body(original="GBP"), body(quoted=now/1000+1), body(next=now/1000),
            body().replace("\"result\":\"success\"", "\"result\":\"error\""), body()+"x", "[".repeat(70000))) {
            assertTrue(runCatching { QuoteDecoder.decode(raw, "USD", "LKR", now, now) }.isFailure)
        }
        assertEquals("0.003", QuoteDecoder.decode(body("3e-3"), "USD", "LKR", now, now).ratePlain)
        assertTrue(runCatching { DecimalJson.parse("{\"a\":1,\"a\":2}") }.isFailure)
    }
    @Test fun failureCooldownAndInvalidPairHaveNoAutomaticRetry() = runTest {
        var calls = 0
        val rates = OpenAccessRates(RateTransport { calls++; RateHttpResponse(429, "") }, Cache(), RateMemoryClock { now })
        assertTrue(rates.quote("USD", "LKR") is Result.Error)
        assertTrue(rates.quote("USD", "LKR") is Result.Error)
        assertTrue(rates.quote("XXX", "LKR") is Result.Error)
        assertTrue(rates.quote("LKR", "LKR") is Result.Error)
        assertEquals(1, calls)
    }
    @Test fun timeoutAndNetworkFailureBecomeFallbackResults() = runTest {
        val slow = OpenAccessRates(RateTransport { delay(30_000); RateHttpResponse(200, body()) }, Cache(), RateMemoryClock { now })
        assertTrue(slow.quote("USD", "LKR") is Result.Error)
        val offline = OpenAccessRates(RateTransport { throw java.io.IOException() }, Cache(), RateMemoryClock { now })
        assertTrue(offline.quote("USD", "LKR") is Result.Error)
    }
}
