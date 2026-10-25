package com.example.clearledger.data.repository

import com.example.clearledger.core.common.CurrencyCore
import com.example.clearledger.core.common.Result
import com.example.clearledger.domain.model.RateMemoryContext
import com.example.clearledger.domain.model.RateMemoryException
import com.example.clearledger.domain.model.RateMemoryFailure
import com.example.clearledger.domain.model.RememberedRate

internal object RateMemoryCodec {
    const val MAX_UID_LENGTH = 256
    const val MAX_INPUT_LENGTH = 128
    const val MAX_RECORD_LENGTH = 96

    /** Exact UTF-16 code-unit hex encoding: collision-free even for unusual JVM String values. */
    fun key(context: RateMemoryContext): String {
        require(context.uid.length in 1..MAX_UID_LENGTH)
        val encodedUid = buildString(context.uid.length * 4) {
            context.uid.forEach { append(it.code.toString(16).padStart(4, '0')) }
        }
        return "rm1:$encodedUid:${context.originalCurrency}:${context.baseCurrency}"
    }

    fun canonicalInput(text: String): String {
        if (text.length > MAX_INPUT_LENGTH) fail(RateMemoryFailure.INVALID_RATE)
        return when (val parsed = CurrencyCore.parseRate(text)) {
            is Result.Success -> parsed.data.stripTrailingZeros().toPlainString()
            is Result.Error -> fail(RateMemoryFailure.INVALID_RATE)
        }
    }

    fun encode(rate: RememberedRate): String = "1|${rate.rememberedAtEpochMillis}|${rate.ratePlain}"

    fun decode(record: String): RememberedRate {
        // Bounds BEFORE splitting, regex matching or BigDecimal parsing.
        if (record.length > MAX_RECORD_LENGTH) fail(RateMemoryFailure.CORRUPT_RECORD)
        val parts = record.split('|')
        if (parts.size != 3 || parts[0] != "1") fail(RateMemoryFailure.CORRUPT_RECORD)
        val timeText = parts[1]
        if (timeText.length !in 1..19 || timeText.any { it !in '0'..'9' }) fail(RateMemoryFailure.CORRUPT_RECORD)
        val time = timeText.toLongOrNull() ?: fail(RateMemoryFailure.CORRUPT_RECORD)
        if (time < 0 || time.toString() != timeText) fail(RateMemoryFailure.CORRUPT_RECORD)
        val rateText = parts[2]
        if (rateText.length !in 1..64) fail(RateMemoryFailure.CORRUPT_RECORD)
        val parsed = CurrencyCore.parseRate(rateText)
        if (parsed !is Result.Success || parsed.data.stripTrailingZeros().toPlainString() != rateText) {
            fail(RateMemoryFailure.CORRUPT_RECORD)
        }
        return RememberedRate(rateText, time)
    }

    private fun fail(reason: RateMemoryFailure): Nothing = throw RateMemoryException(reason)
}
