package com.example.clearledger.core.common

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private const val MONTH_KEY_FORMAT = "yyyy-MM"

    fun dateToMonthKey(date: Date, timeZone: TimeZone): String {
        val sdf = SimpleDateFormat(MONTH_KEY_FORMAT, Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(date)
    }

    fun timestampToMonthKey(timestamp: Timestamp, timeZone: TimeZone): String {
        return dateToMonthKey(timestamp.toDate(), timeZone)
    }
}
