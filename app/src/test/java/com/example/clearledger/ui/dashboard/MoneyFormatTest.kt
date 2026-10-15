package com.example.clearledger.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {
    @Test fun formatsMinorUnitsWithSeparatorsAndTwoDecimals() =
        assertEquals("LKR 1,250.50", formatMinorUnits(125_050))

    @Test fun formatsLargeSalary() =
        assertEquals("LKR 130,000.00", formatMinorUnits(13_000_000))

    @Test fun formatsZero() = assertEquals("LKR 0.00", formatMinorUnits(0))

    @Test fun formatsNegativeWithLeadingMinus() =
        assertEquals("-LKR 3,400.00", formatMinorUnits(-340_000))

    @Test fun formatsSmallAmountsBelowOneRupee() =
        assertEquals("LKR 0.05", formatMinorUnits(5))
}
