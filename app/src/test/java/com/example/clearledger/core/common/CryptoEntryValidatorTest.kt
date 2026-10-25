package com.example.clearledger.core.common

import com.example.clearledger.domain.model.CryptoEntryType
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CryptoEntryValidatorTest {

    private fun validate(
        amount: String = "100.00",
        currency: String = "USD",
        rate: String = "300.50",
        type: String = CryptoEntryType.SELL,
        cost: String = "20000.00",
        asset: String = "BTC",
        date: String = LocalDate.now().toString()
    ): String? = CryptoEntryValidator.validate(
        assetName = asset,
        date = date,
        type = type,
        originalValue = amount,
        currency = currency,
        rate = rate,
        costBasisLkr = cost
    )

    private fun convert(
        amount: String = "100.00",
        currency: String = "USD",
        rate: String = "300.50",
        type: String = CryptoEntryType.SELL,
        cost: String = "20000.00"
    ) = CryptoEntryValidator.convert(
        date = LocalDate.now().toString(),
        type = type,
        originalValue = amount,
        currency = currency,
        rate = rate,
        costBasisLkr = cost
    )

    @Test
    fun sellAndResultPreserveExactValues() {
        for (type in listOf(CryptoEntryType.SELL, CryptoEntryType.RESULT)) {
            assertNull(validate(type = type))
            val values = requireNotNull(convert(type = type))

            assertEquals(10_000L, values.originalValueMinor)
            assertEquals(3_005_000L, values.baseValueMinor)
            assertEquals(2_000_000L, values.costBasisBaseMinor)
            assertEquals("300.5", values.exchangeRate)
            assertEquals(
                LocalDate.now().toString().take(7),
                values.monthKey
            )
        }
    }

    @Test
    fun buyDoesNotRequireCostBasis() {
        assertNull(validate(type = CryptoEntryType.BUY, cost = ""))
        val values = requireNotNull(
            convert(type = CryptoEntryType.BUY, cost = "")
        )
        assertEquals(0L, values.costBasisBaseMinor)
    }

    @Test
    fun zeroCostBasisIsAllowedForSell() {
        assertNull(validate(cost = "0"))
        assertEquals(
            0L,
            requireNotNull(convert(cost = "0")).costBasisBaseMinor
        )
    }

    @Test
    fun excessPrecisionAndInvalidAmountsAreRejected() {
        for (amount in listOf(
            "1.234", "0", "-1", "abc", "92233720368547758.08"
        )) {
            assertNotNull(validate(amount = amount))
            assertNull(convert(amount = amount))
        }
    }

    @Test
    fun invalidCostBasisIsRejected() {
        for (type in listOf(CryptoEntryType.SELL, CryptoEntryType.RESULT)) {
            for (cost in listOf(
                "", "-1", "1.234", "abc", "92233720368547758.08"
            )) {
                assertNotNull(validate(type = type, cost = cost))
                assertNull(convert(type = type, cost = cost))
            }
        }
    }

    @Test
    fun invalidRatesAndConversionsAreRejected() {
        for (rate in listOf(
            "", "0", "-1", "abc",
            "1000000000000000000", "0.000000001"
        )) {
            assertNotNull(validate(rate = rate))
            assertNull(convert(rate = rate))
        }
    }

    @Test
    fun lkrRequiresIdentityRate() {
        assertNull(validate(currency = "LKR", rate = "1"))

        val values = requireNotNull(
            convert(currency = "LKR", rate = "1")
        )
        assertEquals(10_000L, values.baseValueMinor)
        assertEquals("1", values.exchangeRate)

        assertNotNull(validate(currency = "LKR", rate = "2"))
        assertNull(convert(currency = "LKR", rate = "2"))
    }

    @Test
    fun unsupportedCurrencyIsRejected() {
        assertNotNull(validate(currency = "JPY"))
        assertNull(convert(currency = "JPY"))
    }

    @Test
    fun conversionRoundsHalfUp() {
        val values = requireNotNull(
            convert(amount = "0.01", rate = "0.5")
        )
        assertEquals(1L, values.baseValueMinor)
    }

    @Test
    fun originalRequiredFieldRulesRemain() {
        assertNotNull(validate(asset = ""))
        assertNotNull(validate(date = "invalid"))
        assertNotNull(
            validate(date = LocalDate.now().plusDays(1).toString())
        )
        assertNotNull(validate(type = "Unknown"))
    }
}