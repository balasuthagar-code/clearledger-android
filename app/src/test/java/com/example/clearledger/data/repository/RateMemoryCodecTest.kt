package com.example.clearledger.data.repository

import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.model.RememberedRate
import org.junit.Assert.*
import org.junit.Test

class RateMemoryCodecTest {
    @Test fun exactUidEncodingCannotCollideWithSeparatorsPairsOrUnicode() {
        val contexts = listOf("a", "a:USD:LKR", "a|USD|LKR", "a/b", "a:b", "சானோ", "\uD800", "\uD801")
            .flatMap { uid -> listOf(RateMemoryContext(uid, "USD", "LKR"), RateMemoryContext(uid, "LKR", "USD")) }
        val keys = contexts.map(RateMemoryCodec::key)
        assertEquals(contexts.size, keys.toSet().size)
        assertTrue(keys.all { key -> key.all { it.code < 128 } })
    }
    @Test fun boundedLargestCanonicalRateAndLongTimestampRoundTrip() {
        val value = RememberedRate("9".repeat(64), Long.MAX_VALUE)
        assertEquals(value, RateMemoryCodec.decode(RateMemoryCodec.encode(value)))
        assertEquals(RememberedRate("1", 0), RateMemoryCodec.decode("1|0|1"))
    }
}
