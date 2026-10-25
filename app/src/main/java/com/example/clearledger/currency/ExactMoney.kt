package com.example.clearledger.currency

import java.math.BigInteger

/** Aggregation either returns an exact Long or throws ArithmeticException; never wraps or caps. */
fun <T> Iterable<T>.sumMinor(value: (T) -> Long): Long =
    fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(value(item)) }.longValueExact()
fun differenceMinor(first: Long, vararg rest: Long): Long =
    rest.fold(BigInteger.valueOf(first)) { total, item -> total - BigInteger.valueOf(item) }.longValueExact()
