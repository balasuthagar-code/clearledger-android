package com.example.clearledger.currency

/** Positive integer ceiling without overflow in (amount + divisor - 1). */
object ExactProjection {
    fun monthly(remainingMinor: Long, months: Long): Long {
        require(remainingMinor >= 0 && months > 0)
        return remainingMinor / months + if (remainingMinor % months == 0L) 0L else 1L
    }
}
