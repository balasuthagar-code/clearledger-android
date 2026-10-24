package com.example.clearledger.core.common

import org.junit.Assert.*
import org.junit.Test

class FreelanceProjectValidatorTest {

    private fun validate(
        amount: String,
        currency: String = "USD",
        rate: String = "300.50"
    ): String? = FreelanceProjectValidator.validate(
        name = "Website project",
        client = "Test client",
        agreedAmount = amount,
        currency = currency,
        rate = rate
    )

    @Test
    fun foreignProjectConvertsCorrectly() {
        assertNull(validate("100.00"))

        val values = requireNotNull(
            FreelanceProjectValidator.convert("100.00", "USD", "300.50")
        )

        assertEquals(10_000L, values.agreedAmountMinor)
        assertEquals(3_005_000L, values.baseAmountMinor)
        assertEquals("300.5", values.exchangeRate)
    }

    @Test
    fun lkrProjectUsesRateOne() {
        assertNull(validate("100.00", "LKR", "1"))

        val values = requireNotNull(
            FreelanceProjectValidator.convert("100.00", "LKR", "1")
        )

        assertEquals(10_000L, values.baseAmountMinor)
        assertEquals("1", values.exchangeRate)

        assertNotNull(validate("100.00", "LKR", "2"))
        assertNull(
            FreelanceProjectValidator.convert("100.00", "LKR", "2")
        )
    }

    @Test
    fun invalidAmountsNeverProduceProjectValues() {
        for (amount in listOf(
            "1.234", "0", "-1", "abc", "92233720368547758.08"
        )) {
            assertNotNull(validate(amount))
            assertNull(
                FreelanceProjectValidator.convert(amount, "USD", "300.50")
            )
        }
    }

    @Test
    fun invalidRatesNeverProduceProjectValues() {
        for (rate in listOf("", "0", "-1", "abc")) {
            assertNotNull(validate("100.00", rate = rate))
            assertNull(
                FreelanceProjectValidator.convert("100.00", "USD", rate)
            )
        }
    }

    @Test
    fun unsupportedCurrencyIsRejected() {
        assertNotNull(validate("100.00", currency = "JPY"))
        assertNull(
            FreelanceProjectValidator.convert("100.00", "JPY", "1")
        )
    }

    @Test
    fun conversionOverflowAndRoundedZeroAreRejected() {
        for (rate in listOf(
            "1000000000000000000", "0.000000001"
        )) {
            assertNotNull(validate("100.00", rate = rate))
            assertNull(
                FreelanceProjectValidator.convert("100.00", "USD", rate)
            )
        }
    }

    @Test
    fun conversionUsesHalfUpRounding() {
        val values = requireNotNull(
            FreelanceProjectValidator.convert("0.01", "USD", "0.5")
        )

        assertEquals(1L, values.agreedAmountMinor)
        assertEquals(1L, values.baseAmountMinor)
    }

    @Test
    fun projectAndMilestoneRequiredFieldsRemainValidated() {
        assertNotNull(
            FreelanceProjectValidator.validate(
                "", "Client", "100", "LKR", "1"
            )
        )
        assertNotNull(
            FreelanceProjectValidator.validate(
                "Project", "", "100", "LKR", "1"
            )
        )

        assertNull(
            FreelanceProjectValidator.validateMilestone(
                "Design", "100", "2026-11-01"
            )
        )
        assertNotNull(
            FreelanceProjectValidator.validateMilestone(
                "", "100", "2026-11-01"
            )
        )
        assertNotNull(
            FreelanceProjectValidator.validateMilestone(
                "Design", "0", "2026-11-01"
            )
        )
        assertNotNull(
            FreelanceProjectValidator.validateMilestone(
                "Design", "100", ""
            )
        )
    }
}