package com.example.clearledger.ui.dashboard

import java.math.BigDecimal
import java.util.Locale

/**
 * Minor units -> display text: 125050 -> "LKR 1,250.50", -340000 -> "-LKR 3,400.00".
 * Uses BigDecimal, so no floating-point rounding. If Member 2's currency helper handles this
 * later, delete this file and use theirs.
 */
internal fun formatMinorUnits(minor: Long, currency: String = "LKR"): String {
    val text = String.format(Locale.US, "%,.2f", BigDecimal.valueOf(minor, 2).abs())
    return if (minor < 0) "-$currency $text" else "$currency $text"
}
