package com.example.clearledger.data.dto

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MonthKey
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.domain.model.Expense
import com.example.clearledger.domain.model.ExpenseClassification
import com.example.clearledger.domain.model.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Converts between a Firestore document (a plain Map) and the domain [Expense].
 *
 * Why a Map and not a DTO class like CategoryDto? Two reasons:
 *  1. It avoids the Firestore "isXxx" Java-bean naming trap completely.
 *  2. It is pure Kotlin, so it is unit tested without Android or Firebase.
 *
 * The reader is deliberately forgiving (dates may be a Date, an ISO string or epoch millis; numbers
 * may be Int/Long/Double) so documents written by an earlier version of your quick-add screen
 * still load. The writer always produces the SRS schema:
 *
 *   users/{uid}/expenses/{id}:
 *     amountMinor, currency, exchangeRate (String), baseAmountMinor, categoryId,
 *     classification ("COMMITTED"/"DISCRETIONARY"), paymentMethod, merchant, note,
 *     date (Timestamp), monthKey ("YYYY-MM"), createdAt, updatedAt,
 *     recurringId (only when created by confirming a recurring item)
 *
 * The repository converts Firestore Timestamp -> java.util.Date before calling [fromMap].
 */
object ExpenseMapper {

    private val MONTH_KEY = Regex("^\\d{4}-\\d{2}$")

    /** Returns null for a document that is too broken to show (no amount or no usable date). */
    fun fromMap(id: String, data: Map<String, Any?>): Expense? {
        val amountMinor = data.asLong("amountMinor") ?: return null
        val date = parseDate(data["date"]) ?: return null

        val currency = (data["currency"] as? String)?.takeIf { it.isNotBlank() } ?: Currencies.BASE
        val exchangeRate = when (val raw = data["exchangeRate"]) {
            is String -> raw.takeIf { it.isNotBlank() } ?: "1"
            is Number -> raw.toString()
            else -> "1"
        }
        val baseAmountMinor = data.asLong("baseAmountMinor")
            ?: if (currency == Currencies.BASE) amountMinor
            else MoneyInput.parseRate(exchangeRate)?.let { MoneyInput.toBaseMinor(amountMinor, it) }
            ?: return null

        val monthKey = (data["monthKey"] as? String)?.takeIf { MONTH_KEY.matches(it) } ?: MonthKey.of(date)

        return Expense(
            id = id,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRate = exchangeRate,
            baseAmountMinor = baseAmountMinor,
            categoryId = (data["categoryId"] as? String).orEmpty(),
            classification = enumOrDefault(data["classification"], ExpenseClassification.DISCRETIONARY),
            paymentMethod = enumOrDefault(data["paymentMethod"], PaymentMethod.OTHER),
            merchant = (data["merchant"] as? String).orEmpty(),
            note = (data["note"] as? String).orEmpty(),
            date = date,
            monthKey = monthKey,
            createdAtMillis = parseMillis(data["createdAt"]) ?: 0L,
            recurringId = (data["recurringId"] as? String)?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Document fields for Firestore. [isNew] adds createdAt; an update leaves createdAt untouched
     * (the repository uses update(), which changes only the fields in this map).
     */
    fun toMap(expense: Expense, now: Date, isNew: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "amountMinor" to expense.amountMinor,
            "currency" to expense.currency,
            "exchangeRate" to expense.exchangeRate,
            "baseAmountMinor" to expense.baseAmountMinor,
            "categoryId" to expense.categoryId,
            "classification" to expense.classification.name,
            "paymentMethod" to expense.paymentMethod.name,
            "merchant" to expense.merchant,
            "note" to expense.note,
            "date" to toDate(expense.date),
            "monthKey" to expense.monthKey,
            "updatedAt" to now
        )
        if (isNew) map["createdAt"] = now
        expense.recurringId?.let { map["recurringId"] = it }
        return map
    }

    internal fun toDate(date: LocalDate): Date =
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

    internal fun parseDate(value: Any?): LocalDate? = when (value) {
        is Date -> value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        is String -> runCatching { LocalDate.parse(value.trim().take(10)) }.getOrNull()
        is Number -> Instant.ofEpochMilli(value.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
        else -> null
    }

    private fun parseMillis(value: Any?): Long? = when (value) {
        is Date -> value.time
        is Number -> value.toLong()
        else -> null
    }

    private fun Map<String, Any?>.asLong(key: String): Long? = (this[key] as? Number)?.toLong()

    private inline fun <reified E : Enum<E>> enumOrDefault(value: Any?, default: E): E {
        val name = (value as? String)?.trim()?.uppercase()?.replace(' ', '_') ?: return default
        return enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
