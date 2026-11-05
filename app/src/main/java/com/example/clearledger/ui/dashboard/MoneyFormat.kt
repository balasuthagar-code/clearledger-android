package com.example.clearledger.ui.dashboard

import java.math.BigDecimal
import java.util.Locale


internal fun formatMinorUnits(minor: Long, currency: String = "LKR"): String {
    val text = String.format(Locale.US, "%,.2f", BigDecimal.valueOf(minor, 2).abs())
    return if (minor < 0) "-$currency $text" else "$currency $text"
}
