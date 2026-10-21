package com.example.clearledger.data.dto

import com.example.clearledger.core.common.Currencies
import com.example.clearledger.core.common.MoneyInput
import com.example.clearledger.domain.model.RecurringFrequency
import com.example.clearledger.domain.model.RecurringItem
import java.util.Date

/**
 * Firestore document <-> RecurringItem, using a plain Map (no Java-bean "isActive" trap, and pure
 * Kotlin so it is unit tested without Android).
 *
 *   users/{uid}/recurring/{id}:
 *     name, amountMinor, currency, exchangeRate (String), baseAmountMinor, categoryId,
 *     frequency ("WEEKLY"/"MONTHLY"/"YEARLY"), dueDay, dueMonth, isActive, createdAt, updatedAt
 *
 * The SRS also lists lastConfirmedMonthKey / confirmationStatus; they belong to the OPTIONAL
 * confirmation workflow and are not written until that workflow exists.
 */
object RecurringMapper {

    fun fromMap(id: String, data: Map<String, Any?>): RecurringItem? {
        val name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val amountMinor = (data["amountMinor"] as? Number)?.toLong() ?: return null
        val currency = (data["currency"] as? String)?.takeIf { it.isNotBlank() } ?: Currencies.BASE
        val exchangeRate = when (val raw = data["exchangeRate"]) {
            is String -> raw.takeIf { it.isNotBlank() } ?: "1"
            is Number -> raw.toString()
            else -> "1"
        }
        val baseAmountMinor = (data["baseAmountMinor"] as? Number)?.toLong()
            ?: if (currency == Currencies.BASE) amountMinor
            else MoneyInput.parseRate(exchangeRate)?.let { MoneyInput.toBaseMinor(amountMinor, it) }
            ?: return null

        return RecurringItem(
            id = id,
            name = name,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRate = exchangeRate,
            baseAmountMinor = baseAmountMinor,
            categoryId = (data["categoryId"] as? String).orEmpty(),
            frequency = parseFrequency(data["frequency"]),
            dueDay = (data["dueDay"] as? Number)?.toInt() ?: 1,
            dueMonth = (data["dueMonth"] as? Number)?.toInt() ?: 1,
            isActive = data["isActive"] as? Boolean ?: true
        )
    }

    fun toMap(item: RecurringItem, now: Date, isNew: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "name" to item.name,
            "amountMinor" to item.amountMinor,
            "currency" to item.currency,
            "exchangeRate" to item.exchangeRate,
            "baseAmountMinor" to item.baseAmountMinor,
            "categoryId" to item.categoryId,
            "frequency" to item.frequency.name,
            "dueDay" to item.dueDay,
            "dueMonth" to item.dueMonth,
            "isActive" to item.isActive,
            "updatedAt" to now
        )
        if (isNew) map["createdAt"] = now
        return map
    }

    private fun parseFrequency(value: Any?): RecurringFrequency {
        val name = (value as? String)?.trim()?.uppercase() ?: return RecurringFrequency.MONTHLY
        return RecurringFrequency.values().firstOrNull { it.name == name } ?: RecurringFrequency.MONTHLY
    }
}
